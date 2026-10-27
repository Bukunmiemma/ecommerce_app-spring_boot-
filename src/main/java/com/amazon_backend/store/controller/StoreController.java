package com.amazon_backend.store.controller;

import com.amazon_backend.store.dto.CreateStoreRequest;
import com.amazon_backend.store.dto.StoreResponse;
import com.amazon_backend.store.service.StoreService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
public class StoreController {
    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoreResponse createStore(
            @Valid @RequestBody CreateStoreRequest request,
            Authentication authentication
    ) {
        return storeService.createStore(
                authentication.getName(),
                request
        );
    }

    @GetMapping("/getStores")
    public List<StoreResponse> getMyStores(
            Authentication authentication
    ) {
        return storeService.getMyStores(
                authentication.getName()
        );
    }
}
