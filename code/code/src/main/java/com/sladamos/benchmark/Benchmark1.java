package com.sladamos.benchmark;

import com.sladamos.model.Producer;
import com.sladamos.repository.BenchmarkRepository;
import com.sladamos.repository.RepositoryFactory;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

import static com.sladamos.Config.DB_PROFILE;

@State(Scope.Thread)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class Benchmark1 {

    private final RepositoryFactory repositoryFactory = new RepositoryFactory(DB_PROFILE);

    private long counter = 0;

    @Param({"hibernate", "eclipselink", "datanucleus"})
    private String ormProvider;
    private BenchmarkRepository repository;

    @Setup(Level.Trial)
    public void setup() {
        this.repository = repositoryFactory.getBenchmarkRepository(ormProvider);
        this.repository.setup();
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        this.repository.tearDown();
    }

    @Benchmark
    public void singleInsert() {
        for (int i = 0; i < 1000; i++) {
            counter++;
            Producer p = new Producer();
            p.setName("Benchmark1 " + counter);
            p.setCountry("Benchmark1Country");
            repository.save(p);
        }
    }

    @Setup(Level.Iteration)
    public void setupIteration() {
        repository.clearDatabase();
    }
}
