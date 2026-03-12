package ru.urfu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.urfu.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {}