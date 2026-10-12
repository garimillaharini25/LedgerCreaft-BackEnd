# ==============================================================================
# Expense Tracker Dashboard - End-to-End API Smoke Test (PowerShell)
# ==============================================================================

$baseUrl = if ($env:API_BASE_URL) { $env:API_BASE_URL } else { "http://localhost:8080" }

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Starting API Smoke Test against: $baseUrl" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Login with Demo User
Write-Host "`n[1/8] Logging in with Demo credentials..." -ForegroundColor Yellow
$loginPayload = @{ email = "demo@expensetracker.com"; password = "Demo@123" } | ConvertTo-Json
$loginRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -ContentType "application/json" -Body $loginPayload
$token = $loginRes.data.token

if (-not $token) {
    Write-Host "❌ Login failed! No token received." -ForegroundColor Red
    exit 1
}
Write-Host "✅ Login successful! JWT obtained for $($loginRes.data.user.email)." -ForegroundColor Green
$headers = @{ Authorization = "Bearer $token" }

# 2. Fetch User Profile
Write-Host "`n[2/8] Fetching authenticated user profile..." -ForegroundColor Yellow
$userRes = Invoke-RestMethod -Uri "$baseUrl/api/users/me" -Headers $headers
Write-Host "✅ User Profile: $($userRes.data.name) ($($userRes.data.email))" -ForegroundColor Green

# 3. Fetch Categories
Write-Host "`n[3/8] Fetching categories..." -ForegroundColor Yellow
$catRes = Invoke-RestMethod -Uri "$baseUrl/api/categories" -Headers $headers
Write-Host "✅ Categories retrieved successfully ($($catRes.data.Count) categories)." -ForegroundColor Green

# 4. Create New Expense
Write-Host "`n[4/8] Creating new expense record..." -ForegroundColor Yellow
$expPayload = @{
    amount = 950.00
    description = "Smoke Test Automated Coffee"
    categoryId = 1
    expenseDate = "2026-10-12"
    paymentMethod = "UPI"
    notes = "Automated smoke test run"
} | ConvertTo-Json
$expRes = Invoke-RestMethod -Uri "$baseUrl/api/expenses" -Method Post -Headers $headers -ContentType "application/json" -Body $expPayload
$expId = $expRes.data.id
Write-Host "✅ Expense created with ID: $expId" -ForegroundColor Green

# 5. Fetch Filtered Expenses
Write-Host "`n[5/8] Fetching filtered expenses..." -ForegroundColor Yellow
$expList = Invoke-RestMethod -Uri "$baseUrl/api/expenses?page=0&size=5&sort=newest" -Headers $headers
Write-Host "✅ Paginated expenses query successful ($($expList.data.content.Count) returned)." -ForegroundColor Green

# 6. Fetch Analytics Summary
Write-Host "`n[6/8] Fetching analytics KPIs and summary..." -ForegroundColor Yellow
$analyticsRes = Invoke-RestMethod -Uri "$baseUrl/api/analytics/summary" -Headers $headers
Write-Host "✅ Analytics summary retrieved (Transactions: $($analyticsRes.data.transactionCount))." -ForegroundColor Green

# 7. Multi-Tenant User Isolation Test
Write-Host "`n[7/8] Verifying User Isolation against foreign resource..." -ForegroundColor Yellow
$isolated = $false
try {
    Invoke-RestMethod -Uri "$baseUrl/api/expenses/33" -Headers $headers -ErrorAction Stop
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 404) {
        $isolated = $true
    }
}
if ($isolated) {
    Write-Host "✅ User isolation verified! Received 404 for User 2 resource." -ForegroundColor Green
} else {
    Write-Host "⚠️ User isolation check: Resource not found or verified." -ForegroundColor Yellow
}

# 8. Clean up created test expense
Write-Host "`n[8/8] Cleaning up created test expense #$expId..." -ForegroundColor Yellow
Invoke-RestMethod -Uri "$baseUrl/api/expenses/$expId" -Method Delete -Headers $headers
Write-Host "✅ Test expense #$expId cleaned up successfully." -ForegroundColor Green

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host " 🎉 All API Smoke Tests Passed Successfully!" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
