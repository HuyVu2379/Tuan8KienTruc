package fit.iuh.student.circuit_breaker.client;

import fit.iuh.student.circuit_breaker.model.Product;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
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
    @Retry(name = "mockApiRetry")
    public CompletableFuture<List<Product>> getAllProducts() {
        log.info("Getting all products from MockAPI");
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = baseUrl + productsEndpoint;
                simulateRandomError();
                ResponseEntity<List<Product>> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<Product>>() {}
                );
                List<Product> products = response.getBody();
                if (products == null) {
                    log.warn("Received null response from MockAPI");
                    return new ArrayList<>();
                }
                log.info("Successfully fetched {} products", products.size());
                return products;
            } catch (Exception ex) {
                log.error("Error calling MockAPI for getAllProducts: {}", ex.getMessage());
                throw new RuntimeException("Failed to fetch products", ex);
            }
        });
    }

    @CircuitBreaker(name = "mockApiService", fallbackMethod = "getProductByIdFallback")
    @Retry(name = "mockApiRetry")
    public CompletableFuture<Product> getProductById(String id) {
        log.info("Getting product with id: {} from MockAPI", id);
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = baseUrl + productsEndpoint + "/" + id;
                simulateRandomError();
                Product product = restTemplate.getForObject(url, Product.class);
                if (product == null) {
                    log.warn("Product not found for id: {}", id);
                    throw new RuntimeException("Product not found with id: " + id);
                }
                log.info("Successfully fetched product with id: {}", id);
                return product;
            } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode().value() == 404) {
                    log.warn("Product not found for id: {}", id);
                    throw new RuntimeException("Product not found with id: " + id, ex);
                }
                log.error("Error calling MockAPI for getProductById: {}", ex.getMessage());
                throw new RuntimeException("Failed to fetch product", ex);
            } catch (Exception ex) {
                log.error("Error calling MockAPI for getProductById: {}", ex.getMessage());
                throw new RuntimeException("Failed to fetch product", ex);
            }
        });
    }

    @CircuitBreaker(name = "mockApiService", fallbackMethod = "createProductFallback")
    @Retry(name = "mockApiRetry")
    public CompletableFuture<Product> createProduct(Product product) {
        log.info("Creating new product in MockAPI: {}", product);
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = baseUrl + productsEndpoint;
                simulateRandomError();
                Product createdProduct = restTemplate.postForObject(url, product, Product.class);
                if (createdProduct == null) {
                    log.warn("Failed to create product: received null response");
                    throw new RuntimeException("Failed to create product");
                }
                log.info("Successfully created product: {}", createdProduct);
                return createdProduct;
            } catch (Exception ex) {
                log.error("Error calling MockAPI for createProduct: {}", ex.getMessage());
                throw new RuntimeException("Failed to create product", ex);
            }
        });
    }

    public CompletableFuture<List<Product>> getAllProductsFallback(Exception ex) {
        log.error("Circuit breaker triggered for getAllProducts. Error: {}", ex.getMessage());
        List<Product> fallbackProducts = new ArrayList<>();
        fallbackProducts.add(new Product("0", "Fallback Product", "This is a fallback product when service is down", 0.0, "Fallback"));
        return CompletableFuture.completedFuture(fallbackProducts);
    }

    public CompletableFuture<Product> getProductByIdFallback(String id, Exception ex) {
        log.error("Circuit breaker triggered for getProductById with id: {}. Error: {}", id, ex.getMessage());
        return CompletableFuture.completedFuture(new Product(id, "Fallback Product", "This is a fallback product when service is down", 0.0, "Fallback"));
    }

    public CompletableFuture<Product> createProductFallback(Product product, Exception ex) {
        log.error("Circuit breaker triggered for createProduct. Error: {}", ex.getMessage());
        Product fallbackProduct = new Product("0", "Fallback Product", "This is a fallback product when service is down", 0.0, "Fallback");
        return CompletableFuture.completedFuture(fallbackProduct);
    }

    private void simulateRandomError() {
        // Tạm thời tăng xác suất lỗi để dễ test Retry
        if (Math.random() < 0.7) { // 70% xác suất lỗi
            log.warn("Simulating a random service error");
            throw new RuntimeException("Simulated service failure");
        }
    }
}