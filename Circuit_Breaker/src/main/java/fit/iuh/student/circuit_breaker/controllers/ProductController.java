package fit.iuh.student.circuit_breaker.controllers;

import fit.iuh.student.circuit_breaker.client.MockAPIClient;
import fit.iuh.student.circuit_breaker.model.Product;
import fit.iuh.student.circuit_breaker.services.ProductService;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;
    @Autowired
    private MockAPIClient mockAPIClient;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    @GetMapping("/ratelimiter")
    public CompletableFuture<List<Product>> getAllProductsWithRateLimiter() {
        return mockAPIClient.getAllProductsWithRateLimiter();
    }
    @GetMapping
    @Retry(name = "mockApiRetry")
    public ResponseEntity<List<Product>> getAllProducts() throws ExecutionException, InterruptedException {
        List<Product> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/timelimiter")
    public CompletableFuture<List<Product>> getAllProductsWithTimeLimiter() {
        return mockAPIClient.getAllProductsWithTimeLimiter();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product createdProduct = productService.createProduct(product);
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }
}
