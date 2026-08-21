package com.deep.ordexaflow.cart.api;

import java.util.UUID;

import com.deep.ordexaflow.cart.application.CartService;
import com.deep.ordexaflow.common.web.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Access token is missing, invalid, or expired",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Authenticated account does not have customer access",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
})
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @Operation(summary = "Read the authenticated customer's cart")
    public CartResponse cart(@AuthenticationPrincipal Jwt jwt) {
        return cartService.getCart(userId(jwt));
    }

    @PostMapping("/items")
    @Operation(summary = "Add a product or increase its existing cart quantity")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Request validation failed",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Product or authenticated user was not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Inventory or currency conflict",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public CartResponse addItem(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(userId(jwt), request);
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Replace an owned cart item's quantity")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Request validation failed",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Cart item is absent or not owned by the customer",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Inventory, currency, or concurrency conflict",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public CartResponse updateItem(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItem(userId(jwt), itemId, request);
    }

    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove an owned cart item")
    @ApiResponse(responseCode = "404", description = "Cart item is absent or not owned by the customer",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public void removeItem(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID itemId) {
        cartService.removeItem(userId(jwt), itemId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Clear the authenticated customer's cart")
    public void clear(@AuthenticationPrincipal Jwt jwt) {
        cartService.clear(userId(jwt));
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getClaimAsString("userId"));
    }
}
