package com.livraison;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class SuiviDebugTest {

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
    @WithMockUser(username = "admin", roles = "ADMIN")
    void debugTableauBord() throws Exception {
        try {
            MvcResult result = mockMvc.perform(get("/tableau-bord")).andReturn();
            System.out.println("DEBUG-STATUS=" + result.getResponse().getStatus());
            Exception ex = result.getResolvedException();
            if (ex != null) {
                System.out.println("DEBUG-EXCEPTION=" + ex.getClass().getName() + ": " + ex.getMessage());
                ex.printStackTrace(System.out);
                Throwable cause = ex.getCause();
                while (cause != null) {
                    System.out.println("DEBUG-CAUSE=" + cause.getClass().getName() + ": " + cause.getMessage());
                    cause = cause.getCause();
                }
            }
        } catch (Exception e) {
            System.out.println("DEBUG-THROWN=" + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace(System.out);
        }
    }
}
