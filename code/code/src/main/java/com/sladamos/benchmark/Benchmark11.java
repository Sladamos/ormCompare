package com.sladamos.benchmark;

import com.sladamos.model.Product;
import com.sladamos.model.ProductVersioned;
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
public class Benchmark11 {

    public static final BigDecimal SUM = new BigDecimal("1.00");
    @Param({"hibernate", "eclipselink", "datanucleus", "cayenne"})
    private String ormProvider;

    private final RepositoryFactory repositoryFactory = new RepositoryFactory(DB_PROFILE);

    private BenchmarkRepository repository;

    private List<Integer> standardIds;

    private List<Integer> versionedIds;

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
        standardIds = new ArrayList<>();
        versionedIds = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            Product p = new Product();
            p.setName("Benchmark11 St" + counter);
            p.setPrice(new BigDecimal("100.00"));
            repository.save(p);
            standardIds.add(p.getId());

            ProductVersioned pv = new ProductVersioned();
            pv.setName("Benchmark11 Ver" + counter);
            pv.setPrice(new BigDecimal("100.00"));
            repository.save(pv);
            versionedIds.add(pv.getId());
        }
    }

    @Benchmark
    public void updateStandard() {
        for (Integer id : standardIds) {
            repository.updateProduct(id, SUM);
        }
    }

    @Benchmark
    public void updateOptimisticLocking() {
        for (Integer id : versionedIds) {
            repository.updateVersionedProductPrice(id, SUM);
        }
    }
}