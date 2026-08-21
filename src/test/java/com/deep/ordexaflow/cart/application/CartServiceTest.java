package com.deep.ordexaflow.cart.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.deep.ordexaflow.cart.api.AddCartItemRequest;
import com.deep.ordexaflow.cart.api.UpdateCartItemRequest;
import com.deep.ordexaflow.cart.domain.Cart;
import com.deep.ordexaflow.cart.infrastructure.CartRepository;
import com.deep.ordexaflow.catalog.domain.Category;
import com.deep.ordexaflow.catalog.domain.Product;
import com.deep.ordexaflow.catalog.infrastructure.ProductRepository;
import com.deep.ordexaflow.common.exception.ResourceNotFoundException;
import com.deep.ordexaflow.users.domain.Role;
import com.deep.ordexaflow.users.domain.User;
import com.deep.ordexaflow.users.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CartServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-20T22:00:00Z");

    private CartRepository cartRepository;
    private ProductRepository productRepository;
    private UserRepository userRepository;
    private CartService cartService;
    private User user;
    private Category category;

    @BeforeEach
    void setUp() {
        cartRepository = mock(CartRepository.class);
        productRepository = mock(ProductRepository.class);
        userRepository = mock(UserRepository.class);
        cartService = new CartService(
                cartRepository, productRepository, userRepository, Clock.fixed(NOW, ZoneOffset.UTC));
        user = new User("Deep", "Patel", "deep@example.com", "hash", new Role("ROLE_USER"));
        category = new Category("Laptops", "laptops", NOW.minusSeconds(60));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void returnsEmptyCartWithoutCreatingDatabaseState() {
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        var response = cartService.getCart(user.getId());

        assertThat(response.id()).isNull();
        assertThat(response.items()).isEmpty();
        assertThat(response.subtotal()).isEqualByComparingTo("0.00");
        assertThat(response.currency()).isEqualTo("USD");
    }

    @Test
    void lazilyCreatesCartAndCalculatesServerSideTotals() {
        Product product = product("USD", 8);
        when(productRepository.findOneById(product.getId())).thenReturn(Optional.of(product));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        var response = cartService.addItem(user.getId(), new AddCartItemRequest(product.getId(), 2));

        assertThat(response.items()).hasSize(1);
        assertThat(response.totalItems()).isEqualTo(2);
        assertThat(response.subtotal()).isEqualByComparingTo("2598.00");
        assertThat(response.items().getFirst().available()).isTrue();
    }

    @Test
    void addingSameProductIncreasesExistingQuantity() {
        Product product = product("USD", 8);
        Cart cart = new Cart(user, "USD", NOW.minusSeconds(30));
        cart.setProductQuantity(product, 2, NOW.minusSeconds(20));
        when(productRepository.findOneById(product.getId())).thenReturn(Optional.of(product));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        var response = cartService.addItem(user.getId(), new AddCartItemRequest(product.getId(), 3));

        assertThat(response.items()).singleElement().extracting(item -> item.quantity()).isEqualTo(5);
        assertThat(response.subtotal()).isEqualByComparingTo("6495.00");
    }

    @Test
    void rejectsQuantityAboveCurrentInventoryWithoutMutatingCart() {
        Product product = product("USD", 3);
        Cart cart = new Cart(user, "USD", NOW.minusSeconds(30));
        cart.setProductQuantity(product, 2, NOW.minusSeconds(20));
        when(productRepository.findOneById(product.getId())).thenReturn(Optional.of(product));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.addItem(
                user.getId(), new AddCartItemRequest(product.getId(), 2)))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("3 units available; 4 requested");

        assertThat(cart.getItems().getFirst().getQuantity()).isEqualTo(2);
    }

    @Test
    void rejectsInactiveProductAndUnsupportedCurrency() {
        Product inactiveProduct = product("USD", 5);
        inactiveProduct.discontinue(NOW);
        when(productRepository.findOneById(inactiveProduct.getId())).thenReturn(Optional.of(inactiveProduct));

        assertThatThrownBy(() -> cartService.addItem(
                user.getId(), new AddCartItemRequest(inactiveProduct.getId(), 1)))
                .isInstanceOf(ResourceNotFoundException.class);

        Product euroProduct = product("EUR", 5);
        when(productRepository.findOneById(euroProduct.getId())).thenReturn(Optional.of(euroProduct));

        assertThatThrownBy(() -> cartService.addItem(
                user.getId(), new AddCartItemRequest(euroProduct.getId(), 1)))
                .isInstanceOf(CartCurrencyMismatchException.class)
                .hasMessage("Cart currency is USD but product currency is EUR");
    }

    @Test
    void replacesItemQuantityAndReturnsCurrentSubtotal() {
        Product product = product("USD", 10);
        Cart cart = new Cart(user, "USD", NOW.minusSeconds(30));
        var item = cart.setProductQuantity(product, 2, NOW.minusSeconds(20));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        var response = cartService.updateItem(
                user.getId(), item.getId(), new UpdateCartItemRequest(4));

        assertThat(response.totalItems()).isEqualTo(4);
        assertThat(response.subtotal()).isEqualByComparingTo("5196.00");
    }

    @Test
    void hidesCartItemsOwnedByAnotherUser() {
        Product product = product("USD", 10);
        Cart cart = new Cart(user, "USD", NOW.minusSeconds(30));
        cart.setProductQuantity(product, 1, NOW.minusSeconds(20));
        UUID foreignItemId = UUID.randomUUID();
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.updateItem(
                user.getId(), foreignItemId, new UpdateCartItemRequest(2)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Cart item not found: " + foreignItemId);
    }

    @Test
    void removesOwnedItemAndClearsCartIdempotently() {
        Product product = product("USD", 10);
        Cart cart = new Cart(user, "USD", NOW.minusSeconds(30));
        var item = cart.setProductQuantity(product, 1, NOW.minusSeconds(20));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        cartService.removeItem(user.getId(), item.getId());
        cartService.clear(user.getId());

        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository, times(2)).findByUserId(user.getId());
    }

    private Product product(String currency, int quantity) {
        return new Product(
                category, "LAPTOP-001", "Developer Laptop", "Portable workstation",
                new BigDecimal("1299.00"), currency, quantity, NOW.minusSeconds(60));
    }
}
