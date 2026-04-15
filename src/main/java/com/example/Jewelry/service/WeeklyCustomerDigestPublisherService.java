package com.example.Jewelry.service;

import com.example.Jewelry.event.WeeklyCustomerDigestEvent;
import com.example.Jewelry.model.entity.Customer;
import com.example.Jewelry.model.entity.Order;
import com.example.Jewelry.model.entity.OrderDetail;
import com.example.Jewelry.model.entity.Product;
import com.example.Jewelry.repository.OrderDetailRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WeeklyCustomerDigestPublisherService {

    private static final String PAID_STATUS = "PAID";

    private final OrderDetailRepository orderDetailRepository;
    private final ApplicationEventPublisher eventPublisher;

    public WeeklyCustomerDigestPublisherService(OrderDetailRepository orderDetailRepository,
                                                ApplicationEventPublisher eventPublisher) {
        this.orderDetailRepository = orderDetailRepository;
        this.eventPublisher = eventPublisher;
    }

    public void publishDigestForPreviousWeek() {
        LocalDate today = LocalDate.now();
        LocalDate weekStartDate = today.minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate weekEndDate = weekStartDate.plusDays(6);

        LocalDateTime weekStart = weekStartDate.atStartOfDay();
        LocalDateTime weekEnd = weekEndDate.atTime(LocalTime.MAX);

        List<OrderDetail> paidDetails = orderDetailRepository
            .findByOrderStatusAndOrderDateRange(PAID_STATUS, weekStart, weekEnd);

        WeeklyCustomerDigestEvent digestEvent = buildEvent(weekStartDate, weekEndDate, paidDetails);
        eventPublisher.publishEvent(digestEvent);
    }

    private WeeklyCustomerDigestEvent buildEvent(LocalDate weekStartDate,
                                                 LocalDate weekEndDate,
                                                 List<OrderDetail> paidDetails) {
        Map<Integer, ProductAggregate> productTotals = new HashMap<>();
        Map<Integer, CustomerAggregate> customerTotals = new HashMap<>();

        for (OrderDetail detail : paidDetails) {
            if (detail == null || detail.getOrder() == null || detail.getProduct() == null) {
                continue;
            }

            Order order = detail.getOrder();
            Customer customer = order.getCustomer();
            Product product = detail.getProduct();
            int quantity = Math.max(0, detail.getQuantity());
            if (customer == null || quantity <= 0) {
                continue;
            }

            productTotals
                .computeIfAbsent(product.getProductId(), key -> new ProductAggregate(product.getProductName()))
                .add(quantity);

            customerTotals
                .computeIfAbsent(customer.getCustomerId(), key -> new CustomerAggregate(customer))
                .addPurchase(product.getProductName(), quantity);
        }

        ProductAggregate topProduct = productTotals.values().stream()
            .max(Comparator.comparingInt(ProductAggregate::quantity))
            .orElse(new ProductAggregate("Chua co du lieu", 0));

        List<WeeklyCustomerDigestEvent.CustomerDigestMessage> messages = new ArrayList<>();
        for (CustomerAggregate customerAggregate : customerTotals.values()) {
            messages.add(new WeeklyCustomerDigestEvent.CustomerDigestMessage(
                customerAggregate.customerId,
                customerAggregate.customerName,
                customerAggregate.customerPhone,
                buildCustomerMessage(customerAggregate, topProduct, weekStartDate, weekEndDate)
            ));
        }

        return new WeeklyCustomerDigestEvent(
            weekStartDate,
            weekEndDate,
            topProduct.productName,
            topProduct.quantity,
            messages
        );
    }

    private String buildCustomerMessage(CustomerAggregate customerAggregate,
                                        ProductAggregate topProduct,
                                        LocalDate weekStart,
                                        LocalDate weekEnd) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        StringBuilder builder = new StringBuilder();
        builder.append("Xin chao ")
            .append(customerAggregate.customerName)
            .append(",\n");
        builder.append("Tong ket mua sam tuan ")
            .append(weekStart.format(formatter))
            .append(" - ")
            .append(weekEnd.format(formatter))
            .append(":\n");

        if (customerAggregate.products.isEmpty()) {
            builder.append("- Ban chua co don hang thanh toan trong tuan nay.\n");
        } else {
            builder.append("- Ban da mua: ");
            String productList = customerAggregate.products.entrySet().stream()
                .sorted((left, right) -> Integer.compare(right.getValue(), left.getValue()))
                .map(entry -> entry.getKey() + " x" + entry.getValue())
                .reduce((left, right) -> left + ", " + right)
                .orElse("khong co");
            builder.append(productList).append(".\n");
        }

        builder.append("- San pham ban chay nhat toan cua hang: ")
            .append(topProduct.productName)
            .append(" (")
            .append(topProduct.quantity)
            .append(" san pham/tuan).\n");
        builder.append("Cam on ban da dong hanh cung cua hang!");

        return builder.toString();
    }

    private static class ProductAggregate {
        private final String productName;
        private int quantity;

        private ProductAggregate(String productName) {
            this(productName, 0);
        }

        private ProductAggregate(String productName, int quantity) {
            this.productName = productName == null ? "San pham" : productName;
            this.quantity = quantity;
        }

        private void add(int amount) {
            this.quantity += amount;
        }

        private int quantity() {
            return quantity;
        }
    }

    private static class CustomerAggregate {
        private final Integer customerId;
        private final String customerName;
        private final String customerPhone;
        private final Map<String, Integer> products = new LinkedHashMap<>();

        private CustomerAggregate(Customer customer) {
            this.customerId = customer.getCustomerId();
            this.customerName = normalize(customer.getCustomerName(), "Khach hang");
            this.customerPhone = normalize(customer.getPhone(), "");
        }

        private void addPurchase(String productName, int quantity) {
            String key = normalize(productName, "San pham");
            products.merge(key, quantity, Integer::sum);
        }

        private static String normalize(String value, String fallback) {
            if (value == null) {
                return fallback;
            }
            String trimmed = value.trim();
            return trimmed.isEmpty() ? fallback : trimmed;
        }
    }
}
