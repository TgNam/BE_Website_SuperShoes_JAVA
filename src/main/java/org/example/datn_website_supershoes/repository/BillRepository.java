package org.example.datn_website_supershoes.repository;


import org.example.datn_website_supershoes.dto.response.BillResponse;
import org.example.datn_website_supershoes.dto.response.BillStatisticalPieResponse;
import org.example.datn_website_supershoes.dto.response.BillSummaryResponse;
import org.example.datn_website_supershoes.model.Bill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {


    Page<Bill> findAll(Specification<Bill> spec, Pageable pageable);

    @Query("SELECT new org.example.datn_website_supershoes.dto.response.BillSummaryResponse(" +
            "b.codeBill,b.status, b.nameCustomer, b.address, b.phoneNumber, b.note) " +
            "FROM Bill b " +
            "WHERE b.codeBill = :codeBill")
    Optional<BillSummaryResponse> findBillSummaryByCodeBill(@Param("codeBill") String codeBill);

    //dùng cho bán hàng
    @Query("SELECT b FROM Bill b WHERE b.codeBill = :codeBill")
    Optional<Bill> findByCodeBill(@Param("codeBill") String codeBill);

    @Query("""
            SELECT new org.example.datn_website_supershoes.dto.response.BillResponse(
            b.id,b.codeBill,b.nameCustomer ,b.phoneNumber,b.address,
            b.note,b.type,b.deliveryDate,b.receiveDate,b.totalMerchandise,
            b.priceDiscount,b.totalAmount, v.id,v.codeVoucher,v.name, v.value,
            cus.id,emp.id, emp.name, b.status , b.createdAt) 
            FROM Bill b
            LEFT JOIN b.voucher v
            LEFT JOIN Account cus ON cus.id = b.customer.id
            LEFT JOIN Account emp ON emp.id = b.employees.id
            WHERE b.codeBill = :codeBill
            """)
    Optional<BillResponse> findBillResponseByCodeBill(@Param("codeBill") String codeBill);


    @Query(value = """
    SELECT 
        DATE(b.created_at) AS createdAt, 
        COUNT(b.id) AS numberBill, 
        SUM(b.total_amount) AS price
    FROM 
        bill b 
    WHERE 
        b.status = 'COMPLETED'
    GROUP BY 
        DATE(b.created_at)
    ORDER BY 
        DATE(b.created_at) DESC
""", nativeQuery = true)
    List<Object[]> findCompletedBillStatisticsByYear();



}
