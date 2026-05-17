package com.amazon_backend.controller;
import com.amazon_backend.dto.UserResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/user")
    public String userEndpoint(){
        return "User access granted";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public String adminEndpoint(){
        return "Admin access granted";
    }
//    @GetMapping("/profile")
//    public UserResponse profile(Authentication authentication){
//        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
//
//        return new UserResponse(userDetails.getUsername(),
//                userDetails.getAuthorities().toString()
//                );
//
//    }

}
