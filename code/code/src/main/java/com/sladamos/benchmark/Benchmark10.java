package com.sladamos.benchmark;

import com.sladamos.model.Producer;
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
public class Benchmark10 {

    private final RepositoryFactory repositoryFactory = new RepositoryFactory(DB_PROFILE);

    @Param({"hibernate", "eclipselink", "datanucleus", "cayenne"})
    private String ormProvider;

    private BenchmarkRepository repository;
    private List<Product> products;
    private Producer sharedProducer;

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

        sharedProducer = new Producer();
        sharedProducer.setName("Tx Producer");
        sharedProducer.setCountry("TxLand");
        repository.save(sharedProducer);

        products = new ArrayList<>(1000);
        for (int i = 0; i < 1000; i++) {
            Product p = new Product();
            p.setName("Tx Product " + i);
            p.setPrice(new BigDecimal("100.00"));
            p.setProducer(sharedProducer);
            products.add(p);
        }
    }

    @Benchmark
    public void insertOneTransaction() {
        repository.insertInOneTransaction(products);
    }

    @Benchmark
    public void insertMultipleTransactions() {
        repository.insertInMultipleTransactions(products);
    }
}