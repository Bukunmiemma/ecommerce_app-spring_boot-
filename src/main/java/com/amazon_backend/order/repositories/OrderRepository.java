package com.amazon_backend.order.repositories;
import com.amazon_backend.auth.entity.User;
import com.amazon_backend.order.entity.Order;
import com.amazon_backend.order.entity.OrderStatus;
import com.amazon_backend.order.entity.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserOrderByCreatedAtDesc(User user);

    Optional<Order> findByIdAndUser(Long id, User user);

    // Find unpaid orders whose payment window has expired.
    //This allows us to find up to 100 orders that:
    // Are still PENDING.
    //Have payment status UNPAID or PENDING.
    //Have passed their payment expiration time.

    List<Order> findTop100ByStatusAndPaymentStatusInAndPaymentExpiresAtBefore(
            OrderStatus status,
            Collection<PaymentStatus> paymentStatuses,
            LocalDateTime currentTime
    );

    // Lock an order before changing its payment or fulfilment state.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :orderId")
    Optional<Order> findByIdForUpdate(@Param("orderId") Long orderId);
}

//Why these methods matter
//Expired-order query: Finds up to 100 orders that are still pending payment and have passed their expiration time.

//Pessimistic order lock: Prevents two processes from simultaneously changing the same order and restoring its stock twice.

//One important distinction: finding expired orders does not itself cancel them. We'll recheck each order under its lock before changing anything.