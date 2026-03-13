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

import ru.urfu.dto.GoodsDto;
import ru.urfu.entity.*;
import ru.urfu.repository.CategoryRepository; // Раскомментируй/добавь, если используешь категории
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
    // private final CategoryRepository categoryRepository; // Добавь, если нужны категории при создании товара

    @Value("${upload.path}")
    private String uploadDir;

    public SellerController(UserRepository userRepository, OrderService orderService,
                            GoodsService goodsService, GoodsRepository goodsRepository,
                            OrderRepository orderRepository, CartService cartService) {
        this.userRepository = userRepository;
        this.orderService = orderService;
        this.goodsService = goodsService;
        this.goodsRepository = goodsRepository;
        this.orderRepository = orderRepository;
        this.cartService = cartService;
    }

    // ==========================================
    // ✅ ПРОФИЛЬ И ДАШБОРД
    // ==========================================

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User seller = userRepository.findByEmail(userDetails.getUsername());
        model.addAttribute("user", seller);
        model.addAttribute("orders", orderService.findOrdersBySeller(seller));
        model.addAttribute("myGoods", goodsService.findGoodsByUserEmail(seller.getEmail()));
        return "seller/profileSeller";
    }

    @GetMapping("/dashboard")
    public String showDashboard() {
        return "seller/dashboard";
    }

    @GetMapping("/api/dashboard")
    @ResponseBody
    public Map<String, Object> getDashboardData(@AuthenticationPrincipal UserDetails userDetails) {
        User seller = userRepository.findByEmail(userDetails.getUsername());
        List<Order> orders = orderService.findOrdersBySeller(seller);

        List<String> labels = new ArrayList<>();
        List<BigDecimal> revenues = new ArrayList<>();

        Map<String, BigDecimal> revenuePerGoods = orders.stream()
                .filter(o -> "Оплачен".equals(o.getStatus()) || "Доставлен".equals(o.getStatus()))
                .flatMap(o -> o.getItems().stream())
                .collect(Collectors.groupingBy(
                        item -> item.getGoods().getName(),
                        Collectors.reducing(BigDecimal.ZERO, OrderItem::getTotalPrice, BigDecimal::add)
                ));

        revenuePerGoods.forEach((name, rev) -> {
            labels.add(name);
            revenues.add(rev);
        });

        Map<String, Object> data = new HashMap<>();
        data.put("labels", labels);
        data.put("revenues", revenues);
        return data;
    }

    // ==========================================
    // ✅ МОИ ТОВАРЫ (CRUD)
    // ==========================================

    @GetMapping("/my-goods")
    public String myGoods(@AuthenticationPrincipal UserDetails userDetails, Model model,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String search) {
        User seller = userRepository.findByEmail(userDetails.getUsername());
        List<GoodsDto> goodsList = goodsService.findGoodsByUserEmail(seller.getEmail());

        // 🔍 Фильтрация по поиску
        if (search != null && !search.isEmpty()) {
            goodsList = goodsList.stream()
                    .filter(g -> g.getName().toLowerCase().contains(search.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // 🔍 Фильтрация по статусу (опционально)
        if (status != null && !status.isEmpty()) {
            try {
                GoodsStatus targetStatus = GoodsStatus.valueOf(status.toUpperCase());
                goodsList = goodsList.stream()
                        .filter(g -> g.getModerationStatus() == targetStatus)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Игнорируем некорректный статус
            }
        }

        // 📊 Расчёт статистики для правой колонки
        long totalGoods = goodsList.size();
        long pendingGoods = goodsList.stream().filter(g -> g.getModerationStatus() == GoodsStatus.PENDING).count();
        long approvedGoods = goodsList.stream().filter(g -> g.getModerationStatus() == GoodsStatus.APPROVED).count();
        long rejectedGoods = goodsList.stream().filter(g -> g.getModerationStatus() == GoodsStatus.REJECTED).count();

        model.addAttribute("goods", goodsList);
        model.addAttribute("user", seller);

        // ✅ Добавляем статистику в модель
        model.addAttribute("totalGoods", totalGoods);
        model.addAttribute("pendingGoods", pendingGoods);
        model.addAttribute("approvedGoods", approvedGoods);
        model.addAttribute("rejectedGoods", rejectedGoods);

        return "seller/myGoods";
    }

    @GetMapping("/goods/add")
    public String addGoodsForm(Model model) {
        // model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("goods", new Goods());
        return "seller/editGoods"; // Используем один шаблон для добавления и редактирования, если удобно, либо создай seller/goods-add.html
    }

    @PostMapping("/goods/add")
    public String addGoods(@RequestParam String name,
                           @RequestParam String description,
                           @RequestParam BigDecimal price,
                           @RequestParam("image") MultipartFile image,
                           @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        User seller = userRepository.findByEmail(userDetails.getUsername());
        Goods goods = new Goods();
        goods.setName(name);
        goods.setDescription(description);
        goods.setPrice(price);
        goods.setUser(seller);
        goods.setModerationStatus(GoodsStatus.PENDING);

        String filename = saveImageFile(image);
        if (filename != null) {
            goods.setImagePath(filename);
        }

        goodsRepository.save(goods);
        return "redirect:/seller/my-goods?success=created";
    }

    @GetMapping("/goods/edit/{id}")
    public String editGoodsForm(@PathVariable Long id, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        Goods goods = goodsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid goods Id:" + id));

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
        goods.setQuantity(quantity);

        String filename = saveImageFile(image);
        if (filename != null) {
            goods.setImagePath(filename);
        }

        goodsRepository.save(goods);
        return "redirect:/seller/my-goods?success=updated";
    }

    @PostMapping("/goods/delete/{id}")
    public String deleteGoods(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        Goods goods = goodsRepository.findById(id).orElseThrow();
        if (goods.getUser().getEmail().equals(userDetails.getUsername())) {
            goodsRepository.delete(goods);
            return "redirect:/seller/my-goods?success=deleted";
        }
        return "redirect:/seller/my-goods?error=access_denied";
    }

    // ==========================================
    // ✅ ЗАКАЗЫ ПРОДАВЦА
    // ==========================================

    @GetMapping("/orders")
    public String orders(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User seller = userRepository.findByEmail(userDetails.getUsername());
        model.addAttribute("orders", orderService.findOrdersBySeller(seller));
        return "seller/orders"; // Убедись, что этот шаблон существует!
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id, @RequestParam String status,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        Order order = orderRepository.findById(id).orElseThrow();
        // Здесь можно добавить проверку: принадлежит ли хотя бы один товар из заказа этому продавцу
        order.setStatus(status);
        orderRepository.save(order);
        return "redirect:/seller/orders?success=status_updated";
    }

    @PostMapping("/add-order")
    public String addOrderManually(@RequestParam Long goodsId,
                                   @RequestParam Integer quantity,
                                   @RequestParam String buyerEmail) {
        Goods goods = goodsRepository.findById(goodsId).orElseThrow();
        User buyer = userRepository.findByEmail(buyerEmail);

        if (buyer == null) return "redirect:/seller/profile?error=UserNotFound";
        if (goods.getQuantity() < quantity) return "redirect:/seller/profile?error=not_enough_stock";

        goods.setQuantity(goods.getQuantity() - quantity);
        goodsRepository.save(goods);

        Order order = new Order();
        order.setBuyer(buyer);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("CREATED");
        order.setTotalPrice(goods.getPrice().multiply(BigDecimal.valueOf(quantity)));

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setGoods(goods);
        item.setQuantity(quantity);
        item.setPrice(goods.getPrice());

        order.setItems(Collections.singletonList(item));
        orderRepository.save(order);

        return "redirect:/seller/orders?success=order_created";
    }

    // ==========================================
    // ✅ ИМПОРТ ДАННЫХ
    // ==========================================

    @GetMapping("/import")
    public String importPage() {
        return "seller/import";
    }

    @PostMapping("/import")
    public String importGoods(@RequestParam("file") MultipartFile file,
                              @AuthenticationPrincipal UserDetails userDetails) {
        if (file.isEmpty()) return "redirect:/seller/my-goods?error=empty_file";
        goodsService.importGoods(file, userDetails.getUsername());
        return "redirect:/seller/my-goods?success=imported";
    }

    @PostMapping("/import-orders")
    public String importOrders(@RequestParam("file") MultipartFile file,
                               @AuthenticationPrincipal UserDetails userDetails) {
        if (file.isEmpty()) return "redirect:/seller/orders?error=empty_file";
        orderService.importOrders(file, userDetails.getUsername());
        return "redirect:/seller/orders?success=imported";
    }

    // ==========================================
    // 🛠 ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ==========================================

    /**
     * Сохраняет файл изображения и возвращает его имя. Вынесено для устранения дублирования.
     */
    private String saveImageFile(MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) {
            return null;
        }
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String filename = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
        Path filePath = uploadPath.resolve(filename);
        image.transferTo(filePath.toFile());
        return filename;
    }
}