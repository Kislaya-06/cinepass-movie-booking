package com.example.moviereservation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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

    @Test
    void forbiddenPageSaysForbiddenNotNotFound() {
        // A normal user opening /admin: Spring Security answers 403 through Boot's /error page, not our handler
        ResponseEntity<String> loginPage = rest.getForEntity("/login", String.class);
        String csrf = loginPage.getBody().replaceAll("(?s).*name=\"_csrf\" value=\"([^\"]+)\".*", "$1");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add(HttpHeaders.COOKIE, loginPage.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";")[0]);
        ResponseEntity<String> login = rest.postForEntity("/login", new HttpEntity<>(
                "email=rahul.sharma@example.com&password=password123&_csrf=" + csrf, headers), String.class);

        HttpHeaders session = new HttpHeaders();
        session.add(HttpHeaders.COOKIE, login.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";")[0]);
        session.setAccept(java.util.List.of(MediaType.TEXT_HTML)); // a browser, so Boot renders error.html
        ResponseEntity<String> res = rest.exchange("/admin/dashboard", HttpMethod.GET, new HttpEntity<>(session), String.class);

        assertThat(res.getStatusCode().value()).isEqualTo(403);
        assertThat(res.getBody()).contains(">403<").contains("do not have permission").doesNotContain("does not exist");
    }
}
