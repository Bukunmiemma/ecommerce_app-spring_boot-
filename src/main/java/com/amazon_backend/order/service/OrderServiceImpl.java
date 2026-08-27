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
import com.amazon_backend.order.repositories.OrderItemRepository;
import com.amazon_backend.order.repositories.OrderRepository;
import com.amazon_backend.product.entity.Product;
import com.amazon_backend.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService{
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderItemRepository orderItemRepository,
                            CartRepository cartRepository,
                            UserRepository userRepository,
                            ProductRepository productRepository,
                            CartItemRepository cartItemRepository

    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;

    }


    private OrderItemResponse mapToOrderItemResponse (OrderItem item){
        OrderItemResponse response = new OrderItemResponse();
        response.setId(item.getId());
        response.setProductId(item.getProduct().getId());
        response.setQuantity(item.getQuantity());
        response.setPrice(item.getPrice());
        response.setSubtotal(item.getSubtotal());
        return response;
        // we are using OrderItem.price which is this response.setPrice(item.getPrice());
        // not item.getProduct().getPrice()
    }
    private  OrderResponse mapToOrderResponse(Order order){
        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getId());
        response.setStatus(order.getStatus());
        response.setTotalAmount(order.getTotalAmount());
        response.setCreatedAt(order.getCreatedAt());
        List<OrderItemResponse> items = order.getItems()
                .stream()
                .map(this::mapToOrderItemResponse)
                .collect(Collectors.toList());
        response.setItems(items);
        return response;
    }
    //@Transactional means all the database operations belong together,
    //either the whole business operation succeeds or the database should roll back the charges

    //Override tells java this method is implementing/replacing a method that was already
    //declared in my parent class or interface. It also allows java/intellij catch mistakesz
    @Override
    @Transactional
    public OrderResponse checkout(String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow( ()-> new RuntimeException("User not found"));

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow( ()-> new RuntimeException("Cart not found"));

        if(cart.getItems().isEmpty()){
            throw new RuntimeException("Cart is empty");
        }
         Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        BigDecimal totalAmount =  BigDecimal.ZERO;

        // findByIdForUpdate is the concurrency protection
        //The DB lock ensures two transactions occuring to purchase one item left
        //don't both successfully manipulate that same stock value at the same time
        for (CartItem cartItem : cart.getItems()){
            Product product = productRepository.findByIdForUpdate(
                    cartItem.getProduct().getId()).orElseThrow(
                    ()-> new RuntimeException("Product not found")
            );
             Integer  quantity = cartItem.getQuantity();
             if (product.getStockQuantity() < quantity){
                 throw new RuntimeException(" Insufficient stock for product: "
                 + product.getName() + "Available stock: " + product.getStockQuantity()
                 );
             }

             BigDecimal price = product.getPrice();
             BigDecimal subtotal = price.multiply(
                     BigDecimal.valueOf(quantity)
             );
             // create order item
             OrderItem orderItem = new OrderItem();
             orderItem.setOrder(order);
             orderItem.setProduct(product);
            orderItem.setQuantity(quantity);
            //Price snapshot
            orderItem.setPrice(price);
            orderItem.setSubtotal(subtotal);
            order.getItems().add(orderItem);

             //Reduce stock
            product.setStockQuantity(product.getStockQuantity()-quantity);
            //add to order total
            totalAmount = totalAmount.add(subtotal);
        }
        order.setTotalAmount(totalAmount);
        //save order and its order items
        Order savedOrder = orderRepository.save(order);
        //clear cart
        cart.getItems().clear();
        cartRepository.save(cart);
        return mapToOrderResponse(savedOrder);

    }

    // We are deliberately passing the user's email
    // so that one user cannot have access to another user's orders
    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById (String email, Long orderId){
    User user = userRepository.findByEmail(email)
            .orElseThrow(()-> new RuntimeException("User not found"));

    Order order = orderRepository.findById(orderId)
            .orElseThrow(()-> new RuntimeException("Order not found"));
             // Security check
        if(!order.getUser().getId().equals(user.getId())){
            throw new RuntimeException("You are  not allowed to view this order");
        }
return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders (String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new RuntimeException("User not found"));
        List<Order> orders = orderRepository.findByUserOrderByCreatedAtDesc(user);
        return orders.stream().map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }
}
