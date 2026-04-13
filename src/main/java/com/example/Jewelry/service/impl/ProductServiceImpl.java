package com.example.Jewelry.service.impl;

import com.example.Jewelry.model.entity.Category;
import com.example.Jewelry.model.entity.Product;
import com.example.Jewelry.repository.CategoryRepository;
import com.example.Jewelry.repository.ImportDetailRepository;
import com.example.Jewelry.repository.OrderDetailRepository;
import com.example.Jewelry.repository.ProductRepository;
import com.example.Jewelry.service.ProductService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ImportDetailRepository importDetailRepository;

    public ProductServiceImpl(ProductRepository productRepository,
                              CategoryRepository categoryRepository,
                              OrderDetailRepository orderDetailRepository,
                              ImportDetailRepository importDetailRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.importDetailRepository = importDetailRepository;
    }

    @Override
    public Optional<Product> findById(Integer id) {
        return productRepository.findById(id).map(this::normalizeDisplayText);
    }

    @Override
    public List<Product> findAll() {
        List<Product> products = productRepository.findAll();
        products.forEach(this::normalizeDisplayText);
        return products;
    }

    @Override
    public Product save(Product entity) {
        return productRepository.save(entity);
    }

    @Override
    public void deleteById(Integer id) {
        productRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Integer id) {
        return productRepository.existsById(id);
    }

    @Override
    public long count() {
        return productRepository.count();
    }

    @Override
    public Product createProduct(String productCode, String productName, BigDecimal basePrice, String imageUrl, Integer categoryId) {
        validateProductInput(productCode, productName, basePrice, categoryId);

        if (productRepository.findByProductCodeIgnoreCase(productCode.trim()).isPresent()) {
            throw new IllegalArgumentException("Ma san pham da ton tai.");
        }

        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("Danh muc khong ton tai."));

        Product product = new Product();
        product.setProductCode(productCode.trim());
        product.setProductName(productName.trim());
        product.setBasePrice(basePrice);
        product.setImageUrl(normalizeOptionalImageUrl(imageUrl));
        product.setCategory(category);
        return productRepository.save(product);
    }

    @Override
    public Product updateProduct(Integer productId, String productCode, String productName, BigDecimal basePrice, String imageUrl, Integer categoryId) {
        validateProductInput(productCode, productName, basePrice, categoryId);

        Product existingProduct = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("San pham khong ton tai."));

        productRepository.findByProductCodeIgnoreCase(productCode.trim())
            .filter(found -> found.getProductId() != productId)
            .ifPresent(found -> {
                throw new IllegalArgumentException("Ma san pham da ton tai.");
            });

        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("Danh muc khong ton tai."));

        existingProduct.setProductCode(productCode.trim());
        existingProduct.setProductName(productName.trim());
        existingProduct.setBasePrice(basePrice);
        existingProduct.setImageUrl(normalizeOptionalImageUrl(imageUrl));
        existingProduct.setCategory(category);
        return productRepository.save(existingProduct);
    }

    @Override
    public void deleteProduct(Integer productId) {
        if (!productRepository.existsById(productId)) {
            throw new IllegalArgumentException("San pham khong ton tai.");
        }
        if (orderDetailRepository.existsByProduct_ProductId(productId)) {
            throw new IllegalStateException("Khong the xoa san pham da ton tai trong don hang.");
        }
        if (importDetailRepository.existsByProduct_ProductId(productId)) {
            throw new IllegalStateException("Khong the xoa san pham da ton tai trong phieu nhap.");
        }
        productRepository.deleteById(productId);
    }

    private void validateProductInput(String productCode, String productName, BigDecimal basePrice, Integer categoryId) {
        if (productCode == null || productCode.isBlank()) {
            throw new IllegalArgumentException("Ma san pham khong duoc de trong.");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("Ten san pham khong duoc de trong.");
        }
        if (basePrice == null || basePrice.signum() <= 0) {
            throw new IllegalArgumentException("Gia san pham phai lon hon 0.");
        }
        if (categoryId == null) {
            throw new IllegalArgumentException("Danh muc la bat buoc.");
        }
    }

    private String normalizeOptionalImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        return imageUrl.trim();
    }

    private Product normalizeDisplayText(Product product) {
        if (product == null) {
            return null;
        }
        try {
            product.setProductName(fixMojibake(product.getProductName()));
            if (product.getCategory() != null) {
                product.getCategory().setCategoryName(fixMojibake(product.getCategory().getCategoryName()));
            }
        } catch (Exception e) {
            // Log exception nhưng không throw, để tránh làm hỏng toàn bộ danh sách
            System.err.println("Error normalizing product: " + e.getMessage());
        }
        return product;
    }

    private String fixMojibake(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (!value.contains("Ã") && !value.contains("Â") && !value.contains("Ä")) {
            return value;
        }
        return new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
    }
}
