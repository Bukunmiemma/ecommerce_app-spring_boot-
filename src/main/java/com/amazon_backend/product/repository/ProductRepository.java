package com.amazon_backend.product.repository;

import com.amazon_backend.product.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

    public interface ProductRepository extends JpaRepository<Product, Long> {

        Optional<Product> findByName(String name);

        boolean existsByName(String name);

        //This is the solution to the concurrency problem
        //The DB prevents 2 people from buying when one product is left
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT p FROM Product p WHERE")
        Optional<Product> findByIdForUpdate(@Param("id") Long id);

    }