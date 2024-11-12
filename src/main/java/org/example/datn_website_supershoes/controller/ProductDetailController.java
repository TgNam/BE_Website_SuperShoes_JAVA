package org.example.datn_website_supershoes.controller;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

import org.example.datn_website_supershoes.dto.response.*;
import org.example.datn_website_supershoes.model.Color;
import org.example.datn_website_supershoes.model.Product;
import org.example.datn_website_supershoes.model.ProductDetail;
import org.example.datn_website_supershoes.model.Size;
import org.example.datn_website_supershoes.repository.ColorRepository;
import org.example.datn_website_supershoes.repository.ProductRepository;
import org.example.datn_website_supershoes.repository.SizeRepository;
import org.example.datn_website_supershoes.service.ProductDetailService;
import org.example.datn_website_supershoes.service.ProductImageService;
import org.example.datn_website_supershoes.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.awt.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/productDetail")
public class ProductDetailController {

    @Autowired
    private ProductDetailService productDetailService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SizeRepository sizeRepository;

    @Autowired
    private ColorRepository colorRepository;

    @Autowired
    private ProductService productService;

    @GetMapping("/list-productDetail")
    public ResponseEntity<Map<String, Object>> getAllProductDetail(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "category", required = false) Long categoryId,
            @RequestParam(value = "brand", required = false) Long brandId,
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "productId", required = false) Long productId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "productCode", required = false) String productCode,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "300") int size
    ) {
        Specification<ProductDetail> spec = (root, query, criteriaBuilder) -> {
            Predicate p = criteriaBuilder.conjunction();
            if (status != null && !status.isEmpty()) {
                p = criteriaBuilder.and(p, criteriaBuilder.equal(root.get("status"), status));
            }
            if (id != null) {
                p = criteriaBuilder.and(p, criteriaBuilder.equal(root.get("id"), id));
            }
            if (productId != null) {
                Join<ProductDetail, Product> productJoin = root.join("product");
                p = criteriaBuilder.and(p, criteriaBuilder.equal(productJoin.get("id"), productId));
            }
            if (name != null && !name.isEmpty()) {
                Join<ProductDetail, Product> productJoin = root.join("product");
                p = criteriaBuilder.and(p, criteriaBuilder.like(criteriaBuilder.lower(productJoin.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (productCode != null && !productCode.trim().isEmpty()) {
                Join<ProductDetail, Product> productJoin = root.join("product");
                p = criteriaBuilder.and(p, criteriaBuilder.like(criteriaBuilder.lower(productJoin.get("productCode")), "%" + productCode.toLowerCase() + "%"));
            }
            if (categoryId != null) {
                Join<ProductDetail, Product> productJoin = root.join("product");
                p = criteriaBuilder.and(p, criteriaBuilder.equal(productJoin.get("category").get("id"), categoryId));
            }
            if (brandId != null) {
                Join<ProductDetail, Product> productJoin = root.join("product");
                p = criteriaBuilder.and(p, criteriaBuilder.equal(productJoin.get("brand").get("id"), brandId));
            }
            return p;
        };

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDetailResponse> productDetailList = productDetailService.getAllProductDetail(spec, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("DT", productDetailList);
        response.put("EC", 0);
        response.put("EM", "Get all productDetails succeed");
        return ResponseEntity.ok(response);
    }


    @GetMapping("/detail/{id}")
    public ResponseEntity<ProductDetail> getProductById(@PathVariable Long id) {
        Optional<ProductDetail> productDetail = productDetailService.getProductByIdDetail(id);
        return productDetail.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
    private void validateProductDetailFields(ProductDetail productDetail) {

        if (productDetail.getDescription() == null) {
            throw new RuntimeException("Description trong ProductDetail là null");
        }
//        if (productDetail.getProductImage() == null) {
//            throw new RuntimeException("Image trong ProductDetail là null");
//        }
        if (productDetail.getPrice() == null) {
            throw new RuntimeException("Price trong ProductDetail là null");
        }
        if (productDetail.getQuantity() == null) {
            throw new RuntimeException("Quantity trong ProductDetail là null");
        }
    }
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> createProductDetail(@RequestBody ProductDetail productDetail) {
        Map<String, Object> response = new HashMap<>();

        try {
            validateProductDetailFields(productDetail);
            // Kiểm tra productDetail có phải là null không
            if (productDetail == null) {
                response.put("EC", 1);
                response.put("EM", "ProductDetail is null");
                return ResponseEntity.badRequest().body(response);
            }

            // Tìm kiếm và thiết lập các đối tượng
            Product product = productRepository.findById(productDetail.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            Size size = sizeRepository.findById(productDetail.getSize().getId())
                    .orElseThrow(() -> new RuntimeException("Size not found"));
            Color color = colorRepository.findById(productDetail.getColor().getId())
                    .orElseThrow(() -> new RuntimeException("Color not found"));

            String generatedCode = generateProductCode(); // Hàm sinh mã
            product.setProductCode(generatedCode);
            productDetail.setProduct(product);
            productDetail.setSize(size);
            productDetail.setColor(color);


            // Lưu ProductDetail
            ProductDetail createdProductDetail = productDetailService.createProductDetail(productDetail);

            // Tạo phản hồi thành công
            response.put("DT", createdProductDetail);
            response.put("EC", 0);  // Mã lỗi: 0 cho thành công
            response.put("EM", "ProductDetail added successfully");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // Xử lý lỗi và phản hồi
            response.put("EC", 1);
            response.put("EM", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @PutMapping("/update/{id}")
    public ResponseEntity<Map<String, Object>> updateProductDetail(@PathVariable Long id, @RequestBody ProductDetail productDetail) {
        // Tìm sản phẩm hiện tại bằng ID
        Optional<ProductDetail> existingProductOpt = productDetailService.getProductByIdDetail(id);

        if (!existingProductOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("EC", 1, "EM", "ProductDetail not found"));
        }

        // Lấy sản phẩm hiện tại
        ProductDetail existingProduct = existingProductOpt.get();

        // Cập nhật các trường nếu có
        if (productDetail.getStatus() != null) {
            existingProduct.setStatus(productDetail.getStatus());
        }
        if (productDetail.getQuantity() != null) {
            existingProduct.setQuantity(productDetail.getQuantity());
        }
        if (productDetail.getPrice() != null) {
            existingProduct.setPrice(productDetail.getPrice());
        }
        if (productDetail.getDescription() != null) {
            existingProduct.setDescription(productDetail.getDescription());
        }
        if (productDetail.getProductImage() != null) {
            existingProduct.setProductImage(productDetail.getProductImage());
        }

        // Gọi phương thức updateProductDetail với ID và đối tượng ProductDetail
        ProductDetail updatedProductDetail = productDetailService.updateProductDetail(id, existingProduct);

        // Tạo phản hồi JSON
        Map<String, Object> response = new HashMap<>();
        response.put("DT", updatedProductDetail);
        response.put("EC", 0);
        response.put("EM", "ProductDetail updated successfully");

        return ResponseEntity.ok(response);
    }


    @PutMapping("/updateStatus/{id}")
    public ResponseEntity<Map<String, Object>> updateProductDetailStatus(@PathVariable Long id, @RequestBody ProductDetail productDetail) {
        // Tìm sản phẩm hiện tại bằng ID
        Optional<ProductDetail> existingProductOpt = productDetailService.getProductByIdDetail(id);

        if (!existingProductOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        // Lấy sản phẩm hiện tại
        ProductDetail existingProduct = existingProductOpt.get();

        // Chỉ cập nhật trạng thái
        existingProduct.setStatus(productDetail.getStatus()); // Cập nhật chỉ trường trạng thái

        // Lưu sản phẩm đã được cập nhật
        ProductDetail updateProductDetail = productDetailService.createProductDetail(existingProduct);

        Map<String, Object> response = new HashMap<>();
        response.put("DT", updateProductDetail);
        response.put("EC", 0);
        response.put("EM", "ProductDetail updated successfully");

        return ResponseEntity.ok(response);
    }

//    @PutMapping("/updateProductStatus/{id}")
//    public ResponseEntity<Map<String, Object>> updateStatus(@PathVariable Long id, @RequestBody ProductDetail productDetail) {
//        // Tìm ProductDetail hiện tại bằng ID
//        Optional<ProductDetail> existingProductDetailOpt = productDetailService.getProductByIdDetail(id);
//
//        if (!existingProductDetailOpt.isPresent()) {
//            return ResponseEntity.notFound().build();
//        }
//
//        // Lấy ProductDetail hiện tại
//        ProductDetail existingProductDetail = existingProductDetailOpt.get();
//
//        // Cập nhật trạng thái của ProductDetail
//        existingProductDetail.setStatus(productDetail.getStatus());
//
//        // Lấy Product liên kết với ProductDetail và cập nhật trạng thái
//        Product product = existingProductDetail.getProduct();
//        if (product != null) {
//            product.setStatus(productDetail.getStatus());
//            productService.createProduct1(product); // Lưu Product sau khi cập nhật
//        }
//
//        // Lưu ProductDetail sau khi cập nhật
//        ProductDetail updatedProductDetail = productDetailService.createProductDetail(existingProductDetail);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("DT", updatedProductDetail);
//        response.put("EC", 0);
//        response.put("EM", "Status of Product and ProductDetail updated successfully");
//
//        return ResponseEntity.ok(response);
//    }

    @PutMapping("/updateProductStatus/{id}")
    public ResponseEntity<Map<String, Object>> updateStatus(@PathVariable Long id, @RequestBody ProductDetail productDetail) {
        // Tìm ProductDetail hiện tại bằng ID
        Optional<ProductDetail> existingProductDetailOpt = productDetailService.getProductByIdDetail(id);

        if (!existingProductDetailOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        // Lấy ProductDetail hiện tại
        ProductDetail existingProductDetail = existingProductDetailOpt.get();

        // Cập nhật trạng thái của ProductDetail
        existingProductDetail.setStatus(productDetail.getStatus());

        // Lấy Product liên kết với ProductDetail và cập nhật trạng thái
        Product product = existingProductDetail.getProduct();
        if (product != null) {
            product.setStatus(productDetail.getStatus());
            productService.createProduct1(product); // Lưu Product sau khi cập nhật

            // Lấy tất cả ProductDetail liên kết với Product và cập nhật trạng thái
            List<ProductDetail> productDetails = productDetailService.getProductDetailsByProductId(product.getId());
            for (ProductDetail detail : productDetails) {
                detail.setStatus(productDetail.getStatus());
                productDetailService.createProductDetail(detail); // Lưu từng ProductDetail sau khi cập nhật
            }
        }

        // Lưu ProductDetail sau khi cập nhật
        ProductDetail updatedProductDetail = productDetailService.createProductDetail(existingProductDetail);

        Map<String, Object> response = new HashMap<>();
        response.put("DT", updatedProductDetail);
        response.put("EC", 0);
        response.put("EM", "Status of Product and associated ProductDetails updated successfully");

        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteProductDetail(@PathVariable Long id) {
        try {
            productDetailService.deleteProductDetail(id);
            return ResponseEntity.status(HttpStatus.OK).body("ProductDetail deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting productDetail");
        }
    }

    @GetMapping("/listProductDetail")
    public ResponseEntity<?> getProductDetail(@RequestParam("idProducts") List<Long> idProducts) {
        List<ProductDetailResponseByNam> productDetails = productDetailService.findProductDetailRequests(idProducts);
        return ResponseEntity.ok(productDetails);
    }

    @GetMapping("/filterListProductDetail")
    public List<ProductDetailResponseByNam> getAllProductDetailSearch(
            @RequestParam("idProducts") List<Long> idProducts,
            @RequestParam("search") String search,
            @RequestParam("nameSize") String nameSize,
            @RequestParam("nameColor") String nameColor,
            @RequestParam("priceRange") String priceRange) {
        return productDetailService.filterListProductDetail(idProducts, search, nameSize, nameColor, priceRange);
    }

    private String generateProductCode() {
        // Sử dụng UUID để tạo một mã duy nhất
        return UUID.randomUUID().toString();
    }


    @Autowired
    private ProductImageService productImageService;

    @GetMapping("/listProductPromotion")
    public ResponseEntity<?> getProductPromotion() {
        List<ProductPromotionResponse> productDetails = productDetailService.findProductPromotion();
        return ResponseEntity.ok(productDetails);
    }
    @GetMapping("/filterListProductPromotion")
    public List<ProductPromotionResponse> getAllProductPromotionSearch(
            @RequestParam("search") String search,
            @RequestParam("nameSize") String nameSize,
            @RequestParam("nameColor") String nameColor,
            @RequestParam("priceRange") String priceRange) {
        return productDetailService.filterListProductPromotion(search,nameSize,nameColor,priceRange);
    }
    @GetMapping("/findProductDetailByIdProductDetail")
    public ResponseEntity<?> findProductDetailByIdProductDetail(@RequestParam(value = "idProductDetail", required = false) Long idProductDetail){
        try {
            if (idProductDetail == null) {
                return ResponseEntity.badRequest().body(
                        Response.builder()
                                .status(HttpStatus.BAD_REQUEST.toString())
                                .mess("Lỗi: Id sản phẩm chi tiết không được để trống!")
                                .build()
                );
            }
            return ResponseEntity.ok(productDetailService.findProductDetailByIdProductDetail(idProductDetail));
        }catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Response.builder()
                            .status(HttpStatus.CONFLICT.toString())
                            .mess(e.getMessage())
                            .build()
                    );
        }
    }
    @GetMapping("/productPriceRangePromotion")
    public ResponseEntity<List<ProductViewCustomerReponse>> getProductPriceRangeWithPromotion() {
        List<ProductViewCustomerReponse> response = productDetailService.getProductPriceRangeWithPromotion();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/findProductPromotionByIdProcuctAndIdColorAndIdSize")
    public ResponseEntity<?> findProductPromotionByIdProcuctAndIdColorAndIdSize(
            @RequestParam(value = "idProduct", required = false) Long idProduct,
            @RequestParam(value = "idColor", required = false) Long idColor,
            @RequestParam(value = "idSize", required = false) Long idSize
    ){
        try{
            if (idProduct == null) {
                return ResponseEntity.badRequest().body(
                        Response.builder()
                                .status(HttpStatus.BAD_REQUEST.toString())
                                .mess("Lỗi: ID sản phẩm không được để trống!")
                                .build()
                );
            }
            if (idColor == null) {
                return ResponseEntity.badRequest().body(
                        Response.builder()
                                .status(HttpStatus.BAD_REQUEST.toString())
                                .mess("Lỗi: ID màu sắc không được để trống!")
                                .build()
                );
            }
            if (idSize == null) {
                return ResponseEntity.badRequest().body(
                        Response.builder()
                                .status(HttpStatus.BAD_REQUEST.toString())
                                .mess("Lỗi: ID kích cỡ  không được để trống!")
                                .build()
                );
            }
            ProductPromotionResponse productPromotionResponse = productDetailService.findProductPromotionByIdProcuctAndIdColorAndIdSize(idProduct,idColor,idSize);
            return ResponseEntity.ok(productPromotionResponse);
        }catch (RuntimeException e){
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Response.builder()
                            .status(HttpStatus.CONFLICT.toString())
                            .mess(e.getMessage())
                            .build()
                    );
        }
    }
}





