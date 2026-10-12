-- V2__system_categories.sql
-- Production baseline migration: Default system categories only.
-- Custom user data, test accounts and demo expenses are isolated in dev/test seed scripts.

INSERT INTO categories (id, name, description, icon, color, user_id, system_category) VALUES
(1, 'Food', 'Restaurants, cafes, takeouts and dining out', 'utensils', '#0d5f47', NULL, TRUE),
(2, 'Transportation', 'Cabs, fuel, metro, buses and commute', 'car', '#0284c7', NULL, TRUE),
(3, 'Shopping', 'Clothing, electronics, personal care & gadgets', 'shopping-bag', '#d97706', NULL, TRUE),
(4, 'Entertainment', 'Movies, concerts, gaming and events', 'film', '#9333ea', NULL, TRUE),
(5, 'Bills', 'Electricity, water, gas, internet and utility bills', 'file-text', '#dc2626', NULL, TRUE),
(6, 'Healthcare', 'Doctor visits, medicines, lab tests and health insurance', 'activity', '#16a34a', NULL, TRUE),
(7, 'Education', 'Courses, books, workshops and tuition fees', 'book-open', '#2563eb', NULL, TRUE),
(8, 'Travel', 'Flights, hotels, vacations and road trips', 'plane', '#4f46e5', NULL, TRUE),
(9, 'Groceries', 'Supermarket, vegetables, daily essentials & household goods', 'shopping-cart', '#65a30d', NULL, TRUE),
(10, 'Rent', 'Monthly apartment rent and maintenance fee', 'home', '#0f766e', NULL, TRUE),
(11, 'Subscriptions', 'Netflix, Spotify, Prime, YouTube Premium & cloud storage', 'credit-card', '#7c3aed', NULL, TRUE),
(12, 'Other', 'Miscellaneous items, emergency spending and unclassified', 'tag', '#475569', NULL, TRUE);
