package com.sloptech.helfheim.repository;

import com.sloptech.helfheim.dto.VpnPeerDto;
import com.sloptech.helfheim.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;


public interface UserRepository extends JpaRepository<User, Long> {


    User findUserByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.subscriptionExpiresAt < :currentTime")
    List<User> findUsersWithExpiredSubscriptions(@Param("currentTime") Long currentUnixTime);

    @Query("SELECT new com.sloptech.helfheim.dto.VpnPeerDto(u.email,u.publicKey, i.ipAddress) " +
            "FROM User u, Ip i " +
            "WHERE u.id = i.userId AND u.isActive = TRUE AND i.isAssigned = TRUE")
    List<VpnPeerDto> findActivePeers();
}
