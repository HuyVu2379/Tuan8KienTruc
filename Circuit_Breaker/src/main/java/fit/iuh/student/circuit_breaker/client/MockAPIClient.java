package fit.iuh.student.circuit_breaker.client;

import fit.iuh.student.circuit_breaker.model.Product;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
@Component
@Slf4j
public class MockAPIClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String productsEndpoint;

    @Autowired
    public MockAPIClient(
            RestTemplate restTemplate,
            @Value("${mock-api.base-url}") String baseUrl,
            @Value("${mock-api.products-endpoint}") String productsEndpoint) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.productsEndpoint = productsEndpoint;
    }
    @TimeLimiter(name = "mockApiTimeLimiter ")
    public CompletableFuture<List<Product>> getAllProductsWithTimeLimiter() {
        log.info("Fetching all products with TimeLimiter...");
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(3000); // Giả lập độ trễ 3 giây
                return List.of(new Product("1", "Sample Product", "Description", 100.0, "Category"));
            } catch (InterruptedException e) {
                throw new RuntimeException("Error during fetching products", e);
            }
        });
    }
    @RateLimiter(name = "mockApiRateLimiter", fallbackMethod = "rateLimiterFallback")
    public CompletableFuture<List<Product>> getAllProductsWithRateLimiter() {
        log.info("Fetching all products with RateLimiter...");
        return CompletableFuture.supplyAsync(() -> {
            simulateRandomDelay(); // Giả lập một độ trễ ngẫu nhiên
            return List.of(new Product("1", "Sample Product", "Description", 100.0, "Category"));
        });
    }

    private void simulateRandomDelay() {
        try {
            // Generate a random delay between 100ms and 2000ms
            int randomDelay = ThreadLocalRandom.current().nextInt(100, 2001);
            Thread.sleep(randomDelay); // Simulate the delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupted status
            throw new RuntimeException("Simulation of random delay was interrupted", e);
        }
    }

    // Fallback method
    public CompletableFuture<List<Product>> rateLimiterFallback(Throwable ex) {
        log.warn("Rate Limiter triggered. Returning fallback response.");
        return CompletableFuture.completedFuture(Collections.singletonList(
                new Product("0", "Fallback Product", "Fallback Description", 0.0, "Fallback Category")
        ));
    }
    @Retry(name = "mockApiRetry")
//    @CircuitBreaker(name = "mockApiService", fallbackMethod = "getAllProductsFallback")
    public CompletableFuture<List<Product>> getAllProducts() {
        log.info("Fetching all products from MockAPI...");
        return executeAsync(() -> {
            String url = baseUrl + productsEndpoint;
            simulateRandomError();
            ResponseEntity<List<Product>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        });
    }

//    @CircuitBreaker(name = "mockApiService", fallbackMethod = "getProductByIdFallback")
    @Retry(name = "mockApiRetry")
    public CompletableFuture<Product> getProductById(String id) {
        log.info("Fetching product with id: {} from MockAPI...", id);
        return executeAsync(() -> {
            String url = baseUrl + productsEndpoint + "/" + id;
            simulateRandomError();
            Product product = restTemplate.getForObject(url, Product.class);
            if (product == null) {
                log.warn("Product not found for id: {}", id);
                throw new RuntimeException("Product not found with id: " + id);
            }
            return product;
        });
    }

//    @CircuitBreaker(name = "mockApiService", fallbackMethod = "createProductFallback")
    @Retry(name = "mockApiRetry")
    public CompletableFuture<Product> createProduct(Product product) {
        log.info("Creating new product in MockAPI: {}", product);
        return executeAsync(() -> {
            String url = baseUrl + productsEndpoint;
            simulateRandomError();
            Product createdProduct = restTemplate.postForObject(url, product, Product.class);
            if (createdProduct == null) {
                log.warn("Failed to create product: received null response");
                throw new RuntimeException("Failed to create product");
            }
            return createdProduct;
        });
    }

    private <T> CompletableFuture<T> executeAsync(SupplierWithException<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (Exception ex) {
                log.error("Error executing API call: {}", ex.getMessage());
                throw new RuntimeException("API call failed", ex);
            }
        });
    }

    public CompletableFuture<List<Product>> getAllProductsFallback(Exception ex) {
        log.error("Fallback triggered for getAllProducts. Error: {}", ex.getMessage());
        return CompletableFuture.completedFuture(Collections.singletonList(
                new Product("0", "Fallback Product", "This is a fallback product", 0.0, "Fallback")
        ));
    }

    public CompletableFuture<Product> getProductByIdFallback(String id, Exception ex) {
        log.error("Fallback triggered for getProductById with id: {}. Error: {}", id, ex.getMessage());
        return CompletableFuture.completedFuture(
                new Product(id, "Fallback Product", "This is a fallback product", 0.0, "Fallback")
        );
    }

    public CompletableFuture<Product> createProductFallback(Product product, Exception ex) {
        log.error("Fallback triggered for createProduct. Error: {}", ex.getMessage());
        return CompletableFuture.completedFuture(
                new Product("0", "Fallback Product", "This is a fallback product", 0.0, "Fallback")
        );
    }

    private void simulateRandomError() {
        if (Math.random() < 0.7) { // 70% xác suất lỗi
            log.warn("Simulating a random service error");
            throw new RuntimeException("Simulated service failure");
        }
    }

    @FunctionalInterface
    private interface SupplierWithException<T> {
        T get() throws Exception;
    }
}