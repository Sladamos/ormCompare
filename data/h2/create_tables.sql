CREATE SEQUENCE IF NOT EXISTS producer_id_seq START WITH 11 INCREMENT BY 100;
CREATE TABLE IF NOT EXISTS producer (
    id INT DEFAULT nextval('producer_id_seq') PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    country VARCHAR(255) NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS product_id_seq START WITH 1001 INCREMENT BY 100;
CREATE TABLE IF NOT EXISTS product (
    id INT DEFAULT nextval('product_id_seq') PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2),
    producer_id INT,
    CONSTRAINT fk_producer
    FOREIGN KEY (producer_id)
    REFERENCES producer(id)
    ON DELETE CASCADE
);

CREATE SEQUENCE IF NOT EXISTS product_versioned_id_seq START WITH 1001 INCREMENT BY 100;
CREATE TABLE IF NOT EXISTS product_versioned (
    id INT DEFAULT nextval('product_versioned_id_seq') PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2),
    version INT DEFAULT 0
);

CREATE SEQUENCE IF NOT EXISTS review_id_seq START WITH 100001 INCREMENT BY 100;
CREATE TABLE IF NOT EXISTS review (
    id INT DEFAULT nextval('review_id_seq') PRIMARY KEY,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    rating INT CHECK (rating >= 1 AND rating <= 5),
    content TEXT,
    product_id INT,
    CONSTRAINT fk_product
    FOREIGN KEY (product_id)
    REFERENCES product(id)
    ON DELETE CASCADE
);
