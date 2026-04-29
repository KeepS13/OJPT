package com.example.ojpt.controller;

import com.example.ojpt.dto.JudgeEnvironmentHealthDTO;
import com.example.ojpt.dto.ProblemCreateDTO;
import com.example.ojpt.common.PageResult;
import com.example.ojpt.exception.GlobalExceptionHandler;
import com.example.ojpt.judge.JudgeEnvironmentHealthService;
import com.example.ojpt.security.JwtAuthenticationFilter;
import com.example.ojpt.security.JwtService;
import com.example.ojpt.security.TokenBlacklistService;
import com.example.ojpt.service.AdminService;
import com.example.ojpt.service.PasswordResetRequestService;
import com.example.ojpt.service.ProblemService;
import com.example.ojpt.service.ProblemTestCaseService;
import com.example.ojpt.service.TagService;
import com.example.ojpt.service.UserService;
import com.example.ojpt.vo.PasswordResetRequestVO;
import com.example.ojpt.vo.ProblemSimpleVO;
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
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig
@WebAppConfiguration
@ContextConfiguration(classes = AdminProblemControllerSecurityTest.TestConfig.class)
class AdminProblemControllerSecurityTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private ProblemService problemService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private PasswordResetRequestService passwordResetRequestService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Mockito.reset(jwtService, tokenBlacklistService, problemService, adminService, passwordResetRequestService);
        when(tokenBlacklistService.isUserBlacklisted(any())).thenReturn(false);
        when(tokenBlacklistService.isPermissionChangeBlacklisted(any())).thenReturn(false);
        when(tokenBlacklistService.isBlacklisted(any())).thenReturn(false);

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(webApplicationContext.getBean(FilterChainProxy.class))
                .build();
    }

    @Test
    void createProblem_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/admin/problems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateDto())))
                .andExpect(status().isUnauthorized());

        verify(problemService, never()).createDraft(any(), any());
    }

    @Test
    void createProblem_forbidsNonAdminUser() throws Exception {
        when(jwtService.parseToken("user-token")).thenReturn(accessClaims(2L, List.of("USER")));

        mockMvc.perform(post("/api/admin/problems")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateDto())))
                .andExpect(status().isForbidden());

        verify(problemService, never()).createDraft(any(), any());
    }

    @Test
    void createProblem_allowsAdminUser() throws Exception {
        when(jwtService.parseToken("admin-token")).thenReturn(accessClaims(1L, List.of("ADMIN")));
        ProblemSimpleVO result = new ProblemSimpleVO();
        result.setId(1001L);
        result.setTitle("Draft");
        result.setDifficulty("EASY");
        result.setStatus("DRAFT");
        when(problemService.createDraft(eq(1L), any(ProblemCreateDTO.class))).thenReturn(result);

        mockMvc.perform(post("/api/admin/problems")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1001L))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        verify(problemService).createDraft(eq(1L), any(ProblemCreateDTO.class));
    }

    @Test
    void judgeEnvironmentHealth_forbidsNonAdminUser() throws Exception {
        when(jwtService.parseToken("user-token")).thenReturn(accessClaims(2L, List.of("USER")));

        mockMvc.perform(get("/api/admin/judge-environment/health")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listUsers_bindsExplicitRequestParamNames() throws Exception {
        when(jwtService.parseToken("admin-token")).thenReturn(accessClaims(1L, List.of("ADMIN")));
        when(adminService.getUsers(2, 20, 1, "ADMIN", "alice")).thenReturn(PageResult.empty(2, 20));

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer admin-token")
                        .param("page", "2")
                        .param("size", "20")
                        .param("status", "1")
                        .param("roleType", "ADMIN")
                        .param("keyword", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(adminService).getUsers(2, 20, 1, "ADMIN", "alice");
    }

    @Test
    void listPasswordResetRequests_bindsExplicitRequestParamName() throws Exception {
        when(jwtService.parseToken("admin-token")).thenReturn(accessClaims(1L, List.of("ADMIN")));
        when(passwordResetRequestService.listRequests("PENDING")).thenReturn(List.of(new PasswordResetRequestVO()));

        mockMvc.perform(get("/api/admin/password-reset-requests")
                        .header("Authorization", "Bearer admin-token")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(passwordResetRequestService).listRequests("PENDING");
    }

    private Claims accessClaims(Long userId, List<String> roles) {
        DefaultClaims claims = new DefaultClaims();
        claims.setSubject(String.valueOf(userId));
        claims.setId("access-jti");
        claims.put("type", "access");
        claims.put("roles", roles);
        return claims;
    }

    private ProblemCreateDTO buildCreateDto() {
        ProblemCreateDTO dto = new ProblemCreateDTO();
        dto.setTitle("Draft");
        dto.setDifficulty("EASY");
        dto.setStatementMd("## Statement");
        dto.setTimeLimitMs(1000);
        dto.setMemoryLimitKb(256000);
        return dto;
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
        AdminService adminService() {
            return Mockito.mock(AdminService.class);
        }

        @Bean
        PasswordResetRequestService passwordResetRequestService() {
            return Mockito.mock(PasswordResetRequestService.class);
        }

        @Bean
        UserService userService() {
            return Mockito.mock(UserService.class);
        }

        @Bean
        ProblemService problemService() {
            return Mockito.mock(ProblemService.class);
        }

        @Bean
        ProblemTestCaseService problemTestCaseService() {
            return Mockito.mock(ProblemTestCaseService.class);
        }

        @Bean
        TagService tagService() {
            return Mockito.mock(TagService.class);
        }

        @Bean
        JudgeEnvironmentHealthService judgeEnvironmentHealthService() {
            JudgeEnvironmentHealthService service = Mockito.mock(JudgeEnvironmentHealthService.class);
            when(service.checkHealth()).thenReturn(new JudgeEnvironmentHealthDTO("UP", "ok", List.of()));
            return service;
        }

        @Bean
        AdminController adminController(
                AdminService adminService,
                PasswordResetRequestService passwordResetRequestService,
                UserService userService,
                ProblemService problemService,
                ProblemTestCaseService problemTestCaseService,
                TagService tagService
        ) {
            return new AdminController(
                    adminService,
                    passwordResetRequestService,
                    userService,
                    problemService,
                    problemTestCaseService,
                    tagService
            );
        }

        @Bean
        AdminJudgeEnvironmentController adminJudgeEnvironmentController(JudgeEnvironmentHealthService judgeEnvironmentHealthService) {
            return new AdminJudgeEnvironmentController(judgeEnvironmentHealthService);
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
