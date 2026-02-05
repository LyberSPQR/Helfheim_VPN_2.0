package com.sloptech.helfheim.repository;

import com.sloptech.helfheim.entity.Ip;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface IpRepository extends JpaRepository<Ip,Long> {
    @Query(value = "SELECT * FROM ip_pool i WHERE i.is_assigned = false ORDER BY i.id LIMIT 1 FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    Optional<Ip> findFirstFreeIp();

    @Modifying
    @Query("UPDATE Ip i SET i.userId = null, i.isAssigned = false WHERE i.userId = :userId")
    void releaseUserIp(Long userId);

    Ip findIpByUserId(Long userId);
}
