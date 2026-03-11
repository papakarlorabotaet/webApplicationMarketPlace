package ru.urfu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.urfu.entity.User;


public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
}