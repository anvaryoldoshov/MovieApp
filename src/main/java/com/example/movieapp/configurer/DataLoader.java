package com.example.movieapp.configurer;

import com.example.movieapp.entities.User;
import com.example.movieapp.enums.Role;
import com.example.movieapp.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataLoader implements ApplicationRunner {

    private final UserRepo userRepo;
    private final BCryptPasswordEncoder passwordEncoder;

    private static final String SUPER_ADMIN_EMAIL = "superadmin@movieapp.com";
    private static final String SUPER_ADMIN_PASSWORD = "Admin@1234";

    @Override
    public void run(ApplicationArguments args) {
        if (userRepo.findByEmail(SUPER_ADMIN_EMAIL).isEmpty()) {
            User superAdmin = User.builder()
                    .username("SuperAdmin")
                    .email(SUPER_ADMIN_EMAIL)
                    .password(passwordEncoder.encode(SUPER_ADMIN_PASSWORD))
                    .subscription(false)
                    .userId(999999L)
                    .role(Role.ADMIN)
                    .build();
            userRepo.save(superAdmin);
        }
    }
}
