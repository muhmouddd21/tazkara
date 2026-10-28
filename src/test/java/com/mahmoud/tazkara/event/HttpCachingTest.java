package com.mahmoud.tazkara.event;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers 
@SpringBootTest 
@AutoConfigureMockMvc 
@ActiveProfiles ("test")
public class HttpCachingTest {
    @Container 
    @ServiceConnection 
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");


    @Autowired MockMvc mockMvc;

    private String registerAndLogin(String email) throws Exception {
        String credentials = """
                {"email":"%s","password":"correct-horse-battery"}""".formatted(email);
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType("application/json").content(credentials));
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json").content(credentials)).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    private long createEvent(String token, String name) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/v1/events")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("""
                    {"name":"%s","city":"Alexandria","category":"music","totalSeats":100,
                      "startAt": "2027-06-01T18:00:00Z",
                      "endAt": "2027-06-01T21:00:00Z"}
                    """.formatted(name)))
            .andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.id")).longValue();
    }


    @Test 
    void firstReadCarriesEtagAndCachePolicy() throws Exception {
        long id = createEvent(registerAndLogin("etag1@tazkara.dev"), "Etag Night");

        mockMvc.perform(get("/api/v1/events/" + id))
            .andExpect(status().isOk())
            .andExpect(header().string("ETag", "\"0\""))
            .andExpect(header().string("Cache-Control", "max-age=60, public"));
    }

    @Test
    void matchingEtagGets304WithNoBody() throws Exception {
        long id = createEvent(registerAndLogin("etag2@tazkara.dev"), "Etag Night II");

        mockMvc.perform(get("/api/v1/events/" + id).header("If-None-Match", "\"0\""))
            .andExpect(status().isNotModified())
            .andExpect(content().string(""));
    }


    @Test
    void anEditChangesTheEtag() throws Exception {
        String token = registerAndLogin("etag3@tazkara.dev");
        long id = createEvent(token, "Etag Night III");

        mockMvc.perform(put("/api/v1/events/" + id)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("""
                    {"name":"Etag Night III","city":"Alexandriaa","category":"music","totalSeats":100,"version":0}
                    """))
            .andExpect(status().isOk());

        // the client still holds version 0, which is now stale: expect a full 200 and the new tag
        mockMvc.perform(get("/api/v1/events/" + id).header("If-None-Match", "\"0\""))
            .andExpect(status().isOk())
            .andExpect(header().string("ETag", "\"1\""));
    }
}
