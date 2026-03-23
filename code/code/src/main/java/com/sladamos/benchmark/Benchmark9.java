package com.sladamos.benchmark;

import com.sladamos.model.Producer;
import com.sladamos.model.Product;
import com.sladamos.repository.BenchmarkRepository;
import com.sladamos.repository.RepositoryFactory;
import org.openjdk.jmh.annotations.*;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static com.sladamos.benchmark.Config.*;

@State(Scope.Thread)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = WARMUP_ITERATIONS, time = WARMUP_SECONDS)
@Measurement(iterations = MEASUREMENT_ITERATIONS, time = MEASUREMENT_SECONDS)
@Fork(1)
public class Benchmark9 {

    private final RepositoryFactory repositoryFactory = new RepositoryFactory(DB_PROFILE);

    @Param({"hibernate", "eclipselink", "datanucleus", "cayenne"})
    private String ormProvider;

    private BenchmarkRepository repository;

    private Integer targetProducerId;

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

        Producer prod = new Producer();
        prod.setName("Benchmark9");
        prod.setCountry("Benchmark9Country");
        repository.save(prod);
        this.targetProducerId = prod.getId();

        for (int i = 0; i < 1000; i++) {
            Product p = new Product();
            p.setName("Benchmark9 " + counter++);
            p.setPrice(new BigDecimal("100.00"));
            p.setProducer(prod);
            repository.save(p);
        }
    }

    @Benchmark
    public void bulkUpdateNativeJPQL() {
        repository.bulkUpdateJpql(this.targetProducerId);
    }

    @Benchmark
    public void bulkUpdateInRamLoop() {
        repository.bulkUpdateInLoop(this.targetProducerId);
    }
}