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
public class Benchmark3 {

    private final RepositoryFactory repositoryFactory = new RepositoryFactory(DB_PROFILE);

    @Param({"hibernate", "eclipselink", "datanucleus", "cayenne"})
    private String ormProvider;

    private BenchmarkRepository repository;

    private List<Integer> generatedIds;

    @Setup(Level.Trial)
    public void setupTrial() {
        this.repository = repositoryFactory.getBenchmarkRepository(ormProvider);
        this.repository.setup();
    }

    @TearDown(Level.Trial)
    public void tearDownTrial() {
        this.repository.tearDown();
    }

    @Setup(Level.Iteration)
    public void setupIteration() {
        repository.clearDatabase();
        generatedIds = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            Product p = new Product();
            p.setName("Benchmark3 " + i);
            p.setPrice(new BigDecimal("10.00"));
            repository.save(p);
            generatedIds.add(p.getId());
        }
    }

    @TearDown(Level.Iteration)
    public void tearDownIteration() {
        repository.clearDatabase();
    }

    @Benchmark
    public void updateEntity() {
        for (Integer id : generatedIds) {
            repository.updateProduct(id, "Benchmark3_updated " + id);
        }
    }

    @Benchmark
    public void deleteEntity() {
        for (Integer id : generatedIds) {
            repository.deleteProduct(id);
        }
    }
}