package com.mahmoud.tazkara.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.jayway.jsonpath.JsonPath;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;

@Testcontainers 
@SpringBootTest 
@AutoConfigureMockMvc 
@Transactional 
@ActiveProfiles ("test")
class ErrorResponsesTest {

    @Container 
    @ServiceConnection 
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @Autowired 
    MockMvc mockMvc;

    private String registerAndLogin(String email) throws Exception {
        String credentials = """
                {"email":"%s","password":"correct-horse-battery"}""".formatted(email);
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType("application/json").content(credentials));
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json").content(credentials)).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    @Test 
    void unknownEventIsAProblemWithItsMessage() throws Exception {
        mockMvc.perform(get("/api/v1/events/999999"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("https://tazkara.dev/problems/event-not-found"))
            .andExpect(jsonPath("$.detail").value("event 999999 not found"));
    }

    @Test
    void invalidRegistrationListsEveryBadField() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType("application/json")
                .content("""
                    {"email":"not-an-email","password":"x"}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("https://tazkara.dev/problems/validation"))
            .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("email", "password")))
            .andExpect(jsonPath("$.errors[?(@.field == 'password')].message")
                .value(hasItem("password must be at least 8 characters")));
    }

    @Test
    void missingTokenUsesTheSameShape() throws Exception {
        mockMvc.perform(post("/api/v1/events").contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("https://tazkara.dev/problems/unauthenticated"));
    }

    @Test
    void freeUserOnProEndpointGets403NotA500() throws Exception {
        String token = registerAndLogin("free@tazkara.dev");
        mockMvc.perform(get("/api/v1/events/stats").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.type").value("https://tazkara.dev/problems/forbidden"));
    }
}

