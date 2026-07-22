param(
    [string]$BaseUrl = "http://localhost",
    [string]$AdminEmail = "admin@example.com",
    [string]$AdminPassword = "change_me1"
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$outputPath = Join-Path $repoRoot "tmp\stage-03-functional"

function Assert-True {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { throw $Message }
}

function Invoke-JsonApi {
    param(
        [string]$Method,
        [string]$Path,
        [string]$Token,
        [object]$Body
    )
    $parameters = @{
        Method = $Method
        Uri = "$BaseUrl$Path"
        Headers = @{ Authorization = "Bearer $Token" }
    }
    if ($null -ne $Body) {
        $parameters.ContentType = "application/json"
        $parameters.Body = ($Body | ConvertTo-Json -Depth 8 -Compress)
    }
    return Invoke-RestMethod @parameters
}

function Upload-Csv {
    param([string]$Path, [string]$Token, [string]$FilePath)
    $response = & curl.exe --silent --show-error --fail-with-body `
        -H "Authorization: Bearer $Token" `
        -F "file=@$FilePath;type=text/csv" `
        "$BaseUrl$Path"
    if ($LASTEXITCODE -ne 0) { throw "CSV upload failed: $Path" }
    return $response | ConvertFrom-Json
}

$login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/auth/login" -ContentType "application/json" -Body (@{
    email = $AdminEmail
    password = $AdminPassword
} | ConvertTo-Json -Compress)
Assert-True ($login.code -eq 0 -and -not [string]::IsNullOrWhiteSpace($login.data.accessToken)) "Admin login failed"
$adminToken = $login.data.accessToken

& docker run --rm -v "${repoRoot}:/workspace" -w /workspace node:22-alpine `
    node tools/generate-mock-data.mjs --products 120 --days 30 --seed 20260721 `
    --profile functional --output tmp/stage-03-functional | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Functional mock data generation failed" }

$productFile = Join-Path $outputPath "products.csv"
$statFile = Join-Path $outputPath "product-stats.csv"
$productImport = Upload-Csv "/api/v1/admin/imports/products" $adminToken $productFile
Assert-True ($productImport.code -eq 0 -and $productImport.data.status -eq "SUCCESS") "Product import did not succeed"
Assert-True ($productImport.data.totalRows -eq 120 -and $productImport.data.failedRows -eq 0) "Unexpected product import counts"

$firstList = Invoke-JsonApi Get "/api/v1/products?page=1&pageSize=20" $adminToken $null
$repeatImport = Upload-Csv "/api/v1/admin/imports/products" $adminToken $productFile
$secondList = Invoke-JsonApi Get "/api/v1/products?page=1&pageSize=20" $adminToken $null
Assert-True ($repeatImport.data.status -eq "SUCCESS") "Idempotent product re-import failed"
Assert-True ($firstList.data.total -eq $secondList.data.total) "Product re-import created duplicates"
Assert-True ($secondList.data.total -ge 120) "Expected imported products are missing"

$statImport = Upload-Csv "/api/v1/admin/imports/product-stats" $adminToken $statFile
Assert-True ($statImport.code -eq 0 -and $statImport.data.status -eq "SUCCESS") "Product statistics import did not succeed"
Assert-True ($statImport.data.totalRows -eq 3600 -and $statImport.data.failedRows -eq 0) "Unexpected statistics import counts"

$usProducts = Invoke-JsonApi Get "/api/v1/products?page=1&pageSize=20&market=US&sortBy=rating&direction=desc" $adminToken $null
Assert-True ($usProducts.data.items.Count -gt 0) "US product ranking is empty"
$functionalProducts = Invoke-JsonApi Get "/api/v1/products?page=1&pageSize=20&market=US&keyword=Mock%20NEW%20Product%201" $adminToken $null
$functionalProduct = $functionalProducts.data.items | Where-Object { $_.title -eq "Mock NEW Product 1" } | Select-Object -First 1
Assert-True ($null -ne $functionalProduct) "Deterministic functional product is missing"
$productId = $functionalProduct.id
$detail = Invoke-JsonApi Get "/api/v1/products/$productId" $adminToken $null
$stats = Invoke-JsonApi Get "/api/v1/products/$productId/stats" $adminToken $null
Assert-True ($detail.data.market -eq "US") "Product detail market mismatch"
Assert-True ($stats.data.Count -eq 30) "Product trend did not return 30 daily rows"

$invalidFile = Join-Path $outputPath "invalid-products.csv"
$header = [IO.File]::ReadLines($productFile) | Select-Object -First 1
$badRow = "invalid-1,TIKTOK_SHOP,US,Invalid Currency,c-1,Category 1,s-1,Shop 1,GBP,10.00,12.00,,,,0,,2026-07-21T08:00:00Z"
[IO.File]::WriteAllText($invalidFile, "$header`n$badRow`n", [Text.UTF8Encoding]::new($false))
$partialImport = Upload-Csv "/api/v1/admin/imports/products" $adminToken $invalidFile
Assert-True ($partialImport.data.status -eq "PARTIAL_SUCCESS" -and $partialImport.data.failedRows -eq 1) "Invalid row was not recorded"
$errors = Invoke-JsonApi Get "/api/v1/admin/imports/$($partialImport.data.id)/errors?page=1&pageSize=20" $adminToken $null
Assert-True ($errors.data.items[0].rowNumber -eq 2 -and $errors.data.items[0].fieldName -eq "currency") "Import error details are incorrect"

$unique = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$userEmail = "stage03-$unique@example.com"
$userSession = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/auth/register" -ContentType "application/json" -Body (@{
    email = $userEmail
    username = "stage03user$unique"
    password = "Password1"
} | ConvertTo-Json -Compress)
$deniedBody = Join-Path $outputPath "denied-response.json"
$statusCode = & curl.exe --silent --output $deniedBody --write-out "%{http_code}" `
    -H "Authorization: Bearer $($userSession.data.accessToken)" `
    -F "file=@$productFile;type=text/csv" `
    "$BaseUrl/api/v1/admin/imports/products"
Assert-True ($statusCode -eq "403") "Normal user unexpectedly accessed the import API"
$denied = Get-Content -LiteralPath $deniedBody -Raw | ConvertFrom-Json
Assert-True ($denied.code -eq 40301) "Import permission error code is incorrect"

Write-Host "Stage 03 smoke test passed: product/stat imports, idempotency, errors, ranking, detail, trends, and permissions."
