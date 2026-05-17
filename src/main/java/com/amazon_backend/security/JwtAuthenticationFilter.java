package com.amazon_backend.security;
import com.amazon_backend.repository.UserRepository;
import com.amazon_backend.service.BlacklistedTokenService;
import com.amazon_backend.service.JwtService;
import io.jsonwebtoken.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

    @Component
    public class JwtAuthenticationFilter extends OncePerRequestFilter {
        private final JwtService jwtService;
        private final UserDetailsService userDetailsService;
        private final BlacklistedTokenService blacklistedTokenService;


        public JwtAuthenticationFilter(
                JwtService jwtService,
                UserRepository userRepository,
                UserDetailsService userDetailsService, BlacklistedTokenService blacklistedTokenService) {
            this.jwtService = jwtService;
            this.userDetailsService =userDetailsService;
            this.blacklistedTokenService = blacklistedTokenService;
        }
        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException, java.io.IOException {

            final String authHeader = request.getHeader("Authorization");
            final String token;
            final String email;

            // Check if header exists
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            // Extract token
            token = authHeader.substring(7);

            //Check blacklist here
            if (blacklistedTokenService.isBlacklisted(token)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                filterChain.doFilter(request, response);
                return;
            }
            // Extract email from token
            email =jwtService.extractEmail(token);

            // Check if user is not already authenticated
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = userDetailsService.loadUserByUsername(email);


//                User user = userRepository.findByEmail(email)
//                        .orElseThrow(() -> new RuntimeException("User not found"));

                //  Create authentication object
                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // Set authentication in context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            filterChain.doFilter(request, response);
        }
    }

