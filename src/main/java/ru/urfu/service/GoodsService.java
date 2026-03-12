package ru.urfu.service;

import ru.urfu.dto.GoodsDto;
import ru.urfu.entity.Goods;
import ru.urfu.entity.GoodsStatus;
import ru.urfu.entity.User;

import java.math.BigDecimal;
import java.util.List;

public interface GoodsService {
    void saveGoods(GoodsDto goodsDto, String email); // Создание товара пользователем

    void deleteGoods(Long goodsId);

    Goods findGoodsById(Long id);

    List<GoodsDto> findAllGoods();

    //    List<GoodsDto> findAllApprovedGoods();
    List<ru.urfu.dto.GoodsDto> findFilteredGoods(Long categoryId,
                                                 String search,
                                                 BigDecimal minPrice,
                                                 BigDecimal maxPrice,
                                                 String sellerEmail);

    List<GoodsDto> findApprovedGoodsByCategoryId(Long categoryId);

    List<GoodsDto> findAllPendingGoods();

    List<GoodsDto> findGoodsByUserEmail(String email); // Изменили название метода

    GoodsDto goodsToGoodsDto(Goods goods);

    void updateGoods(GoodsDto goodsDto);

    List<GoodsDto> searchGoods(String name);

    void importGoods(org.springframework.web.multipart.MultipartFile file, String email); // Массовая загрузка товаров (CSV/Excel)

    void updateStatus(Long goodsId, GoodsStatus status, User sender);
}