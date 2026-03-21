package com.sladamos.benchmark;

import com.sladamos.model.Product;
import com.sladamos.repository.BenchmarkRepository;
import com.sladamos.repository.RepositoryFactory;
import org.openjdk.jmh.annotations.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.sladamos.benchmark.Config.*;

@State(Scope.Thread)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = WARMUP_ITERATIONS, time = WARMUP_SECONDS)
@Measurement(iterations = MEASUREMENT_ITERATIONS, time = MEASUREMENT_SECONDS)
@Fork(1)
public class Benchmark8 {

    private final RepositoryFactory repositoryFactory = new RepositoryFactory(DB_PROFILE);

    @Param({"hibernate", "eclipselink", "datanucleus", "cayenne"})
    private String ormProvider;

    @Param({"1", "50", "100"})
    private int batchSize;

    private BenchmarkRepository repository;
    private List<Product> productsToInsert;

    private long counter = 0;

    @Setup(Level.Trial)
    public void setupTrial() {
        this.repository = repositoryFactory.getBenchmarkRepository(ormProvider);
        this.repository.setup();
    }

    @TearDown(Level.Trial)
    public void tearDownTrial() {
        this.repository.clearDatabase();
        this.repository.tearDown();
    }

    @Setup(Level.Iteration)
    public void setupIteration() {
        repository.clearDatabase();
        productsToInsert = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Product p = new Product();
            p.setName("Benchmark8 " + counter++);
            p.setPrice(new BigDecimal("100.00"));
            productsToInsert.add(p);
        }
    }

    @Benchmark
    public void insertBatched() {
        repository.insertBatched(productsToInsert, batchSize);
    }
}