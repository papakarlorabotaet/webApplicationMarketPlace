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
import ru.urfu.repository.UserRepository;

import javax.persistence.EntityNotFoundException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class GoodsServiceImpl implements GoodsService {

    private final GoodsRepository goodsRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    public GoodsServiceImpl(GoodsRepository goodsRepository,
                            UserRepository userRepository,
                            MessageRepository messageRepository) {
        this.goodsRepository = goodsRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
    }

    @Override
    public void saveGoods(GoodsDto goodsDto, String email) {
        // Находим пользователя (который выступает в роли продавца)
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new EntityNotFoundException("Пользователь с email " + email + " не найден");
        }

        Goods goods = new Goods();
//        goods.setsellerName(goodsDto.getSellerName());
        goods.setName(goodsDto.getName());
        goods.setDescription(goodsDto.getDescription());
        goods.setPrice(goodsDto.getPrice());
        goods.setUser(user); // Привязываем к User (в сущности Goods поле должно называться user)
        goods.setModerationStatus(GoodsStatus.PENDING);
        goods.setDeliveryStatus(0);

        goodsRepository.save(goods);
    }

    @Override //Метод поиска товаров (для каталога)
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
                .orElseThrow(() -> new EntityNotFoundException("Товар с id " + id + " не найден"));
    }

    @Override
    public List<GoodsDto> findAllGoods() {
        return goodsRepository.findAll().stream()
                .map(this::goodsToGoodsDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<GoodsDto> findAllApprovedGoods() {
        return goodsRepository.findByModerationStatus(GoodsStatus.APPROVED).stream()
                .map(this::goodsToGoodsDto)
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
        Goods goods = goodsRepository.findById(id).orElseThrow();
        goods.setModerationStatus(status);
        goodsRepository.save(goods);

        // Уведомление продавцу в чат
        Message msg = new Message();
        msg.setSender(sender);
        msg.setReceiver(goods.getUser());
        msg.setContent("Статус вашего товара '" + goods.getName() + "' обновлен на: " + status);
        msg.setTimestamp(java.time.LocalDateTime.now());
        // Отправителя можно оставить null (системное)
        messageRepository.save(msg);
    }

    @Override
    public GoodsDto goodsToGoodsDto(Goods goods) {
        GoodsDto dto = new GoodsDto();
        dto.setSellerName(goods.getUser().getName());
        dto.setSellerEmail(goods.getUser().getEmail());
        dto.setId(goods.getId());
        dto.setName(goods.getName());
        dto.setDescription(goods.getDescription());
        dto.setPrice(goods.getPrice());
        dto.setQuantity(goods.getQuantity());
        // При необходимости можно добавить статус модерации или email продавца
        return dto;
    }

    @Override
    public void updateGoods(GoodsDto goodsDto) {

        Goods goods = goodsRepository.findById(goodsDto.getId())
                .orElseThrow(() -> new RuntimeException("Товар не найден"));

        goods.setName(goodsDto.getName());
        goods.setDescription(goodsDto.getDescription());
        goods.setPrice(goodsDto.getPrice());

        goodsRepository.save(goods);
    }

    @Override
    public void importGoods(MultipartFile file, String email) {
        User seller = userRepository.findByEmail(email);
        List<Goods> goodsList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                } // Пропуск заголовка

                String[] parts = line.split(",");
                if (parts.length < 3 || parts[0].isBlank()) continue;

                Goods goods = new Goods();
                goods.setName(parts[0].trim());
                goods.setDescription(parts[1].trim());
                try {// Парсим цену как BigDecimal
                    BigDecimal price = new BigDecimal(parts[2].trim());
                    goods.setPrice(price);
                } catch (NumberFormatException e) {
                    // Можно выбросить исключение
                    throw new RuntimeException("Неверный формат цены в строке: " + line);
                }
                goods.setUser(seller);
                goods.setModerationStatus(GoodsStatus.PENDING);
                goods.setDeliveryStatus(0);

                goodsList.add(goods);
            }
            goodsRepository.saveAll(goodsList);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка парсинга CSV: " + e.getMessage());
        }
    }


}