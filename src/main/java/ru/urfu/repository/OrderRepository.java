package ru.urfu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.urfu.entity.Order;
import ru.urfu.entity.User;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByGoods_User(User user);
}
