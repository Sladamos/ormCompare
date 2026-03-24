import os
import random
from faker import Faker

LOCALE = 'en_US'
NUM_PRODUCERS = 10
NUM_PRODUCTS = 1000
NUM_VERSIONED = 1000
NUM_REVIEWS = 100000

fake = Faker(LOCALE)
Faker.seed(42)
random.seed(42)

def get_header():
    return (
        "BEGIN;\n\n"
        "DELETE FROM review;\n"
        "DELETE FROM product_versioned;\n"
        "DELETE FROM product;\n"
        "DELETE FROM producer;\n\n"
    )

def get_postgres_footer():
    return (
        "\nCOMMIT;\n\n"
        "-- Reset sequences\n"
        f"SELECT setval('producer_id_seq', {NUM_PRODUCERS + 10}, true);\n"
        f"SELECT setval('product_id_seq', {NUM_PRODUCTS + 100}, true);\n"
        f"SELECT setval('product_versioned_id_seq', {NUM_VERSIONED + 100}, true);\n"
        f"SELECT setval('review_id_seq', {NUM_REVIEWS + 100000}, true);\n"
    )

def get_h2_footer():
    return (
        "\nCOMMIT;\n\n"
        "-- Reset sequences\n"
        f"ALTER SEQUENCE producer_id_seq RESTART WITH {NUM_PRODUCERS + 10};\n"
        f"ALTER SEQUENCE product_id_seq RESTART WITH {NUM_PRODUCTS + 100};\n"
        f"ALTER SEQUENCE product_versioned_id_seq RESTART WITH {NUM_VERSIONED + 100};\n"
        f"ALTER SEQUENCE review_id_seq RESTART WITH {NUM_REVIEWS + 100000};\n"
    )

def main():
    os.makedirs('postgres', exist_ok=True)
    os.makedirs('h2', exist_ok=True)

    pg_file_path = os.path.join('postgres', 'insert_data.sql')
    h2_file_path = os.path.join('h2', 'insert_data.sql')

    print(f"Generowanie danych do:\n - {pg_file_path}\n - {h2_file_path}...")

    with open(pg_file_path, 'w', encoding='utf-8') as f_pg, \
         open(h2_file_path, 'w', encoding='utf-8') as f_h2:

        header = get_header()
        f_pg.write(header)
        f_h2.write(header)

        f_pg.write("-- TABLE: PRODUCER\n")
        f_h2.write("-- TABLE: PRODUCER\n")
        producer_ids = []
        for i in range(1, NUM_PRODUCERS + 1):
            name = fake.company().replace("'", "")
            country = fake.country().replace("'", "")
            sql = f"INSERT INTO producer (id, name, country) VALUES ({i}, '{name}', '{country}');\n"
            f_pg.write(sql)
            f_h2.write(sql)
            producer_ids.append(i)

        f_pg.write("\n-- TABLE: PRODUCT\n")
        f_h2.write("\n-- TABLE: PRODUCT\n")
        product_ids = []
        for i in range(1, NUM_PRODUCTS + 1):
            name = f"{fake.word().capitalize()} {fake.bothify(text='?-###')}".replace("'", "")
            price = round(random.uniform(10.0, 2000.0), 2)
            prod_id = random.choice(producer_ids)
            sql = f"INSERT INTO product (id, name, price, producer_id) VALUES ({i}, '{name}', {price}, {prod_id});\n"
            f_pg.write(sql)
            f_h2.write(sql)
            product_ids.append(i)

        f_pg.write("\n-- TABLE: PRODUCT_VERSIONED\n")
        f_h2.write("\n-- TABLE: PRODUCT_VERSIONED\n")
        for i in range(1, NUM_VERSIONED + 1):
            name = f"Ver {fake.word().capitalize()} {fake.bothify(text='V-###')}".replace("'", "")
            price = round(random.uniform(10.0, 2000.0), 2)
            sql = f"INSERT INTO product_versioned (id, name, price, version) VALUES ({i}, '{name}', {price}, 0);\n"
            f_pg.write(sql)
            f_h2.write(sql)

        f_pg.write("\n-- TABLE: REVIEW\n")
        f_h2.write("\n-- TABLE: REVIEW\n")
        positive_templates = ["Highly recommended", "Great product", "Amazing quality", "Will buy again", "Excellent"]
        negative_templates = ["Not recommended", "Terrible quality", "Broken on arrival", "Too expensive", "Waste of money"]

        for i in range(1, NUM_REVIEWS + 1):
            first_name = fake.first_name().replace("'", "")
            last_name = fake.last_name().replace("'", "")
            rating = random.randint(1, 5)
            product_id = random.choice(product_ids)

            if rating >= 4:
                base_text = random.choice(positive_templates)
            else:
                base_text = random.choice(negative_templates)

            comment = f"{base_text}. {fake.sentence()}".replace("'", "")

            sql = f"INSERT INTO review (id, first_name, last_name, rating, content, product_id) VALUES ({i}, '{first_name}', '{last_name}', {rating}, '{comment}', {product_id});\n"
            f_pg.write(sql)
            f_h2.write(sql)

            if i % 10000 == 0:
                print(f"Wygenerowano {i}/{NUM_REVIEWS} recenzji...")

        f_pg.write(get_postgres_footer())
        f_h2.write(get_h2_footer())

    print("Gotowe! Pliki wygenerowane w folderach 'postgres' i 'h2'.")

if __name__ == "__main__":
    main()