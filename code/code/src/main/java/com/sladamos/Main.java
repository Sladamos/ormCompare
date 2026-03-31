package com.sladamos;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.sladamos.benchmark.Config.*;

public class Main {
    public static void main(String[] args) throws RunnerException, IOException {
        if (RUN_ALL) {
            for (int i = 1; i <= NUMBER_OF_BENCHMARKS; i++) {
                runBenchmark(i);
            }
        } else {
            runBenchmark(BENCHMARK_NUMBER);
        }
    }

    private static void runBenchmark(int benchmarkNumber) throws IOException, RunnerException {
        String resultFile = createResultFile(benchmarkNumber);
        Options opt = new OptionsBuilder()
                .include("Benchmark" + benchmarkNumber + "\\.")
                .resultFormat(ResultFormatType.JSON)
                .result(resultFile)
                .addProfiler("gc")
                .addProfiler("comp")
                .build();

        new Runner(opt).run();
    }

    private static String createResultFile(int benchmarkNumber) throws IOException {
        Path outputDir = Paths.get("..", "..", "profiler", "jsons");
        Files.createDirectories(outputDir);
        String resultFile = outputDir.resolve("jmh-result-%s-%d.json".formatted(DB_PROFILE, benchmarkNumber)).toString();
        return resultFile;
    }
}