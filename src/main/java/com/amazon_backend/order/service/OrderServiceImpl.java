package com.amazon_backend.order.service;
import com.amazon_backend.auth.entity.User;
import com.amazon_backend.auth.repository.UserRepository;
import com.amazon_backend.cart.entity.Cart;
import com.amazon_backend.cart.entity.CartItem;
import com.amazon_backend.cart.repository.CartItemRepository;
import com.amazon_backend.cart.repository.CartRepository;
import com.amazon_backend.order.dto.OrderItemResponse;
import com.amazon_backend.order.dto.OrderResponse;
import com.amazon_backend.order.entity.Order;
import com.amazon_backend.order.entity.OrderItem;
import com.amazon_backend.order.entity.OrderStatus;
import com.amazon_backend.order.entity.PaymentStatus;
import com.amazon_backend.order.repositories.OrderItemRepository;
import com.amazon_backend.order.repositories.OrderRepository;
import com.amazon_backend.product.entity.Product;
import com.amazon_backend.product.exception.ForbiddenException;
import com.amazon_backend.product.repository.ProductRepository;
import com.amazon_backend.store.entity.StoreStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            CartRepository cartRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            CartItemRepository cartItemRepository) {

        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
    }

    private OrderItemResponse mapToOrderItemResponse(OrderItem item) {

        OrderItemResponse response = new OrderItemResponse();
        response.setOrderItemId(item.getId());
        response.setProductId(item.getProduct().getId());
        response.setProductName(item.getProduct().getName());
        response.setQuantity(item.getQuantity());
        response.setPrice(item.getPrice());
        response.setSubtotal(item.getSubtotal());
        return response;
    }

    private OrderResponse mapToOrderResponse(Order order) {

        OrderResponse response = new OrderResponse();

        response.setOrderId(order.getId());
        response.setStatus(order.getStatus());
        response.setTotalAmount(order.getTotalAmount());
        response.setCreatedAt(order.getCreatedAt());

        List<OrderItemResponse> items = order.getItems()
                .stream()
                .map(this::mapToOrderItemResponse)
                .toList();

        response.setItems(items);

        return response;
    }

    @Override
    @Transactional
    public OrderResponse checkout(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        User lockedUser = userRepository.findByIdForUpdate(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!lockedUser.isEmailVerified()) {

            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Please verify your email before placing an order"
            );
        }
        Cart cart = cartRepository.findByUserIdForUpdate(lockedUser.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        Order order = new Order();
//This gives customers 15 minutes to complete payment.
// our stock is deducted during checkout. Therefore, the expiration process must restore that stock exactly once if payment genuinely fails or expires.
        LocalDateTime now = LocalDateTime.now();

        order.setUser(lockedUser);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setCreatedAt(now);
        order.setPaymentExpiresAt(now.plusMinutes(15));

    //Your checkout now:
        // - Reserves product stock.
        //- Creates an order with PENDING status.
        //- Initializes payment as UNPAID.
        //- Sets a 15-minute payment window.
        //- Preserves the purchase price in OrderItem.

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<CartItem> cartItems = cart.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getProduct().getId()))
                .toList();
        for (CartItem cartItem : cartItems) {

            Integer quantity = cartItem.getQuantity();

            if (quantity == null || quantity <= 0) {
                throw new IllegalArgumentException(
                        "Invalid quantity in cart");
            }

            Product product = productRepository.findByIdForUpdate(
                            cartItem.getProduct().getId())
                    .orElseThrow(() ->
                            new RuntimeException("Product not found"));

            if (!product.isActive()
                    || product.getStore() == null
                    || product.getStore().getStatus() != StoreStatus.ACTIVE) {

                throw new IllegalArgumentException(
                        "Product or its store is no longer available: "
                                + product.getName());
            }

            if (product.getStockQuantity() == null
                    || product.getStockQuantity() < quantity) {

                throw new IllegalArgumentException(
                        "Insufficient stock for product: "
                                + product.getName()
                                + ". Available stock: "
                                + product.getStockQuantity());
            }

            BigDecimal price = product.getPrice();

            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException(
                        "Invalid price configured for product: "
                                + product.getName());
            }

            BigDecimal subtotal = price.multiply(
                    BigDecimal.valueOf(quantity));

            OrderItem orderItem = new OrderItem();

            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(quantity);

            // Preserve the price at the time of purchase.
            orderItem.setPrice(price);
            orderItem.setSubtotal(subtotal);

            order.getItems().add(orderItem);

            // Reduce available stock.
            product.setStockQuantity(
                    product.getStockQuantity() - quantity);

            totalAmount = totalAmount.add(subtotal);
        }

        order.setTotalAmount(totalAmount);

        // Save order and associated order items.
        Order savedOrder = orderRepository.save(order);

        // Remove purchased items from cart.
        cart.getItems().clear();

        cartRepository.save(cart);

        return mapToOrderResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(String email, Long orderId) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException(
                    "You are not allowed to view this order");
        }

        return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Order> orders =
                orderRepository.findByUserOrderByCreatedAtDesc(user);

        return orders.stream()
                .map(this::mapToOrderResponse)
                .toList();
    }
}

//User locking: Prevents simultaneous cart creation/mutation conflicts.
//Cart locking: Prevents checkout and cart updates from interfering.
//Product locking order: Reduces the risk of database deadlocks during concurrent checkouts.
//Your checkout now:
//- Reserves product stock.
//- Creates an order with PENDING status.
//- Initializes payment as UNPAID.
//- Sets a 15-minute payment window.
//- Preserves the purchase price in OrderItem.