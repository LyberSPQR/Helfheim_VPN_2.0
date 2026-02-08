package com.sloptech.helfheim.dto;

import lombok.Data;

import java.net.InetAddress;

@Data
public class VpnPeerDto {
    private String email;
    private String publicKey;
    private InetAddress ipAddress;

    public VpnPeerDto(String email, String publicKey, InetAddress ipAddress) {
        this.email = email;
        this.publicKey = publicKey;
        this.ipAddress = ipAddress;
    }
}
