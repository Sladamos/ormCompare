package com.sladamos.repository;

import com.sladamos.model.Producer;
import com.sladamos.model.Product;
import com.sladamos.model.Review;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;

import java.math.BigDecimal;
import java.util.List;

public class EclipseLinkRepository implements BenchmarkRepository {

    private EntityManagerFactory emf;
    private final String persistenceUnitName;

    public EclipseLinkRepository(String profile) {
        this.persistenceUnitName = "eclipselink-" + profile;
    }

    public EclipseLinkRepository() {
        this("postgres");
    }

    @Override
    public void setup() {
        try {
            emf = Persistence.createEntityManagerFactory(persistenceUnitName);
        } catch (Exception e) {
            throw new RuntimeException("Nie udało się zbudować EntityManagerFactory dla: " + persistenceUnitName, e);
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
        try (em) {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            em.persist(o);
            tx.commit();
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public void clearDatabase() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createNativeQuery("DELETE FROM review WHERE id > 100000").executeUpdate();
            em.createNativeQuery("DELETE FROM product WHERE id > 1000").executeUpdate();
            em.createNativeQuery("DELETE FROM producer WHERE id > 10").executeUpdate();
            tx.commit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            em.clear();
            em.close();
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
            List<Product> products = em.createQuery("SELECT p FROM Product p WHERE p.id <= " + PRODUCTS_REVIEWS_JOIN_COUNT_EAGER_LAZY, Product.class).getResultList();
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
            List<Product> products = em.createQuery("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.reviews WHERE p.id <= " + PRODUCTS_REVIEWS_JOIN_COUNT_EAGER_LAZY, Product.class).getResultList();
            long count = 0;
            for (Product p : products) {
                count += p.getReviews().size();
            }
            return count;
        }
    }

    @Override
    public void insertBatched(List<Product> products, int batchSize) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            for (int i = 0; i < products.size(); i++) {
                em.persist(products.get(i));
                if (i > 0 && (i + 1) % batchSize == 0) {
                    em.flush();
                    em.clear();
                }
            }
            tx.commit();
        }
    }

    @Override
    public void bulkUpdateJpql(Integer producerId) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.createQuery("UPDATE Product p SET p.price = p.price * 1.1 WHERE p.producer.id = :pid")
                    .setParameter("pid", producerId)
                    .executeUpdate();
            em.getTransaction().commit();
        }
    }

    @Override
    public void bulkUpdateInLoop(Integer producerId) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            List<Product> products = em.createQuery("SELECT p FROM Product p WHERE p.producer.id = :pid", Product.class)
                    .setParameter("pid", producerId)
                    .getResultList();
            for (Product p : products) {
                p.setPrice(p.getPrice().multiply(new BigDecimal("1.1")));
            }
            em.getTransaction().commit();
        }
    }
}