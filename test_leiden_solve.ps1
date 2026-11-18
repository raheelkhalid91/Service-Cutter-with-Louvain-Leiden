# Test Louvain/Leiden solve directly against Engine

$engineBase = "http://localhost:8090/engine"

# 1. Import JPetStore model
$model = Get-Content 'Samples/jpetstore_1_model.json' | ConvertFrom-Json
$import_req = @{ entities = $model.entities; name = $model.name } | ConvertTo-Json -Depth 10

Write-Output '=== Step 1: Importing JPetStore Model (Engine) ==='
$import_resp = Invoke-WebRequest -Uri "$engineBase/import" -Method POST -ContentType 'application/json' -Body $import_req -UseBasicParsing
$import_data = $import_resp.Content | ConvertFrom-Json
$system_id = $import_data.id
if (-not $system_id) {
    throw "Import failed: system id not returned."
}
Write-Output "✅ System imported, ID: $system_id"
Write-Output ""

# 2. Import user representations
$repr = Get-Content 'Samples/jpetstore_2_user_representation.json' | ConvertFrom-Json
$repr_req = @{
    nanoentities = $repr.nanoentities
    useCases = $repr.useCases
    relatedGroups = $repr.relatedGroups
} | ConvertTo-Json -Depth 10

Write-Output '=== Step 2: Importing User Representations ==='
$repr_resp = Invoke-WebRequest -Uri "$engineBase/import/$system_id/userrepresentations/" -Method POST -ContentType 'application/json' -Body $repr_req -UseBasicParsing
Write-Output "✅ User representations imported"
Write-Output ""

# 3. Run Louvain and Leiden solvers
$priorities = @{
    "Semantic Proximity" = 5
    "Cohesive Groups" = 5
    "Exclusive Groups" = 3
    "Shared Owner" = 2
    "Separated by Constraint" = 1
    "Related Group" = 3
}

$algorithms = @("Louvain", "Leiden")
foreach ($alg in $algorithms) {
    $solver_config = @{
        algorithm = $alg
        algorithmParams = @{
            resolution = 1.0
        }
        priorities = $priorities
    } | ConvertTo-Json -Depth 10

    Write-Output "=== Step 3: Running $alg Solver ==="
    Write-Output "Solver Config: $solver_config"
    Write-Output ""

    $solve_resp = Invoke-WebRequest -Uri "$engineBase/solver/$system_id" -Method POST -ContentType 'application/json' -Body $solver_config -UseBasicParsing
    $solve_data = $solve_resp.Content | ConvertFrom-Json

    Write-Output "✅ $alg solve completed"
    Write-Output "Number of services: $($solve_data.services.Length)"
    Write-Output ""

    foreach ($svc in $solve_data.services) {
        Write-Output "Service $($svc.name):"
        Write-Output "  Nanoentities: $($svc.nanoentities -join ', ')"
    }
    Write-Output ""
}
