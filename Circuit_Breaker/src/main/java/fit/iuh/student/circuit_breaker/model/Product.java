package fit.iuh.student.circuit_breaker.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class Product {
    private String id;
    private String name;
    private String description;
    private double price;
    private String supplier;

    public Product(String id, String name, String description, double price, String supplier) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.supplier = supplier;
    }
    public Product() {
        // Constructor mặc định
    }
}
