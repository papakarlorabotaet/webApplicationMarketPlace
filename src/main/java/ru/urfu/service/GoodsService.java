package ru.urfu.service;

import ru.urfu.dto.GoodsDto;
import ru.urfu.entity.Goods;
import ru.urfu.entity.GoodsStatus;

import java.util.List;

public interface GoodsService {
    void saveGoods(GoodsDto goodsDto, String email); // Создание товара пользователем
    void deleteGoods(Long goodsId);
    Goods findGoodsById(Long id);
    List<GoodsDto> findAllGoods();
    List<GoodsDto> findAllApprovedGoods();
    List<GoodsDto> findAllPendingGoods();
    List<GoodsDto> findGoodsByUserEmail(String email); // Изменили название метода
    void updateStatus(Long id, GoodsStatus goodsStatus);
    GoodsDto goodsToGoodsDto(Goods goods);
    void updateGoods(GoodsDto goodsDto);
    List<GoodsDto> searchGoods(String name);
}