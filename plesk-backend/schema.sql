-- LoyPOS Plesk MySQL / MariaDB Schema
-- Compatible with MySQL 8.0+ and MariaDB 10.4+
-- Import this file via Plesk -> Databases -> phpMyAdmin after creating your database.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS branches (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(500) NOT NULL DEFAULT '',
    phone VARCHAR(64) NOT NULL DEFAULT '',
    created_at BIGINT NOT NULL DEFAULT 0,
    archived TINYINT(1) NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL DEFAULT 0,
    INDEX idx_branches_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS categories (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    position INT NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL DEFAULT 0,
    INDEX idx_categories_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS products (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    category_id VARCHAR(64) NULL,
    name VARCHAR(255) NOT NULL,
    price_cents BIGINT NOT NULL DEFAULT 0,
    cost_cents BIGINT NOT NULL DEFAULT 0,
    sku VARCHAR(128) NOT NULL DEFAULT '',
    barcode VARCHAR(128) NOT NULL DEFAULT '',
    track_stock TINYINT(1) NOT NULL DEFAULT 0,
    stock DOUBLE NOT NULL DEFAULT 0,
    archived TINYINT(1) NOT NULL DEFAULT 0,
    branch_ids TEXT NOT NULL,
    image_path VARCHAR(512) NOT NULL DEFAULT '',
    updated_at BIGINT NOT NULL DEFAULT 0,
    INDEX idx_products_category (category_id),
    INDEX idx_products_sku (sku),
    INDEX idx_products_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS customers (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(64) NOT NULL DEFAULT '',
    email VARCHAR(255) NOT NULL DEFAULT '',
    note TEXT NOT NULL,
    points INT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL DEFAULT 0,
    INDEX idx_customers_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sales (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    number INT NOT NULL,
    created_at BIGINT NOT NULL,
    subtotal_cents BIGINT NOT NULL,
    discount_cents BIGINT NOT NULL DEFAULT 0,
    discount_type VARCHAR(32) NULL,
    discount_value DOUBLE NULL,
    tax_cents BIGINT NOT NULL DEFAULT 0,
    total_cents BIGINT NOT NULL,
    paid_cents BIGINT NOT NULL,
    change_cents BIGINT NOT NULL DEFAULT 0,
    payment_type VARCHAR(32) NOT NULL DEFAULT 'cash',
    customer_id VARCHAR(64) NULL,
    customer_name VARCHAR(255) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'completed',
    note TEXT NOT NULL,
    branch_id VARCHAR(64) NULL,
    cashier_id VARCHAR(64) NULL,
    cashier_name VARCHAR(255) NULL,
    updated_at BIGINT NOT NULL DEFAULT 0,
    INDEX idx_sales_created (created_at),
    INDEX idx_sales_updated (updated_at),
    INDEX idx_sales_branch (branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sale_items (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    receipt_id VARCHAR(64) NOT NULL,
    item_id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    price_cents BIGINT NOT NULL,
    qty DOUBLE NOT NULL,
    line_total_cents BIGINT NOT NULL,
    INDEX idx_sale_items_receipt (receipt_id),
    INDEX idx_sale_items_item (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_movements (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    item_id VARCHAR(64) NOT NULL,
    qty DOUBLE NOT NULL,
    type VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL,
    note TEXT NOT NULL,
    receipt_id VARCHAR(64) NULL,
    INDEX idx_stock_movements_item (item_id),
    INDEX idx_stock_movements_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS shifts (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    opened_at BIGINT NOT NULL,
    opening_float_cents BIGINT NOT NULL DEFAULT 0,
    branch_id VARCHAR(64) NULL,
    closed_at BIGINT NULL,
    counted_cents BIGINT NULL,
    expected_cents BIGINT NULL,
    INDEX idx_shifts_opened (opened_at),
    INDEX idx_shifts_closed (closed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cash_movements (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    shift_id VARCHAR(64) NOT NULL,
    type VARCHAR(16) NOT NULL,
    amount_cents BIGINT NOT NULL,
    note TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    INDEX idx_cash_movements_shift (shift_id),
    INDEX idx_cash_movements_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS shop_settings (
    setting_key VARCHAR(128) NOT NULL PRIMARY KEY,
    setting_value TEXT NOT NULL,
    updated_at BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
