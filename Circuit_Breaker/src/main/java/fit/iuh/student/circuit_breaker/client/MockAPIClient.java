package fit.iuh.student.circuit_breaker.client;

import fit.iuh.student.circuit_breaker.model.Product;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
    @CircuitBreaker(name = "mockApiService", fallbackMethod = "getAllProductsFallback")
//    @TimeLimiter(name = "mockApiService")
//    @Retry(name = "mockApiService")
    public CompletableFuture<List<Product>> getAllProducts() {
        log.info("Getting all products from MockAPI");

        return CompletableFuture.supplyAsync(() -> {
            String url = baseUrl + productsEndpoint;

            // Giả lập lỗi ngẫu nhiên để test circuit breaker
            simulateRandomError();

            ResponseEntity<List<Product>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Product>>() {}
            );

            return response.getBody();
        });
    }

    @CircuitBreaker(name = "mockApiService", fallbackMethod = "getProductByIdFallback")
//    @TimeLimiter(name = "mockApiService")
//    @Retry(name = "mockApiService")
    public CompletableFuture<Product> getProductById(String id) {
        log.info("Getting product with id: {} from MockAPI", id);

        return CompletableFuture.supplyAsync(() -> {
            String url = baseUrl + productsEndpoint + "/" + id;

            simulateRandomError();

            return restTemplate.getForObject(url, Product.class);
        });
    }

    @CircuitBreaker(name = "mockApiService", fallbackMethod = "createProductFallback")
//    @TimeLimiter(name = "mockApiService")
//    @Retry(name = "mockApiService")
    public CompletableFuture<Product> createProduct(Product product) {
        log.info("Creating new product in MockAPI: {}", product);

        return CompletableFuture.supplyAsync(() -> {
            String url = baseUrl + productsEndpoint;

            // Giả lập lỗi ngẫu nhiên để test circuit breaker
            simulateRandomError();

            return restTemplate.postForObject(url, product, Product.class);
        });
    }
    public CompletableFuture<List<Product>> getAllProductsFallback(Exception ex) {
        log.error("Circuit breaker triggered for getAllProducts. Error: {}", ex.getMessage());
        List<Product> fallbackProducts = new ArrayList<>();
        fallbackProducts.add(new Product("0","Fallback Product","This is a fallback product when service is down", 0.0, "Fallback"));
        return CompletableFuture.completedFuture(fallbackProducts);
    }

    public CompletableFuture<Product> getProductByIdFallback(String id, Exception ex) {
        log.error("Circuit breaker triggered for getProductById. Error: {}", ex.getMessage());
        return CompletableFuture.completedFuture(new Product(id,"Fallback Product","This is a fallback product when service is down", 0.0, "Fallback"));
    }

    public CompletableFuture<Product> createProductFallback(Product product, Exception ex) {
        log.error("Circuit breaker triggered for createProduct. Error: {}", ex.getMessage());
        Product fallbackProduct = new Product("0","Fallback Product","This is a fallback product when service is down", 0.0, "Fallback");
        return CompletableFuture.completedFuture(fallbackProduct);
    }

    private void simulateRandomError() {
        // Tạo lỗi với xác suất 25% để test circuit breaker
        if (Math.random() < 1) {
            log.warn("Simulating a random service error");
            throw new RuntimeException("Simulated service failure");
        }
    }
}
