package com.sladamos.repository;

import com.sladamos.model.Producer;
import com.sladamos.model.Product;
import com.sladamos.model.Review;
import org.apache.cayenne.*;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.query.SQLExec;

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

        switch (o) {
            case Producer p -> saveProducer(p, context);
            case Product p -> saveProduct(p, context);
            case Review r -> saveReview(r, context);
            default -> throw new IllegalArgumentException("Cayenne nie obsługuje tego typu obiektu: " + o.getClass().getName());
        }

        context.commitChanges();
    }

    @Override
    public void clearDatabase() {
        ObjectContext context = cayenneRuntime.newContext();
        SQLExec.query("DELETE FROM review WHERE id > 100000").execute(context);
        SQLExec.query("DELETE FROM product WHERE id > 1000").execute(context);
        SQLExec.query("DELETE FROM producer WHERE id > 10").execute(context);

        if (profile.contains("h2")) {
            SQLExec.query("ALTER TABLE producer ALTER COLUMN id RESTART WITH 11").execute(context);
            SQLExec.query("ALTER TABLE product ALTER COLUMN id RESTART WITH 1001").execute(context);
            SQLExec.query("ALTER TABLE review ALTER COLUMN id RESTART WITH 100001").execute(context);
        } else {
            SQLExec.query("ALTER SEQUENCE producer_id_seq RESTART WITH 11").execute(context);
            SQLExec.query("ALTER SEQUENCE product_id_seq RESTART WITH 1001").execute(context);
            SQLExec.query("ALTER SEQUENCE review_id_seq RESTART WITH 100001").execute(context);
        }
    }

    private void saveProducer(com.sladamos.model.Producer p, ObjectContext context) {
        CayenneDataObject cayenneProducer = new CayenneDataObject();
        cayenneProducer.setObjectId(ObjectId.of("Producer"));
        context.registerNewObject(cayenneProducer);

        cayenneProducer.writeProperty("name", p.getName());
        cayenneProducer.writeProperty("country", p.getCountry());
    }

    private void saveProduct(Product p, ObjectContext context) {
        CayenneDataObject cayenneProduct = new CayenneDataObject();
        cayenneProduct.setObjectId(ObjectId.of("Product"));
        context.registerNewObject(cayenneProduct);

        cayenneProduct.writeProperty("name", p.getName());
        cayenneProduct.writeProperty("price", p.getPrice());

        if (p.getProducer() != null && p.getProducer().getId() != null) {
            DataObject dbProducer = (DataObject) Cayenne.objectForPK(context, "Producer", p.getProducer().getId());
            cayenneProduct.setToOneTarget("producer", dbProducer, true);
        }
    }

    private void saveReview(Review r, ObjectContext context) {
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
    }
}