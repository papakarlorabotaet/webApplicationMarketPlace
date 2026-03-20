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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.urfu.dto.GoodsDto;
import ru.urfu.entity.*;
import ru.urfu.repository.*;
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
    private final CartItemRepository cartItemRepository;
    private final CartService cartService;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    // private final CategoryRepository categoryRepository; // Добавь, если нужны категории при создании товара

    @Value("${upload.path}")
    private String uploadDir;

    public SellerController(UserRepository userRepository, OrderService orderService,
                            GoodsService goodsService, GoodsRepository goodsRepository,
                            OrderRepository orderRepository, CartService cartService, CartItemRepository cartItemRepository, AuctionRepository auctionRepository, BidRepository bidRepository) {
        this.userRepository = userRepository;
        this.orderService = orderService;
        this.goodsService = goodsService;
        this.goodsRepository = goodsRepository;
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.cartService = cartService;
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
    }

    // ==========================================
    // ✅ ПРОФИЛЬ И ДАШБОРД
    // ==========================================

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User seller = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
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
        User seller = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
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
        User seller = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        List<GoodsDto> goodsList = goodsService.findGoodsByUserEmail(seller.getEmail());


        try {
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


            //АУКЦИОН
            List<Auction> activeAuctions = auctionRepository.findAllByIsActiveTrueAndGoods_User_Id(seller.getId()); //получаем активные аукционы продавца
            Map<Long, Long> goodsToAuctionId = activeAuctions.stream()
                    .collect(Collectors.toMap(
                            auction -> auction.getGoods().getId(),
                            Auction::getId
                    ));
            model.addAttribute("goodsToAuctionId", goodsToAuctionId);

        } catch (Exception e) {
            e.printStackTrace(); // или логгер
            model.addAttribute("error", e.getMessage());
            return "error";
        }


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
                           @RequestParam Long quantity,
                           @RequestParam("image") MultipartFile image,
                           @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        User seller = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        Goods goods = new Goods();
        goods.setName(name);
        goods.setDescription(description);
        goods.setPrice(price);
        goods.setUser(seller);
        goods.setQuantity(quantity);
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
    public String deleteGoods(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        Goods goods = goodsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

        // Проверка прав доступа
        if (!goods.getUser().getEmail().equals(userDetails.getUsername())) {
            redirectAttributes.addFlashAttribute("error", "Нет прав на удаление этого товара.");
            return "redirect:/seller/my-goods";
        }

        // 1. Проверка, есть ли аукцион, связанный с товаром (любой)
        if (auctionRepository.existsByGoodsId(id)) {
            redirectAttributes.addFlashAttribute("error",
                    "Невозможно удалить товар, так как он участвует в аукционе. Сначала завершите или удалите аукцион.");
            return "redirect:/seller/my-goods";
        }

                        // Проверка, есть ли товар в чьей-либо корзине
        if (cartItemRepository.existsByGoodsId(id)) {
            redirectAttributes.addFlashAttribute("error",
                    "Невозможно удалить товар, так как он находится в корзине у покупателя. Сначала удалите его из корзины.");
            return "redirect:/seller/my-goods";
        }

        // Если проверки пройдены – удаляем
        goodsRepository.delete(goods);
        redirectAttributes.addFlashAttribute("success", "Товар успешно удалён.");
        return "redirect:/seller/my-goods";
    }

    // ==========================================
    // ✅ ЗАКАЗЫ ПРОДАВЦА
    // ==========================================

    @GetMapping("/orders")
    public String orders(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User seller = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        model.addAttribute("orders", orderService.findOrdersBySeller(seller));
        return "seller/orders"; // Убедись, что этот шаблон существует!
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam String status,
                                    @RequestHeader(value = "Referer", required = false) String referer,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        Order order = orderRepository.findById(id).orElseThrow();
        // Здесь можно добавить проверку: принадлежит ли хотя бы один товар из заказа этому продавцу
        order.setStatus(status);
        orderRepository.save(order);
        if (referer != null && referer.contains("/seller/profile")) {
            return "redirect:/seller/profile?success=status_updated";
        }
        return "redirect:/seller/orders?success=status_updated";
    }

    @PostMapping("/add-order")
    public String addOrderManually(@RequestParam Long goodsId,
                                   @RequestParam Integer quantity,
                                   @RequestParam String buyerEmail) {
        Goods goods = goodsRepository.findById(goodsId).orElseThrow();
        User buyer = userRepository.findByEmail(buyerEmail).orElse(null);

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

    @GetMapping("/auction/create")

    public String showCreateAuctionForm(@RequestParam Long goodsId,

                                        @AuthenticationPrincipal UserDetails userDetails,

                                        Model model) {

        Goods goods = goodsRepository.findById(goodsId)

                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));


// проверка прав: товар принадлежит продавцу и одобрен

        User seller = userRepository.findByEmail(userDetails.getUsername()).orElse(null);

        if (!goods.getUser().getId().equals(seller.getId())) {

            return "redirect:/seller/my-goods?error=access_denied";

        }

//        if (goods.getModerationStatus() != GoodsStatus.APPROVED) {
//
//            return "redirect:/seller/my-goods?error=not_approved";
//
//        }


// проверка, что товар ещё не в активном аукционе

        boolean alreadyInAuction = auctionRepository.existsByGoodsIdAndIsActiveTrue(goodsId);

        if (alreadyInAuction) {

            return "redirect:/seller/my-goods?error=already_in_auction";

        }


        model.addAttribute("goods", goods);

        return "/seller/create-auction";

    }


    @PostMapping("/auction/create")
    public String createAuction(@RequestParam Long goodsId,
                                @RequestParam(required = false) BigDecimal startingPrice,
                                @RequestParam(required = false) BigDecimal durationHours,
                                @AuthenticationPrincipal UserDetails userDetails) {

        System.out.println("goodsId = " + goodsId);
        System.out.println("startingPrice = " + startingPrice);
        System.out.println("durationHours = " + durationHours);

        if (goodsId == null || startingPrice == null || durationHours == null) {
            return "redirect:/seller/my-goods?error=missing_params";
        }


        // Поиск товара
        Goods goods = goodsRepository.findById(goodsId)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

        // Поиск текущего пользователя (продавца)
        User seller = userRepository.findByEmail(userDetails.getUsername()).orElse(null);

        // --- ПРОВЕРКИ БЕЗОПАСНОСТИ ---

        // Проверка владения: только хозяин товара может создать аукцион
        if (!goods.getUser().getId().equals(seller.getId())) {
            return "redirect:/seller/my-goods?error=access_denied";
        }

        // Проверка на дубликаты
        if (auctionRepository.existsByGoodsIdAndIsActiveTrue(goodsId)) {
            return "redirect:/seller/my-goods?error=already_in_auction";
        }

        // --- СОЗДАНИЕ АУКЦИОНА ---

        Auction auction = new Auction();
        auction.setGoods(goods);
        auction.setSeller(seller);
        auction.setStartingPrice(startingPrice);
        auction.setCurrentHighestBid(startingPrice); // Начальная "высшая" ставка равна стартовой

        // Установка времени окончания
        auction.setEndTime(LocalDateTime.now().plusHours(durationHours.longValue()));

        // Поля isActive и startTime заполнятся автоматически из дефолтных значений в сущности

        auctionRepository.save(auction);

        return "redirect:/seller/my-goods?success=auction_created";
    }

    @GetMapping("/auctions/{auctionId}")
    public String viewAuction(@PathVariable Long auctionId,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Model model) {
        // 1. Найти аукцион по ID
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Аукцион не найден"));

        // 2. Получить текущего продавца
        User seller = userRepository.findByEmail(userDetails.getUsername()).orElse(null);

        // 3. Проверить, что аукцион принадлежит этому продавцу
        if (!auction.getSeller().getId().equals(seller.getId())) {
            return "redirect:/seller/my-goods?error=access_denied";
        }

        List<Bid> bids = bidRepository.findByAuctionIdOrderByBidTimeDesc(auctionId);
        model.addAttribute("bids", bids);

        // Последняя ставка (первая в списке)
        Bid lastBid = bids.isEmpty() ? null : bids.get(0);
        model.addAttribute("lastBid", lastBid);

        // Вычисляем оставшееся время до окончания аукциона
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = auction.getEndTime();
        Duration remaining = Duration.between(now, endTime);
        model.addAttribute("remainingTime", remaining);
        // Для удобства в шаблоне можно вывести отдельно дни, часы, минуты
        model.addAttribute("remainingDays", remaining.toDays());
        model.addAttribute("remainingHours", remaining.toHours() % 24);
        model.addAttribute("remainingMinutes", remaining.toMinutes() % 60);
        model.addAttribute("remainingSeconds", remaining.getSeconds() % 60);

        model.addAttribute("auction", auction);

        model.addAttribute("auction", auction);

        return "seller/auction-details";
    }

}