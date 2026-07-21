$ErrorActionPreference = 'Stop'

$baseUrl = if ($env:BASE_URL) { $env:BASE_URL.TrimEnd('/') } else { 'http://localhost' }
$runId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$email = "stage02-$runId@example.com"
$username = "stage02-$runId"
$password = 'SmokePass1'

function Invoke-ExpectedApiError {
    param(
        [scriptblock]$Action,
        [int]$ExpectedCode
    )

    try {
        & $Action | Out-Null
        throw "Expected API error $ExpectedCode but request succeeded"
    }
    catch {
        $responseBody = $_.ErrorDetails.Message
        if (-not $responseBody -and $_.Exception.Response) {
            $stream = $_.Exception.Response.GetResponseStream()
            if ($stream) {
                $reader = New-Object System.IO.StreamReader($stream)
                $responseBody = $reader.ReadToEnd()
                $reader.Dispose()
            }
        }
        if (-not $responseBody) {
            throw
        }
        $errorBody = $responseBody | ConvertFrom-Json
        if ($errorBody.code -ne $ExpectedCode) {
            throw "Expected API error $ExpectedCode but received $($errorBody.code)"
        }
    }
}

$registerBody = @{
    email = $email
    username = $username
    password = $password
} | ConvertTo-Json

$registration = Invoke-RestMethod `
    -Method Post `
    -Uri "$baseUrl/api/v1/auth/register" `
    -ContentType 'application/json' `
    -Body $registerBody `
    -SessionVariable authSession

if ($registration.code -ne 0 -or $registration.data.user.role -ne 'USER') {
    throw 'Registration did not return an authenticated USER session'
}

$accessToken = $registration.data.accessToken
$authorization = @{ Authorization = "Bearer $accessToken" }
$currentUser = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/v1/auth/me" -Headers $authorization
if ($currentUser.data.email -ne $email) {
    throw 'Current user response did not match the registered account'
}

Invoke-ExpectedApiError -ExpectedCode 40301 -Action {
    Invoke-RestMethod `
        -Method Put `
        -Uri "$baseUrl/api/v1/admin/users/$($registration.data.user.id)/status" `
        -Headers $authorization `
        -ContentType 'application/json' `
        -Body '{"status":"DISABLED"}'
}

$cookieUri = [Uri]"$baseUrl/api/v1/auth/refresh"
$oldRefreshToken = ($authSession.Cookies.GetCookies($cookieUri) |
    Where-Object Name -eq 'refresh_token').Value
$rotation = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/v1/auth/refresh" -WebSession $authSession
$rotatedRefreshToken = ($authSession.Cookies.GetCookies($cookieUri) |
    Where-Object Name -eq 'refresh_token').Value
if ($rotation.code -ne 0 -or $rotation.data.accessToken -eq $accessToken -or $rotatedRefreshToken -eq $oldRefreshToken) {
    throw 'Refresh rotation did not issue a new access token'
}

$oldSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$oldCookie = New-Object System.Net.Cookie('refresh_token', $oldRefreshToken, '/api/v1/auth', $cookieUri.Host)
$oldSession.Cookies.Add($oldCookie)
Invoke-ExpectedApiError -ExpectedCode 40103 -Action {
    Invoke-RestMethod -Method Post -Uri "$baseUrl/api/v1/auth/refresh" -WebSession $oldSession
}

$currentRefreshToken = $rotatedRefreshToken
Invoke-RestMethod -Method Post -Uri "$baseUrl/api/v1/auth/logout" -WebSession $authSession | Out-Null
$revokedSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$revokedCookie = New-Object System.Net.Cookie('refresh_token', $currentRefreshToken, '/api/v1/auth', $cookieUri.Host)
$revokedSession.Cookies.Add($revokedCookie)
Invoke-ExpectedApiError -ExpectedCode 40103 -Action {
    Invoke-RestMethod -Method Post -Uri "$baseUrl/api/v1/auth/refresh" -WebSession $revokedSession
}

$wrongLoginBody = @{ email = $email; password = 'WrongPass1' } | ConvertTo-Json
for ($attempt = 1; $attempt -le 5; $attempt++) {
    Invoke-ExpectedApiError -ExpectedCode 40104 -Action {
        Invoke-RestMethod `
            -Method Post `
            -Uri "$baseUrl/api/v1/auth/login" `
            -ContentType 'application/json' `
            -Body $wrongLoginBody
    }
}

$correctLoginBody = @{ email = $email; password = $password } | ConvertTo-Json
Invoke-ExpectedApiError -ExpectedCode 40105 -Action {
    Invoke-RestMethod `
        -Method Post `
        -Uri "$baseUrl/api/v1/auth/login" `
        -ContentType 'application/json' `
        -Body $correctLoginBody
}

Write-Host "Authentication smoke test passed. user=$email requestId=$($registration.requestId)"
