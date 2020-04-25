package com.raptors.dashboard.services;

import com.raptors.dashboard.entities.Instance;
import com.raptors.dashboard.entities.User;
import com.raptors.dashboard.model.InstanceRequest;
import com.raptors.dashboard.repositories.UserRepository;
import com.raptors.dashboard.security.SecurityPropertyHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.raptors.dashboard.crytpo.CryptoModule.decryptAes;
import static com.raptors.dashboard.crytpo.CryptoModule.encryptAes;
import static com.raptors.dashboard.crytpo.HexUtils.bytesToHex;
import static com.raptors.dashboard.crytpo.HexUtils.hexToBytes;
import static java.nio.charset.StandardCharsets.US_ASCII;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final SecurityPropertyHolder securityPropertyHolder;

    public UserService(UserRepository userRepository,
                       SecurityPropertyHolder securityPropertyHolder) {
        this.userRepository = userRepository;
        this.securityPropertyHolder = securityPropertyHolder;
    }

    public User getUserByLoginOrThrowException(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public ResponseEntity addInstance(String login, String encryptedKey, InstanceRequest instanceRequest) {
        return userRepository.findByLogin(login)
                .map(user -> {
                    user.addInstance(Instance.builder()
                            .uuid(UUID.randomUUID())
                            .name(instanceRequest.getName())
                            .login(instanceRequest.getLogin())
                            .uri(instanceRequest.getUri())
                            .encryptedPassword(encryptPassword(encryptedKey, instanceRequest))
                            .build());
                    userRepository.save(user);
                    return ResponseEntity.ok().build();
                }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private String encryptPassword(String encryptedKey, InstanceRequest instanceRequest) {
        byte[] plainKey = decryptAes(hexToBytes(securityPropertyHolder.getKek()), hexToBytes(encryptedKey));
        return bytesToHex(encryptAes(plainKey, instanceRequest.getPassword().getBytes(US_ASCII)));
    }

    public void removeInstance(String login, UUID uuid) {
        userRepository.findByLogin(login)
                .ifPresent(user -> {
                    user.removeInstance(uuid);
                    userRepository.save(user);
                });
    }
}
