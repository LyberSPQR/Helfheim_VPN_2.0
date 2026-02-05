package com.sloptech.helfheim.service;

import com.sloptech.helfheim.dto.UserUpdateRequestDto;
import com.sloptech.helfheim.entity.Ip;
import com.sloptech.helfheim.entity.User;
import com.sloptech.helfheim.repository.IpRepository;
import com.sloptech.helfheim.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CoreService {
    private final UserRepository userRepository;
    private final IpRepository ipRepository;

    @Value("${SERVER_PUBLIC_KEY}")
    private String serverPublicKey;
    @Value("${SERVER_SERVER_ENDPOINT}")
    private String serverEndpoint;

    public User saveUser(User user){
        userRepository.save(user);
        return user;
    }
    public void  deleteUser(User user){
        userRepository.delete(user);
    }
    public User updateUser(UserUpdateRequestDto userUpdateDto){
        User updatedUser = userRepository.findUserByEmail(userUpdateDto.getEmail());
        updatedUser.setSubscriptionExpiresAt(Instant.now()
                .plusSeconds(userUpdateDto.getSubscriptionTimeInDays() * 86400L)
                .getEpochSecond());
        userRepository.save(updatedUser);
        return updatedUser;
}
@Transactional(rollbackFor = Exception.class)
public void activateSubscription(UserUpdateRequestDto userUpdateDto) throws IOException, InterruptedException {

        User user = userRepository.findUserByEmail(userUpdateDto.getEmail());
    System.out.println("Шаг 1: Пользователь найден");
    List<String> keys = generateKeys();
    System.out.println("Шаг 2: Ключи сгенерированы");
//        user.setSubscriptionExpiresAt(Instant.now()
//                .plusSeconds(userUpdateDto.getSubscriptionTimeInDays() * 86400L)
//                .getEpochSecond());
    user.setSubscriptionExpiresAt(Instant.now()
            .plusSeconds(30L * userUpdateDto.getSubscriptionTimeInDays())
            .getEpochSecond());

        user.setPrivateKey(keys.get(0));
        user.setPublicKey(keys.get(1));

        Ip freeIp = ipRepository.findFirstFreeIp()
                .orElseThrow(() -> new RuntimeException("No free IP available"));

    freeIp.setUserId(user.getId());
    freeIp.setIsAssigned(true);
    user.setIsActive(true);
    ipRepository.save(freeIp);
    userRepository.save(user);
    System.out.println("Перед скриптом add to vpn");
    addUserToVpnConf(user.getPublicKey(),freeIp.getIpAddress());
    System.out.println("После скриптом add to vpn");

}

    public List<String> generateKeys() throws IOException, InterruptedException {
        String privateKey;
        String publicKey;
        Process genPrivateKey = new ProcessBuilder("wg", "genkey").start();
        var genkeyReader = new BufferedReader(
                new InputStreamReader(genPrivateKey.getInputStream()));
        privateKey = genkeyReader.readLine().trim();
        System.out.println("Приватный ключ сгенерирован");
        boolean finished = genPrivateKey.waitFor(5, TimeUnit.SECONDS);
        if (!finished) {
            genPrivateKey.destroyForcibly();
            throw new RuntimeException("failed to generate private key");
        }
        System.out.println("Перед генерацией публичного");
        Process genPublicKey = new ProcessBuilder("wg", "pubkey").start();
        OutputStreamWriter writer = new OutputStreamWriter(genPublicKey.getOutputStream());
        System.out.println("Перед записью в команду");
        writer.write(privateKey);
        writer.flush();
        writer.close();
        System.out.println("После передачи приватного и закрытия");
        var pubkeyReader = new BufferedReader(
                new InputStreamReader(genPublicKey.getInputStream()));
        publicKey = pubkeyReader.readLine().trim();
        System.out.println("Публичный ключ записан");
        boolean pub_finished = genPublicKey.waitFor(5, TimeUnit.SECONDS);
        if (!pub_finished) {
            genPublicKey.destroyForcibly();
            throw new RuntimeException("failed to generate public key");
        }
        System.out.println("Публичный ключ сгенерирован");
        return List.of(privateKey, publicKey);
    }
    public void addUserToVpnConf(String publicKey, InetAddress ip) throws IOException, InterruptedException {
System.out.println("Передаваемый в addToUser ключ " + publicKey);
        ProcessBuilder pb = new ProcessBuilder("sudo", "/home/lyber_spqr/Учёба/vpn/add-peer.sh", publicKey, ip.getHostAddress());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        boolean finished = process.waitFor(5, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("failed to add peers");
        }
    }
    public void removeUserFromVpnConf(String publicKey) throws IOException, InterruptedException {
        System.out.println("Передаваемый в RemoveToUser ключ " + publicKey);
        ProcessBuilder pb = new ProcessBuilder("sudo", "/home/lyber_spqr/Учёба/vpn/remove-peer.sh", publicKey);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        boolean finished = process.waitFor(5, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("failed to add peers");
        }
    }
//    public void syncVpnConfigs() throws IOException, InterruptedException {
//
//    }
    public String generateConfig(String email){
        User user =  userRepository.findUserByEmail(email);
        Ip ip = ipRepository.findIpByUserId(user.getId());

        return String.format(
                "[Interface]\n" +
                        "PrivateKey = %s\n" +
                        "Address = %s/32\n" +
                        "DNS = 1.1.1.1\n\n" +
                        "[Peer]\n" +
                        "PublicKey = %s\n" +
                        "Endpoint = %s\n" +
                        "AllowedIPs = 0.0.0.0/0\n" +
                        "PersistentKeepalive = 20\n\n",
                user.getPrivateKey(), ip.getIpAddress().getHostAddress(), serverPublicKey, serverEndpoint
        );
    }
}