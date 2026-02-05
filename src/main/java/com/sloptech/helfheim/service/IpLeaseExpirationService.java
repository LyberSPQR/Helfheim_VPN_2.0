package com.sloptech.helfheim.service;

import com.sloptech.helfheim.entity.User;
import com.sloptech.helfheim.repository.IpRepository;
import com.sloptech.helfheim.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class IpLeaseExpirationService {
    private final UserRepository userRepository;
    private final IpRepository ipRepository;
    private final CoreService coreService;

    @Scheduled( fixedRate = 60000)
    @Transactional
    public void leaseExpiredIps() throws IOException, InterruptedException {
        List<String> removedPubKeys = new ArrayList<>();

       Long currentUnixTime = Instant.now().getEpochSecond();

       List<User> expiredUsers = userRepository.findUsersWithExpiredSubscriptions(currentUnixTime);
        System.out.println(expiredUsers.size());

        for (User u : expiredUsers) {
            String pubKey = u.getPublicKey();

            if (pubKey != null && !pubKey.trim().isEmpty()) {

                coreService.removeUserFromVpnConf(pubKey);
                removedPubKeys.add(pubKey);

                u.setIsActive(false);
                u.setSubscriptionExpiresAt(null);
                u.setPrivateKey(null);
                u.setPublicKey(null);
                userRepository.save(u);

                ipRepository.releaseUserIp(u.getId());
            }
        }
        if (!removedPubKeys.isEmpty()) {
            try {
                ProcessBuilder pb = new ProcessBuilder("sudo", "wg", "syncconf", "wg0", "/etc/wireguard/wg0.peers.conf");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                process.waitFor(10, TimeUnit.SECONDS);
                System.out.println("syncconf выполнен после удаления " + removedPubKeys.size() + " пиров");
            } catch (Exception e) {
                System.err.println("syncconf в конце не удался: " + e.getMessage());
            }
        }
        expiredUsers.clear();
    }
}
