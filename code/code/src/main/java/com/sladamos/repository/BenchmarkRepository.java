package com.sladamos.repository;

public interface BenchmarkRepository {
    void setup();
    void tearDown();
    void save(Object o);
    void clearDatabase();
}
