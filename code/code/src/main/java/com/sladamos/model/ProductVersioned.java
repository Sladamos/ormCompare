package com.sladamos.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "product_versioned")
public class ProductVersioned {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "prod_ver_seq")
    @SequenceGenerator(name = "prod_ver_seq", sequenceName = "product_versioned_id_seq", allocationSize = 100)
    private Integer id;

    @Column(nullable = false)
    private String name;

    private BigDecimal price;

    @Version
    private Integer version;

    @Transient
    private Object[] dnDetachedState;

    @Transient
    private Object[] dnStateManager;

    public ProductVersioned() {}

    public ProductVersioned(String name, BigDecimal price) {
        this.name = name;
        this.price = price;
    }

    public ProductVersioned(Integer id, String name, BigDecimal price) {
        this.name = name;
        this.price = price;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getVersion() {
        return version;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}