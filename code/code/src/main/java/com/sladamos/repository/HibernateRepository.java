package com.sladamos.repository;

import com.sladamos.model.Producer;
import com.sladamos.model.Product;
import com.sladamos.model.Review;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import java.util.List;

public class HibernateRepository implements BenchmarkRepository {

    private SessionFactory sessionFactory;
    private final String configFileName;

    public HibernateRepository(String profile) {
        configFileName = "hibernate-%s.cfg.xml".formatted(profile);
    }

    public HibernateRepository() {
        this("postgres");
    }

    @Override
    public void setup() {
        try {
            sessionFactory = new Configuration().configure(configFileName).buildSessionFactory();
        } catch (Exception e) {
            throw new RuntimeException("Nie udało się zbudować SessionFactory z XML", e);
        }
    }

    @Override
    public void tearDown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @Override
    public void save(Object o) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            session.persist(o);
            tx.commit();
        }
    }

    @Override
    public void clearDatabase() {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            session.createNativeMutationQuery("DELETE FROM review WHERE id > 100000").executeUpdate();
            session.createNativeMutationQuery("DELETE FROM product WHERE id > 1000").executeUpdate();
            session.createNativeMutationQuery("DELETE FROM producer WHERE id > 10").executeUpdate();
            tx.commit();
        }
    }

    @Override
    public Review findReviewById(Integer id) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(Review.class, id);
        }
    }

    @Override
    public void updateProduct(Integer id, String newName) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            Product product = session.get(Product.class, id);
            if (product != null) {
                product.setName(newName);
            }
            tx.commit();
        }
    }

    @Override
    public void deleteProduct(Integer id) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            Product product = session.get(Product.class, id);
            if (product != null) {
                session.remove(product);
            }
            tx.commit();
        }
    }

    @Override
    public List<Product> findProductsByProducerCountry(String country) {
        try (Session session = sessionFactory.openSession()) {
            String hql = "SELECT p FROM Product p JOIN p.producer pr WHERE pr.country = :country";
            return session.createQuery(hql, Product.class)
                    .setParameter("country", country)
                    .getResultList();
        }
    }

    @Override
    public List<Producer> findProducersWithTopReviews(Integer rating) {
        try (Session session = sessionFactory.openSession()) {
            String hql = "SELECT DISTINCT pr FROM Producer pr JOIN pr.products p JOIN p.reviews r WHERE r.rating = :rating";
            return session.createQuery(hql, Producer.class)
                    .setParameter("rating", rating)
                    .getResultList();
        }
    }

    @Override
    public long countReviewsNPlusOne() {
        try (Session session = sessionFactory.openSession()) {
            List<Product> products = session.createQuery("SELECT p FROM Product p WHERE p.id <= " + PRODUCTS_REVIEWS_JOIN_COUNT, Product.class).getResultList();
            long count = 0;
            for (Product p : products) {
                count += p.getReviews().size();
            }
            return count;
        }
    }

    @Override
    public long countReviewsJoinFetch() {
        try (Session session = sessionFactory.openSession()) {
            List<Product> products = session.createQuery("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.reviews WHERE p.id <= " + PRODUCTS_REVIEWS_JOIN_COUNT, Product.class).getResultList();
            long count = 0;
            for (Product p : products) {
                count += p.getReviews().size();
            }
            return count;
        }
    }

    @Override
    public void insertBatched(List<Product> products, int batchSize) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            for (int i = 0; i < products.size(); i++) {
                session.persist(products.get(i));
                if (i > 0 && (i + 1) % batchSize == 0) {
                    session.flush();
                    session.clear();
                }
            }
            tx.commit();
        }
    }
}