package com.sladamos;

import com.sladamos.benchmark.Benchmark1;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class Main {
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(Benchmark1.class.getSimpleName())
                .addProfiler("gc")
                .addProfiler("stack")
                .build();

        new Runner(opt).run();
    }
}