CREATE TABLE IF NOT EXISTS producer (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    country VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS product (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2),
    producer_id INT,
    CONSTRAINT fk_producer
    FOREIGN KEY (producer_id)
    REFERENCES producer(id)
    ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS review (
    id SERIAL PRIMARY KEY,
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

ALTER SEQUENCE producer_id_seq INCREMENT BY 100;
ALTER SEQUENCE product_id_seq INCREMENT BY 100;
ALTER SEQUENCE review_id_seq INCREMENT BY 100;
