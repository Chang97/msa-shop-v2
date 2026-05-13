package com.msashop.product.integration.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.product.adapter.in.web.dto.CreateProductRequest;
import com.msashop.product.adapter.out.persistence.entity.ProductEntity;
import com.msashop.product.adapter.out.persistence.repo.ProductCommandJpaRepository;
import com.msashop.product.domain.model.ProductStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.kafka.consumers.order-payment-saga-enabled=false",
        "app.kafka.producer.relay-enabled=false"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ProductApiIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("product_service_api_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProductCommandJpaRepository productCommandJpaRepository;

    @Value("${security.internal.header-name}")
    private String internalHeaderName;

    @Value("${security.internal.service-secret}")
    private String internalServiceSecret;

    @BeforeEach
    @AfterEach
    void resetDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    stock_reservation_item,
                    stock_reservation,
                    outbox_event,
                    processed_event,
                    product
                RESTART IDENTITY CASCADE
                """);
    }

    @Test
    @DisplayName("상품 목록 조회는 공개 API로 판매 가능한 상품만 반환한다")
    void should_return_only_orderable_products() throws Exception {
        productCommandJpaRepository.save(ProductEntity.builder()
                .productName("상품 A")
                .price(new BigDecimal("10000"))
                .stock(5)
                .status(ProductStatus.ON_SALE)
                .useYn(true)
                .build());
        productCommandJpaRepository.save(ProductEntity.builder()
                .productName("상품 B")
                .price(new BigDecimal("20000"))
                .stock(3)
                .status(ProductStatus.ON_SALE)
                .useYn(true)
                .build());
        productCommandJpaRepository.save(ProductEntity.builder()
                .productName("비활성 상품")
                .price(new BigDecimal("30000"))
                .stock(3)
                .status(ProductStatus.ON_SALE)
                .useYn(false)
                .build());
        productCommandJpaRepository.save(ProductEntity.builder()
                .productName("판매중단 상품")
                .price(new BigDecimal("40000"))
                .stock(3)
                .status(ProductStatus.STOPPED)
                .useYn(true)
                .build());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].productName").value("상품 A"))
                .andExpect(jsonPath("$[1].productName").value("상품 B"));
    }

    @Test
    @DisplayName("상품 단건 조회는 공개 API로 상품 상세를 반환한다")
    void should_return_single_product() throws Exception {
        ProductEntity saved = productCommandJpaRepository.save(ProductEntity.builder()
                .productName("상품 상세")
                .price(new BigDecimal("15000"))
                .stock(7)
                .status(ProductStatus.ON_SALE)
                .useYn(true)
                .build());

        mockMvc.perform(get("/api/products/{productId}", saved.getProductId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(saved.getProductId()))
                .andExpect(jsonPath("$.productName").value("상품 상세"))
                .andExpect(jsonPath("$.price").value(15000))
                .andExpect(jsonPath("$.stock").value(7))
                .andExpect(jsonPath("$.status").value("ON_SALE"))
                .andExpect(jsonPath("$.useYn").value(true));
    }

    @Test
    @DisplayName("상품 생성은 ROLE_ADMIN 사용자만 가능하다")
    void should_create_product_when_admin_requests() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("X-User-Id", 1L)
                        .header("X-Roles", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProductRequest(
                                "신규 상품",
                                new BigDecimal("30000"),
                                9
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").isNumber());

        assertThat(productCommandJpaRepository.findAll())
                .singleElement()
                .satisfies(product -> {
                    assertThat(product.getProductName()).isEqualTo("신규 상품");
                    assertThat(product.getPrice()).isEqualByComparingTo("30000");
                    assertThat(product.getStock()).isEqualTo(9);
                    assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
                    assertThat(product.getUseYn()).isTrue();
                });
    }

    @Test
    @DisplayName("상품 생성은 관리자 권한이 없으면 거부된다")
    void should_forbid_product_create_when_user_is_not_admin() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("X-User-Id", 1L)
                        .header("X-Roles", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProductRequest(
                                "권한 없는 상품",
                                new BigDecimal("30000"),
                                9
                        ))))
                .andExpect(status().isForbidden());

        assertThat(productCommandJpaRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("내부 상품 조회는 internal secret이 있어야 접근할 수 있다")
    void should_require_internal_secret_for_internal_product_api() throws Exception {
        ProductEntity saved = productCommandJpaRepository.save(ProductEntity.builder()
                .productName("내부 조회 상품")
                .price(new BigDecimal("11000"))
                .stock(4)
                .status(ProductStatus.ON_SALE)
                .useYn(true)
                .build());

        mockMvc.perform(get("/internal/products/{productId}", saved.getProductId()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/internal/products/{productId}", saved.getProductId())
                        .header(internalHeaderName, internalServiceSecret))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(saved.getProductId()))
                .andExpect(jsonPath("$.productName").value("내부 조회 상품"));
    }
}
