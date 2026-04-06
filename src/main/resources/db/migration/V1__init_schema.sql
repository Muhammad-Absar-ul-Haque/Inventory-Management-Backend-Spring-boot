-- V1__init_schema.sql
-- Initial database schema for Inventory & Order Management System

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    sku VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(12, 2) NOT NULL,
    stock_quantity INTEGER NOT NULL,
    version BIGINT DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    category VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_price DECIMAL(12, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE order_items (
    id SERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    price_at_purchase DECIMAL(12, 2) NOT NULL,
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES products (id)
);
