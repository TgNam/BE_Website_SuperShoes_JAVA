package org.example.datn_website_supershoes.repository;

import org.example.datn_website_supershoes.dto.response.ProductPromotionResponse;
import org.example.datn_website_supershoes.dto.response.ProductPromotionResponseByQuang;
import org.example.datn_website_supershoes.model.PromotionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionDetailRepository extends JpaRepository<PromotionDetail,Long> {
    @Query("select p from PromotionDetail p where p.productDetail.id=:idProductDetail and p.status IN (:statuses)")
    Optional<PromotionDetail> findPromotionDetailByIdProductDetailAndStatuses(@Param("idProductDetail") Long idProductDetail, @Param("statuses") List<String> statuses);
    @Query("select p from PromotionDetail p where p.promotion.id = :idPromotion and p.status IN (:statuses)")
    List<PromotionDetail> findPromotionDetailByIdPromotionAndStatuses(@Param("idPromotion") Long idPromotion, @Param("statuses") List<String> statuses);

    @Query("""
    SELECT new org.example.datn_website_supershoes.dto.response.ProductPromotionResponse(
    p.id, p.name, c.id, c.name, s.id, s.name, pd.id, pd.quantity, pd.price,
    pro.id, pro.codePromotion,pro.value, pro.endAt, prod.id, prod.quantity, prod.status
    ) 
    FROM PromotionDetail prod
    INNER JOIN prod.promotion pro
    INNER JOIN prod.productDetail pd
    INNER JOIN pd.product p 
    INNER JOIN pd.color c 
    INNER JOIN pd.size s 
    WHERE pro.id=:idPromotion
    """)
    List<ProductPromotionResponse> findProductByIdPromotion(@Param("idPromotion") Long idPromotion);


    @Query("""
    SELECT new org.example.datn_website_supershoes.dto.response.ProductPromotionResponseByQuang(
    p.id, p.name, c.id, c.name, s.id, s.name, pd.id, pd.quantity, pd.price,
    pro.id, pro.codePromotion,pro.value, pro.endAt, prod.id, prod.quantity,p.brand.id,p.category.id
    ) 
    FROM PromotionDetail prod
    INNER JOIN prod.promotion pro
    INNER JOIN prod.productDetail pd
    INNER JOIN pd.product p 
    INNER JOIN pd.color c 
    INNER JOIN pd.size s 
    WHERE pro.id=:idPromotion
    """)
    List<ProductPromotionResponseByQuang> findProductByIdPromotionByQuang(@Param("idPromotion") Long idPromotion);

    Optional<PromotionDetail> findByIdAndAndStatus(Long id, String status);
}
