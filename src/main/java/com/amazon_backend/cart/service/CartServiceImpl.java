package com.amazon_backend.cart.service;
import com.amazon_backend.auth.entity.User;
import com.amazon_backend.auth.repository.UserRepository;
import com.amazon_backend.cart.dto.AddToCartRequest;
import com.amazon_backend.cart.dto.CartItemResponse;
import com.amazon_backend.cart.dto.CartResponse;
import com.amazon_backend.cart.dto.UpdateCartItemRequest;
import com.amazon_backend.cart.entity.Cart;
import com.amazon_backend.cart.entity.CartItem;
import com.amazon_backend.cart.repository.CartItemRepository;
import com.amazon_backend.cart.repository.CartRepository;
import com.amazon_backend.product.entity.Product;
import com.amazon_backend.product.exception.ProductNotFoundException;
import com.amazon_backend.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartServiceImpl(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Cart getOrCreateCartForLockedUser(User lockedUser) {
        return cartRepository.findByUserId(lockedUser.getId())
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(lockedUser);
                    return cartRepository.saveAndFlush(cart);
                });
    }

    private Cart getOrCreateCart(User user) {
        User lockedUser = userRepository.findByIdForUpdate(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return getOrCreateCartForLockedUser(lockedUser);
    }

    private Cart getCartForUpdate(User user) {
        User lockedUser = userRepository.findByIdForUpdate(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        getOrCreateCartForLockedUser(lockedUser);

        return cartRepository.findByUserIdForUpdate(lockedUser.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));
    }
            //The user-row lock prevents two requests from creating the same user's cart simultaneously.
            //The cart-row lock protects cart mutations.
            //Both cart mutations and checkout will follow the same locking order: User → Cart.
    @Override
    @Transactional
    public CartResponse addToCart(String email, AddToCartRequest request) {

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        User user = getUserByEmail(email);
        Cart cart = getCartForUpdate(user);
        Product product = productRepository.findById(request.getProductId())
                .filter(Product::isActive)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found or unavailable"));

        if (product.getStockQuantity() == null) {
            throw new IllegalStateException("Product stock is not configured");
        }

        CartItem cartItem = cartItemRepository
                .findByCartAndProduct(cart, product)
                .orElse(null);

        int requestedQuantity = request.getQuantity();

        if (cartItem != null) {

            int newQuantity = cartItem.getQuantity() + requestedQuantity;

            if (newQuantity > product.getStockQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock. Available stock: "
                                + product.getStockQuantity());
            }

            cartItem.setQuantity(newQuantity);
            cartItemRepository.save(cartItem);

        } else {

            if (requestedQuantity > product.getStockQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock. Available stock: "
                                + product.getStockQuantity());
            }

            CartItem newCartItem = new CartItem();
            newCartItem.setCart(cart);
            newCartItem.setProduct(product);
            newCartItem.setQuantity(requestedQuantity);

            cart.getItems().add(newCartItem);
            cartItemRepository.save(newCartItem);
        }

        return buildCartResponse(cart);
    }

    private CartResponse buildCartResponse(Cart cart) {

        List<CartItemResponse> itemResponses = cart.getItems()
                .stream()
                .map(this::mapCartItem)
                .toList();

        BigDecimal total = itemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CartResponse response = new CartResponse();
        response.setCartId(cart.getId());
        response.setItems(itemResponses);
        response.setTotal(total);

        return response;
    }

    private CartItemResponse mapCartItem(CartItem item) {

        Product product = item.getProduct();

        BigDecimal subtotal = product.getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        CartItemResponse response = new CartItemResponse();
        response.setCartItemId(item.getId());
        response.setProductId(product.getId());
        response.setProductName(product.getName());
        response.setPrice(product.getPrice());
        response.setQuantity(item.getQuantity());
        response.setSubtotal(subtotal);
        response.setAvailable(product.isActive());

        return response;
    }

    @Override
    @Transactional
    public CartResponse getCart(String email) {

        User user = getUserByEmail(email);
        Cart cart = getOrCreateCart(user);

        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(
            String email,
            Long itemId,
            UpdateCartItemRequest request) {

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        User user = getUserByEmail(email);
        Cart cart = getCartForUpdate(user);

        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("You cannot modify this cart item");
        }

        Product product = cartItem.getProduct();

        if (!product.isActive()) {
            throw new IllegalArgumentException(
                    "This product is no longer available. Remove it from your cart.");
        }

        if (product.getStockQuantity() == null
                || request.getQuantity() > product.getStockQuantity()) {
            throw new IllegalArgumentException(
                    "Insufficient stock. Available stock: "
                            + product.getStockQuantity());
        }

        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);

        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    public void removeCartItem(String email, Long itemId) {

        User user = getUserByEmail(email);
        Cart cart = getCartForUpdate(user);
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("You cannot remove this cart item");
        }

        cartItemRepository.delete(cartItem);
    }

    @Override
    @Transactional
    public void clearCart(String email) {

        User user = getUserByEmail(email);
        Cart cart = getCartForUpdate(user);
        cart.getItems().clear();
        cartRepository.save(cart);
    }
}
