package com.livraison.config;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void pageDeConnexionAccessibleAnonymement() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void pageProtegeeRedirigeVersConnexion() throws Exception {
        mockMvc.perform(get("/commandes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "disp", roles = "DISPATCHER")
    void dispatcherAccedeAuxCommandes() throws Exception {
        mockMvc.perform(get("/commandes")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "disp", roles = "DISPATCHER")
    void dispatcherInterditSurLesBaremes() throws Exception {
        mockMvc.perform(get("/tarifs")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminAccedeAuxBaremes() throws Exception {
        mockMvc.perform(get("/tarifs")).andExpect(status().isOk());
    }
}
