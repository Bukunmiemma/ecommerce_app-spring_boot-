package com.amazon_backend.service;

import com.amazon_backend.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {
   @Value("${jwt.secret}")
    private String secret;

   private Key getSigningKey(){
       return  Keys.hmacShaKeyFor(secret.getBytes());
   }

   public String generateToken ( User user) {
       return Jwts.builder()
               .setSubject(user.getEmail())
               .claim("role", user.getRole().name())
               .setIssuedAt(new Date())
               .setExpiration(
                       new Date(System.currentTimeMillis() + 1000 * 60 * 60))
               .signWith(getSigningKey()).compact();
   }
   public Claims extractAllClaims(String token){
       return Jwts.parserBuilder().setSigningKey(getSigningKey())
               .build()
               .parseClaimsJws(token)
               .getBody();

   }
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject(); //to get the users email
    }


    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);


        return (email.equals(userDetails.getUsername())&& !isTokenExpired(token)) ;
    }



}
