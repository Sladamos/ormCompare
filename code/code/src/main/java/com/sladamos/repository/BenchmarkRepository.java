package com.sladamos.repository;

import com.sladamos.model.Producer;
import com.sladamos.model.Review;

public interface BenchmarkRepository {
    void setup();
    void tearDown();
    void save(Object o);
    void clearDatabase();
    Review findReviewById(Integer id);
    Producer findProducerById(Integer id);
}
