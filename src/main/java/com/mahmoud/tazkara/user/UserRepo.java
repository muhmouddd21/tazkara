package com.mahmoud.tazkara.user;

import java.util.Optional;

import org.springframework.data.repository.Repository;


public interface UserRepo extends Repository<User, Long> {  

    Optional<User> findByEmail(String email);

    User save(User user);

    void deleteByEmail(String email);
}