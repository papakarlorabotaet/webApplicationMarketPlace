package ru.urfu.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urfu.dto.GoodsDto;
import ru.urfu.entity.Goods;
import ru.urfu.entity.GoodsStatus;

import ru.urfu.entity.User;
import ru.urfu.repository.GoodsRepository;
import ru.urfu.repository.UserRepository;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class GoodsServiceImpl implements GoodsService {

    private final GoodsRepository goodsRepository;
    private final UserRepository userRepository;

    public GoodsServiceImpl(GoodsRepository goodsRepository,
                            UserRepository userRepository) {
        this.goodsRepository = goodsRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void saveGoods(GoodsDto goodsDto, String email) {
        // Находим пользователя (который выступает в роли продавца)
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new EntityNotFoundException("Пользователь с email " + email + " не найден");
        }

        Goods goods = new Goods();
        goods.setName(goodsDto.getName());
        goods.setDescription(goodsDto.getDescription());
        goods.setPrice(goodsDto.getPrice());
        goods.setUser(user); // Привязываем к User (в сущности Goods поле должно называться user)
        goods.setModerationStatus(GoodsStatus.PENDING);

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
    public void updateStatus(Long id, GoodsStatus goodsStatus) {
        Goods goods = goodsRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Товар с id " + id + " не найден"));
        goods.setModerationStatus(goodsStatus);
        goodsRepository.save(goods);
    }

    @Override
    public GoodsDto goodsToGoodsDto(Goods goods) {
        GoodsDto dto = new GoodsDto();
        dto.setId(goods.getId());
        dto.setName(goods.getName());
        dto.setDescription(goods.getDescription());
        dto.setPrice(goods.getPrice());
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
}