package com.sloptech.helfheim.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;


import java.net.InetAddress;

@Entity
@AllArgsConstructor
@Data
@Table(name = "ip_pool")
public class Ip {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "ip_address", nullable = false)
    private InetAddress ipAddress;
    @Column(name = "is_assigned")
    private Boolean isAssigned;

    @Column(name = "user_id")
    private Long userId;
    public Ip() {}
}
