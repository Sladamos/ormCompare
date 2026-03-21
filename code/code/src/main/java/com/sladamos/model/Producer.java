package com.sladamos.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "producer")
public class Producer {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "producer_seq")
    @SequenceGenerator(name = "producer_seq", sequenceName = "producer_id_seq", allocationSize = 100)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String country;

    @OneToMany(mappedBy = "producer")
    private List<Product> products;

    @Transient
    private Object[] dnDetachedState;

    @Transient
    private Object[] dnStateManager;

    public Producer() {}

    public Producer(Integer id) {
        this.id = id;
    }

    public Producer(Integer id, String name, String country, List<Product> products) {
        this.id = id;
        this.name = name;
        this.country = country;
        this.products = products;
    }

    public Integer getId() { return id; }

    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getCountry() { return country; }

    public void setCountry(String country) { this.country = country; }

    public List<Product> getProducts() { return products; }

    public void setProducts(List<Product> products) { this.products = products; }
}