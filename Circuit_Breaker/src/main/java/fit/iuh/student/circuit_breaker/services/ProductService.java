package fit.iuh.student.circuit_breaker.services;

import fit.iuh.student.circuit_breaker.client.MockAPIClient;
import fit.iuh.student.circuit_breaker.model.Product;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class ProductService {
    private  final MockAPIClient mockAPIClient;
    public  ProductService(MockAPIClient mockAPIClient) {
        this.mockAPIClient = mockAPIClient;
    }

    public List<Product> getAllProducts() throws ExecutionException, InterruptedException {
        try {
            CompletableFuture<List<Product>> future = mockAPIClient.getAllProducts();
            return future.get(); // Đợi kết quả từ CompletableFuture
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Error while getting all products: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve products", e);
        }
    }
    public Product getProductById(String id) {
        try {
            CompletableFuture<Product> future = mockAPIClient.getProductById(id);
            return future.get(); // Đợi kết quả từ CompletableFuture
        } catch (InterruptedException | ExecutionException e) {
//            log.error("Error while getting product with id: {}", id, e);
            throw new RuntimeException("Failed to retrieve product with id: " + id, e);
        }
    }

    public Product createProduct(Product product) {
        try {
            CompletableFuture<Product> future = mockAPIClient.createProduct(product);
            return future.get(); // Đợi kết quả từ CompletableFuture
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to create product", e);
        }
    }
}
