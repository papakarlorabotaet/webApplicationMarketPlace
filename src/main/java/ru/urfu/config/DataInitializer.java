package ru.urfu.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.urfu.entity.Role;
import ru.urfu.entity.User;
import ru.urfu.repository.RoleRepository;
import ru.urfu.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UserRepository userRepository,
                                      RoleRepository roleRepository,
                                      PasswordEncoder passwordEncoder) {
        return args -> {
            // 1. Создаем роль, если её нет
            Role supportRole = roleRepository.findByName("ROLE_SUPPORT");
            if (supportRole == null) {
                supportRole = new Role();
                supportRole.setName("ROLE_SUPPORT");
                roleRepository.save(supportRole);
            }

            // 2. Создаем пользователя, если его нет
            if (userRepository.findByEmail("support@marketplace.ru") == null) {
                User support = new User();
                support.setEmail("support@marketplace.ru");
                support.setName("Support");
                support.setSurname("System");
                support.setCity("Ekaterinburg");
                support.setPassword(passwordEncoder.encode("password")); // Хешируем!
                support.setRegistrationDate(LocalDateTime.now());
                support.setAccountBalance(BigDecimal.ZERO);
                support.setAccountAmount(0);
                support.setRoles(Collections.singleton(supportRole));

                userRepository.save(support);
                System.out.println(">>> Support user created!");
            }
        };
    }
}
