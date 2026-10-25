param(
    [int]$Iterations = 200,
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Usuario = "cliente",
    [string]$Clave = "Clave123!",
    [double]$Monto = 1500.00
)

$ErrorActionPreference = "Stop"

function Get-Token {
    $body = @{ username = $Usuario; password = $Clave } | ConvertTo-Json
    $resp = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/auth/token" `
        -ContentType "application/json" -Body $body
    return $resp.accessToken
}

function Get-Percentil($sorted, [double]$p) {
    $index = [math]::Ceiling($sorted.Count * $p) - 1
    $index = [math]::Max(0, [math]::Min($index, $sorted.Count - 1))
    return $sorted[$index]
}

$token = Get-Token
$headersBase = @{ Authorization = "Bearer $token" }
$times = New-Object System.Collections.Generic.List[double]

Write-Host "Calentamiento (50 solicitudes)..."
for ($i = 0; $i -lt 50; $i++) {
    $body = @{
        idTransaccion = "WARM-$([guid]::NewGuid())"
        cuentaOrigen  = "001-1234567890"
        cuentaDestino = "002-0987654321"
        monto         = $Monto
        moneda        = "PEN"
        descripcion   = "calentamiento"
    } | ConvertTo-Json

    Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/transacciones" `
        -Headers ($headersBase + @{ "Idempotency-Key" = [guid]::NewGuid().ToString() }) `
        -ContentType "application/json" -Body $body | Out-Null
}

Write-Host "Midiendo $Iterations solicitudes..."
for ($i = 1; $i -le $Iterations; $i++) {
    $body = @{
        idTransaccion = "TX-$([guid]::NewGuid())"
        cuentaOrigen  = "001-1234567890"
        cuentaDestino = "002-0987654321"
        monto         = $Monto
        moneda        = "PEN"
        descripcion   = "prueba de latencia"
    } | ConvertTo-Json

    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/transacciones" `
        -Headers ($headersBase + @{ "Idempotency-Key" = [guid]::NewGuid().ToString() }) `
        -ContentType "application/json" -Body $body | Out-Null
    $sw.Stop()

    $times.Add($sw.Elapsed.TotalMilliseconds)

    if ($i % 25 -eq 0) {
        Write-Host "  $i/$Iterations..."
    }
}

$sorted = $times | Sort-Object
$avg = ($times | Measure-Object -Average).Average
$min = ($times | Measure-Object -Minimum).Minimum
$max = ($times | Measure-Object -Maximum).Maximum
$p50 = Get-Percentil $sorted 0.50
$p95 = Get-Percentil $sorted 0.95
$p99 = Get-Percentil $sorted 0.99

$resultado = [pscustomobject]@{
    Iteraciones = $times.Count
    PromedioMs  = [math]::Round($avg, 2)
    MinMs       = [math]::Round($min, 2)
    MaxMs       = [math]::Round($max, 2)
    P50Ms       = [math]::Round($p50, 2)
    P95Ms       = [math]::Round($p95, 2)
    P99Ms       = [math]::Round($p99, 2)
    Fecha       = (Get-Date).ToString("o")
    BaseUrl     = $BaseUrl
}

$resultado | Format-List

$outDir = Join-Path $PSScriptRoot "..\docs\evidencias\latencia"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$resultado | ConvertTo-Json | Set-Content -Path (Join-Path $outDir "resultado.json") -Encoding UTF8
$times | ForEach-Object { [pscustomobject]@{ ms = $_ } } |
    Export-Csv -Path (Join-Path $outDir "tiempos.csv") -NoTypeInformation -Encoding UTF8

Write-Host "Evidencias guardadas en $outDir"
