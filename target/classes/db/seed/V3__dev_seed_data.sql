-- V3__dev_seed_data.sql
-- Development & testing seed dataset: Demo accounts, test transactions, and initial budgets.
-- This script is strictly excluded from the production Flyway migration path.

-- 1. Demo Users
INSERT IGNORE INTO users (id, name, email, password_hash, currency, theme, default_date_range, budget_notifications_enabled, active) VALUES
(1, 'Demo User', 'demo@expensetracker.com', '$2a$10$J4fjoLm1TILP/uQbPrbOP.hkWi1dEjfFFtHRCDWskdNXFseW9rldG', 'INR', 'light', 'THIS_MONTH', TRUE, TRUE),
(2, 'Sarah Jenkins', 'user2@expensetracker.com', '$2a$10$.TOSQCcnEoglWLKkQ4mSCe8iF9/ZQ7ciH16vO/KrIbyKIya2c7FoW', 'INR', 'dark', 'THIS_MONTH', TRUE, TRUE);

-- 2. Realistic Expenses for Demo User (id = 1) across August, September, October 2026
INSERT IGNORE INTO expenses (id, user_id, amount, description, category_id, expense_date, payment_method, notes) VALUES
-- August 2026
(1, 1, 15000.00, 'August Apartment Rent', 10, '2026-08-01', 'BANK_TRANSFER', 'Paid via NEFT to landlord'),
(2, 1, 2450.00, 'Monthly Grocery Run', 9, '2026-08-03', 'UPI', 'Purchased at Nature Basket'),
(3, 1, 450.00, 'Uber Ride to Tech Park', 2, '2026-08-04', 'UPI', 'Morning commute during rain'),
(4, 1, 850.00, 'Team Lunch at Barbeque', 1, '2026-08-08', 'CREDIT_CARD', 'Project kickoff lunch'),
(5, 1, 1299.00, 'Electricity Bill August', 5, '2026-08-10', 'DEBIT_CARD', 'Bescom online portal'),
(6, 1, 649.00, 'Netflix Premium Plan', 11, '2026-08-15', 'CREDIT_CARD', 'Auto-debit monthly subscription'),
(7, 1, 3200.00, 'Zara Casual Shirts & Jeans', 3, '2026-08-18', 'CREDIT_CARD', 'Weekend shopping sale'),
(8, 1, 1200.00, 'Dental Checkup & Cleaning', 6, '2026-08-22', 'UPI', 'Apollo clinic appointment'),
(9, 1, 1800.00, 'Python & Cloud Architecture Course', 7, '2026-08-25', 'CREDIT_CARD', 'Udemy certification purchase'),
(10, 1, 750.00, 'Dinner with Friends at Cafe Bistro', 1, '2026-08-29', 'CASH', 'Split bill with college mates'),

-- September 2026
(11, 1, 15000.00, 'September Apartment Rent', 10, '2026-09-01', 'BANK_TRANSFER', 'Monthly rent transfer'),
(12, 1, 3120.00, 'Supermarket Stock-up', 9, '2026-09-02', 'UPI', 'BigBasket weekly groceries'),
(13, 1, 520.00, 'Metro Card Recharge', 2, '2026-09-05', 'UPI', 'Bangalore Metro Smart Card recharge'),
(14, 1, 1450.00, 'Broadband & Fiber Internet Bill', 5, '2026-09-07', 'CREDIT_CARD', 'Airtel Xstream Fiber 200Mbps'),
(15, 1, 649.00, 'Netflix Subscription', 11, '2026-09-15', 'CREDIT_CARD', 'Monthly recurring fee'),
(16, 1, 890.00, 'Dinner at Truffles', 1, '2026-09-18', 'UPI', 'Burger and shake dinner'),
(17, 1, 2800.00, 'Running Shoes from Nike', 3, '2026-09-20', 'DEBIT_CARD', 'Sports equipment upgrade'),
(18, 1, 650.00, 'Cinema Tickets & Popcorn', 4, '2026-09-22', 'CREDIT_CARD', 'IMAX evening movie show'),
(19, 1, 1400.00, 'Prescription Medicines', 6, '2026-09-24', 'UPI', '1mg pharmacy delivery'),
(20, 1, 8500.00, 'Weekend Goa Resort Booking', 8, '2026-09-27', 'CREDIT_CARD', 'Advance stay booking'),
(21, 1, 1850.00, 'Organic Groceries & Fruits', 9, '2026-09-29', 'UPI', 'Farmers market fruits and vegetables'),

-- October 2026 (Current Month)
(22, 1, 15000.00, 'October Apartment Rent', 10, '2026-10-01', 'BANK_TRANSFER', 'First of the month rent transfer'),
(23, 1, 3450.00, 'Monthly Essentials & Groceries', 9, '2026-10-02', 'UPI', 'Blinkit monthly pantry essentials'),
(24, 1, 780.00, 'Cab to Airport & Toll', 2, '2026-10-04', 'UPI', 'Uber Premier to terminal'),
(25, 1, 1380.00, 'Electricity & Water Utility Bill', 5, '2026-10-05', 'DEBIT_CARD', 'State utility combined bill payment'),
(26, 1, 649.00, 'Netflix Monthly Subscription', 11, '2026-10-06', 'CREDIT_CARD', 'Streaming entertainment auto-renew'),
(27, 1, 299.00, 'Spotify Premium Family Plan', 11, '2026-10-07', 'DEBIT_CARD', 'Monthly music stream'),
(28, 1, 1150.00, 'Weekend Brunch with Colleagues', 1, '2026-10-08', 'UPI', 'Artisan cafe breakfast buffet'),
(29, 1, 2200.00, 'Wireless Noise Cancelling Earbuds', 3, '2026-10-09', 'CREDIT_CARD', 'Electronics accessories discount sale'),
(30, 1, 850.00, 'Pharmacy & Vitamin Supplements', 6, '2026-10-10', 'UPI', 'Multivitamins and first-aid replenishment'),
(31, 1, 620.00, 'Weekend Movie & Snack combo', 4, '2026-10-10', 'UPI', 'PVR Cinemas Friday ticket'),
(32, 1, 450.00, 'Auto Rickshaw & Metro Commute', 2, '2026-10-11', 'CASH', 'Local city transit fare');

-- Expenses for User 2 (for user isolation testing)
INSERT IGNORE INTO expenses (id, user_id, amount, description, category_id, expense_date, payment_method, notes) VALUES
(33, 2, 22000.00, 'Sarah Luxury Studio Rent', 10, '2026-10-01', 'BANK_TRANSFER', 'Indiranagar studio apartment'),
(34, 2, 4500.00, 'Organic Health Food Market', 9, '2026-10-03', 'CREDIT_CARD', 'Gluten-free items and cold pressed oils'),
(35, 2, 1200.00, 'Yoga Studio Membership Class', 6, '2026-10-05', 'UPI', 'Weekend morning vinyasa flow');

-- 3. Budgets for Demo User (October 2026)
INSERT IGNORE INTO monthly_budgets (id, user_id, `month`, `year`, total_budget, warning_threshold) VALUES
(1, 1, 10, 2026, 40000.00, 80),
(2, 2, 10, 2026, 50000.00, 85);

-- Category budgets for User 1
INSERT IGNORE INTO budget_categories (id, monthly_budget_id, category_id, amount) VALUES
(1, 1, 10, 15000.00),
(2, 1, 9, 6000.00),
(3, 1, 1, 4000.00),
(4, 1, 2, 3000.00),
(5, 1, 5, 2500.00),
(6, 1, 3, 5000.00),
(7, 1, 11, 1500.00),
(8, 1, 4, 2000.00),
(9, 1, 6, 1000.00);

-- 4. Seed Notifications for Demo User
INSERT IGNORE INTO notifications (id, user_id, title, message, type, is_read) VALUES
(1, 1, 'Welcome to Expense Tracker', 'Your financial dashboard is ready. Start tracking your daily expenses, setting monthly category budgets, and monitoring cash flow.', 'GENERAL', TRUE),
(2, 1, 'Monthly Budget Set', 'Your budget for October 2026 has been set to ₹40,000 with an 80% warning threshold.', 'SYSTEM', FALSE),
(3, 1, 'Rent Payment Recorded', 'Your Rent payment of ₹15,000 for October was logged successfully.', 'GENERAL', FALSE);
