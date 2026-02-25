package com.sladamos.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producer_id")
    private Producer producer;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<Review> reviews;

    @Transient
    private Object[] dnDetachedState;

    @Transient
    private Object[] dnStateManager;

    public Product() {}

    public Product(String name, BigDecimal price, Producer producer) {
        this.name = name;
        this.price = price;
        this.producer = producer;
    }

    public Product(Integer id, String name, BigDecimal price, Producer producer, List<Review> reviews) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.producer = producer;
        this.reviews = reviews;
    }

    public Integer getId() { return id; }

    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }

    public void setPrice(BigDecimal price) { this.price = price; }

    public Producer getProducer() { return producer; }

    public void setProducer(Producer producer) { this.producer = producer; }

    public List<Review> getReviews() { return reviews; }

    public void setReviews(List<Review> reviews) { this.reviews = reviews; }
}