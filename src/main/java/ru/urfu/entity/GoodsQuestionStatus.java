package ru.urfu.entity;

public enum GoodsQuestionStatus {
    PENDING,   // Ожидает проверки модератором
    APPROVED,  // Одобрен (виден на странице товара)
    REJECTED   // Отклонен/Удален
}