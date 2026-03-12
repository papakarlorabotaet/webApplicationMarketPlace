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

@Configuration
@Component
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    public DataInitializer(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

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


    @Override
    public void run(String... args) throws Exception {
        // Проверяем, пустая ли таблица категорий.
        // Если пустая — заполняем её пачкой данных.
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

    // Вспомогательный метод для быстрого создания категории
    private Category createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        return category;
    }
}
