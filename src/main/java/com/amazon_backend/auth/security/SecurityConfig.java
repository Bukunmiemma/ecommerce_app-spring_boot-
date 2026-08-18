package com.amazon_backend.auth.security;
import com.amazon_backend.auth.entity.Role;
import com.amazon_backend.auth.entity.User;
import com.amazon_backend.auth.repository.UserRepository;
import com.amazon_backend.auth.service.CustomUserDetailsService;
import com.amazon_backend.auth.service.JwtService;
import com.amazon_backend.auth.service.RefreshTokenService;
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
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**",
                                "/oauth2/**",
                                "/login/**",
                                "/error",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // Product Management for ADMIN Only
                        .requestMatchers(HttpMethod.POST,"/api/categories/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,"/api/categories/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE,"/api/categories/**")
                        .hasRole("ADMIN")


                        // Product Management for ADMIN Only
                        .requestMatchers(HttpMethod.POST,"/api/products/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**")
                        .hasRole("ADMIN")

                        //Product/category viewing for users
                        .requestMatchers(HttpMethod.GET,"/api/products/**")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET,"/api/categories/**")
                        .authenticated()
                        .anyRequest().authenticated()



                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler((request, response, authentication) -> {

                            OAuth2User user = (OAuth2User) authentication.getPrincipal();

                            String email = user.getAttribute("email");
                            String name = user.getAttribute("name");


                            User appUser = userRepository.findByEmail(email)
//                                    .map(existingUser -> {
//
//                                   if(existingUser.getProvider().equals("LOCAL")) {
//                                       try{
//                                           response.sendError( HttpServletResponse.SC_BAD_REQUEST,"Please login with email and password");
//                                       }catch(IOException e){
//                                           throw new RuntimeException(e);
//
//                                       }
//                                   }
//                                        return existingUser;
//                                    })
                                    .orElseGet(() -> {
                                        User newUser = new User();
                                        newUser.setEmail(email);
                                        newUser.setName(name);
                                        newUser.setPassword("");
                                        newUser.setRole(Role.USER);
//                                        newUser.setProvider("GOOGLE");
                                        return userRepository.save(newUser);
                                    });

                            String jwt = jwtService.generateToken(appUser);
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
