package com.amazon_backend.cart.service;
import com.amazon_backend.auth.entity.User;
import com.amazon_backend.auth.repository.UserRepository;
import com.amazon_backend.cart.dto.AddToCartRequest;
import com.amazon_backend.cart.dto.CartResponse;
import com.amazon_backend.cart.entity.Cart;
import com.amazon_backend.cart.entity.CartItem;
import com.amazon_backend.cart.repository.CartItemRepository;
import com.amazon_backend.cart.repository.CartRepository;
import com.amazon_backend.product.entity.Product;
import com.amazon_backend.product.exception.ProductNotFoundException;
import com.amazon_backend.product.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository  userRepository;

    public CartServiceImpl(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           ProductRepository productRepository,
                           UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }
  private User getUserByEmail (String email){
        return userRepository.findByEmail(email)
                .orElseThrow(()-> new RuntimeException(("User not found"))
                );
  }


    private Cart getOrCreateCart(User user){
        return cartRepository.findByUserId(user.getId())
                .orElseGet( ()->{
                    Cart cart = new Cart();
                cart.setUser(user);
                return  cartRepository.save(cart);
                } );
    }

    @Override
    @Transactional
    public  CartResponse addToCart(
        String email,
                AddToCartRequest request){
        User user = getUserByEmail(email);

        Cart  cart = getOrCreateCart(user);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(()-> new ProductNotFoundException("Product not found"));
        CartItem existingItem = cartItemRepository
                .findByCartAndProduct(cart, product).orElse(null);
        int newQuantity;
        if(existingItem != null){
            newQuantity =  existingItem.getQuantity() + request.getQuantity();
        }else{
             newQuantity = request.getQuantity();
        }


        //Check available stock
        if (newQuantity > product.getStockQuantity()) {
            throw new IllegalArgumentException("Insufficient stock. Available stock: "
                    + product.getStockQuantity());
        }
        if(existingItem != null){
            existingItem.setQuantity(newQuantity);

        }else{
            CartItem cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setQuantity(request.getQuantity());
            cartItemRepository.save(cartItem);
        }
        return buildCartResponse(cart);
    }

}
