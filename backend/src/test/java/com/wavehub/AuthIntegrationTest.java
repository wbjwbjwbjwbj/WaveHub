package com.wavehub;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"WAVEHUB_ADMIN_USERNAME=integration-admin", "WAVEHUB_ADMIN_PASSWORD=integration-only-password"})
@AutoConfigureMockMvc
@ActiveProfiles("standalone")
class AuthIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    void sessionLoginAndLogoutKeepCsrfProtection() throws Exception {
        mvc.perform(post("/api/auth/login").param("username", "integration-admin").param("password", "integration-only-password"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login").with(csrf()).param("username", "integration-admin").param("password", "wrong"))
                .andExpect(status().isUnauthorized());
        var result = mvc.perform(post("/api/auth/login").with(csrf()).param("username", "integration-admin")
                        .param("password", "integration-only-password"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.username").value("integration-admin")).andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);
        mvc.perform(get("/api/auth/session").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/stations").session(session)).andExpect(status().isServiceUnavailable());
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isOk());
        assertTrue(session.isInvalid());
        mvc.perform(get("/api/auth/session")).andExpect(status().isUnauthorized());
    }
}
