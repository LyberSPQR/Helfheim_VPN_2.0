package com.sloptech.helfheim.dto;

import lombok.Data;

@Data
public class UserUpdateRequestDto {
    private String email;
    private Integer subscriptionTimeInDays;
}
