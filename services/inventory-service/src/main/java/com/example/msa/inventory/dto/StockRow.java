package com.example.msa.inventory.dto;

/** STOCK 테이블의 한 줄이다. 상품과 남은 수량을 담는다. */
public record StockRow(String productId, int quantity) {}
