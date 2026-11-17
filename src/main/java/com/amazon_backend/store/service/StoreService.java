package com.amazon_backend.store.service;

import com.amazon_backend.store.dto.CreateStoreRequest;
import com.amazon_backend.store.dto.StoreResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StoreService {

    StoreResponse createStore(
            String email,
            CreateStoreRequest request
    );

    List<StoreResponse> getMyStores(String email);
    List<StoreResponse> getActiveStores();

    StoreResponse getActiveStoreById(Long storeId);

    Page<StoreResponse> searchActiveStores(
            String email,
            String keyword,
            Pageable pageable
    );
}
