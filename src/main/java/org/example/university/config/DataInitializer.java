package org.example.university.config;

import org.example.university.model.Role;
import org.example.university.model.UserAccount;
import org.example.university.repository.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedUsers(UserAccountRepository users, PasswordEncoder encoder) {
        return args -> {
            if (users.count() > 0) {
                return;
            }
            save(users, encoder, "admin", "admin123", Role.ROLE_ADMIN);
            save(users, encoder, "teacher", "teacher123", Role.ROLE_TEACHER);
            save(users, encoder, "student", "student123", Role.ROLE_STUDENT);
        };
    }

    private void save(UserAccountRepository users, PasswordEncoder encoder, String username, String password, Role role) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPassword(encoder.encode(password));
        user.setRole(role);
        users.save(user);
    }
}
