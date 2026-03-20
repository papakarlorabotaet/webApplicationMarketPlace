package ru.urfu.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.urfu.entity.Category;
import ru.urfu.entity.Role;
import ru.urfu.entity.User;
import ru.urfu.repository.CategoryRepository;
import ru.urfu.repository.RoleRepository;
import ru.urfu.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;


@Component
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(CategoryRepository categoryRepository,
                           UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. Инициализация ролей и пользователя support
        Role supportRole = roleRepository.findByName("ROLE_SUPPORT");
        if (supportRole == null) {
            supportRole = new Role();
            supportRole.setName("ROLE_SUPPORT");
            roleRepository.save(supportRole);
        }

        if (userRepository.findByEmail("support@marketplace.ru").isEmpty()) {
            User support = new User();
            support.setEmail("support@marketplace.ru");
            support.setName("Support");
            support.setSurname("System");
            support.setCity("Ekaterinburg");
            support.setPassword(passwordEncoder.encode("password"));
            support.setRegistrationDate(LocalDateTime.now());
            support.setAccountBalance(BigDecimal.ZERO);
            support.setAccountAmount(0);
            support.setRoles(Collections.singleton(supportRole));

            userRepository.save(support);
            System.out.println(">>> Support user created!");
        }

        // 2. Инициализация категорий
        if (categoryRepository.count() == 0) {
            List<Category> defaultCategories = List.of(
                    createCategory("Электроника"),
                    createCategory("Бытовая техника"),
                    createCategory("Одежда и обувь"),
                    createCategory("Дом и сад"),
                    createCategory("Красота и здоровье"),
                    createCategory("Детские товары"),
                    createCategory("Спорт и отдых"),
                    createCategory("Автотовары"),
                    createCategory("Книги и канцелярия"),
                    createCategory("Зоотовары"),
                    createCategory("Продукты питания"),
                    createCategory("Ювелирные изделия")
            );
            categoryRepository.saveAll(defaultCategories);
            System.out.println("✅ Категории успешно загружены в базу данных!");
        }
    }

    private Category createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        return category;
    }
}
