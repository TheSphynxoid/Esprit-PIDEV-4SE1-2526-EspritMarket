# Local microservices stack (no docker compose required, only the shared PostgreSQL)
# Usage:
#   powershell -File local-stack.ps1 start    -> starts eureka + 5 services + gateway
#   powershell -File local-stack.ps1 stop     -> stops everything
#   powershell -File local-stack.ps1 status   -> prints process state + health endpoints
#
# Prerequisites:
#   - a PostgreSQL reachable at $env:SPRING_DATASOURCE_URL (defaults below use localhost:55432,
#     e.g. a container seeded with devops/db/init/01-espritmarket-base-schema.sql)
#   - jars built: ./mvnw package -DskipTests   (from backend/)
param(
    [Parameter(Position = 0)]
    [string]$Action = 'status'
)

$ErrorActionPreference = 'SilentlyContinue'

$RepoRoot = Split-Path -Parent $PSScriptRoot
$Backend  = Join-Path $RepoRoot 'backend'
$PidFile  = Join-Path $env:TEMP 'esprit-local-stack.pids'

$JwtSecret = 'Y291clN1cGVyU2VjcmV0S2V5Rm9ySldUVG9rZW5HZW5lcmF0aW9uQW5kVmFsaWRhdGlvbkluU3ByaW5nQm9vdEFwcGxpY2F0aW9u'

$Services = @(
    @{ Name = 'eureka';        Jar = 'eureka-server\target\eureka-server-0.2.0.jar';              Port = '8761';  Delay = 20 },
    @{ Name = 'auth';          Jar = 'esprit-auth\target\esprit-auth-0.2.0.jar';                  Port = '18081'; Delay = 30 },
    @{ Name = 'marketplace';   Jar = 'esprit-marketplace\target\esprit-marketplace-0.2.0.jar';    Port = '8082';  Delay = 0 },
    @{ Name = 'srv';           Jar = 'esprit-srv\target\esprit-srv-0.2.0.jar';                    Port = '8083';  Delay = 0 },
    @{ Name = 'eventplanning'; Jar = 'esprit-eventplanning\target\esprit-eventplanning-0.2.0.jar';Port = '8084';  Delay = 0 },
    @{ Name = 'partnership';   Jar = 'esprit-partnership\target\esprit-partnership-0.2.0.jar';    Port = '8085';  Delay = 45 },
    @{ Name = 'gateway';       Jar = 'esprit-gateway\target\esprit-gateway-0.2.0.jar';            Port = '8088';  Delay = 15 }
)

$env:JWT_SECRET = $JwtSecret
$env:GATEWAY_SHARED_TOKEN = 'local-dev-shared-token'
if (-not $env:SPRING_DATASOURCE_URL) {
    $env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:55432/esprit_market'
    $env:SPRING_DATASOURCE_USERNAME = 'postgres'
    $env:SPRING_DATASOURCE_PASSWORD = 'postgres'
}

function Stop-Stack {
    if (Test-Path $PidFile) {
        Get-Content $PidFile | ForEach-Object {
            $proc = Get-Process -Id $_ -ErrorAction SilentlyContinue
            if ($proc -and $proc.ProcessName -eq 'java') {
                Stop-Process -Id $_ -Force
                Write-Output "stopped java pid $_"
            }
        }
        Remove-Item $PidFile -Force
    } else {
        Write-Output 'no pid file - nothing tracked to stop'
    }
}

function Get-Status {
    $alive = 0
    if (Test-Path $PidFile) {
        foreach ($line in (Get-Content $PidFile)) {
            $parts = $line -split '\|'
            $proc = Get-Process -Id $parts[0] -ErrorAction SilentlyContinue
            $state = if ($proc -and $proc.ProcessName -eq 'java') { 'RUNNING' } else { 'STOPPED ' }
            if ($state -eq 'RUNNING') { $alive++ }
            $health = 'n/a'
            try {
                $h = Invoke-RestMethod -Uri "http://localhost:$($parts[2])/actuator/health" -TimeoutSec 3
                $health = $h.status
            } catch { $health = 'DOWN' }
            Write-Output ("{0,-14} {1}  port={2,-5} health={3}" -f $parts[1], $state, $parts[2], $health)
        }
    }
    if ($alive -eq 0) { Write-Output 'stack not running (no live java processes tracked)' }
}

function Start-Stack {
    $pids = @()
    foreach ($svc in $Services) {
        $jarPath = Join-Path $Backend $svc.Jar
        if (-not (Test-Path $jarPath)) {
            Write-Output "MISSING JAR: $jarPath - run 'mvnw package -DskipTests' in backend/ first"
            Stop-Stack
            return
        }
        $env:SERVER_PORT = $svc.Port
        $log = Join-Path $env:TEMP "local-$($svc.Name).log"
        $err = Join-Path $env:TEMP "local-$($svc.Name)-err.log"
        $p = Start-Process -FilePath 'java' -ArgumentList '-jar', $jarPath -PassThru -NoNewWindow -RedirectStandardOutput $log -RedirectStandardError $err
        $pids += "$($p.Id)|$($svc.Name)|$($svc.Port)"
        Write-Output ("started {0,-14} pid={1} port={2}" -f $svc.Name, $p.Id, $svc.Port)
        if ($svc.Delay -gt 0) { Start-Sleep -Seconds $svc.Delay }
    }
    Set-Content -Path $PidFile -Value $pids
    Write-Output ''
    Write-Output 'waiting for the gateway to come up...'
    Start-Sleep -Seconds 25
    Get-Status
    Write-Output ''
    Write-Output 'gateway:        http://localhost:8088'
    Write-Output 'eureka console: http://localhost:8761'
    Write-Output 'logs:           %TEMP%\local-<service>.log'
}

switch ($Action.ToLower()) {
    'start'  { Start-Stack }
    'stop'   { Stop-Stack }
    'status' { Get-Status }
    default  { Write-Output 'usage: local-stack.ps1 [start|stop|status]' }
}
