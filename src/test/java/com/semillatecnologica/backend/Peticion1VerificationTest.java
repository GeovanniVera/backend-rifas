package com.semillatecnologica.backend;

import com.semillatecnologica.backend.modules.admin.service.AuditService;
import com.semillatecnologica.backend.modules.auth.dto.LoginRequest;
import com.semillatecnologica.backend.modules.auth.model.Role;
import com.semillatecnologica.backend.modules.auth.model.User;
import com.semillatecnologica.backend.modules.auth.repository.*;
import com.semillatecnologica.backend.modules.auth.service.AuthServiceImpl;
import com.semillatecnologica.backend.modules.notification.inapp.service.InAppNotificationService;
import com.semillatecnologica.backend.modules.notification.service.NotificationService;
import com.semillatecnologica.backend.modules.notification.template.TemplateRenderer;
import com.semillatecnologica.backend.modules.payments.service.PaymentService;
import com.semillatecnologica.backend.modules.products.model.Product;
import com.semillatecnologica.backend.modules.products.repository.ProductRepository;
import com.semillatecnologica.backend.modules.raffles.dto.CreateCheckoutIntentRequest;
import com.semillatecnologica.backend.modules.raffles.model.Raffle;
import com.semillatecnologica.backend.modules.raffles.repository.RaffleRepository;
import com.semillatecnologica.backend.modules.raffles.repository.RaffleTicketRepository;
import com.semillatecnologica.backend.modules.raffles.service.RaffleService;
import com.semillatecnologica.backend.modules.raffles.service.RaffleTicketService;
import com.semillatecnologica.backend.modules.storage.service.StorageService;
import com.semillatecnologica.backend.security.cookie.AuthCookieService;
import com.semillatecnologica.backend.security.jwt.JwtTokenService;
import com.semillatecnologica.backend.shared.exception.ValidationException;
import com.semillatecnologica.backend.shared.ratelimit.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Peticion1VerificationTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private VerificationTokenRepository verificationTokenRepository;
    @Mock private OtpChallengeRepository otpChallengeRepository;
    @Mock private JwtTokenService jwtTokenService;
    @Mock private AuthCookieService authCookieService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;
    @Mock private StorageService storageService;
    @Mock private RateLimiterService rateLimiterService;

    @Mock private RaffleRepository raffleRepository;
    @Mock private ProductRepository productRepository;
    @Mock private RaffleTicketService ticketService;

    @Mock private RaffleTicketRepository ticketRepository;
    @Mock private PaymentService paymentService;
    @Mock private InAppNotificationService inAppNotificationService;

    @Test
    void templateRendererRendersLoginAlertAndEscapesHtml() {
        TemplateRenderer renderer = new TemplateRenderer();
        String html = renderer.render("login-alert", Map.of(
                "userName", "Admin <Test>",
                "roleLabel", "Administrador",
                "loginDate", "19/11/2026 11:40:00",
                "ipAddress", "127.0.0.1",
                "userAgent", "Mozilla/5.0"
        ));

        assertThat(html).contains("Nuevo inicio de sesión detectado");
        assertThat(html).contains("Admin &lt;Test&gt;");
        assertThat(html).contains("Administrador");
        assertThat(html).contains("127.0.0.1");
    }

    @Test
    void loginSendsEmailAlertForAdminAndClient() {
        AuthServiceImpl authService = new AuthServiceImpl(
                userRepository, roleRepository, refreshTokenRepository,
                verificationTokenRepository, otpChallengeRepository,
                jwtTokenService, authCookieService, passwordEncoder,
                auditService, notificationService, storageService, rateLimiterService
        );
        ReflectionTestUtils.setField(authService, "accessTokenExpiration", 900);

        Role adminRole = Role.builder().id("role-admin").name("admin").build();
        User admin = User.builder()
                .id("u-1")
                .email("admin@test.com")
                .name("Admin User")
                .password("hashed")
                .isVerified(true)
                .suspended(false)
                .roles(Set.of(adminRole))
                .build();

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        when(req.getRemoteAddr()).thenReturn("10.0.0.5");
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getHeader("User-Agent")).thenReturn("JUnit");

        when(rateLimiterService.tryConsume(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(userRepository.findByEmailWithRolesAndPermissions("admin@test.com"))
                .thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);
        when(jwtTokenService.generateAccessToken("u-1", "admin@test.com")).thenReturn("jwt-token");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("admin@test.com");
        loginReq.setPassword("secret");

        authService.login(loginReq, req, res);

        verify(notificationService).sendLoginAlertEmail(
                eq("admin@test.com"),
                eq("Admin User"),
                eq("Administrador"),
                eq("10.0.0.5"),
                eq("JUnit"),
                any(LocalDateTime.class)
        );
    }

    @Test
    void createRaffleRejectsDrawDateBeforeStartDate() {
        RaffleService raffleService = new RaffleService(
                raffleRepository, productRepository, storageService,
                auditService, ticketService, userRepository, notificationService
        );
        ReflectionTestUtils.setField(raffleService, "ticketRate", new BigDecimal("0.07"));
        ReflectionTestUtils.setField(raffleService, "minTicketPrice", new BigDecimal("1.00"));

        Product product = Product.builder()
                .id("p-1")
                .name("Laptop")
                .price(new BigDecimal("10000.00"))
                .stock(1)
                .category("Gaming")
                .build();

        when(productRepository.findById("p-1")).thenReturn(Optional.of(product));
        when(raffleRepository.existsByProductIdAndStatusIn(eq("p-1"), anyList())).thenReturn(false);

        LocalDateTime start = LocalDateTime.now().plusDays(5);
        LocalDateTime end = LocalDateTime.now().plusDays(2);

        assertThatThrownBy(() -> raffleService.create("p-1", start, end, "admin-1", null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("posterior a la fecha de inicio");
    }

    @Test
    void checkoutRejectsPurchaseBeforeRaffleStartDate() {
        RaffleTicketService raffleTicketService = new RaffleTicketService(
                raffleRepository, ticketRepository, paymentService,
                auditService, inAppNotificationService, notificationService, userRepository
        );

        Raffle futureRaffle = Raffle.builder()
                .id("r-1")
                .productId("p-1")
                .name("Rifa futura")
                .marginPercent(new BigDecimal("100.00"))
                .ticketCount(29)
                .ticketPrice(new BigDecimal("100"))
                .startDate(LocalDateTime.now().plusDays(2))
                .drawDate(LocalDateTime.now().plusDays(10))
                .status(Raffle.Status.ACTIVE)
                .build();

        when(raffleRepository.findById("r-1")).thenReturn(Optional.of(futureRaffle));

        assertThatThrownBy(() -> raffleTicketService.createCheckoutIntent(
                new CreateCheckoutIntentRequest("r-1", List.of(1)),
                "user-1",
                null
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("iniciará el");
    }

    @Test
    void createRaffleRejectsStartDateFromPreviousDay() {
        RaffleService raffleService = new RaffleService(
                raffleRepository, productRepository, storageService,
                auditService, ticketService, userRepository, notificationService
        );
        ReflectionTestUtils.setField(raffleService, "ticketRate", new BigDecimal("0.07"));
        ReflectionTestUtils.setField(raffleService, "minTicketPrice", new BigDecimal("1.00"));

        Product product = Product.builder()
                .id("p-1")
                .name("Laptop")
                .price(new BigDecimal("10000.00"))
                .stock(1)
                .category("Gaming")
                .build();

        when(productRepository.findById("p-1")).thenReturn(Optional.of(product));
        when(raffleRepository.existsByProductIdAndStatusIn(eq("p-1"), anyList())).thenReturn(false);

        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

        assertThatThrownBy(() -> raffleService.create("p-1", yesterday, null, "admin-1", null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("día anterior");
    }

    @Test
    void cancelCheckoutReleasesReservedTicketsToAvailable() {
        RaffleTicketService raffleTicketService = new RaffleTicketService(
                raffleRepository, ticketRepository, paymentService,
                auditService, inAppNotificationService, notificationService, userRepository
        );

        com.semillatecnologica.backend.modules.raffles.model.RaffleTicket ticket =
                com.semillatecnologica.backend.modules.raffles.model.RaffleTicket.builder()
                        .id("t-19")
                        .raffleId("r-1")
                        .ticketNumber(19)
                        .userId("user-1")
                        .paymentIntentId("pi_123")
                        .paymentMethod("CARD")
                        .status(com.semillatecnologica.backend.modules.raffles.model.RaffleTicket.Status.RESERVED)
                        .reservedAt(LocalDateTime.now())
                        .expiresAt(LocalDateTime.now().plusMinutes(10))
                        .build();

        when(ticketRepository.findByPaymentIntentId("pi_123")).thenReturn(List.of(ticket));
        when(userRepository.findByRoleName("admin")).thenReturn(List.of());

        var result = raffleTicketService.cancelCheckout("pi_123", "user-1", null);

        assertThat(result).hasSize(1);
        assertThat(ticket.getStatus())
                .isEqualTo(com.semillatecnologica.backend.modules.raffles.model.RaffleTicket.Status.AVAILABLE);
        assertThat(ticket.getUserId()).isNull();
        assertThat(ticket.getPaymentIntentId()).isNull();
        verify(ticketRepository).saveAll(List.of(ticket));
        verify(paymentService).cancelPayment("pi_123");
    }
}
