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
    long countReviewsNPlusOne();
    long countReviewsJoinFetch();
    void insertBatched(List<Product> products, int batchSize);
    void bulkUpdateJpql(Integer producerId);
    void bulkUpdateInLoop(Integer producerId);

    long PRODUCTS_REVIEWS_JOIN_COUNT_EAGER_LAZY = 50;
}
