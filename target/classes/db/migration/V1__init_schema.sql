-- V1__init_schema.sql
-- TiDB-compatible simple DDL migrations

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    theme VARCHAR(20) NOT NULL DEFAULT 'light',
    default_date_range VARCHAR(20) NOT NULL DEFAULT 'THIS_MONTH',
    budget_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    icon VARCHAR(50) NOT NULL DEFAULT 'tag',
    color VARCHAR(20) NOT NULL DEFAULT '#6366f1',
    user_id BIGINT NULL,
    system_category BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_categories_user_id ON categories (user_id);

CREATE TABLE IF NOT EXISTS expenses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    description VARCHAR(200) NOT NULL,
    category_id BIGINT NOT NULL,
    expense_date DATE NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_expenses_user_id ON expenses (user_id);
CREATE INDEX idx_expenses_category_id ON expenses (category_id);
CREATE INDEX idx_expenses_user_date ON expenses (user_id, expense_date);

CREATE TABLE IF NOT EXISTS monthly_budgets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    `month` INT NOT NULL,
    `year` INT NOT NULL,
    total_budget DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    warning_threshold INT NOT NULL DEFAULT 80,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_month_year UNIQUE (user_id, `month`, `year`)
);

CREATE INDEX idx_monthly_budgets_user ON monthly_budgets (user_id);

CREATE TABLE IF NOT EXISTS budget_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    monthly_budget_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    CONSTRAINT uk_budget_category UNIQUE (monthly_budget_id, category_id)
);

CREATE INDEX idx_bc_monthly_budget ON budget_categories (monthly_budget_id);
CREATE INDEX idx_bc_category ON budget_categories (category_id);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    message VARCHAR(500) NOT NULL,
    type VARCHAR(30) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user_id ON notifications (user_id);
CREATE INDEX idx_notifications_user_read ON notifications (user_id, is_read);
