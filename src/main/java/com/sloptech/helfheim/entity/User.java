package com.sloptech.helfheim.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;


@Entity
@AllArgsConstructor
@Data
@Table(name = "users")
public class User {

    @Id
    @Column(name = "user_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;
    private String password;
    @Column(name = "public_key")
    private String publicKey;
    @Column(name = "private_key")
    private String privateKey;
    @Column(name = "is_active")
    private Boolean isActive;
    @Column(name = "subscription_expires_at")
    private Long subscriptionExpiresAt;

    public User() {
    }

}
