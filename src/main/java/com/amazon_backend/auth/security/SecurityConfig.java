package com.amazon_backend.auth.security;
import com.amazon_backend.auth.entity.Role;
import com.amazon_backend.auth.entity.User;
import com.amazon_backend.auth.repository.UserRepository;
import com.amazon_backend.auth.service.CustomUserDetailsService;
import com.amazon_backend.auth.service.JwtService;
import com.amazon_backend.auth.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity

public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository ;
    private final JwtService jwtService;
    private final RefreshTokenService  refreshTokenService;


    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CustomUserDetailsService userDetailsService, UserRepository userRepository, JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;

        this.refreshTokenService = refreshTokenService;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider =
                new DaoAuthenticationProvider(userDetailsService);

        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    @Bean
    public AuthenticationManager
    authenticationManager(AuthenticationConfiguration config) throws Exception{
        return config.getAuthenticationManager();
    }



    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exception -> exception
                        .defaultAuthenticationEntryPointFor(
                                (request, response, authException) ->
                                        response.sendError(
                                                HttpServletResponse.SC_UNAUTHORIZED,
                                                "Unauthorized"
                                        ),
                                PathPatternRequestMatcher.withDefaults()
                                        .matcher("/api/**")
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        // Email verification requires an authenticated user.
                        // These rules MUST come before /auth/**.
                        .requestMatchers(
                                "/auth/email-verification/**"
                        ).authenticated()

                        // Public authentication endpoints.
                        .requestMatchers(
                                "/auth/**",
                                "/oauth2/**",
                                "/login/**",
                                "/error",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // Category of product Management for ADMIN Only
                        .requestMatchers(HttpMethod.POST,"/api/categories/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,"/api/categories/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE,"/api/categories/**")
                        .hasRole("ADMIN")


                        // Product Management for authenticated vendors of their respective store
                        .requestMatchers(HttpMethod.POST,"/api/products/store/*")
                        .authenticated()

                        .requestMatchers(HttpMethod.PUT, "/api/products/**")
                        .authenticated()

                        .requestMatchers(HttpMethod.DELETE, "/api/products/*")
                        .authenticated()

                        .requestMatchers(HttpMethod.PATCH,"/api/products/*/restock")
                        .authenticated()

                        //Product/category viewing for users
                                // A user's private store list requires authentication.
                      // IMPORTANT: This must come before the public store matcher.
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/stores/getStores"
                                ).authenticated()

                         // Public store discovery.
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/stores/**"
                                ).permitAll()

                       // Public product browsing.
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/products/**"
                                ).permitAll()

                     // Public category browsing.
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/categories/**"
                                ).permitAll()

                        //Users should be able to search a store
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/stores/search"
                        ).authenticated()
           // Users should be able to search for products
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/products/search"
                        ).authenticated()
                                .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler((request, response, authentication) -> {

                            OAuth2User user = (OAuth2User) authentication.getPrincipal();

                            String email = user.getAttribute("email");
                            String name = user.getAttribute("name");
                            Boolean googleEmailVerified = user.getAttribute("email_verified");
                            User appUser = userRepository.findByEmail(email)
                                    .orElseGet(() -> {
                                        User newUser = new User();
                                        newUser.setEmail(email);
                                        newUser.setName(name);
                                        newUser.setPassword("");
                                        newUser.setRole(Role.USER);
                                        newUser.setEmailVerified(
                                                Boolean.TRUE.equals(googleEmailVerified)
                                        );

                                        return userRepository.save(newUser);
                                    });
                            // Google-verified email addresses can be trusted.
                          // Do not downgrade an already verified account.
                            if (Boolean.TRUE.equals(googleEmailVerified)
                                    && !appUser.isEmailVerified()) {

                                appUser.setEmailVerified(true);
                                userRepository.save(appUser);
                            }
                            String accessToken = jwtService.generateToken(appUser);
                            String refreshToken = refreshTokenService.createRefreshToken(email).getToken();

                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"accessToken\":\"" + accessToken + "\"," +
                                            " \"refreshToken\":\"" + refreshToken + "\"}"

                            );
                        })
                )


                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);


        return http.build();
    }
}
