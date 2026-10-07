package com.example.msa.inventory.mapper;

import com.example.msa.inventory.dto.StockRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 재고 수량을 다루는 Mapper다. SQL은 resources/mapper/StockMapper.xml에 있다.
 * 여러 행을 잠글 때는 부르는 쪽이 언제나 상품 ID 순서로 부른다. 순서가 다르면 교착이 생긴다.
 */
@Mapper
public interface StockMapper {

    /** 재고 행을 잠그고 읽는다(SELECT ... FOR UPDATE). 재고 데이터에 없는 상품이면 null이다. */
    StockRow selectStockForUpdate(@Param("productId") String productId);

    /** 수량을 줄인다. 바뀐 행 수를 돌려준다. */
    int updateDecreaseQuantity(@Param("productId") String productId, @Param("quantity") int quantity);

    /** 수량을 되돌린다(해제). 바뀐 행 수를 돌려준다. */
    int updateIncreaseQuantity(@Param("productId") String productId, @Param("quantity") int quantity);
}
