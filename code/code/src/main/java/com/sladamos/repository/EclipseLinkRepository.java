package com.sladamos.repository;

import com.sladamos.model.Product;
import com.sladamos.model.Review;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;

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

            tx.begin();
            if (persistenceUnitName.contains("h2")) {
                em.createNativeQuery("ALTER TABLE producer ALTER COLUMN id RESTART WITH 11").executeUpdate();
                em.createNativeQuery("ALTER TABLE product ALTER COLUMN id RESTART WITH 1001").executeUpdate();
                em.createNativeQuery("ALTER TABLE review ALTER COLUMN id RESTART WITH 100001").executeUpdate();
            } else {
                em.createNativeQuery("ALTER SEQUENCE producer_id_seq RESTART WITH 11").executeUpdate();
                em.createNativeQuery("ALTER SEQUENCE product_id_seq RESTART WITH 1001").executeUpdate();
                em.createNativeQuery("ALTER SEQUENCE review_id_seq RESTART WITH 100001").executeUpdate();
            }
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
}