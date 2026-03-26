package com.sladamos.repository;

import com.sladamos.model.Producer;
import com.sladamos.model.Product;
import com.sladamos.model.Review;

import java.math.BigDecimal;
import java.util.List;

public interface BenchmarkRepository {
    void setup();
    void tearDown();
    void save(Object o);
    void clearDatabase();
    Review findReviewById(Integer id);
    void updateProduct(Integer id, String newName);
    void updateProduct(Integer id, BigDecimal sum);
    void deleteProduct(Integer id);
    List<Product> findProductsByProducerCountry(String country);
    List<Producer> findProducersWithTopReviews(Integer rating);
    long countReviewsNPlusOne();
    long countReviewsJoinFetch();
    void insertBatched(List<Product> products, int batchSize);
    void bulkUpdateJpql(Integer producerId);
    void bulkUpdateInLoop(Integer producerId);
    void insertInOneTransaction(List<Product> products);
    void insertInMultipleTransactions(List<Product> products);

    void updateVersionedProductPrice(Integer id, BigDecimal sum);

    long PRODUCTS_REVIEWS_JOIN_COUNT_EAGER_LAZY = 50;
}
