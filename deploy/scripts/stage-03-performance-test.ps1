param(
    [string]$BaseUrl = "http://localhost",
    [string]$AdminEmail = "admin@example.com",
    [string]$AdminPassword = "change_me1"
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path

function Assert-True {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { throw $Message }
}

function Generate-Products {
    param([int]$Count, [string]$Output)
    & docker run --rm -v "${repoRoot}:/workspace" -w /workspace node:22-alpine `
        node tools/generate-mock-data.mjs --products $Count --days 0 --seed 20260721 `
        --profile performance --output $Output | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Performance data generation failed for $Count products" }
}

function Upload-Products {
    param([string]$Token, [string]$FilePath)
    $stopwatch = [Diagnostics.Stopwatch]::StartNew()
    $response = & curl.exe --silent --show-error --fail-with-body `
        -H "Authorization: Bearer $Token" `
        -F "file=@$FilePath;type=text/csv" `
        "$BaseUrl/api/v1/admin/imports/products"
    $stopwatch.Stop()
    if ($LASTEXITCODE -ne 0) { throw "Performance product upload failed" }
    return @{
        Response = ($response | ConvertFrom-Json)
        ElapsedSeconds = [math]::Round($stopwatch.Elapsed.TotalSeconds, 3)
    }
}

$login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/auth/login" -ContentType "application/json" -Body (@{
    email = $AdminEmail
    password = $AdminPassword
} | ConvertTo-Json -Compress)
$token = $login.data.accessToken
Assert-True (-not [string]::IsNullOrWhiteSpace($token)) "Admin login failed"

Generate-Products 10000 "tmp/import-10000"
Generate-Products 100000 "tmp/performance-data"
$tenThousandFile = Join-Path $repoRoot "tmp\import-10000\products.csv"
$hundredThousandFile = Join-Path $repoRoot "tmp\performance-data\products.csv"
Assert-True ((Get-Item $hundredThousandFile).Length -lt 20MB) "100,000-row product CSV exceeds the upload limit"

$tenThousand = Upload-Products $token $tenThousandFile
Assert-True ($tenThousand.Response.data.status -eq "SUCCESS") "10,000-row import failed"
Assert-True ($tenThousand.Response.data.totalRows -eq 10000) "10,000-row import count mismatch"

$hundredThousand = Upload-Products $token $hundredThousandFile
Assert-True ($hundredThousand.Response.data.status -eq "SUCCESS") "100,000-row import failed"
Assert-True ($hundredThousand.Response.data.totalRows -eq 100000) "100,000-row import count mismatch"

$headers = @{ Authorization = "Bearer $token" }
for ($warmup = 0; $warmup -lt 5; $warmup++) {
    Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/v1/products?page=1&pageSize=20&market=US&sortBy=collectedAt&direction=desc" -Headers $headers | Out-Null
}
$latencies = @()
for ($sample = 0; $sample -lt 30; $sample++) {
    $stopwatch = [Diagnostics.Stopwatch]::StartNew()
    $page = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/v1/products?page=1&pageSize=20&market=US&sortBy=collectedAt&direction=desc" -Headers $headers
    $stopwatch.Stop()
    Assert-True ($page.data.items.Count -eq 20) "Performance page did not return 20 rows"
    $latencies += $stopwatch.Elapsed.TotalMilliseconds
}
$sorted = $latencies | Sort-Object
$p95Index = [math]::Ceiling($sorted.Count * 0.95) - 1
$p95 = [math]::Round($sorted[$p95Index], 2)
Assert-True ($p95 -le 2000) "Product list P95 exceeded 2 seconds: $p95 ms"

$indexOutput = & docker compose exec -T mysql mysql -uroot -pchange_me -N -e `
    "SELECT index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) FROM information_schema.statistics WHERE table_schema='tiktok_insight' AND table_name IN ('product','product_daily_stat','import_job') GROUP BY table_name,index_name ORDER BY table_name,index_name;"
if ($LASTEXITCODE -ne 0) { throw "Index verification query failed" }
$indexOutput | Set-Content -LiteralPath (Join-Path $repoRoot "tmp\performance-data\index-verification.txt") -Encoding UTF8
$requiredIndexes = @(
    "idx_product_market_category_status_price",
    "idx_product_market_category_listed",
    "idx_product_status_latest_stat",
    "idx_product_daily_stat_product_date",
    "idx_product_daily_stat_date_product",
    "idx_import_job_status_created"
)
foreach ($index in $requiredIndexes) {
    Assert-True (($indexOutput -join "`n").Contains($index)) "Required index is missing: $index"
}

$explainOutput = & docker compose exec -T mysql mysql -uroot -pchange_me -e `
    "USE tiktok_insight; EXPLAIN SELECT p.id,p.title,c.name,s.name FROM product p JOIN category c ON c.id=p.category_id JOIN shop s ON s.id=p.shop_id WHERE p.market='US' AND p.status='ACTIVE' ORDER BY p.collected_at DESC,p.id DESC LIMIT 20;"
if ($LASTEXITCODE -ne 0) { throw "SQL EXPLAIN failed" }
$explainOutput | Set-Content -LiteralPath (Join-Path $repoRoot "tmp\performance-data\product-list-explain.txt") -Encoding UTF8

$backendStats = & docker stats --no-stream --format "{{.Name}} {{.MemUsage}}" tiktok-product-insight-backend-1
$result = [ordered]@{
    import10000Seconds = $tenThousand.ElapsedSeconds
    import100000Seconds = $hundredThousand.ElapsedSeconds
    productListP95Milliseconds = $p95
    backendMemory = $backendStats
}
$result | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $repoRoot "tmp\performance-data\stage-03-performance-result.json") -Encoding UTF8
Write-Host "Stage 03 performance test passed: 10,000-row streaming import, 100,000 products, P95=$p95 ms."
