package com.sladamos;

import com.sladamos.model.ProductVersioned;
import com.sladamos.repository.BenchmarkRepository;
import com.sladamos.repository.RepositoryFactory;
import jakarta.persistence.OptimisticLockException;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sladamos.benchmark.Config.DB_PROFILE;

public class OptimisticLockingProof {

    public static void main(String[] args) throws InterruptedException {
        Logger.getLogger("org.hibernate").setLevel(Level.OFF);

        RepositoryFactory factory = new RepositoryFactory(DB_PROFILE);
        BenchmarkRepository repository = factory.getBenchmarkRepository("hibernate");
        repository.setup();
        repository.clearDatabase();

        Integer targetId = createProduct(repository);

        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGun = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            int threadNum = i + 1;
            executor.submit(run(startGun, repository, targetId, successCount, conflictCount, threadNum));
        }

        startGun.countDown();

        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.SECONDS);

        System.out.println("Zakończono test.");
        System.out.println("Zatwierdzone transakcje: " + successCount.get());
        System.out.println("Wykryte konflikty: " + conflictCount.get());

        repository.clearDatabase();
        repository.tearDown();
        executor.shutdownNow();
    }

    private static Runnable run(CountDownLatch startGun, BenchmarkRepository repository, Integer targetId, AtomicInteger successCount, AtomicInteger conflictCount, int threadNum) {
        return () -> {
            try {
                startGun.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            boolean success = false;
            int retries = 0;

            while (!success) {
                try {
                    repository.updateVersionedProductPrice(targetId, new BigDecimal("10.00"));

                    successCount.incrementAndGet();
                    success = true;
                    System.out.println("Wątek " + threadNum + ": Zapis zakończony pomyślnie. Liczba prób: " + (retries + 1));
                } catch (OptimisticLockException e) {
                    retries++;
                    conflictCount.incrementAndGet();
                    System.out.println("Wątek " + threadNum + ": Wykryto konflikt wersji. Ponawianie transakcji.");
                }
            }
        };
    }

    private static Integer createProduct(BenchmarkRepository repository) {
        ProductVersioned p = new ProductVersioned();
        p.setName("OptimisticProduct");
        p.setPrice(new BigDecimal("100.00"));
        repository.save(p);
        return p.getId();
    }
}