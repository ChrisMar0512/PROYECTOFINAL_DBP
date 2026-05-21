package com.parkshare.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkshare.dto.AuthResponse;
import com.parkshare.dto.LoginRequest;
import com.parkshare.dto.RegisterRequest;
import com.parkshare.entity.User;
import com.parkshare.exception.DuplicateResourceException;
import com.parkshare.security.JwtAuthFilter;
import com.parkshare.security.JwtService;
import com.parkshare.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Deshabilitar seguridad para pruebas del controller
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private WalletService walletService;

    @MockBean
    private org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder;

    @MockBean
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    // TODO: Falta mockear UserRepository para que el código actual del AuthController
    // funcione en los tests, pero como se usa @MockBean en un @WebMvcTest,
    // necesitamos añadirlo.
    @MockBean
    private com.parkshare.repository.UserRepository userRepository;

    @BeforeEach
    void setUp() {
    }

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test");
        request.setEmail("test@test.com");
        request.setPassword("password");
        request.setPhone("123456789");
        request.setRole("DRIVER");

        User user = new User();
        user.setId(1L);
        user.setEmail(request.getEmail());
        user.setRole(User.Role.DRIVER);

        when(userRepository.findByEmail(request.getEmail())).thenReturn(java.util.Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(any(), any(User.class))).thenReturn("jwt.token.here");

        mockMvc.perform(post("/api/v1/auth/register-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt.token.here"))
                .andExpect(jsonPath("$.email").value("test@test.com"));
    }

    @Test
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test");
        request.setEmail("existing@test.com");
        request.setPassword("password");
        request.setPhone("123456789");
        request.setRole("DRIVER");

        when(userRepository.findByEmail(request.getEmail())).thenThrow(new DuplicateResourceException("Email exists"));

        mockMvc.perform(post("/api/v1/auth/register-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("password");

        User user = new User();
        user.setId(1L);
        user.setEmail(request.getEmail());
        user.setRole(User.Role.DRIVER);

        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(java.util.Optional.of(user));
        when(jwtService.generateToken(any(), any(User.class))).thenReturn("jwt.token.here");

        mockMvc.perform(post("/api/v1/auth/login-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.here"));
    }
}
