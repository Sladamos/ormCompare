package com.sladamos.repository;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

public class HibernateRepository implements BenchmarkRepository {

    private SessionFactory sessionFactory;
    //private final static String configFileName = "hibernate-h2.cfg.xml";
    private final static String configFileName = "hibernate.cfg.xml";

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


            Transaction tx2 = session.beginTransaction();
            if (configFileName.contains("h2")) {
                session.createNativeMutationQuery("ALTER TABLE producer ALTER COLUMN id RESTART WITH 11").executeUpdate();
                session.createNativeMutationQuery("ALTER TABLE product ALTER COLUMN id RESTART WITH 1001").executeUpdate();
                session.createNativeMutationQuery("ALTER TABLE review ALTER COLUMN id RESTART WITH 100001").executeUpdate();
            } else {
                session.createNativeMutationQuery("ALTER SEQUENCE producer_id_seq RESTART WITH 11").executeUpdate();
                session.createNativeMutationQuery("ALTER SEQUENCE product_id_seq RESTART WITH 1001").executeUpdate();
                session.createNativeMutationQuery("ALTER SEQUENCE review_id_seq RESTART WITH 100001").executeUpdate();
            }

            tx2.commit();

            session.clear();

        }
    }
}