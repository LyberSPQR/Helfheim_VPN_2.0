package com.sloptech.helfheim.dto;

import lombok.Data;

@Data
public class LoginResponseDto {
    private String privateKey;
    private String ipAddress;
    private String serverPublicKey ;
    private String endpoint;
}
