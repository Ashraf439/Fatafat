package com.ashraf.config;

import com.ashraf.entity.User;
import com.ashraf.enums.Status;
import com.ashraf.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    @Value("${admin.seed.email}")
    private  String email;

    @Value("${admin.seed.password}")
    private String password;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if(!userRepository.existsByRole(Role.SUPER_ADMIN)) {
            User user = new User();
            user.setEmail(email);
            user.setRole(Role.SUPER_ADMIN);
            user.setPasswordHash(encoder.encode(password));
            user.setStatus(Status.ACTIVE);
            userRepository.save(user);
        }
    }
}
