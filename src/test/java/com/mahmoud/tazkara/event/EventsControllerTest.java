package com.mahmoud.tazkara.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import jakarta.transaction.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;

@SpringBootTest 
@Transactional 
@AutoConfigureMockMvc 
@ActiveProfiles ("test")
public class EventsControllerTest {
    
    @Autowired 
    private MockMvc mockMvc;
    
    private static final String EVENT_JSON = """
        {"name":"Jazz Night","city":"Cairo","category":"music","totalSeats":100}
        """;


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
    public void getEvents_ispublic() throws Exception {
        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk());
    }
    @Test 
    public void createEvent_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                .contentType("application/json").content(EVENT_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test 
    public void registerloginAndCreateEvent() throws Exception {
        String token = registerAndLogin("test@example.com");
        mockMvc.perform(post("/api/v1/events")
                .contentType("application/json").content(EVENT_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
    }

    @Test
    void bearerWithoutSpaceIsRejected() throws Exception {     // the trial
        String token = registerAndLogin("sloppy@tazkara.dev");

        mockMvc.perform(post("/api/v1/events")
                .header("Authorization", "Bearer" + token)     // no space — malformed
                .contentType("application/json").content(EVENT_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validationRunsThroughTheRealPipeline() throws Exception {
        String token = registerAndLogin("validator@tazkara.dev");

        mockMvc.perform(post("/api/v1/events")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("""
                        {"name":"  ","city":"Cairo","category":"music","totalSeats":0}"""))
                .andExpect(status().isBadRequest());
    }

    
}
