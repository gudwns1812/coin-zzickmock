package coin.coinzzickmock.common.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import coin.coinzzickmock.testsupport.PermitAllSecurityTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(classes = FuturesUnsafeMethodOriginFilterTest.TestApplication.class)
@AutoConfigureMockMvc
class FuturesUnsafeMethodOriginFilterTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsSafeFuturesRequestsWithoutOrigin() throws Exception {
        mockMvc.perform(get("/api/futures/test"))
                .andExpect(status().isOk());
    }

    @Test
    void allowsUnsafeFuturesRequestsFromProductionFrontendOrigin() throws Exception {
        mockMvc.perform(post("/api/futures/test")
                        .header(HttpHeaders.ORIGIN, "https://coin-zzickmock-frontend.vercel.app"))
                .andExpect(status().isOk());
    }

    @Test
    void allowsUnsafeFuturesRequestsFromLocalFrontendOrigin() throws Exception {
        mockMvc.perform(post("/api/futures/test")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000"))
                .andExpect(status().isOk());
    }

    @Test
    void allowsUnsafeFuturesRequestsWithAllowedRefererWhenOriginIsMissing() throws Exception {
        mockMvc.perform(post("/api/futures/test")
                        .header(HttpHeaders.REFERER, "https://coin-zzickmock-frontend.vercel.app/markets"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsUnsafeFuturesRequestsWithoutOriginOrReferer() throws Exception {
        mockMvc.perform(post("/api/futures/test"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void rejectsUnsafeFuturesRequestsFromUntrustedOrigin() throws Exception {
        mockMvc.perform(post("/api/futures/test")
                        .header(HttpHeaders.ORIGIN, "https://attacker.example"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void skipsNonFuturesRequests() throws Exception {
        mockMvc.perform(post("/other/test"))
                .andExpect(status().isOk());
    }

    @SpringBootConfiguration
    @Import({FuturesUnsafeMethodOriginFilter.class, PermitAllSecurityTestConfiguration.class, TestController.class})
    static class TestApplication {
    }

    @RestController
    static class TestController {
        @GetMapping("/api/futures/test")
        String readFutures() {
            return "ok";
        }

        @PostMapping("/api/futures/test")
        String writeFutures() {
            return "ok";
        }

        @PostMapping("/other/test")
        String writeOther() {
            return "ok";
        }
    }
}
