package stock.stockzzickmock.extern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static stock.stockzzickmock.storage.db.Address.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import stock.stockzzickmock.storage.db.member.entity.MemberEntity;
import stock.stockzzickmock.storage.db.member.repository.MemberJpaRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberJpaRepository memberRepository;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    private ValueOperations<String, Object> valueOperations;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
        valueOperations = Mockito.mock(ValueOperations.class);
        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Mockito.when(valueOperations.get(anyString())).thenReturn(null);
    }

    @AfterEach
    void tearDown() {
        memberRepository.deleteAll();
    }

    @Test
    void registerLoginRefreshLogoutFlowWorks() throws Exception {
        String registerBody = """
                {
                  "account": "tester",
                  "password": "Password!123",
                  "name": "테스터",
                  "phoneNumber": "010-1234-5678",
                  "email": "tester@example.com",
                  "zipcode": "12345",
                  "address": "서울시 강남구",
                  "addressDetail": "101동",
                  "fgOffset": "ignored"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.error").doesNotExist());

        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "tester",
                                  "password": "Password!123"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        List<String> cookieHeaders = loginResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        String accessToken = extractCookie(cookieHeaders, "accessToken");
        String refreshToken = extractCookie(cookieHeaders, "refreshToken");

        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        var refreshResult = mockMvc.perform(get("/api/auth/refresh")
                        .cookie(new MockCookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andReturn();

        List<String> refreshedCookieHeaders = refreshResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        String rotatedRefreshToken = extractCookie(refreshedCookieHeaders, "refreshToken");

        assertThat(refreshedCookieHeaders)
                .anyMatch(header -> header.startsWith("accessToken="));
        assertThat(rotatedRefreshToken).isNotBlank();
        assertThat(rotatedRefreshToken).isNotEqualTo(refreshToken);

        mockMvc.perform(get("/api/auth/refresh")
                        .cookie(new MockCookie("refreshToken", refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.result").value("FAIL"))
                .andExpect(jsonPath("$.error.errorCode").value("AUTH_401_INVALID_JWT"));

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new MockCookie("accessToken", accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));
    }

    @Test
    void investRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/invest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": "member-1",
                                  "investScore": 3
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.result").value("FAIL"))
                .andExpect(jsonPath("$.error.errorCode").value("AUTH_401_AUTHENTICATION_REQUIRED"));
    }

    @Test
    void withdrawRequiresAuthentication() throws Exception {
        mockMvc.perform(delete("/api/auth/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": "member-1"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.result").value("FAIL"))
                .andExpect(jsonPath("$.error.errorCode").value("AUTH_401_AUTHENTICATION_REQUIRED"));
    }

    @Test
    void refreshRejectsInvalidCookie() throws Exception {
        mockMvc.perform(get("/api/auth/refresh")
                        .cookie(new MockCookie("refreshToken", "invalid-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.result").value("FAIL"))
                .andExpect(jsonPath("$.error.errorCode").value("AUTH_401_INVALID_JWT"));
    }

    @Test
    void duplicateReturnsSuccessWhenAvailable() throws Exception {
        mockMvc.perform(post("/api/auth/duplicate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "available"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data").value(true))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void duplicateReturnsConflictWhenDuplicated() throws Exception {
        memberRepository.save(memberEntity("tester"));

        mockMvc.perform(post("/api/auth/duplicate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "tester"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.result").value("FAIL"))
                .andExpect(jsonPath("$.error.errorCode").value("AUTH_409_DUPLICATE_ACCOUNT"))
                .andExpect(jsonPath("$.error.message").value("이미 사용 중인 아이디입니다."));
    }

    @Test
    void investRejectsDifferentMemberId() throws Exception {
        MemberEntity member = memberRepository.save(memberEntity("tester"));

        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "tester",
                                  "password": "Password!123"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = extractCookie(loginResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE), "accessToken");

        mockMvc.perform(post("/api/auth/invest")
                        .cookie(new MockCookie("accessToken", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LinkedHashMap<>() {{
                            put("memberId", "another-member");
                            put("investScore", 4);
                        }})))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.result").value("FAIL"))
                .andExpect(jsonPath("$.error.errorCode").value("AUTH_403_MEMBER_ACCESS_DENIED"));

        assertThat(memberRepository.findById(member.getMemberId())).isPresent();
    }

    private String extractCookie(List<String> headers, String cookieName) {
        return headers.stream()
                .filter(header -> header.startsWith(cookieName + "="))
                .findFirst()
                .map(header -> header.substring((cookieName + "=").length(), header.indexOf(';')))
                .orElseThrow();
    }

    private MemberEntity memberEntity(String account) {
        return MemberEntity.builder()
                .memberId("member-" + account)
                .account(account)
                .passwordHash(new BCryptPasswordEncoder().encode("Password!123"))
                .name("테스터")
                .email("tester@example.com")
                .phoneNumber("010-1234-5678")
                .address(builder()
                        .zipCode("12345")
                        .address("서울시 강남구")
                        .addressDetail("101동")
                        .build())
                .invest(0)
                .refreshTokenVersion(0L)
                .build();
    }
}
