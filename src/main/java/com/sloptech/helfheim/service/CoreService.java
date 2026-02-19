package com.sloptech.helfheim.service;

import com.sloptech.helfheim.dto.*;
import com.sloptech.helfheim.entity.Ip;
import com.sloptech.helfheim.entity.User;
import com.sloptech.helfheim.repository.IpRepository;
import com.sloptech.helfheim.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoreService {
    private final UserRepository userRepository;
    private final IpRepository ipRepository;

    private final TransactionTemplate transactionTemplate;

    private final ReentrantLock configLock = new ReentrantLock();

    @Value("${SERVER_PUBLIC_KEY}")
    private String serverPublicKey;

    @Value("${SERVER_SERVER_ENDPOINT}")
    private String serverEndpoint;

    @Value("${SERVER_PRIVATE_KEY}")
    private String serverPrivateKey;

    @Value("${SERVER_PORT:443}")
    private int serverPort;

    public User saveUser(UserCreateRequestDto dto) {
        log.info("Сохранение пользователя: {}", dto.getEmail());
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setIsActive(false);
        userRepository.save(user);
        return user;
    }

    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            log.info("Удаление пользователя: {}", user.getEmail());
            userRepository.delete(user);
        }
    }

    public User updateUser(UserUpdateRequestDto userUpdateDto) throws IOException, InterruptedException {
        log.info("Обновление пользователя: {}", userUpdateDto.getEmail());
        User updatedUser = userRepository.findUserByEmail(userUpdateDto.getEmail());
        if (updatedUser == null) throw new RuntimeException("user not found");

        long currentUnixTime = Instant.now().getEpochSecond();

        if (updatedUser.getSubscriptionExpiresAt() == null || updatedUser.getSubscriptionExpiresAt() <= currentUnixTime) {

            log.info("Начало полного обновления подписки пользователя {} ", userUpdateDto.getEmail());
            activateSubscription(userUpdateDto);
            log.info("Успешное полное обновление подписки пользователя {} ", userUpdateDto.getEmail());
        } else {
            updatedUser.setSubscriptionExpiresAt(Instant.now()
                    .plusSeconds(userUpdateDto.getSubscriptionTimeInDays() * 86400L)
                    .getEpochSecond());
            userRepository.save(updatedUser);
            log.info("Срок подписки пользователя {} обновлен", userUpdateDto.getEmail());
        }
        return updatedUser;
    }

    public void activateSubscription(UserUpdateRequestDto userUpdateDto) throws IOException, InterruptedException {
        log.info("Активация подписки для пользователя: {}", userUpdateDto.getEmail());
        transactionTemplate.executeWithoutResult(status -> {
            User user = userRepository.findUserByEmail(userUpdateDto.getEmail());

            if (user == null) throw new RuntimeException("user not found");

            log.info("Пользователь найден - {}", user.getEmail());

            List<String> keys = null;
            try {
                keys = generateKeys();
            } catch (IOException | InterruptedException e) {
                log.info("Не удалось сгенерировать ключи для пользователя {}", user.getEmail());
                throw new RuntimeException(e);
            }
            log.info("Ключи сгенерированы для пользователя {}", user.getEmail());

            user.setSubscriptionExpiresAt(Instant.now()
                    .plusSeconds(86400L * userUpdateDto.getSubscriptionTimeInDays())
                    .getEpochSecond());

            user.setPrivateKey(keys.get(0));
            user.setPublicKey(keys.get(1));

            Ip freeIp = ipRepository.findFirstFreeIp()
                    .orElseThrow(() -> {
                        log.error("Нет свободных IP адресов для пользователя {}", user.getEmail());
                        return new RuntimeException("No free IP available");
                    });

            log.info("Назначен свободный IP: {} для пользователя {}",
                    freeIp.getIpAddress().getHostAddress(), user.getEmail());

            freeIp.setUserId(user.getId());
            freeIp.setIsAssigned(true);
            user.setIsActive(true);

            ipRepository.save(freeIp);
            userRepository.save(user);
            log.info(" Пользователь {} активирован", user.getEmail());

        });

        log.info("Регенерация конфигурации WireGuard");
        regenerateAndApplyWireGuardConfig();

        log.info("Активация подписки для пользователя {} завершена успешно", userUpdateDto.getEmail());
    }

    public void regenerateAndApplyWireGuardConfig() throws IOException, InterruptedException {
        if (!configLock.tryLock(10, TimeUnit.SECONDS)) {
            log.warn("Не удалось захватить lock на обновление WireGuard");
            return;
        }

        try {
            log.info("Захвачен lock → начинается регенерация конфигурации");
            log.info("Начало регенерации конфигурации WireGuard");

            List<VpnPeerDto> activePeers = userRepository.findActivePeers();
            log.info("Найдено активных пользователей: {}", activePeers.size());

            StringBuilder config = new StringBuilder();
            int addedPeers = 0;

            config.append("[Interface]\n")
                    .append("PrivateKey = ").append(serverPrivateKey).append("\n")
                    .append("ListenPort = ").append(serverPort).append("\n");

            config.append("\n");

            for (VpnPeerDto peer : activePeers) {
                if (peer.getPublicKey() == null || peer.getPublicKey().trim().isEmpty()) {
                    log.warn("Пропущен пользователь {}: отсутствует публичный ключ", peer.getEmail());
                    continue;
                }
                if (peer.getIpAddress() == null || peer.getIpAddress().getHostAddress() == null) {
                    log.warn("Пропущен пользователь {}: отсутствует назначенный IP", peer.getEmail());
                    continue;
                }

                String ip = peer.getIpAddress().getHostAddress();
                config.append("[Peer]\n")
                        .append("PublicKey = ").append(peer.getPublicKey()).append("\n")
                        .append("AllowedIPs = ").append(ip).append("/32\n\n");
                addedPeers++;

                log.debug("Добавлен пир: {} -> {}", peer.getEmail(), ip);
            }

            log.info("Сформирована конфигурация для {} пиров", addedPeers);

            ProcessBuilder pb = new ProcessBuilder("sudo", "tee", "/etc/wireguard/wg0.peers.conf");
            Process process = pb.start();

            try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(config.toString());
            }

            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished || process.exitValue() != 0) {
                log.error("Ошибка при записи конфига через sudo tee");
                throw new RuntimeException("Failed to write config via sudo tee");
            }

            log.info("Конфигурационный файл обновлен через sudo tee");

            syncWireGuardConfig();
        } catch (Exception e) {
            log.error("Ошибка при обновлении WireGuard конфига", e);
        } finally {
            if (configLock.isHeldByCurrentThread()) {
                configLock.unlock();
            }
        }
    }

    public List<String> generateKeys() throws IOException, InterruptedException {
        log.debug("Генерация ключей WireGuard");

        String privateKey;
        String publicKey;

        Process genPrivateKey = new ProcessBuilder("wg", "genkey").start();
        var genkeyReader = new BufferedReader(
                new InputStreamReader(genPrivateKey.getInputStream()));
        privateKey = genkeyReader.readLine().trim();
        log.debug("Приватный ключ сгенерирован");

        boolean finished = genPrivateKey.waitFor(5, TimeUnit.SECONDS);
        if (!finished) {
            genPrivateKey.destroyForcibly();
            log.error("Таймаут при генерации приватного ключа");
            throw new RuntimeException("failed to generate private key");
        }

        log.debug("Генерация публичного ключа");
        Process genPublicKey = new ProcessBuilder("wg", "pubkey").start();
        OutputStreamWriter writer = new OutputStreamWriter(genPublicKey.getOutputStream());
        writer.write(privateKey);
        writer.flush();
        writer.close();

        var pubkeyReader = new BufferedReader(
                new InputStreamReader(genPublicKey.getInputStream()));
        publicKey = pubkeyReader.readLine().trim();
        log.debug("Публичный ключ сгенерирован");

        boolean pub_finished = genPublicKey.waitFor(5, TimeUnit.SECONDS);
        if (!pub_finished) {
            genPublicKey.destroyForcibly();
            log.error("Таймаут при генерации публичного ключа");
            throw new RuntimeException("failed to generate public key");
        }

        log.info("Ключи успешно сгенерированы");
        return List.of(privateKey, publicKey);
    }

    public void syncWireGuardConfig() throws IOException, InterruptedException {
        log.info("Синхронизация конфигурации WireGuard");

        ProcessBuilder pb = new ProcessBuilder("sudo", "wg", "syncconf",
                "wg0", "/etc/wireguard/wg0.peers.conf");
        pb.redirectErrorStream(true);
        Process process = pb.start();

        boolean finished = process.waitFor(10, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            log.error("Таймаут при синхронизации конфигурации WireGuard");
            throw new RuntimeException("wg syncconf timeout");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            String errorOutput = new BufferedReader(
                    new InputStreamReader(process.getInputStream())).lines()
                    .collect(Collectors.joining("\n"));
            log.error("Ошибка синхронизации конфигурации WireGuard: {}", errorOutput);
            throw new RuntimeException("wg syncconf failed: " + errorOutput);
        }

        log.info("Конфигурация WireGuard успешно синхронизирована");
    }

    public String generateConfig(String email) {
        log.info("Генерация конфигурационного файла для пользователя: {}", email);

        User user = userRepository.findUserByEmail(email);

        if (user == null) throw new RuntimeException("user not found");

        if (!user.getIsActive()) throw new RuntimeException("user is not active");

        Ip ip = ipRepository.findIpByUserId(user.getId());
        if (ip == null || ip.getIpAddress() == null) throw new RuntimeException("no IP assigned for user");

        String config = String.format(
                "[Interface]\n" +
                        "PrivateKey = %s\n" +
                        "Address = %s/32\n" +
                        "DNS = 1.1.1.1, 8.8.8.8\n\n" +
                        "[Peer]\n" +
                        "PublicKey = %s\n" +
                        "Endpoint = %s\n" +
                        "AllowedIPs = 0.0.0.0/0\n" +
                        "PersistentKeepalive = 20\n\n",
                user.getPrivateKey(), ip.getIpAddress().getHostAddress(), serverPublicKey, serverEndpoint
        );

        log.debug("Конфигурация сгенерирована для {} -> {}", email, ip.getIpAddress().getHostAddress());
        return config;
    }

    public LoginResponseDto generateConfigForFrontend(LoginRequestDto dto) {
        log.info("Генерация конфигурационного файла для пользователя: {}", dto.getLogin());

        User user = userRepository.findUserByEmail(dto.getLogin());
        log.debug("данные из фронта " + dto.getLogin() + " " + dto.getPassword());
        if (user == null) throw new RuntimeException("user not found");

        if (!user.getIsActive()) throw new RuntimeException("user is not active");
        if (!user.getPassword().equals(dto.getPassword())) throw new RuntimeException("password is incorrect");
        Ip ip = ipRepository.findIpByUserId(user.getId());
        if (ip == null || ip.getIpAddress() == null) throw new RuntimeException("no IP assigned for user");

        LoginResponseDto loginResponseDto = new LoginResponseDto();
        loginResponseDto.setServerPublicKey(serverPublicKey);
        loginResponseDto.setPrivateKey(user.getPrivateKey());
        loginResponseDto.setIpAddress(ip.getIpAddress().getHostAddress());
        loginResponseDto.setEndpoint(serverEndpoint);
        loginResponseDto.setExpiresAt(user.getSubscriptionExpiresAt());

        log.debug("Конфигурация сгенерирована для {} -> {}", dto.getLogin(), ip.getIpAddress().getHostAddress());
        return loginResponseDto;
    }
}