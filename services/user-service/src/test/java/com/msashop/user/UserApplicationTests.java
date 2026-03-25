package com.msashop.user;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@Disabled("Placeholder context test is disabled until test-specific datasource and security properties are provided.")
class UserApplicationTests {

    @Test
    void contextLoads() {
    }

}
