package net.thesphynx.espritmarket;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class EspritMarketApplicationTests {

    @Test
    void applicationClassShouldBeLoadable() {
        assertNotNull(EspritMarketApplication.class);
    }

    @Test
    void springApplicationInstanceShouldBeCreated() {
        SpringApplication app = new SpringApplication(EspritMarketApplication.class);
        assertNotNull(app);
    }

}
