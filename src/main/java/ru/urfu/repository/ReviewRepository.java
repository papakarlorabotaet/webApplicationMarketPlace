package ru.urfu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.urfu.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {
}
