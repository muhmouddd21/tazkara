package com.mahmoud.tazkara.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class EventServiceTest {
    private EventService service;


    @BeforeEach 
    void setUp() {
        service = new EventService(new EventRepo());   // real service, real repo, no Spring
    }

    private CreateEventRequest request(String name,String city){
        return new CreateEventRequest(name, city, "music", 100);
    }

    @Test 
    void listWithoutCityFilterReturnsAllEvents() {
        service.create(request("Jazz Night", "Cairo"), "a@tazkara.dev", true);
        service.create(request("Tech Summit", "Riyadh"), "a@tazkara.dev", true);

        EventPageDTO page = service.list(null, 0, 10);   // null = no filter

        assertThat(page.items()).hasSize(2);
    }
    @Test
    void cityFilterIsCaseInsensitive() {
        service.create(request("Jazz Night", "Cairo"), "a@tazkara.dev", true);

        assertThat(service.list("CAIRO", 0, 10).items()).hasSize(1);
    }

    @Test
    void duplicateNameAndCityIsRejected() {          // the swapped-args bug's grave
        service.create(request("Jazz Night", "Cairo"), "a@tazkara.dev", true);

        assertThatThrownBy(() ->
                service.create(request("Jazz Night", "Cairo"), "b@tazkara.dev", true))
            .hasMessageContaining("already exists");
    }

    @Test
    void freePlanIsLimitedToThreeEvents() {
        service.create(request("One", "Cairo"), "free@tazkara.dev", false);
        service.create(request("Two", "Cairo"), "free@tazkara.dev", false);
        service.create(request("Three", "Cairo"), "free@tazkara.dev", false);

        assertThatThrownBy(() ->
                service.create(request("Four", "Cairo"), "free@tazkara.dev", false))
            .hasMessageContaining("free plan");
    }

    @Test
    void onlyTheOrganizerMayDelete() {
        Event event = service.create(request("Jazz Night", "Cairo"), "owner@tazkara.dev", true);

        assertThatThrownBy(() -> service.delete(event.id(), "intruder@tazkara.dev",true))
            .hasMessageContaining("organizer");
    }

    @Test 
    void updateCannotChangeTheOrganizer(){
        Event event = service.create(request("Jazz Night", "Cairo"), "organizer@tazkara.dev", true);
        UpdateEventRequest newEvent = new UpdateEventRequest("mahmoud", "benha", "music", 100);


        assertThatThrownBy(() -> service.update(event.id(), newEvent, "mahmoud@tazkara.dev", true))
        .hasMessageContaining("organizer may modify");
        ;

    }

}
