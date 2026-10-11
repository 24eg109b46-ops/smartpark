package com.smartpark.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AuthMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Public endpoints should be accessible without authentication")
    void testPublicEndpoints() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/pricing"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/about"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Form login with seeded driver credentials redirects to /driver/dashboard")
    void testDriverLogin() throws Exception {
        mockMvc.perform(post("/login")
                .param("email", "driver@smartpark.com")
                .param("password", "Driver@123")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/driver/dashboard"));
    }

    @Test
    @DisplayName("Form login with seeded admin credentials redirects to /admin/dashboard")
    void testAdminLogin() throws Exception {
        mockMvc.perform(post("/login")
                .param("email", "admin@smartpark.com")
                .param("password", "Admin@123")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    @DisplayName("Unauthenticated access to protected /admin/dashboard redirects to /login")
    void testAdminProtectedUnauthorized() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "driver@smartpark.com", authorities = {"ROLE_DRIVER"})
    @DisplayName("Driver role accessing /admin/dashboard should be denied access (403)")
    void testDriverAccessToAdminDenied() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/error/403"));
    }
}
