package com.example.ojpt.controller;

import com.example.ojpt.config.JwtProperties;
import com.example.ojpt.converter.AuthConverter;
import com.example.ojpt.exception.GlobalExceptionHandler;
import com.example.ojpt.security.JwtAuthenticationFilter;
import com.example.ojpt.security.JwtService;
import com.example.ojpt.security.RefreshTokenStore;
import com.example.ojpt.security.TokenBlacklistService;
import com.example.ojpt.service.PasswordResetRequestService;
import com.example.ojpt.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig
@WebAppConfiguration
@ContextConfiguration(classes = AuthPasswordResetControllerTest.TestConfig.class)
class AuthPasswordResetControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordResetRequestService passwordResetRequestService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Mockito.reset(passwordResetRequestService);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(webApplicationContext.getBean(FilterChainProxy.class))
                .build();
    }

    @Test
    void submitPasswordResetRequest_acceptsAccountAndReturnsSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Payload("user@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(passwordResetRequestService).submitRequest("user@example.com");
    }

    @Test
    void submitPasswordResetRequest_rejectsBlankAccount() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Payload(" "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").isString());

        verify(passwordResetRequestService, never()).submitRequest(anyString());
    }

    private record Payload(String account) {
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    @Import(GlobalExceptionHandler.class)
    static class TestConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/auth/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/problems", "/api/problems/**").permitAll()
                            .requestMatchers("/api/admin/**").hasRole("ADMIN")
                            .anyRequest().authenticated()
                    )
                    .exceptionHandling(ex -> ex
                            .authenticationEntryPoint((request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                            .accessDeniedHandler((request, response, accessDeniedException) -> response.sendError(HttpServletResponse.SC_FORBIDDEN))
                    )
                    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            return http.build();
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, TokenBlacklistService tokenBlacklistService) {
            return new JwtAuthenticationFilter(jwtService, tokenBlacklistService);
        }

        @Bean
        AuthenticationManager authenticationManager() {
            return Mockito.mock(AuthenticationManager.class);
        }

        @Bean
        JwtService jwtService() {
            return Mockito.mock(JwtService.class);
        }

        @Bean
        RefreshTokenStore refreshTokenStore() {
            return Mockito.mock(RefreshTokenStore.class);
        }

        @Bean
        TokenBlacklistService tokenBlacklistService() {
            return Mockito.mock(TokenBlacklistService.class);
        }

        @Bean
        JwtProperties jwtProperties() {
            return Mockito.mock(JwtProperties.class);
        }

        @Bean
        UserService userService() {
            return Mockito.mock(UserService.class);
        }

        @Bean
        PasswordResetRequestService passwordResetRequestService() {
            return Mockito.mock(PasswordResetRequestService.class);
        }

        @Bean
        AuthConverter authConverter() {
            return Mockito.mock(AuthConverter.class);
        }

        @Bean
        AuthController authController(
                AuthenticationManager authenticationManager,
                JwtService jwtService,
                RefreshTokenStore refreshTokenStore,
                TokenBlacklistService tokenBlacklistService,
                JwtProperties jwtProperties,
                UserService userService,
                PasswordResetRequestService passwordResetRequestService,
                AuthConverter authConverter
        ) {
            return new AuthController(
                    authenticationManager,
                    jwtService,
                    refreshTokenStore,
                    tokenBlacklistService,
                    jwtProperties,
                    userService,
                    passwordResetRequestService,
                    authConverter
            );
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(ObjectMapper objectMapper) {
            return new MappingJackson2HttpMessageConverter(objectMapper);
        }
    }
}
