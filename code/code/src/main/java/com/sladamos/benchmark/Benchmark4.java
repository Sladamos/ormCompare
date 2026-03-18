package com.sladamos.benchmark;

import com.sladamos.repository.BenchmarkRepository;
import com.sladamos.repository.RepositoryFactory;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

import static com.sladamos.benchmark.Config.*;

@State(Scope.Thread)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = WARMUP_ITERATIONS, time = WARMUP_SECONDS)
@Measurement(iterations = MEASUREMENT_ITERATIONS, time = MEASUREMENT_SECONDS)
@Fork(1)
public class Benchmark4 {

    private final RepositoryFactory repositoryFactory = new RepositoryFactory(DB_PROFILE);

    @Param({"hibernate", "eclipselink", "datanucleus", "cayenne"})
    private String ormProvider;

    private BenchmarkRepository repository;

    @Setup(Level.Trial)
    public void setupTrial() {
        this.repository = repositoryFactory.getBenchmarkRepository(ormProvider);
        this.repository.setup();
    }

    @TearDown(Level.Trial)
    public void tearDownTrial() {
        this.repository.tearDown();
    }

    @Benchmark
    public void singleJoinQuery(Blackhole bh) {
        for (int i = 0; i < 100; i++) {
            bh.consume(repository.findProductsByProducerCountry("Poland"));
        }
    }
}