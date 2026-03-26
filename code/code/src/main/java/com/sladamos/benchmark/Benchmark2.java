package com.sladamos.benchmark;

import com.sladamos.repository.BenchmarkRepository;
import com.sladamos.repository.RepositoryFactory;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.sladamos.benchmark.Config.*;

@State(Scope.Thread)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = WARMUP_ITERATIONS, time = WARMUP_SECONDS)
@Measurement(iterations = MEASUREMENT_ITERATIONS, time = MEASUREMENT_SECONDS)
@Fork(1)
public class Benchmark2 {

    @Param({"hibernate", "eclipselink", "datanucleus", "cayenne"})
    private String ormProvider;

    private final RepositoryFactory repositoryFactory = new RepositoryFactory(DB_PROFILE);

    private BenchmarkRepository repository;

    @Setup(Level.Trial)
    public void setup() {
        this.repository = repositoryFactory.getBenchmarkRepository(ormProvider);
        this.repository.setup();
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        this.repository.clearDatabase();
        this.repository.tearDown();
    }

    @Benchmark
    public void findById(Blackhole bh) {
        for (int i = 0; i < 5000; i++) {
            int randomId = ThreadLocalRandom.current().nextInt(1, 100001);
            bh.consume(repository.findReviewById(randomId));
        }
    }
}