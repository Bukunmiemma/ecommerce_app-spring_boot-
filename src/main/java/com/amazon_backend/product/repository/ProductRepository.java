package com.amazon_backend.product.repository;

import com.amazon_backend.product.entity.Product;
import com.amazon_backend.store.entity.StoreStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

    public interface ProductRepository extends JpaRepository<Product, Long> {

        Optional<Product> findByName(String name);

        boolean existsByName(String name);

        List<Product> findByStoreId(Long storeId);

        Optional<Product> findByIdAndStoreId(Long id, Long storeId);

       //we check whether that name exists inside a particular store:
        //boolean existsByNameAndStoreId(Long storeId, String name);

        boolean existsByNameAndStoreId(String name, Long storeId);

        Optional<Product> findByNameAndStoreId(String name, Long storeId);

        List<Product> findByActiveTrue();

        List<Product> findByStoreIdAndActiveTrue(Long storeId);

       // It checks two things:
        //Does the product exist?
        //Does the store associated with that product belong to the authenticated user?
        //If either condition fails, the product cannot be accessed through this ownership check.
        //Why this matters, Imagine Vendor A owns Store 1.
        //Vendor B owns Store 2.
        //Product 10 belongs to Vendor B.
        //Vendor A must not be able to edit or delete Product 10 simply because they know its ID.
        Optional<Product> findByIdAndStoreOwnerEmail(Long id, String email);

        //This is the solution to the concurrency problem
        //The DB prevents 2 people from buying when one product is left
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT p FROM Product p WHERE p.id = :id")
        Optional<Product> findByIdForUpdate(@Param("id") Long id);

        //We are retaining findByIdForUpdate() because it protects stock updates during checkout.


        List<Product> findByActiveTrueAndStore_Status(StoreStatus status);

        Optional<Product> findByIdAndActiveTrueAndStore_Status(
                Long id,
                StoreStatus status
        );

        List<Product> findByStore_IdAndActiveTrueAndStore_Status(
                Long storeId,
                StoreStatus status
        );

        @Query("""
    SELECT DISTINCT p
    FROM Product p
    LEFT JOIN p.categories c
    WHERE p.active = true
    AND p.store.status = :status
    AND (
        LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(COALESCE(p.description, ''))
            LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
    )
    """)
        Page<Product> searchActiveProducts(
                @Param("keyword") String keyword,
                @Param("status") StoreStatus status,
                Pageable pageable
        );
    }