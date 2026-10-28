package com.mahmoud.tazkara.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import com.jayway.jsonpath.JsonPath;

import java.time.Duration;
import java.time.Instant;



import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest 
@Transactional 
@AutoConfigureMockMvc 
@ActiveProfiles ("test")
public class EventsControllerTest {

        @Container 
        @ServiceConnection
        static PostgreSQLContainer postgres =
                new PostgreSQLContainer("postgres:16");
    
    @Autowired 
    private MockMvc mockMvc;
    
    private static final String EVENT_JSON = """
        {
            "name": "Jazz Night",
            "city": "Cairo",
            "category": "music",
            "totalSeats": 100,
            "startAt": "2027-06-01T18:00:00Z",
            "endAt": "2027-06-01T21:00:00Z"
        }
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
                        {"name":"  ","city":"Cairo","category":"music","totalSeats":0,  "startAt": "2027-06-01T18:00:00Z",
        "endAt": "2027-06-01T21:00:00Z"}"""))
                .andExpect(status().isBadRequest());
    }
    @Test
    void nonOwnerCannotDeleteAnEvent() throws Exception {
        String token1 = registerAndLogin("non-owner@tazkara.dev");
        String token2 = registerAndLogin("owner@tazkara.dev");

        MvcResult result = mockMvc.perform(post("/api/v1/events")
                .contentType("application/json").content(EVENT_JSON)
                .header("Authorization", "Bearer " + token2)).andReturn();

        Integer eventId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        mockMvc.perform(delete("/api/v1/events/{id}", eventId)
                .header("Authorization", "Bearer " + token1))
                .andExpect(status().isForbidden());
    }   

    private static final Instant START_AT = Instant.now().plus(Duration.ofDays(30));

    @Test
    void endBeforeStartIsRejectedWithBadRequest() throws Exception {      // B8
        String token = registerAndLogin("backwards@tazkara.dev");

        mockMvc.perform(post("/api/v1/events")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("""
                        {"name":"Backwards Gig","city":"Cairo","category":"music","totalSeats":10,
                         "startAt":"%s","endAt":"%s"}""".formatted(START_AT, START_AT.minus(Duration.ofHours(3)))))
                .andExpect(status().isBadRequest());
    }

    private Long createEventWithSeats(String token, String name, int seats) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/events")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("""
                        {"name":"%s","city":"Cairo","category":"music","totalSeats":%d,
                         "startAt":"%s","endAt":"%s"}""".formatted(name, seats, START_AT, START_AT.plus(Duration.ofHours(3)))))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Integer) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test
    void seatsLeftDropsOnPurchaseAndComesBackOnCancel() throws Exception {
        String token = registerAndLogin("seats@tazkara.dev");
        Long eventId = createEventWithSeats(token, "One Seat Show", 1);

        mockMvc.perform(get("/api/v1/events/{id}/availableseats", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numberOfAvailableSeats").value(1));

        MvcResult ticket = mockMvc.perform(post("/api/v1/events/{id}/tickets", eventId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();
        Integer ticketId = JsonPath.read(ticket.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/events/{id}/availableseats", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numberOfAvailableSeats").value(0));

        mockMvc.perform(patch("/api/v1/events/{eventId}/tickets/{ticketId}/cancellation", eventId, ticketId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/events/{id}/availableseats", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numberOfAvailableSeats").value(1));
    }

    @Test
    void seatsLeftForUnknownEventIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/events/{id}/availableseats", 999_999))
                .andExpect(status().isNotFound());
    }

}
