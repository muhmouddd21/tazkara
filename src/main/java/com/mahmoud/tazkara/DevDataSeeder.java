package com.mahmoud.tazkara;

import java.time.Duration;
import java.time.Instant;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.mahmoud.tazkara.event.Event;
import com.mahmoud.tazkara.event.EventRepo;
import com.mahmoud.tazkara.user.Plan;
import com.mahmoud.tazkara.user.User;
import com.mahmoud.tazkara.user.UserRepo;

@Configuration 
@Profile("dev")
public class DevDataSeeder {
    @Bean 
    CommandLineRunner seedDevData(UserRepo users, EventRepo events, PasswordEncoder encoder){
        return args -> {
            if (users.findByEmail("organizer@dev.tazkara").isPresent()) {
                return;              // idempotent: restarting must not duplicate
            }
            User organizer = users.save(new User(null, "organizer@dev.tazkara",
                    encoder.encode("dev-password"), Plan.PRO));
            users.save(new User(null, "buyer@dev.tazkara",
                    encoder.encode("dev-password"), Plan.FREE));

            events.save(new Event(null, organizer.email(), "Cairo Jazz Night", "Cairo", "music", 300,Instant.now().plus(Duration.ofDays(30)),Instant.now().plus(Duration.ofDays(33))));
            events.save(new Event(null, organizer.email(), "Riyadh Tech Summit", "Riyadh", "conference", 1200,Instant.now().plus(Duration.ofDays(30)),Instant.now().plus(Duration.ofDays(33))));
            events.save(new Event(null, organizer.email(), "Alexandria Book Fair", "Alexandria", "culture", 500,Instant.now().plus(Duration.ofDays(30)),Instant.now().plus(Duration.ofDays(33))));
            System.out.println(">>> dev data seeded");
        };
    }
    
}

