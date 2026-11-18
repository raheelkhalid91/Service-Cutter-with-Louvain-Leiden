#!/usr/bin/env powershell
# Diagnostic script to check ServiceCutter metrics implementation

Write-Host "ServiceCutter Metrics Diagnostic" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "1. Checking Docker containers..." -ForegroundColor Yellow
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

Write-Host ""
Write-Host "2. Testing Engine API connectivity..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8090/engine/health" -ErrorAction Stop
    if ($response.StatusCode -eq 200) {
        Write-Host "   OK: Engine is healthy" -ForegroundColor Green
    }
}
catch {
    Write-Host "   ERROR: Engine health check failed" -ForegroundColor Red
}

Write-Host ""
Write-Host "3. Checking Editor UI..." -ForegroundColor Yellow
try {
    $editorResp = Invoke-WebRequest -Uri "http://localhost:8080" -ErrorAction Stop
    if ($editorResp.StatusCode -eq 200) {
        Write-Host "   OK: Editor is accessible" -ForegroundColor Green
        if ($editorResp.Content -match "metrics|Metrics") {
            Write-Host "   OK: Metrics code found in HTML" -ForegroundColor Green
        } else {
            Write-Host "   ERROR: Metrics code NOT found in HTML" -ForegroundColor Red
        }
    }
}
catch {
    Write-Host "   ERROR: Editor is not responding" -ForegroundColor Red
}

Write-Host ""
Write-Host "4. Checking Engine logs for metrics calculation..." -ForegroundColor Yellow
$logs = docker logs servicecutter-master-engine-1 --tail 50 2>&1
if ($logs -match "Calculated metrics for") {
    Write-Host "   OK: Metrics calculation code is executing" -ForegroundColor Green
    $logs | Select-String "Calculated metrics for"
} else {
    Write-Host "   INFO: No metrics calculation logs found (metrics only show after solving)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "1. Open http://localhost:8080 in your browser"
Write-Host "2. Import your model and representation files" 
Write-Host "3. Go to 'Service Cutter' page"
Write-Host "4. Select a model and algorithm"
Write-Host "5. Click 'Recalculate'"
Write-Host "6. Scroll down to see the 'Service Metrics' table"
Write-Host ""
Write-Host "Monitor logs with: docker logs -f servicecutter-master-engine-1"
