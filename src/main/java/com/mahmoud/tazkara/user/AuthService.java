package com.mahmoud.tazkara.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mahmoud.tazkara.error.DomainException;
import com.mahmoud.tazkara.security.JwtService;
import static com.mahmoud.tazkara.error.DomainException.Kind.*;




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
    @Transactional 
    public void register(RegisterRequest registerRequest){
        if(userRepo.findByEmail(registerRequest.email()).isPresent()){
            throw new DomainException(CONFLICT, "email-taken", "email already registered");
        }
        userRepo.save(new User(null,registerRequest.email(),encoder.encode(registerRequest.password()), Plan.FREE));
    }


    @Transactional 
    public LoginResponse login(LoginRequest request) {
        return userRepo.findByEmail(request.email())
                .filter(user -> encoder.matches(request.password(), user.passwordHash()))
                .map(user -> new LoginResponse(jwtservice.generate(user)))
                .orElseThrow(() -> new DomainException(UNAUTHENTICATED, "bad-credentials",
                        "invalid email or password"));
    }

    @Transactional 
    public void upgradeToPro(String email){
        
        User user =userRepo.findByEmail(email).orElseThrow(()-> new DomainException(NOT_FOUND, "user-not-found", "no such user"));
        userRepo.save(new User(user.id(),user.email(),user.passwordHash(),Plan.PRO));
    }
}
