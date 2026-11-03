package com.amazon_backend.store.service;

import com.amazon_backend.auth.entity.User;
import com.amazon_backend.auth.repository.UserRepository;
import com.amazon_backend.store.dto.CreateStoreRequest;
import com.amazon_backend.store.dto.StoreResponse;
import com.amazon_backend.store.entity.Store;
import com.amazon_backend.store.repositories.StoreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class StoreServiceImpl implements StoreService {
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    public StoreServiceImpl(
            StoreRepository storeRepository,
            UserRepository userRepository
    ) {
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public StoreResponse createStore(
            String email,
            CreateStoreRequest request
    ) {
        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User account not found"
                ));

        Store store = new Store();
        store.setName(request.getName().trim());
        store.setDescription(request.getDescription());
        store.setOwner(owner);

        // The Store entity supplies the default 5% commission
        // and ACTIVE status. Clients cannot set these fields.

        Store savedStore = storeRepository.save(store);

        return mapToResponse(savedStore);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreResponse> getMyStores(String email) {

        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User account not found"
                ));

        return storeRepository.findByOwnerId(owner.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private StoreResponse mapToResponse(Store store) {

        StoreResponse response = new StoreResponse();

        response.setStoreId(store.getId());
        response.setName(store.getName());
        response.setDescription(store.getDescription());
        response.setOwnerId(store.getOwner().getId());
        response.setCommissionRate(store.getCommissionRate());
        response.setStatus(store.getStatus());

        return response;
    }

}
