package com.mahmoud.tazkara.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@Component 
public class JwtAuthFilter extends OncePerRequestFilter{

    private final JwtService jwtService;


    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

            String header = request.getHeader("Authorization");
            if(header != null && header.startsWith("Bearer ")){
                jwtService.parse(header.substring(7)).ifPresent(claims ->{
                    var authentication = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(),
                        null,
                        List.of(new SimpleGrantedAuthority(
                            "PLAN_" + claims.get("plan", String.class)))
                        );
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                });
            }

            filterChain.doFilter(request, response);
    }
    
}
