package com.nexaris.authservice.dtos;

import java.util.List;

// UserResponse.java (Ce que tu renvoies au Front)
public record UserResponse(
        Integer id,
        String firstName,
        String lastName,
        String email,
        String profileImageUrl,
        String countryCode,  // "BE"
        String languageCode, // "FR"
        List<String> roles
){}
