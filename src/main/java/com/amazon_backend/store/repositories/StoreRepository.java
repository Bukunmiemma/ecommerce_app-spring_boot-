package com.amazon_backend.store.repositories;

import com.amazon_backend.store.entity.Store;
import com.amazon_backend.store.entity.StoreStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

        List<Store> findByOwnerId(Long ownerId);


    //Spring Data JPA interprets this method name and generates a database query that checks:
    //Does this store exist with the supplied ID?
    //Does its owner have the authenticated user's email?
    //Both conditions must be true.
    //For example:
    //Suppose Vendor A owns Store 1, while Vendor B owns Store 2.
    //If Vendor A attempts to create a product under Store 2, the ownership check will reject the request.

    Optional<Store> findByIdAndOwnerEmail(Long id, String email);
    List<Store> findByStatus(StoreStatus status);

    Optional<Store> findByIdAndStatus(Long id, StoreStatus status);

}

