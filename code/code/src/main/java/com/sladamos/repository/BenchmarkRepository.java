package com.sladamos.repository;

import com.sladamos.model.Producer;
import com.sladamos.model.Product;
import com.sladamos.model.Review;

import java.util.List;

public interface BenchmarkRepository {
    void setup();
    void tearDown();
    void save(Object o);
    void clearDatabase();
    Review findReviewById(Integer id);
    void updateProduct(Integer id, String newName);
    void deleteProduct(Integer id);
    List<Product> findProductsByProducerCountry(String country);
    List<Producer> findProducersWithTopReviews(Integer rating);
}
