package com.sloptech.helfheim.dto;

import com.sloptech.helfheim.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    private Long id;
    private String email;
    private Boolean isActive;
    private Long subscriptionExpiresAt;

    public static UserResponseDto from(User user) {
        if (user == null) return null;
        return new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getIsActive(),
                user.getSubscriptionExpiresAt()
        );
    }
}
