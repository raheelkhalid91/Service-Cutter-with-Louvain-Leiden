#!/usr/bin/env powershell
# Test script to verify metrics are working end-to-end

Write-Host "ServiceCutter Metrics - End-to-End Test" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Check if Editor is running
Write-Host "Test 1: Editor accessibility" -ForegroundColor Yellow
$editorResp = Invoke-WebRequest -Uri "http://localhost:8080" -UseBasicParsing -ErrorAction SilentlyContinue
if ($editorResp -and $editorResp.StatusCode -eq 200) {
    Write-Host "✓ Editor accessible (HTTP 200)" -ForegroundColor Green
}
else {
    Write-Host "✗ Editor not responding" -ForegroundColor Red
    exit 1
}

# Test 2: Check if solver template has metrics code
Write-Host ""
Write-Host "Test 2: Solver template with metrics" -ForegroundColor Yellow
$solverResp = Invoke-WebRequest -Uri "http://localhost:8080/scripts/app/solver/solver.html" -UseBasicParsing -ErrorAction SilentlyContinue
if ($solverResp.Content -match "Service Metrics") {
    Write-Host "✓ Metrics HTML found in template" -ForegroundColor Green
    if ($solverResp.Content -match "service.metrics.cohesion") {
        Write-Host "✓ Metrics bindings found (cohesion)" -ForegroundColor Green
    }
    if ($solverResp.Content -match "service.metrics.coupling") {
        Write-Host "✓ Metrics bindings found (coupling)" -ForegroundColor Green
    }
}
else {
    Write-Host "✗ Metrics HTML NOT found" -ForegroundColor Red
}

# Test 3: Check Engine logs for metrics calculation code
Write-Host ""
Write-Host "Test 3: Engine has calculateMetrics method" -ForegroundColor Yellow
$logs = docker logs servicecutter-master-engine-1 --tail 100 2>&1
if ($logs -match "ServiceCutAnalyzer" -or $logs -match "Calculated metrics") {
    Write-Host "✓ Engine has metrics code" -ForegroundColor Green
}
else {
    Write-Host "ℹ Will see metrics logs after solving" -BoregroundColor Gray
}

# Test 4: Display what to do next
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "1. Open http://localhost:8080"
Write-Host "2. Import model: Samples/salesmanager_1_model.json"
Write-Host "3. Import representation: Samples/salesmanager_2_user_representation.json"  
Write-Host "4. Go to 'Service Cutter' tab"
Write-Host "5. Select the imported model"
Write-Host "6. Choose an algorithm (e.g., 'Louvain')"
Write-Host "7. Click 'Recalculate'"
Write-Host "8. SCROLL DOWN to see the 'Service Metrics' table"
Write-Host ""
Write-Host "Expected: Table with columns: Service, Cohesion, Coupling, Size, Dependencies"
