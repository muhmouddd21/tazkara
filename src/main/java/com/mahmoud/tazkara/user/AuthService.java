package com.mahmoud.tazkara.user;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.mahmoud.tazkara.security.JwtService;



@Service 
public class AuthService {
    private final UserRepo userRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtservice;
    
    public AuthService(JwtService jwtService, UserRepo userRepo, PasswordEncoder encoder){
        this.jwtservice =jwtService;
        this.userRepo =userRepo;
        this.encoder = encoder;
    }

    public void register(RegisterRequest registerRequest){
        if(userRepo.findByEmail(registerRequest.email()).isPresent()){
            throw new ResponseStatusException(HttpStatus.CONFLICT,"email already registered");
        }
        userRepo.save(new User(null,registerRequest.email(),encoder.encode(registerRequest.password()), "FREE"));
    }


    public LoginResponse login(LoginRequest request) {
        return userRepo.findByEmail(request.email())
                .filter(user -> encoder.matches(request.password(), user.passwordHash()))
                .map(user -> new LoginResponse(jwtservice.generate(user)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "invalid email or password"));
    }
}
