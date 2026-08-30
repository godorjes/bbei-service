package com.bibei;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "bibei.cors.allowed-origins=https://private.example"
)
class WebDeploymentIntegrationTest {
    private static final String SPA_MARKER = "data-test=\"bibei-spa-shell\"";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Test
    void rootAndVueHistoryRoutesReturnTheSpaHtml() {
        for (String path : Arrays.asList("/", "/organize", "/scenes/1")) {
            ResponseEntity<String> response = restTemplate.getForEntity(path, String.class);

            assertThat(response.getStatusCode()).as(path).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).as(path).contains(SPA_MARKER);
        }
    }

    @Test
    void unknownApiPathIsNotForwardedToTheSpa() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/not-a-real-endpoint", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).doesNotContain(SPA_MARKER);
    }

    @Test
    void staticAssetsAreServedWithoutSpaForwarding() {
        List<String> paths = Arrays.asList(
                "/assets/test-app.js",
                "/assets/test-style.css",
                "/manifest.webmanifest",
                "/sw.js",
                "/icon.svg"
        );

        for (String path : paths) {
            ResponseEntity<String> response = restTemplate.getForEntity(path, String.class);

            assertThat(response.getStatusCode()).as(path).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).as(path).contains("bibei-static-test");
            assertThat(response.getBody()).as(path).doesNotContain(SPA_MARKER);
        }
    }

    @Test
    void healthCheckReturnsOnlyNonSensitiveStatus() {
        ResponseEntity<String> response = restTemplate.getForEntity("/healthz", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("{\"status\":\"UP\"}");
    }

    @Test
    void corsAllowsLocalCapacitorAndExplicitOriginsButNotUnknownOrigins() throws Exception {
        assertCorsAllowed("http://localhost:5173");
        assertCorsAllowed("capacitor://localhost");
        assertCorsAllowed("https://private.example");

        mockMvc().perform(options("/api/items")
                        .header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    private void assertCorsAllowed(String origin) throws Exception {
        mockMvc().perform(options("/api/items")
                        .header("Origin", origin)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origin));
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }
}
