package ru.urfu.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import ru.urfu.entity.*;
import ru.urfu.repository.GoodsRepository;
import ru.urfu.repository.OrderRepository;
import ru.urfu.repository.UserRepository;
import ru.urfu.service.CartService;
import ru.urfu.service.GoodsService;
import ru.urfu.service.OrderService;


@Controller
@RequestMapping("/seller")
public class SellerController {

    private final UserRepository userRepository;
    private final OrderService orderService;
    private final GoodsService goodsService;
    private final GoodsRepository goodsRepository;
    private final OrderRepository orderRepository;
    private final CartService cartService;

    public SellerController(UserRepository userRepository,
                            OrderService orderService,
                            GoodsService goodsService, GoodsRepository goodsRepository, OrderRepository orderRepository, CartService cartService) {
        this.userRepository = userRepository;
        this.orderService = orderService;
        this.goodsService = goodsService;
        this.goodsRepository = goodsRepository;
        this.orderRepository = orderRepository;
        this.cartService = cartService;
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User seller = userRepository.findByEmail(userDetails.getUsername());

        int cartCount = 0;
        if (userDetails != null) {
            User user = userRepository.findByEmail(userDetails.getUsername());
            cartCount = cartService.getCartByUser(user).getItems().size(); //циферка корзины
        }
        model.addAttribute("user", seller);
        model.addAttribute("orders", orderService.findOrdersBySeller(seller));
        model.addAttribute("myGoods", goodsService.findGoodsByUserEmail(seller.getEmail()));
        model.addAttribute("cartItemsCount", cartCount);
        unreadMessagesCount
        return "seller/profileSeller";
    }

    @PostMapping("/import-orders")
    public String importOrders(@RequestParam("file") MultipartFile file,
                               @AuthenticationPrincipal UserDetails userDetails) {
        orderService.importOrders(file, userDetails.getUsername());
        return "redirect:/seller/profile";
    }

    @PostMapping("/import-goods")
    public String importGoods(@RequestParam("file") MultipartFile file,
                              @AuthenticationPrincipal UserDetails userDetails) {
        if (file.isEmpty()) {
            return "redirect:/seller/profile?error=empty_file";
        }
        goodsService.importGoods(file, userDetails.getUsername());
        return "redirect:/seller/profile?success=imported";
    }

    @Value("${upload.path}")
    private String uploadDir;

    @GetMapping("/my-goods")
    public String myGoods(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User seller = userRepository.findByEmail(userDetails.getUsername());
        model.addAttribute("myGoods", goodsService.findGoodsByUserEmail(seller.getEmail()));
        model.addAttribute("user", seller);
        return "seller/myGoods"; // new template
    }

    @GetMapping("/goods/edit/{id}")
    public String editGoodsForm(@PathVariable Long id, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        Goods goods = goodsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid goods Id:" + id));

        // Проверка: может ли этот продавец редактировать этот товар
        if (!goods.getUser().getEmail().equals(userDetails.getUsername())) {
            return "redirect:/seller/my-goods?error=access_denied";
        }

        model.addAttribute("goods", goods);
        return "seller/editGoods";
    }

    @PostMapping("/goods/edit/{id}")
    public String updateGoods(@PathVariable Long id,
                              @RequestParam String name,
                              @RequestParam String description,
                              @RequestParam BigDecimal price,
                              @RequestParam Long quantity,
                              @RequestParam(value = "image", required = false) MultipartFile image,
                              @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        Goods goods = goodsRepository.findById(id).orElseThrow();
        if (!goods.getUser().getEmail().equals(userDetails.getUsername())) {
            return "redirect:/seller/my-goods?error=access_denied";
        }

        goods.setName(name);
        goods.setDescription(description);
        goods.setPrice(price);
        goods.setQuantity(quantity); // Обновляем склад

        if (image != null && !image.isEmpty()) {
            // ИСПРАВЛЕНИЕ ПУТИ: Создаем абсолютный путь вне временных папок Tomcat
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String filename = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
            Path filePath = uploadPath.resolve(filename);

            image.transferTo(filePath.toFile());
            goods.setImagePath(filename);
        }

        goodsRepository.save(goods);
        return "redirect:/seller/my-goods?success=updated";
    }

    @PostMapping("/goods/delete/{id}")
    public String deleteGoods(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        Goods goods = goodsRepository.findById(id).orElseThrow();

        // Проверка владения
        if (goods.getUser().getEmail().equals(userDetails.getUsername())) {
            goodsRepository.delete(goods);
            return "redirect:/seller/my-goods?success=deleted";
        }

        return "redirect:/seller/my-goods?error=access_denied";
    }

    @PostMapping("/add-product")
    public String addProductManually(@RequestParam String name,
                                     @RequestParam String description,
                                     @RequestParam BigDecimal price,
                                     @RequestParam("image") MultipartFile image,
                                     @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        System.out.println("=== Добавление товара ===");
        System.out.println("Имя: " + name);
        System.out.println("Файл: " + image.getOriginalFilename());
        System.out.println("Пустой? " + image.isEmpty());
        System.out.println("Размер: " + image.getSize());

        User seller = userRepository.findByEmail(userDetails.getUsername());
        Goods goods = new Goods();
        goods.setName(name);
        goods.setDescription(description);
        goods.setPrice(price);
        goods.setUser(seller);
        goods.setModerationStatus(GoodsStatus.PENDING); // adjust as needed
        if (!image.isEmpty()) {
            try {
                String filename = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
                System.out.println("Генерируем имя файла: " + filename);

                Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
                System.out.println("Путь загрузки: " + uploadPath);

                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                    System.out.println("Директория создана");
                }

                Path filePath = uploadPath.resolve(filename);
                image.transferTo(filePath.toFile());
                System.out.println("Файл сохранён: " + filePath);

                goods.setImagePath(filename);
            } catch (IOException e) {
                e.printStackTrace();
                // Можно добавить сообщение об ошибке в модель и вернуть форму
            }
        } else {
            System.out.println("Файл не выбран, imagePath остаётся null");
        }

        goodsRepository.save(goods);
        System.out.println("Товар сохранён с imagePath = " + goods.getImagePath());
        return "redirect:/seller/my-goods";
    }

    @PostMapping("/seller/add-order")
    public String addOrderManually(@RequestParam Long goodsId,
                                   @RequestParam Integer quantity,
                                   @RequestParam String buyerEmail) {

        Goods goods = goodsRepository.findById(goodsId).orElseThrow();
        User buyer = userRepository.findByEmail(buyerEmail);

        if (buyer == null) {
            // Логика создания "виртуального" покупателя или возврат ошибки
            return "redirect:/seller/profile?error=UserNotFound";
        }
        if (goods.getQuantity() < quantity) {

            return "redirect:/seller/profile?error=not_enough_stock";
        }

        // Уменьшаем количество товара
        goods.setQuantity(goods.getQuantity() - quantity);
        goodsRepository.save(goods);

        Order order = new Order();

        order.setBuyer(buyer);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("CREATED");


        BigDecimal total = goods.getPrice().multiply(BigDecimal.valueOf(quantity));
        order.setTotalPrice(total);

        //Создаем позицию заказа (OrderItem) !!!
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setGoods(goods);
        item.setQuantity(quantity);
        item.setPrice(goods.getPrice());

        // Добавляем позицию в список заказа
        order.setItems(Collections.singletonList(item));

        // Сохраняем (если у вас стоит CascadeType.ALL в Order, сохранится и OrderItem)
        orderRepository.save(order);

        return "redirect:/seller/profile";

    }

    @GetMapping("/api/dashboard")
    @ResponseBody
    public Map<String, Object> getDashboardData(@AuthenticationPrincipal UserDetails userDetails) {
        ru.urfu.entity.User seller = userRepository.findByEmail(userDetails.getUsername());
        List<Order> orders = orderService.findOrdersBySeller(seller);

        // Списки для осей графика
        List<String> labels = new ArrayList<>();
        List<java.math.BigDecimal> revenues = new ArrayList<>();

        // Группируем успешные заказы: Название товара -> Сумма выручки
        Map<String, BigDecimal> revenuePerGoods = orders.stream()
                .filter(o -> "Оплачен".equals(o.getStatus()) || "Доставлен".equals(o.getStatus()))
                .flatMap(o -> o.getItems().stream())   // преобразуем заказ в поток его позиций
                .collect(Collectors.groupingBy(
                        item -> item.getGoods().getName(),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                OrderItem::getTotalPrice,  // используем метод из п.2
                                BigDecimal::add
                        )
                ));

        // Заполняем списки для JSON
        revenuePerGoods.forEach((name, rev) -> {
            labels.add(name);
            revenues.add(rev);
        });

        Map<String, Object> data = new HashMap<>();
        data.put("labels", labels);       // Названия товаров
        data.put("revenues", revenues);   // Выручка
        return data;
    }

    // Метод для отображения самой страницы с графиками
    @GetMapping("/dashboard")
    public String showDashboard() {
        // Эта строка говорит Spring Boot: "Верни файл src/main/resources/templates/seller/dashboard.html"
        return "seller/dashboard";
    }



}