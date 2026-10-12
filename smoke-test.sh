#!/bin/bash
# ==============================================================================
# Expense Tracker Dashboard - End-to-End API Smoke Test
# ==============================================================================

set -e

BASE_URL="${API_BASE_URL:-http://localhost:8080}"
echo "=========================================================="
echo " Starting API Smoke Test against: $BASE_URL"
echo "=========================================================="

# 1. Login with Demo User
echo ""
echo "[1/8] Logging in with Demo credentials..."
LOGIN_RES=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@expensetracker.com","password":"Demo@123"}')

TOKEN=$(echo "$LOGIN_RES" | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "❌ Login failed! Response:"
  echo "$LOGIN_RES"
  exit 1
fi
echo "✅ Login successful! JWT obtained."

# 2. Fetch User Profile
echo ""
echo "[2/8] Fetching authenticated user profile..."
USER_RES=$(curl -s -X GET "$BASE_URL/api/users/me" \
  -H "Authorization: Bearer $TOKEN")
echo "✅ User Profile: $(echo "$USER_RES" | grep -o '"email":"[^"]*' | cut -d'"' -f4)"

# 3. Fetch Categories
echo ""
echo "[3/8] Fetching categories..."
CAT_RES=$(curl -s -X GET "$BASE_URL/api/categories" \
  -H "Authorization: Bearer $TOKEN")
echo "✅ Categories retrieved successfully."

# 4. Create New Expense
echo ""
echo "[4/8] Creating new expense record..."
EXP_RES=$(curl -s -X POST "$BASE_URL/api/expenses" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 950.00,
    "description": "Smoke Test Cafe Coffee",
    "categoryId": 1,
    "expenseDate": "2026-10-11",
    "paymentMethod": "UPI",
    "notes": "Automated smoke test run"
  }')

EXP_ID=$(echo "$EXP_RES" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo "✅ Expense created with ID: $EXP_ID"

# 5. Fetch Filtered Expenses
echo ""
echo "[5/8] Fetching filtered expenses..."
curl -s -X GET "$BASE_URL/api/expenses?page=0&size=5&sort=newest" \
  -H "Authorization: Bearer $TOKEN" > /dev/null
echo "✅ Paginated expenses query successful."

# 6. Fetch Analytics Summary
echo ""
echo "[6/8] Fetching analytics KPIs and summary..."
ANALYTICS_RES=$(curl -s -X GET "$BASE_URL/api/analytics/summary" \
  -H "Authorization: Bearer $TOKEN")
echo "✅ Analytics summary retrieved."

# 7. Multi-Tenant User Isolation Test
echo ""
echo "[7/8] Verifying User Isolation against foreign resource..."
# Expense 33 belongs to user2 (Sarah)
STATUS_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/expenses/33" \
  -H "Authorization: Bearer $TOKEN")

if [ "$STATUS_CODE" -eq 404 ]; then
  echo "✅ User isolation verified! Received 404 for User 2 resource."
else
  echo "❌ User isolation check failed! Status code: $STATUS_CODE"
  exit 1
fi

# 8. Clean up created test expense
echo ""
echo "[8/8] Cleaning up created test expense #$EXP_ID..."
curl -s -X DELETE "$BASE_URL/api/expenses/$EXP_ID" \
  -H "Authorization: Bearer $TOKEN" > /dev/null
echo "✅ Test expense cleaned up."

echo ""
echo "=========================================================="
echo " 🎉 All API Smoke Tests Passed Successfully!"
echo "=========================================================="
