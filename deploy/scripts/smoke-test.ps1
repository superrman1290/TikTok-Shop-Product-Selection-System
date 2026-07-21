$ErrorActionPreference = 'Stop'

$baseUrl = if ($env:BASE_URL) { $env:BASE_URL.TrimEnd('/') } else { 'http://localhost' }
$response = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/v1/health"

if ($response.code -ne 0) {
    throw "Health endpoint returned application code $($response.code)"
}

foreach ($component in 'application', 'database', 'redis', 'storage') {
    if ($response.data.$component -ne 'UP') {
        throw "$component is $($response.data.$component)"
    }
}

Write-Host "Health check passed. requestId=$($response.requestId)"
