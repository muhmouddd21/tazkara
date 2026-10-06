package com.mahmoud.tazkara.ticket;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.jayway.jsonpath.JsonPath;

import jakarta.transaction.Transactional;


@Testcontainers
@SpringBootTest
@Transactional 
@AutoConfigureMockMvc  
@ActiveProfiles("tests")
public class TicketsControllerTest {

    @Container 
    @ServiceConnection 
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16");
    
    @Autowired
    private MockMvc mockMvc;

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
    void buyTicketOverHttp() throws Exception {

        // 1. Register + login
        String token = registerAndLogin("buyer@test.com");

        // 2. Create an event
        String eventJson = """
                {
                    "name": "Jazz Night",
                    "city": "Cairo",
                    "category": "music",
                    "totalSeats": 100
                }
                """;

        MvcResult eventResult = mockMvc.perform(
                post("/api/v1/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(eventJson)
        )
        .andExpect(status().isCreated())
        .andReturn();

        // 3. Extract event ID
        Number id = JsonPath.read(eventResult.getResponse().getContentAsString(), "$.id");
        long eventId = id.longValue();

        // 4. Buy ticket
        mockMvc.perform(
                post("/api/v1/events/{id}/tickets", eventId)
                        .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isCreated());

        // 5. Get my tickets
        mockMvc.perform(
                get("/api/v1/events/tickets")
                        .header("Authorization", "Bearer " + token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));
    }

}
