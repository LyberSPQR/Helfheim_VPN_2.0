package com.sloptech.helfheim.dto;

import lombok.Data;

@Data
public class UserCreateRequestDto {
    private String email;
    private String password;
}
