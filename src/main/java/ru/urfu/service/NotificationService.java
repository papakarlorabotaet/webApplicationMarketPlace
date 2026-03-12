package ru.urfu.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import ru.urfu.entity.Order;
import ru.urfu.entity.OrderItem;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final JavaMailSender mailSender;

    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }



    public void sendOrderStatusNotification(Order order) {
        if (order.getBuyer() == null || order.getBuyer().getEmail() == null) return;

        List<OrderItem> items = order.getItems();
        String goodsDescription; // // Формируем описание товаров
        if (items.isEmpty()) {
            goodsDescription = "нет товаров";
        } else if (items.size() == 1) {
            goodsDescription = "товар '" + items.get(0).getGoods().getName() + "'";
        } else {
            // Собираем названия (можно ограничить длину, если нужно)
            String names = items.stream()
                    .map(item -> item.getGoods().getName())
                    .collect(Collectors.joining(", "));
            goodsDescription = "товары: " + names;
        }


        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("a4aaba001@smtp-brevo.com"); // Должен совпадать с spring.mail.username
        message.setTo(order.getBuyer().getEmail());
        message.setSubject("MarketPlace: Статус заказа №" + order.getId() + " изменен");
        message.setText("Здравствуйте!\n\nВаш заказ на товар '" + goodsDescription +
                "' перешел в статус: " + order.getStatus());

        try {
            //mailSender.send(message); // Реальная отправка
            System.out.println("TESTMESSAGES");
            System.out.println("TESTMESSAGES");
            System.out.println("TESTMESSAGES");
            System.out.println("TESTMESSAGES");
            System.out.println("TESTMESSAGES");
        } catch (Exception e) {
            System.err.println("Ошибка при отправке email на " + order.getBuyer().getEmail());
            e.printStackTrace(); // Вывод реальной ошибки в лог
        }
    }
}

