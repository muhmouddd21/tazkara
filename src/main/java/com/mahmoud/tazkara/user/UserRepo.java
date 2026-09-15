package com.mahmoud.tazkara.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

@Repository
public class UserRepo {

    private Map<Long, User> users = new HashMap<>();

    private final AtomicLong counter = new AtomicLong();

    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    public Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    public User save(User user) {
        Long id = (user.id() != null)
                ? user.id()
                : counter.incrementAndGet();

        User stored = new User(
                id,
                user.email(),
                user.passwordHash(),
                user.plan()
        );

        users.put(id, stored);

        return stored;
    }

    public void deleteById(Long id) {
        users.remove(id);
    }
}