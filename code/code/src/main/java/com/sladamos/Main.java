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

import static com.sladamos.benchmark.Config.DB_PROFILE;
import static com.sladamos.benchmark.Config.BENCHMARK_NUMBER;

public class Main {
    public static void main(String[] args) throws RunnerException, IOException {
        String resultFile = createResultFile();
        Options opt = new OptionsBuilder()
                .include("Benchmark" + BENCHMARK_NUMBER)
                .resultFormat(ResultFormatType.JSON)
                .result(resultFile)
                .addProfiler("gc")
                .addProfiler("comp")
                .build();

        new Runner(opt).run();
    }

    private static String createResultFile() throws IOException {
        Path outputDir = Paths.get("..", "..", "profiler", "jsons");
        Files.createDirectories(outputDir);
        String resultFile = outputDir.resolve("jmh-result-%s-%d.json".formatted(DB_PROFILE, BENCHMARK_NUMBER)).toString();
        return resultFile;
    }
}