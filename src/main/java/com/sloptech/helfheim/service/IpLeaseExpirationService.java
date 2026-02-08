package com.sloptech.helfheim.service;

import com.sloptech.helfheim.entity.User;
import com.sloptech.helfheim.repository.IpRepository;
import com.sloptech.helfheim.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IpLeaseExpirationService {
    private final UserRepository userRepository;
    private final IpRepository ipRepository;
    private final CoreService coreService;

    private final TransactionTemplate transactionTemplate;

    @Scheduled(fixedDelay = 60000)
    public void leaseExpiredIps() throws IOException, InterruptedException {
        Long currentUnixTime = Instant.now().getEpochSecond();
        final boolean[] needsSync = {false};
        transactionTemplate.executeWithoutResult(status -> {
            List<User> expiredUsers = userRepository.findUsersWithExpiredSubscriptions(currentUnixTime);

            log.info("Найдено пользователей с истекшей подпиской: {}", expiredUsers.size());

            if (expiredUsers.isEmpty()) {
                log.debug("Нет пользователей с истекшей подпиской");
                return;
            }

            for (User user : expiredUsers) {
                String pubKey = user.getPublicKey();

                if (pubKey != null && !pubKey.trim().isEmpty()) {
                    log.info("Деактивация пользователя: {}, публичный ключ: {}", user.getEmail(), pubKey);

                    user.setIsActive(false);
                    user.setSubscriptionExpiresAt(null);
                    user.setPrivateKey(null);
                    user.setPublicKey(null);

                    ipRepository.releaseUserIp(user.getId());
                    log.debug("IP освобожден для пользователя: {}", user.getEmail());
                } else {
                    log.warn("Пользователь {} не имеет публичного ключа, пропускаем", user.getEmail());
                }
            }

            userRepository.saveAll(expiredUsers);
            needsSync[0] = true;
            if (needsSync[0] == true)
                log.info("Запуск регенерации конфигурации WireGuard после деактивации {} пользователей", expiredUsers.size());
            else
                log.info("Запуск регенерации конфигурации WireGuard не требуется");
        });
        if (needsSync[0] == true) {
            coreService.regenerateAndApplyWireGuardConfig();
        }
        log.info("Обработка истекших подписок завершена");
    }
}