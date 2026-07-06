package com.foodify.foodiesapi.service;

import com.foodify.foodiesapi.io.UserRequest;
import com.foodify.foodiesapi.io.UserResponse;

public interface UserService {
    UserResponse registerUser(UserRequest request);
    String findByUserId();
}
