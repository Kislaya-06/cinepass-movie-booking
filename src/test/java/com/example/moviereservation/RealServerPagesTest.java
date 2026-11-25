package com.example.moviereservation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against real Tomcat: MockMvc never commits the response, so it can't catch a page that
 * dies half-way when the CSRF token needs a session after the first bytes were already sent.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"dev", "test"})
class RealServerPagesTest {

    @Autowired private TestRestTemplate rest;

    @ParameterizedTest
    @ValueSource(strings = {"/login", "/register"})
    void firstVisitGetsTheWholeFormPage(String path) {
        ResponseEntity<String> res = rest.getForEntity(path, String.class); // fresh client, no session cookie

        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(res.getBody()).contains("name=\"password\"").contains("name=\"_csrf\"").contains("</html>");
    }
}
