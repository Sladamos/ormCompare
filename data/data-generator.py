from faker import Faker
import random

# CONFIGURATION
# ---------------------------------------------------------
LOCALE = 'en_US'  # English data (names, companies)
FILENAME = 'insert_data.sql'

# Data Volume
NUM_PRODUCERS = 10
NUM_PRODUCTS = 1000
NUM_REVIEWS = 100000

# Initialize Faker
fake = Faker(LOCALE)


def get_header():
    """Returns the SQL header with transaction start."""
    return "-- Auto-generated SQL Data\nBEGIN;\n\n"


def get_footer(num_producers, num_products, num_reviews):
    """
    Returns the transaction commit and sequence reset commands.
    NOTE: The 'setval' command is specific to PostgreSQL.
    For H2, you might need: ALTER SEQUENCE seq_name RESTART WITH value;
    """
    sql = "\nCOMMIT;\n\n"
    sql += "-- Reset sequences (PostgreSQL specific syntax)\n"
    sql += f"SELECT setval('producer_id_seq', {num_producers}, true);\n"
    sql += f"SELECT setval('product_id_seq', {num_products}, true);\n"
    sql += f"SELECT setval('review_id_seq', {num_reviews}, true);\n"
    return sql


def generate_producers(file_handle, count):
    """Generates SQL inserts for Producers and returns a list of their IDs."""
    print(f"Generating {count} producers...")
    file_handle.write(f"-- TABLE: PRODUCER ({count} records)\n")

    ids = []
    companies = ['Sony', 'Samsung', 'Apple', 'Xiaomi', 'Dell', 'Lenovo', 'Asus', 'HP', 'LG', 'Logitech']

    for i in range(1, count + 1):
        # Use predefined list first, then fallback to Faker
        if i <= len(companies):
            name = companies[i - 1]
        else:
            name = fake.company()

        country = fake.country().replace("'", "")  # Escape single quotes

        # SQL Statement
        sql = f"INSERT INTO producer (id, name, country) VALUES ({i}, '{name}', '{country}');\n"
        file_handle.write(sql)
        ids.append(i)

    return ids


def generate_products(file_handle, count, producer_ids):
    """Generates SQL inserts for Products linked to Producers."""
    print(f"Generating {count} products...")
    file_handle.write(f"\n-- TABLE: PRODUCT ({count} records)\n")

    ids = []

    for i in range(1, count + 1):
        # Example: "Smartphone X-900"
        product_name = f"{fake.word().capitalize()} {fake.random_letter().upper()}-{fake.random_int(100, 999)}"
        price = round(random.uniform(50.0, 4000.0), 2)
        producer_id = random.choice(producer_ids)

        sql = f"INSERT INTO product (id, name, price, producer_id) VALUES ({i}, '{product_name}', {price}, {producer_id});\n"
        file_handle.write(sql)
        ids.append(i)

    return ids


def generate_reviews(file_handle, count, product_ids):
    """Generates SQL inserts for Reviews linked to Products."""
    print(f"Generating {count} reviews...")
    file_handle.write(f"\n-- TABLE: REVIEW ({count} records)\n")

    # Simple templates for performance
    positive_templates = ["Great product", "Highly recommended", "Works perfectly", "Good value", "Amazing quality"]
    negative_templates = ["Not recommended", "Terrible quality", "Broken on arrival", "Too expensive", "Waste of money"]

    for i in range(1, count + 1):
        first_name = fake.first_name()
        last_name = fake.last_name()
        rating = random.randint(1, 5)
        product_id = random.choice(product_ids)

        # Logic: High rating = positive text, Low rating = negative text
        if rating >= 4:
            base_text = random.choice(positive_templates)
        else:
            base_text = random.choice(negative_templates)

        comment = f"{base_text}. {fake.sentence()}"
        comment = comment.replace("'", "")  # Escape SQL issues

        sql = f"INSERT INTO review (id, first_name, last_name, rating, content, product_id) VALUES ({i}, '{first_name}', '{last_name}', {rating}, '{comment}', {product_id});\n"
        file_handle.write(sql)


def main():
    """Main orchestration function."""
    with open(FILENAME, 'w', encoding='utf-8') as f:
        # 1. Start Transaction
        f.write(get_header())

        # 2. Generate Data (A -> B -> C)
        producer_ids = generate_producers(f, NUM_PRODUCERS)
        product_ids = generate_products(f, NUM_PRODUCTS, producer_ids)
        generate_reviews(f, NUM_REVIEWS, product_ids)

        # 3. Close Transaction & Reset Sequences
        f.write(get_footer(NUM_PRODUCERS, NUM_PRODUCTS, NUM_REVIEWS))

    print(f"SUCCESS! File '{FILENAME}' created.")


if __name__ == "__main__":
    main()