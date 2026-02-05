package com.sloptech.helfheim.dto;

import lombok.Data;

@Data
public class UserUpdateRequestDto {
    String email;
    Integer subscriptionTimeInDays;
}
