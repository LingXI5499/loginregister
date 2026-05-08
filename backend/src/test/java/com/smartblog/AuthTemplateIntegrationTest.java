package com.smartblog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartblog.service.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class AuthTemplateIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.0")
            .withDatabaseName("smartblog_auth")
            .withUsername("root")
            .withPassword("test_password");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.schema-locations", () -> "classpath:sql/schema.sql");
        registry.add("jwt.secret", () -> "12345678901234567890123456789012");
        registry.add("security.email-code.send-interval-seconds", () -> "0");
        registry.add("account.delete.cooldown-days", () -> "7");
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    MailService mailService;

    private final Map<String, String> sentCodes = new ConcurrentHashMap<>();

    @BeforeEach
    void setUpMailMock() {
        sentCodes.clear();
        Mockito.doAnswer(invocation -> {
            String to = invocation.getArgument(0);
            String scene = invocation.getArgument(1);
            String code = invocation.getArgument(2);
            sentCodes.put(key(to, scene), code);
            return null;
        }).when(mailService).sendVerificationCode(anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void registerLoginRefreshLogoutAndProfileFlow() throws Exception {
        String suffix = suffix();
        String username = "user_" + suffix;
        String email = "user_" + suffix + "@example.com";
        String password = "Password123";

        register(username, email, password);
        JsonNode login = passwordLogin(username, password);
        String accessToken = login.at("/data/accessToken").asText();
        String refreshToken = login.at("/data/refreshToken").asText();

        JsonNode me = getMe(accessToken, 200);
        assertThat(me.at("/data/username").asText()).isEqualTo(username);

        JsonNode refreshed = postJson("/api/auth/token/refresh", Map.of("refreshToken", refreshToken));
        assertThat(refreshed.get("code").asInt()).isEqualTo(200);
        String newAccessToken = refreshed.at("/data/accessToken").asText();
        String newRefreshToken = refreshed.at("/data/refreshToken").asText();

        JsonNode oldRefreshResult = postJson("/api/auth/token/refresh", Map.of("refreshToken", refreshToken));
        assertThat(oldRefreshResult.get("code").asInt()).isEqualTo(400);

        JsonNode updated = putJsonWithToken("/api/user/profile", Map.of(
                "nickname", "模板用户",
                "avatarUrl", "https://example.com/avatar.png"
        ), newAccessToken, 200);
        assertThat(updated.get("code").asInt()).isEqualTo(200);

        JsonNode afterProfile = getMe(newAccessToken, 200);
        assertThat(afterProfile.at("/data/nickname").asText()).isEqualTo("模板用户");
        assertThat(afterProfile.at("/data/avatarUrl").asText()).isEqualTo("https://example.com/avatar.png");

        JsonNode logout = postJsonWithToken("/api/auth/logout", Map.of(), newAccessToken, 200);
        assertThat(logout.get("code").asInt()).isEqualTo(200);
        getMe(newAccessToken, 401);

        JsonNode refreshAfterLogout = postJson("/api/auth/token/refresh", Map.of("refreshToken", newRefreshToken));
        assertThat(refreshAfterLogout.get("code").asInt()).isEqualTo(400);
    }

    @Test
    void passwordChangeRevokesAllSessions() throws Exception {
        String suffix = suffix();
        String username = "pwd_" + suffix;
        String email = "pwd_" + suffix + "@example.com";
        String oldPassword = "Password123";
        String newPassword = "Password456";

        register(username, email, oldPassword);
        JsonNode login1 = passwordLogin(username, oldPassword);
        JsonNode login2 = passwordLogin(email, oldPassword);

        String access1 = login1.at("/data/accessToken").asText();
        String access2 = login2.at("/data/accessToken").asText();
        String refresh2 = login2.at("/data/refreshToken").asText();

        JsonNode changed = postJsonWithToken("/api/auth/password/change", Map.of(
                "oldPassword", oldPassword,
                "newPassword", newPassword
        ), access1, 200);
        assertThat(changed.get("code").asInt()).isEqualTo(200);

        getMe(access1, 401);
        getMe(access2, 401);
        JsonNode refreshResult = postJson("/api/auth/token/refresh", Map.of("refreshToken", refresh2));
        assertThat(refreshResult.get("code").asInt()).isEqualTo(400);
        assertThat(passwordLogin(username, newPassword).get("code").asInt()).isEqualTo(200);
        assertThat(passwordLogin(username, oldPassword).get("code").asInt()).isEqualTo(400);
    }

    @Test
    void passwordResetRevokesAllSessions() throws Exception {
        String suffix = suffix();
        String username = "reset_" + suffix;
        String email = "reset_" + suffix + "@example.com";
        String oldPassword = "Password123";
        String newPassword = "Password456";

        register(username, email, oldPassword);
        JsonNode login = passwordLogin(username, oldPassword);
        String accessToken = login.at("/data/accessToken").asText();
        String refreshToken = login.at("/data/refreshToken").asText();

        postJson("/api/auth/password/reset/request", Map.of("email", email));
        String code = sentCodes.get(key(email, "RESET_PASSWORD"));
        assertThat(code).isNotBlank();

        JsonNode reset = postJson("/api/auth/password/reset/confirm", Map.of(
                "email", email,
                "code", code,
                "newPassword", newPassword
        ));
        assertThat(reset.get("code").asInt()).isEqualTo(200);
        getMe(accessToken, 401);
        assertThat(postJson("/api/auth/token/refresh", Map.of("refreshToken", refreshToken)).get("code").asInt()).isEqualTo(400);
        assertThat(passwordLogin(username, newPassword).get("code").asInt()).isEqualTo(200);
    }

    @Test
    void emailChangeRevokesSessionsAndUsesNewEmail() throws Exception {
        String suffix = suffix();
        String username = "mail_" + suffix;
        String email = "mail_" + suffix + "@example.com";
        String newEmail = "mail_new_" + suffix + "@example.com";
        String password = "Password123";

        register(username, email, password);
        JsonNode login = passwordLogin(username, password);
        String accessToken = login.at("/data/accessToken").asText();
        String refreshToken = login.at("/data/refreshToken").asText();

        JsonNode send = postJsonWithToken("/api/user/email/change/code/send", Map.of("newEmail", newEmail), accessToken, 200);
        assertThat(send.get("code").asInt()).isEqualTo(200);
        String code = sentCodes.get(key(newEmail, "CHANGE_EMAIL"));
        assertThat(code).isNotBlank();

        JsonNode wrongPassword = postJsonWithToken("/api/user/email/change/confirm", Map.of(
                "newEmail", newEmail,
                "emailCode", code,
                "currentPassword", "wrong_password"
        ), accessToken, 200);
        assertThat(wrongPassword.get("code").asInt()).isEqualTo(400);

        JsonNode changed = postJsonWithToken("/api/user/email/change/confirm", Map.of(
                "newEmail", newEmail,
                "emailCode", code,
                "currentPassword", password
        ), accessToken, 200);
        assertThat(changed.get("code").asInt()).isEqualTo(200);
        getMe(accessToken, 401);
        assertThat(postJson("/api/auth/token/refresh", Map.of("refreshToken", refreshToken)).get("code").asInt()).isEqualTo(400);
        assertThat(passwordLogin(newEmail, password).get("code").asInt()).isEqualTo(200);
        assertThat(passwordLogin(email, password).get("code").asInt()).isEqualTo(400);
    }

    @Test
    void accountDeletionAndCancelFlow() throws Exception {
        String suffix = suffix();
        String username = "del_" + suffix;
        String email = "del_" + suffix + "@example.com";
        String password = "Password123";

        register(username, email, password);
        JsonNode login = passwordLogin(username, password);
        String accessToken = login.at("/data/accessToken").asText();

        postJsonWithToken("/api/account/delete/code/send", Map.of(), accessToken, 200);
        String deleteCode = sentCodes.get(key(email, "DELETE_ACCOUNT"));
        assertThat(deleteCode).isNotBlank();

        JsonNode requestDelete = postJsonWithToken("/api/account/delete/request", Map.of(
                "emailCode", deleteCode,
                "reason", "integration test"
        ), accessToken, 200);
        assertThat(requestDelete.get("code").asInt()).isEqualTo(200);
        getMe(accessToken, 401);
        assertThat(passwordLogin(username, password).get("code").asInt()).isEqualTo(400);

        postJson("/api/account/delete/cancel/code/send", Map.of("email", email));
        String cancelCode = sentCodes.get(key(email, "CANCEL_DELETE_ACCOUNT"));
        assertThat(cancelCode).isNotBlank();

        JsonNode cancel = postJson("/api/account/delete/cancel/confirm", Map.of(
                "email", email,
                "emailCode", cancelCode
        ));
        assertThat(cancel.get("code").asInt()).isEqualTo(200);
        assertThat(passwordLogin(username, password).get("code").asInt()).isEqualTo(200);
    }

    @Test
    void invalidUsernameShouldFail() throws Exception {
        String suffix = suffix();
        String email = "bad_" + suffix + "@example.com";
        postJson("/api/auth/email-code/send", Map.of("email", email));
        String code = sentCodes.get(key(email, "REGISTER_EMAIL"));
        JsonNode result = postJson("/api/auth/register", Map.of(
                "username", "中文用户名",
                "password", "Password123",
                "email", email,
                "emailCode", code
        ));
        assertThat(result.get("code").asInt()).isEqualTo(400);
        assertThat(result.get("message").asText()).contains("用户名只能包含字母、数字和下划线");
    }

    private void register(String username, String email, String password) throws Exception {
        postJson("/api/auth/email-code/send", Map.of("email", email));
        String code = sentCodes.get(key(email, "REGISTER_EMAIL"));
        assertThat(code).isNotBlank();
        JsonNode register = postJson("/api/auth/register", Map.of(
                "username", username,
                "nickname", username,
                "email", email,
                "emailCode", code,
                "password", password
        ));
        assertThat(register.get("code").asInt()).isEqualTo(200);
    }

    private JsonNode passwordLogin(String account, String password) throws Exception {
        return postJson("/api/auth/login/password", Map.of(
                "account", account,
                "password", password,
                "deviceName", "integration-test"
        ));
    }

    private JsonNode getMe(String token, int expectedHttpStatus) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/user/me").header("Authorization", "Bearer " + token))
                .andExpect(status().is(expectedHttpStatus))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode postJson(String uri, Object body) throws Exception {
        MvcResult result = mockMvc.perform(post(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode postJsonWithToken(String uri, Object body, String token, int expectedHttpStatus) throws Exception {
        MvcResult result = mockMvc.perform(post(uri)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is(expectedHttpStatus))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode putJsonWithToken(String uri, Object body, String token, int expectedHttpStatus) throws Exception {
        MvcResult result = mockMvc.perform(put(uri)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is(expectedHttpStatus))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String key(String email, String scene) {
        return email.toLowerCase() + "|" + scene;
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}