package com.foodify.foodiesapi.service;


import com.foodify.foodiesapi.io.CartRequest;
import com.foodify.foodiesapi.io.CartResponse;

public interface CartService {

    CartResponse addToCart(CartRequest request);

    CartResponse getCart();

    void clearCart();

    CartResponse removeFromCart(CartRequest cartRequest);
}
