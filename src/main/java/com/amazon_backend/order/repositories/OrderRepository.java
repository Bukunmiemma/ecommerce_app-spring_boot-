package com.amazon_backend.order.repositories;

import com.amazon_backend.auth.entity.User;
import com.amazon_backend.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // Find all orders belonging to this user and return the newest ones first
 List<Order> findByUserOrderByCreatedAtDesc(User user);


    Optional<Order> findByIdAndUser (Long id, User user);
}
