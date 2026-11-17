package com.amazon_backend.store.controller;

import com.amazon_backend.store.dto.CreateStoreRequest;
import com.amazon_backend.store.dto.StoreResponse;
import com.amazon_backend.store.service.StoreService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @GetMapping
    public ResponseEntity<List<StoreResponse>> getActiveStores() {

        return ResponseEntity.ok(storeService.getActiveStores());
    }

    @GetMapping("/{storeId}")
    public ResponseEntity<StoreResponse> getActiveStoreById(
            @PathVariable Long storeId
    ) {

        return ResponseEntity.ok(
                storeService.getActiveStoreById(storeId)
        );
    }

    @GetMapping("/search")
    public ResponseEntity<Page<StoreResponse>> searchStores(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                storeService.searchActiveStores(
                        authentication.getName(),
                        q,
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(Sort.Direction.ASC, "name")
                        )
                )
        );
    }
}
