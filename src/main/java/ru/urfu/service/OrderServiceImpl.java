package ru.urfu.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.urfu.entity.Goods;
import ru.urfu.entity.Order;
import ru.urfu.entity.OrderItem;
import ru.urfu.entity.User;
import ru.urfu.repository.GoodsRepository;
import ru.urfu.repository.OrderRepository;
import ru.urfu.repository.UserRepository;

import javax.transaction.Transactional;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final GoodsRepository goodsRepository;

    public OrderServiceImpl(OrderRepository orderRepository,
                            UserRepository userRepository,
                            GoodsRepository goodsRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.goodsRepository = goodsRepository;
    }
    @Transactional
    @Override
    public List<Order> findOrdersBySeller(User seller) {
        // Используем метод репозитория, который ищет заказы по товарам продавца
        return orderRepository.findOrdersByGoodsSeller(seller);
    }
    @Transactional
    @Override
    public List<Order> findOrdersByBuyer(User consumer) {
        // Используем метод репозитория, который ищет заказы по товарам покупателя
        return orderRepository.findByBuyer(consumer);
    }

    @Override
    @Transactional
    public void importOrders(MultipartFile file, String sellerEmail) {
        // Получаем продавца по email
        User seller = userRepository.findByEmail(sellerEmail);
        if (seller == null) {
            throw new RuntimeException("Продавец не найден");
        }

        List<Order> orders = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false; // пропускаем заголовок
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length < 5) continue;

                String goodsName = parts[0].trim();
                int quantity = Integer.parseInt(parts[1].trim());
                BigDecimal totalPrice = new BigDecimal(parts[2].trim());
                LocalDateTime orderDate = LocalDateTime.parse(parts[3].trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                String status = parts[4].trim();

                // Находим товар по имени (предполагаем, что товары уникальны у продавца)
                Goods goods = goodsRepository.findByNameAndUser(goodsName, seller);
                if (goods == null) {
                    // Можно пропустить или создать новый товар – зависит от логики
                    continue;
                }

                // 3. Создаём заказ
                Order order = new Order();
                order.setOrderDate(orderDate);
                order.setStatus(status);
                order.setBuyer(null); // покупатель неизвестен при импорте
                order.setTotalPrice(totalPrice);
                order.setQuantity(quantity); // если нужно дублировать общее количество

                // 4. Создаём позицию заказа
                OrderItem item = new OrderItem();
                item.setGoods(goods);
                item.setQuantity(quantity);
                item.setOrder(order); // двусторонняя связь

                // 5. Привязываем позицию к заказу
                order.setItems(List.of(item));

                orders.add(order);
            }

            // Сохраняем все заказы (каскадно сохранятся и позиции)
            orderRepository.saveAll(orders);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка при импорте заказов: " + e.getMessage());
        }
    }
}