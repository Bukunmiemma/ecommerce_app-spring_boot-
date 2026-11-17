package com.amazon_backend.store.repositories;
import com.amazon_backend.store.entity.Store;
import com.amazon_backend.store.entity.StoreStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
    SELECT s
    FROM Store s
    WHERE s.status = :status
    AND (
        LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(COALESCE(s.description, ''))
            LIKE LOWER(CONCAT('%', :keyword, '%'))
    )
    """)
    Page<Store> searchActiveStores(
            @Param("keyword") String keyword,
            @Param("status") StoreStatus status,
            Pageable pageable
    );

}

