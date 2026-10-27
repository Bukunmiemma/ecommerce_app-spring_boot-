package com.amazon_backend.store.service;

import com.amazon_backend.store.dto.CreateStoreRequest;
import com.amazon_backend.store.dto.StoreResponse;

import java.util.List;

public interface StoreService {
    StoreResponse createStore(
            String email,
            CreateStoreRequest request
    );

    List<StoreResponse> getMyStores(String email);

}
