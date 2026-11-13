package com.amazon_backend.product.entity;

import com.amazon_backend.category.entity.Category;
import com.amazon_backend.store.entity.Store;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "product",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_store_name",
                        columnNames = {"store_id", "name"}
                ) })
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;


    private String description;


    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;



    @Column(nullable = false)
    private Integer stockQuantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    //ManyToOne: Many products can belong to one store.
    //LAZY: Don't automatically fetch the complete Store object whenever you retrieve a Product.
    //store_id: Create a foreign-key relationship to the stores table.
   // nullable = false: Every product must belong to a store.

    @ManyToMany
    @JoinTable(
            name = "product_categories",
            joinColumns =  @JoinColumn(name = "product_id"),
            inverseJoinColumns =
                    @JoinColumn(name ="category_id")
    )
    private Set<Category> categories = new HashSet<>();

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Set<Category> getCategories() {
        return categories;
    }

    public void setCategories(Set<Category> categories) {
        this.categories = categories;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}