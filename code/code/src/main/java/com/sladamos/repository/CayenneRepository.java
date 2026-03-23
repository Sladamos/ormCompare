package com.sladamos.repository;

import com.sladamos.model.Producer;
import com.sladamos.model.Product;
import com.sladamos.model.Review;
import org.apache.cayenne.*;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SQLExec;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CayenneRepository implements BenchmarkRepository {

    private ServerRuntime cayenneRuntime;
    private final String profile;

    public CayenneRepository(String profile) {
        this.profile = profile;
    }

    @Override
    public void setup() {
        String configName = "cayenne-" + profile + ".xml";
        cayenneRuntime = ServerRuntime.builder().addConfig(configName).build();
    }

    @Override
    public void tearDown() {
        if (cayenneRuntime != null) {
            cayenneRuntime.shutdown();
        }
    }
    @Override
    public void save(Object o) {
        ObjectContext context = cayenneRuntime.newContext();
        CayenneDataObject savedObj = saveObject(o, context);
        context.commitChanges();
        int generatedId = Cayenne.intPKForObject(savedObj);
        switch (o) {
            case Producer p -> p.setId(generatedId);
            case Product p -> p.setId(generatedId);
            case Review r -> r.setId(generatedId);
            default -> {}
        }

    }

    private CayenneDataObject saveObject(Object o, ObjectContext context) {
        return switch (o) {
            case Producer p -> saveProducer(p, context);
            case Product p -> saveProduct(p, context);
            case Review r -> saveReview(r, context);
            default -> throw new IllegalArgumentException("Cayenne nie obsługuje tego typu obiektu: " + o.getClass().getName());
        };
    }

    @Override
    public void clearDatabase() {
        ObjectContext context = cayenneRuntime.newContext();
        SQLExec.query("DELETE FROM review WHERE id > 100000").execute(context);
        SQLExec.query("DELETE FROM product WHERE id > 1000").execute(context);
        SQLExec.query("DELETE FROM producer WHERE id > 10").execute(context);
    }

    @Override
    public Review findReviewById(Integer id) {
        ObjectContext context = cayenneRuntime.newContext();
        DataObject cayenneReview = (DataObject) Cayenne.objectForPK(context, "Review", id);

        if (cayenneReview == null) {
            return null;
        }

        Review r = new Review();
        r.setId(id);
        r.setFirstName((String) cayenneReview.readProperty("firstName"));
        r.setLastName((String) cayenneReview.readProperty("lastName"));
        r.setRating((Integer) cayenneReview.readProperty("rating"));
        r.setContent((String) cayenneReview.readProperty("content"));

        return r;
    }

    @Override
    public void updateProduct(Integer id, String newName) {
        ObjectContext context = cayenneRuntime.newContext();
        DataObject cayenneProduct = (DataObject) Cayenne.objectForPK(context, "Product", id);
        if (cayenneProduct != null) {
            cayenneProduct.writeProperty("name", newName);
            context.commitChanges();
        }
    }

    @Override
    public void deleteProduct(Integer id) {
        ObjectContext context = cayenneRuntime.newContext();
        DataObject cayenneProduct = (DataObject) Cayenne.objectForPK(context, "Product", id);
        if (cayenneProduct != null) {
            context.deleteObject(cayenneProduct);
            context.commitChanges();
        }
    }

    @Override
    public List<Product> findProductsByProducerCountry(String country) {
        ObjectContext context = cayenneRuntime.newContext();

        List<DataObject> results = ObjectSelect.query(DataObject.class, "Product")
                .where(ExpressionFactory.exp("producer.country = $country", country))
                .select(context);

        List<Product> mappedResults = new ArrayList<>();
        for(DataObject row : results) {
            Product p = new Product();
            p.setId(Cayenne.intPKForObject(row));
            p.setName((String) row.readProperty("name"));
            p.setPrice((java.math.BigDecimal) row.readProperty("price"));
            mappedResults.add(p);
        }
        return mappedResults;
    }

    @Override
    public List<Producer> findProducersWithTopReviews(Integer rating) {
        ObjectContext context = cayenneRuntime.newContext();

        List<DataObject> results = ObjectSelect.query(DataObject.class, "Producer")
                .where(ExpressionFactory.exp("products.reviews.rating = $rating", rating))
                .distinct()
                .select(context);

        List<Producer> mappedResults = new ArrayList<>();
        for(DataObject row : results) {
            Producer p = new Producer();
            p.setId(Cayenne.intPKForObject(row));
            p.setName((String) row.readProperty("name"));
            p.setCountry((String) row.readProperty("country"));
            mappedResults.add(p);
        }
        return mappedResults;
    }

    @Override
    public long countReviewsNPlusOne() {
        org.apache.cayenne.ObjectContext context = cayenneRuntime.newContext();
        List<org.apache.cayenne.DataObject> products = org.apache.cayenne.query.ObjectSelect.query(org.apache.cayenne.DataObject.class, "Product")
                .where(org.apache.cayenne.exp.ExpressionFactory.exp("db:id <= " + PRODUCTS_REVIEWS_JOIN_COUNT_EAGER_LAZY))
                .select(context);

        long count = 0;
        for (org.apache.cayenne.DataObject p : products) {
            List<?> reviews = (List<?>) p.readProperty("reviews");
            count += reviews.size();
        }
        return count;
    }

    @Override
    public long countReviewsJoinFetch() {
        org.apache.cayenne.ObjectContext context = cayenneRuntime.newContext();
        List<org.apache.cayenne.DataObject> products = org.apache.cayenne.query.ObjectSelect.query(org.apache.cayenne.DataObject.class, "Product")
                .where(org.apache.cayenne.exp.ExpressionFactory.exp("db:id <= " + PRODUCTS_REVIEWS_JOIN_COUNT_EAGER_LAZY))
                .prefetch("reviews", org.apache.cayenne.query.PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS)
                .select(context);

        long count = 0;
        for (org.apache.cayenne.DataObject p : products) {
            List<?> reviews = (List<?>) p.readProperty("reviews");
            count += reviews.size();
        }
        return count;
    }

    @Override
    public void insertBatched(List<Product> products, int batchSize) {
        org.apache.cayenne.ObjectContext context = cayenneRuntime.newContext();
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            org.apache.cayenne.CayenneDataObject cayenneProduct = new org.apache.cayenne.CayenneDataObject();
            cayenneProduct.setObjectId(org.apache.cayenne.ObjectId.of("Product"));
            context.registerNewObject(cayenneProduct);
            cayenneProduct.writeProperty("name", p.getName());
            cayenneProduct.writeProperty("price", p.getPrice());

            if (i > 0 && (i + 1) % batchSize == 0) {
                context.commitChanges();
            }
        }
        if (context.hasChanges()) {
            context.commitChanges();
        }
    }

    @Override
    public void bulkUpdateJpql(Integer producerId) {
        ObjectContext context = cayenneRuntime.newContext();
        SQLExec.query("UPDATE product SET price = price * 1.1 WHERE producer_id = " + producerId).execute(context);
    }

    @Override
    public void bulkUpdateInLoop(Integer producerId) {
        ObjectContext context = cayenneRuntime.newContext();
        List<DataObject> products = ObjectSelect.query(DataObject.class, "Product")
                .where(ExpressionFactory.exp("producer.id = $id", producerId))
                .select(context);

        for (DataObject p : products) {
            BigDecimal oldPrice = (BigDecimal) p.readProperty("price");
            p.writeProperty("price", oldPrice.multiply(new BigDecimal("1.1")));
        }
        context.commitChanges();
    }

    private CayenneDataObject saveProducer(com.sladamos.model.Producer p, ObjectContext context) {
        CayenneDataObject cayenneProducer = new CayenneDataObject();
        cayenneProducer.setObjectId(ObjectId.of("Producer"));
        context.registerNewObject(cayenneProducer);

        cayenneProducer.writeProperty("name", p.getName());
        cayenneProducer.writeProperty("country", p.getCountry());
        return cayenneProducer;
    }

    private CayenneDataObject saveProduct(Product p, ObjectContext context) {
        CayenneDataObject cayenneProduct = new CayenneDataObject();
        cayenneProduct.setObjectId(ObjectId.of("Product"));
        context.registerNewObject(cayenneProduct);

        cayenneProduct.writeProperty("name", p.getName());
        cayenneProduct.writeProperty("price", p.getPrice());

        if (p.getProducer() != null && p.getProducer().getId() != null) {
            DataObject dbProducer = (DataObject) Cayenne.objectForPK(context, "Producer", p.getProducer().getId());
            cayenneProduct.setToOneTarget("producer", dbProducer, true);
        }
        return cayenneProduct;
    }

    private CayenneDataObject saveReview(Review r, ObjectContext context) {
        CayenneDataObject cayenneReview = new CayenneDataObject();
        cayenneReview.setObjectId(ObjectId.of("Review"));
        context.registerNewObject(cayenneReview);

        cayenneReview.writeProperty("firstName", r.getFirstName());
        cayenneReview.writeProperty("lastName", r.getLastName());
        cayenneReview.writeProperty("rating", r.getRating());
        cayenneReview.writeProperty("content", r.getContent());

        if (r.getProduct() != null && r.getProduct().getId() != null) {
            DataObject dbProduct = (DataObject) Cayenne.objectForPK(context, "Product", r.getProduct().getId());
            cayenneReview.setToOneTarget("product", dbProduct, true);
        }
        return cayenneReview;
    }
}