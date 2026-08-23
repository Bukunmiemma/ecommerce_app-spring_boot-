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
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartServiceImpl(CartRepository cartRepository,
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
                .orElseThrow(() -> new RuntimeException(("User not found"))
                );
    }
// Get or create cart

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    return cartRepository.save(cart);
                });
    }

    // Add to cart
    @Override
    @Transactional
    public CartResponse addToCart (String email, AddToCartRequest request){
        User user = getUserByEmail(email);
         Cart cart = getOrCreateCart(user);

         Product product = productRepository.findById(request.getProductId())
                 .orElseThrow(()-> new ProductNotFoundException("Product not found with id: "
                 + request.getProductId()));

         CartItem cartItem = cartItemRepository
                 .findByCartAndProduct(cart, product)
                         .orElse(null);

         if(cartItem != null) {
             int newQuantity = cartItem.getQuantity() + request.getQuantity();
             if (newQuantity > product.getStockQuantity()) {
                 throw new RuntimeException("Insufficient stock. Available stock: "
                         + product.getStockQuantity());
             }
             cartItem.setQuantity(newQuantity);
             cartItemRepository.save(cartItem);
         }else{
             if(request.getQuantity() > product.getStockQuantity()){
                 throw new RuntimeException("Insuffient stock. Available stock: " +
                         product.getStockQuantity()
                         );
             }

             CartItem newCartItem = new CartItem();
             newCartItem.setCart(cart);
             newCartItem.setProduct(product);
             newCartItem.setQuantity(request.getQuantity());
             cart.getItems().add(newCartItem);
             cartItemRepository.save(newCartItem);
         }
         return buildCartResponse(cart);

    }

    private CartResponse buildCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream().map(this::mapCartItem)
                .collect(Collectors.toList());
        BigDecimal total = itemResponses.stream().
                map(CartItemResponse::getSubtotal).reduce(BigDecimal.ZERO,
                        BigDecimal::add);
        CartResponse response = new CartResponse();
        response.setCartId(cart.getId());
        response.setItems(itemResponses);
        response.setTotal(total);
        return response;

    }

    private CartItemResponse mapCartItem(CartItem item) {

        Product product = item.getProduct();
        BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        CartItemResponse response = new CartItemResponse();
        response.setId(product.getId());
        response.setProductId(product.getId());
        response.setProductName(product.getName());
        response.setPrice(product.getPrice());
        response.setQuantity(item.getQuantity());
        response.setSubtotal(subtotal);
        return response;
    }



    //GET CART

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(String email) {
        User user = getUserByEmail(email);
        Cart cart = getOrCreateCart(user);
        return buildCartResponse(cart);

    }

    // Update cart item
    @Override
    @Transactional
    public CartResponse updateCartItem(String email, Long itemId,
                                       UpdateCartItemRequest request) {
        User user = getUserByEmail(email);
        Cart cart = getOrCreateCart(user);
        CartItem cartItem =
        cartItemRepository.findById(itemId).orElseThrow(
                () -> new RuntimeException("Cart Item not found"));
        //Security check
        //Make sure the item belongs to the logged-in user's cart
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("You cannot modify this cartItem");
        }
        Product product = cartItem.getProduct();
        if (request.getQuantity() > product.getStockQuantity()) {
            throw new IllegalArgumentException("Insufficient stock. Available stock: "
                    + product.getStockQuantity());
        }
        cartItem.setQuantity(request.getQuantity());

        cartItemRepository.save(cartItem);
        return  buildCartResponse(cart);

    }

// Remove cart item
    @Override
    @Transactional
    public void removeCartItem (String email, Long itemId){
        User user = getUserByEmail(email);

        Cart cart = getOrCreateCart(user);
        CartItem cartItem = cartItemRepository.findById(itemId).orElseThrow(
                ()->new RuntimeException("Cart item not found"));

        //Security check
        if(!cartItem.getCart().getId().equals(cart.getId())){
            throw new RuntimeException("You cannot remove this cart item");
        }
        cartItemRepository.delete(cartItem);
    }

     // Clear cart
    @Override
    @Transactional
    public void clearCart (String email){
        User user = getUserByEmail(email);
        Cart cart = getOrCreateCart(user);
        List<CartItem>items =
        cartItemRepository.findAll();
        for (CartItem item : items ){
            if(item.getCart().getId().equals(cart.getId())){
                cartItemRepository.delete(item);
            }

            }
        }
    }





