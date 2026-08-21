package com.deep.ordexaflow.cart.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.deep.ordexaflow.cart.api.AddCartItemRequest;
import com.deep.ordexaflow.cart.api.CartResponse;
import com.deep.ordexaflow.cart.api.UpdateCartItemRequest;
import com.deep.ordexaflow.cart.domain.Cart;
import com.deep.ordexaflow.cart.domain.CartItem;
import com.deep.ordexaflow.cart.infrastructure.CartRepository;
import com.deep.ordexaflow.catalog.domain.Product;
import com.deep.ordexaflow.catalog.infrastructure.ProductRepository;
import com.deep.ordexaflow.common.exception.InvalidRequestException;
import com.deep.ordexaflow.common.exception.ResourceNotFoundException;
import com.deep.ordexaflow.users.domain.User;
import com.deep.ordexaflow.users.infrastructure.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {
    private static final String CART_CURRENCY = "USD";

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public CartService(CartRepository cartRepository, ProductRepository productRepository,
            UserRepository userRepository, Clock clock) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @PreAuthorize("hasRole('USER')")
    @Transactional(readOnly = true)
    public CartResponse getCart(UUID userId) {
        return cartRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseGet(() -> CartResponse.empty(CART_CURRENCY));
    }

    @PreAuthorize("hasRole('USER')")
    @Transactional
    public CartResponse addItem(UUID userId, AddCartItemRequest request) {
        Product product = findSellableProduct(request.productId());
        validateCurrency(product);
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> new Cart(findUser(userId), CART_CURRENCY, clock.instant()));
        long desiredQuantity = cart.findItemByProductId(product.getId())
                .map(item -> (long) item.getQuantity() + request.quantity())
                .orElse((long) request.quantity());
        validateInventory(product, desiredQuantity);
        if (desiredQuantity > Integer.MAX_VALUE) {
            throw new InvalidRequestException("Cart item quantity is too large");
        }
        cart.setProductQuantity(product, (int) desiredQuantity, clock.instant());
        return toResponse(cartRepository.save(cart));
    }

    @PreAuthorize("hasRole('USER')")
    @Transactional
    public CartResponse updateItem(UUID userId, UUID itemId, UpdateCartItemRequest request) {
        Cart cart = findCart(userId);
        CartItem item = cart.findItemById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", itemId));
        Product product = item.getProduct();
        ensureSellable(product);
        validateCurrency(product);
        validateInventory(product, request.quantity());
        cart.changeQuantity(itemId, request.quantity(), clock.instant());
        return toResponse(cartRepository.save(cart));
    }

    @PreAuthorize("hasRole('USER')")
    @Transactional
    public void removeItem(UUID userId, UUID itemId) {
        Cart cart = findCart(userId);
        if (!cart.removeItem(itemId, clock.instant())) {
            throw new ResourceNotFoundException("Cart item", itemId);
        }
    }

    @PreAuthorize("hasRole('USER')")
    @Transactional
    public void clear(UUID userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> cart.clear(clock.instant()));
    }

    private Cart findCart(UUID userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", userId));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private Product findSellableProduct(UUID productId) {
        Product product = productRepository.findOneById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        ensureSellable(product);
        return product;
    }

    private void ensureSellable(Product product) {
        if (!product.isActive() || !product.getCategory().isActive()) {
            throw new ResourceNotFoundException("Product", product.getId());
        }
    }

    private void validateCurrency(Product product) {
        if (!CART_CURRENCY.equals(product.getCurrency())) {
            throw new CartCurrencyMismatchException(CART_CURRENCY, product.getCurrency());
        }
    }

    private void validateInventory(Product product, long requestedQuantity) {
        int available = product.getInventory().getQuantity();
        if (requestedQuantity > available) {
            throw new InsufficientInventoryException(product.getId(), requestedQuantity, available);
        }
    }

    private CartResponse toResponse(Cart cart) {
        cart.getItems().forEach(item -> validateCurrency(item.getProduct()));
        return CartResponse.from(cart);
    }
}
