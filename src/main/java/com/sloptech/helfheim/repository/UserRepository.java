package com.sloptech.helfheim.repository;

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

    @Lock(PESSIMISTIC_WRITE)
    List<User> findUsersByIsActive(Boolean isActive);
}
