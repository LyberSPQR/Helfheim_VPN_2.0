package com.sloptech.helfheim.repository;

import com.sloptech.helfheim.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {


    User findUserByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.subscriptionExpiresAt < :currentTime")
    List<User> findUsersWithExpiredSubscriptions(@Param("currentTime") Long currentUnixTime);

}
