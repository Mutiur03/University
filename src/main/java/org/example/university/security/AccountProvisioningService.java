package org.example.university.security;

import java.security.SecureRandom;
import org.example.university.model.Role;
import org.example.university.model.UserAccount;
import org.example.university.repository.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AccountProvisioningService {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private final UserAccountRepository users;
    private final PasswordEncoder encoder;
    private final SecureRandom random = new SecureRandom();

    public AccountProvisioningService(UserAccountRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public Credentials create(String email, Role role) {
        if (users.findByUsername(email).isPresent()) {
            throw new DataIntegrityViolationException("An account already exists for " + email);
        }
        String temporaryPassword = generatePassword();
        UserAccount user = new UserAccount();
        user.setUsername(email);
        user.setPassword(encoder.encode(temporaryPassword));
        user.setRole(role);
        users.save(user);
        return new Credentials(email, temporaryPassword);
    }

    public void rename(String previousEmail, String email) {
        if (previousEmail.equals(email)) {
            return;
        }
        users.findByUsername(previousEmail).ifPresent(user -> {
            if (users.findByUsername(email).isPresent()) {
                throw new DataIntegrityViolationException("An account already exists for " + email);
            }
            user.setUsername(email);
            users.save(user);
        });
    }

    public void delete(String email) {
        users.findByUsername(email).ifPresent(users::delete);
    }

    private String generatePassword() {
        StringBuilder password = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            password.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        }
        return password.toString();
    }

    public record Credentials(String username, String temporaryPassword) {}
}
