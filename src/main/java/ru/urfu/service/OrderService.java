package ru.urfu.service;

import org.springframework.web.multipart.MultipartFile;
import ru.urfu.entity.Order;
import ru.urfu.entity.User;

import java.util.List;

public interface OrderService {
    List<Order> findOrdersBySeller(User user);
    void importOrders(MultipartFile file, String userEmail);
}
