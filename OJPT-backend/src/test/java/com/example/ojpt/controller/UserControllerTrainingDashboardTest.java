package com.example.ojpt.controller;

import com.example.ojpt.common.PageResult;
import com.example.ojpt.exception.GlobalExceptionHandler;
import com.example.ojpt.security.JwtAuthenticationFilter;
import com.example.ojpt.security.JwtService;
import com.example.ojpt.security.TokenBlacklistService;
import com.example.ojpt.service.SubmissionService;
import com.example.ojpt.service.TrainingDashboardService;
import com.example.ojpt.service.UserService;
import com.example.ojpt.vo.training.dashboard.TrainingDashboardRecentSubmissionVO;
import com.example.ojpt.vo.training.dashboard.UserTrainingDashboardVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig
@WebAppConfiguration
@ContextConfiguration(classes = UserControllerTrainingDashboardTest.TestConfig.class)
class UserControllerTrainingDashboardTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private TrainingDashboardService trainingDashboardService;

    @Autowired
    private SubmissionService submissionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Mockito.reset(jwtService, tokenBlacklistService, trainingDashboardService, submissionService);
        when(tokenBlacklistService.isUserBlacklisted(any())).thenReturn(false);
        when(tokenBlacklistService.isPermissionChangeBlacklisted(any())).thenReturn(false);
        when(tokenBlacklistService.isBlacklisted(any())).thenReturn(false);

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(webApplicationContext.getBean(FilterChainProxy.class))
                .build();
    }

    @Test
    void getTrainingDashboard_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me/training-dashboard"))
                .andExpect(status().isUnauthorized());

        verify(trainingDashboardService, never()).getTrainingDashboard(any());
    }

    @Test
    void getTrainingDashboard_returnsCurrentUserDashboard() throws Exception {
        when(jwtService.parseToken("user-token")).thenReturn(accessClaims(1001L, List.of("USER")));

        UserTrainingDashboardVO dashboard = new UserTrainingDashboardVO();
        dashboard.setTotalSubmissions(12L);
        dashboard.setAcceptedSubmissions(9L);
        dashboard.setSolvedProblemCount(5L);
        dashboard.setAcceptanceRate(75.0);
        dashboard.setStatusDistribution(Map.of("AC", 9L, "WA", 3L));
        dashboard.setDifficultyDistribution(Map.of("EASY", 3L, "MEDIUM", 2L));
        dashboard.setRecentSubmissions(List.of(
                new TrainingDashboardRecentSubmissionVO(
                        9001L,
                        2001L,
                        1,
                        "Two Sum",
                        "Java",
                        "AC",
                        12,
                        128,
                        LocalDateTime.of(2026, 4, 29, 10, 30)
                )
        ));
        when(trainingDashboardService.getTrainingDashboard(1001L)).thenReturn(dashboard);

        mockMvc.perform(get("/api/users/me/training-dashboard")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalSubmissions").value(12))
                .andExpect(jsonPath("$.data.acceptedSubmissions").value(9))
                .andExpect(jsonPath("$.data.solvedProblemCount").value(5))
                .andExpect(jsonPath("$.data.statusDistribution.AC").value(9))
                .andExpect(jsonPath("$.data.recentSubmissions[0].problemTitle").value("Two Sum"));

        verify(trainingDashboardService).getTrainingDashboard(1001L);
    }

    @Test
    void getCurrentUserSubmissions_bindsExplicitRequestParamNames() throws Exception {
        when(jwtService.parseToken("user-token")).thenReturn(accessClaims(1001L, List.of("USER")));
        when(submissionService.getCurrentUserSubmissions(1001L, 2, 20)).thenReturn(PageResult.empty(2, 20));

        mockMvc.perform(get("/api/users/me/submissions")
                        .header("Authorization", "Bearer user-token")
                        .param("page", "2")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(submissionService).getCurrentUserSubmissions(1001L, 2, 20);
    }

    private Claims accessClaims(Long userId, List<String> roles) {
        DefaultClaims claims = new DefaultClaims();
        claims.setSubject(String.valueOf(userId));
        claims.setId("access-jti");
        claims.put("type", "access");
        claims.put("roles", roles);
        return claims;
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
        JwtService jwtService() {
            return Mockito.mock(JwtService.class);
        }

        @Bean
        TokenBlacklistService tokenBlacklistService() {
            return Mockito.mock(TokenBlacklistService.class);
        }

        @Bean
        UserService userService() {
            return Mockito.mock(UserService.class);
        }

        @Bean
        SubmissionService submissionService() {
            return Mockito.mock(SubmissionService.class);
        }

        @Bean
        TrainingDashboardService trainingDashboardService() {
            return Mockito.mock(TrainingDashboardService.class);
        }

        @Bean
        UserController userController(
                UserService userService,
                SubmissionService submissionService,
                TrainingDashboardService trainingDashboardService
        ) {
            return new UserController(userService, submissionService, trainingDashboardService);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

    }
}
