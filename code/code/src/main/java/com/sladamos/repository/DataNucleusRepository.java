package com.sladamos.repository;

import com.sladamos.model.Producer;
import com.sladamos.model.Product;
import com.sladamos.model.Review;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import org.datanucleus.ExecutionContext;
import org.datanucleus.store.connection.ManagedConnection;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

public class DataNucleusRepository implements BenchmarkRepository {

    private EntityManagerFactory emf;
    private final String persistenceUnitName;

    public DataNucleusRepository(String profile) {
        this.persistenceUnitName = "datanucleus-" + profile;
    }

    public DataNucleusRepository() {
        this("postgres");
    }

    @Override
    public void setup() {
        try {
            emf = Persistence.createEntityManagerFactory(persistenceUnitName);
        } catch (Exception e) {
            throw new RuntimeException("Nie udało się zbudować DataNucleus EMF: " + persistenceUnitName, e);
        }
    }

    @Override
    public void tearDown() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @Override
    public void save(Object o) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(o);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    @Override
    public void clearDatabase() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            ExecutionContext ec = (ExecutionContext) em.getDelegate();
            ManagedConnection mc = ec.getStoreManager().getConnectionManager().getConnection(ec);

            try {
                Connection conn = (Connection) mc.getConnection();
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("DELETE FROM review WHERE id > 100000");
                    stmt.execute("DELETE FROM product WHERE id > 1000");
                    stmt.execute("DELETE FROM producer WHERE id > 10");

                    if (persistenceUnitName.contains("h2")) {
                        stmt.execute("ALTER TABLE producer ALTER COLUMN id RESTART WITH 11");
                        stmt.execute("ALTER TABLE product ALTER COLUMN id RESTART WITH 1001");
                        stmt.execute("ALTER TABLE review ALTER COLUMN id RESTART WITH 100001");
                    } else {
                        stmt.execute("ALTER SEQUENCE producer_id_seq RESTART WITH 11");
                        stmt.execute("ALTER SEQUENCE product_id_seq RESTART WITH 1001");
                        stmt.execute("ALTER SEQUENCE review_id_seq RESTART WITH 100001");
                    }
                }
            } finally {
                mc.release();
            }

            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            e.printStackTrace();
            throw new RuntimeException("Błąd czyszczenia bazy (DN Internal Connection)", e);
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    @Override
    public Review findReviewById(Integer id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(Review.class, id);
        }
    }

    @Override
    public void updateProduct(Integer id, String newName) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            Product product = em.find(Product.class, id);
            if (product != null) {
                product.setName(newName);
            }
            tx.commit();
        }
    }

    @Override
    public void deleteProduct(Integer id) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            Product product = em.find(Product.class, id);
            if (product != null) {
                em.remove(product);
            }
            tx.commit();
        }
    }

    @Override
    public List<Product> findProductsByProducerCountry(String country) {
        try (EntityManager em = emf.createEntityManager()) {
            String jpql = "SELECT p FROM Product p JOIN p.producer pr WHERE pr.country = :country";
            return em.createQuery(jpql, Product.class)
                    .setParameter("country", country)
                    .getResultList();
        }
    }

    @Override
    public List<Producer> findProducersWithTopReviews(Integer rating) {
        try (EntityManager em = emf.createEntityManager()) {
            String jpql = "SELECT DISTINCT pr FROM Producer pr JOIN pr.products p JOIN p.reviews r WHERE r.rating = :rating";
            return em.createQuery(jpql, Producer.class)
                    .setParameter("rating", rating)
                    .getResultList();
        }
    }

    @Override
    public long countReviewsNPlusOne() {
        try (EntityManager em = emf.createEntityManager()) {
            List<Product> products = em.createQuery("SELECT p FROM Product p WHERE p.id <= " + PRODUCTS_REVIEWS_JOIN_COUNT, Product.class).getResultList();
            long count = 0;
            for (Product p : products) {
                count += p.getReviews().size();
            }
            return count;
        }
    }

    @Override
    public long countReviewsJoinFetch() {
        try (EntityManager em = emf.createEntityManager()) {
            List<Product> products = em.createQuery("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.reviews WHERE p.id <= " + PRODUCTS_REVIEWS_JOIN_COUNT, Product.class).getResultList();
            long count = 0;
            for (Product p : products) {
                count += p.getReviews().size();
            }
            return count;
        }
    }
}