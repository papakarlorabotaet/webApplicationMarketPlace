package ru.urfu.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.urfu.dto.GoodsDto;
import ru.urfu.entity.Goods;
import ru.urfu.entity.GoodsStatus;
import ru.urfu.entity.Message;
import ru.urfu.entity.User;
import ru.urfu.repository.GoodsRepository;
import ru.urfu.repository.MessageRepository;
import ru.urfu.repository.ReviewRepository;
import ru.urfu.repository.UserRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;

@Service
@Transactional
public class GoodsServiceImpl implements GoodsService {

    private final GoodsRepository goodsRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final ReviewRepository reviewRepository; // 🔥 Добавляем поле

    public GoodsServiceImpl(GoodsRepository goodsRepository,
                            UserRepository userRepository,
                            MessageRepository messageRepository,
                            ReviewRepository reviewRepository) { // 🔥 Добавляем в конструктор
        this.goodsRepository = goodsRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.reviewRepository = reviewRepository;
    }

    @Override
    public void saveGoods(GoodsDto goodsDto, String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("Пользователь с email " + email + " не найден");
        }

        Goods goods = new Goods();
        goods.setName(goodsDto.getName());
        goods.setDescription(goodsDto.getDescription());
        goods.setPrice(goodsDto.getPrice());
        goods.setQuantity(goodsDto.getQuantity()); // 🔥 Исправлено: было goods.getQuantity()
        goods.setUser(user);
        goods.setModerationStatus(GoodsStatus.PENDING);
        goods.setDeliveryStatus(0);
        goods.setAverageRating(0.0);  // 🔥 Инициализируем рейтинг
        goods.setReviewCount(0L);     // 🔥 Инициализируем счётчик

        goodsRepository.save(goods);
    }

    @Override
    public List<GoodsDto> searchGoods(String name) {
        return goodsRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(this::goodsToGoodsDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteGoods(Long goodsId) {
        goodsRepository.deleteById(goodsId);
    }

    @Override
    public Goods findGoodsById(Long id) {
        return goodsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Товар с id " + id + " не найден"));
    }

    @Override
    public List<GoodsDto> findAllGoods() {
        return goodsRepository.findAll().stream()
                .map(this::goodsToGoodsDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<GoodsDto> findApprovedGoodsByCategoryId(Long categoryId) {
        return goodsRepository.findByCategoryId(categoryId).stream()
                .filter(goods -> goods.getModerationStatus() == GoodsStatus.APPROVED)
                .map(this::goodsToGoodsDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<GoodsDto> findFilteredGoods(Long categoryId,
                                            String search,
                                            BigDecimal minPrice,
                                            BigDecimal maxPrice,
                                            String sellerEmail) {
        return goodsRepository.findFilteredGoods(categoryId, search, minPrice, maxPrice,
                        GoodsStatus.APPROVED, sellerEmail)
                .stream()
                .map(this::goodsToGoodsDto)  // 🔥 Рейтинг подставляется внутри goodsToGoodsDto
                .collect(Collectors.toList());
    }

    @Override
    public List<GoodsDto> findAllPendingGoods() {
        return goodsRepository.findByModerationStatus(GoodsStatus.PENDING).stream()
                .map(this::goodsToGoodsDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<GoodsDto> findGoodsByUserEmail(String email) {
        return goodsRepository.findByUserEmail(email).stream()
                .map(this::goodsToGoodsDto)
                .collect(Collectors.toList());
    }

    @Override
    public void updateStatus(Long id, GoodsStatus status, User sender) {
        Goods goods = goodsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Товар не найден"));

        goods.setModerationStatus(status);
        goodsRepository.save(goods);

        // Уведомление продавцу
        Message msg = new Message();
        msg.setSender(sender);
        msg.setReceiver(goods.getUser());
        msg.setContent("Статус вашего товара '" + goods.getName() + "' обновлён на: " + status);
        msg.setTimestamp(java.time.LocalDateTime.now());
        messageRepository.save(msg);
    }

    @Override
    public GoodsDto goodsToGoodsDto(Goods goods) {
        GoodsDto dto = new GoodsDto();
        dto.setId(goods.getId());
        dto.setName(goods.getName());
        dto.setDescription(goods.getDescription());
        dto.setPrice(goods.getPrice());
        dto.setQuantity(goods.getQuantity());
        dto.setImagePath(goods.getImagePath());
        dto.setModerationStatus(goods.getModerationStatus());

        // Продавец
        if (goods.getUser() != null) {
            dto.setSellerName(goods.getUser().getName());
            dto.setSellerEmail(goods.getUser().getEmail());
        }


        if (goods.getAverageRating() != null) {
            dto.setAverageRating(goods.getAverageRating());
            dto.setReviewCount(goods.getReviewCount() != null ? goods.getReviewCount() : 0L);
        } else {
            // Fallback: считаем из Reviews, если не хранится в Goods
            dto.setAverageRating(calculateAverageRating(goods));
            dto.setReviewCount(reviewRepository.countByGoods(goods));
        }

        return dto;
    }

    @Override
    public void updateGoods(GoodsDto goodsDto) {
        Goods goods = goodsRepository.findById(goodsDto.getId())
                .orElseThrow(() -> new RuntimeException("Товар не найден"));

        goods.setName(goodsDto.getName());
        goods.setDescription(goodsDto.getDescription());
        goods.setPrice(goodsDto.getPrice());
        goods.setQuantity(goodsDto.getQuantity());

        goodsRepository.save(goods);
    }

    @Override
    public void importGoods(MultipartFile file, String email) {
        User seller = userRepository.findByEmail(email);
        List<Goods> goodsList = new ArrayList<>();


        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean isFirstLine = true;
            int importedCount = 0;

            while ((line = reader.readLine()) != null) {
                // Пропускаем заголовок
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                // Пропускаем пустые строки
                if (line.trim().isEmpty()) {
                    continue;
                }

                // Разделяем по запятой или точке с запятой
                String[] parts = line.split("[,;]");

                // ✅ УПРОЩЕНО: теперь только 4 поля
                if (parts.length < 4) {
                    continue; // пропускаем некорректные строки
                }

                try {
                    Goods goods = new Goods();
                    goods.setName(parts[0].trim());
                    goods.setDescription(parts[1].trim());
                    goods.setPrice(new BigDecimal(parts[2].trim()));
                    goods.setQuantity(Long.parseLong(parts[3].trim()));
                    goods.setUser(seller);
                    goods.setModerationStatus(GoodsStatus.PENDING);

                    goodsRepository.save(goods);
                    importedCount++;

                } catch (Exception e) {
                    // Логируем ошибку, но продолжаем импорт
                    System.err.println("Ошибка при импорте строки: " + line);
                }
            }

            System.out.println("Импортировано товаров: " + importedCount);

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при чтении файла: " + e.getMessage());
        }
    }

    // 🔥 Вспомогательный метод для расчёта рейтинга
    private Double calculateAverageRating(Goods goods) {
        Double avg = reviewRepository.getAverageRating(goods);
        return avg != null ? avg : 0.0;
    }
}