package com.example.drones.emailService;

import com.example.drones.orders.EmailService;
import com.example.drones.orders.OrdersEntity;
import com.example.drones.orders.OrderStatus;
import com.example.drones.services.ServicesEntity;
import com.example.drones.user.UserEntity;
import com.example.drones.user.UserRole;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = EmailServiceTests.TestConfig.class)
public class EmailServiceTests {

    @Configuration
    @EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
    })
    static class TestConfig {
        @Bean
        public JavaMailSender javaMailSender() {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost("localhost");
            mailSender.setPort(3025);
            return mailSender;
        }

        @Bean
        public EmailService emailService(JavaMailSender mailSender) {
            return new EmailService(mailSender);
        }
    }

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("test", "test"))
            .withPerMethodLifecycle(true);

    @Autowired
    private EmailService emailService;

    private UserEntity testOperator1;
    private UserEntity testOperator2;
    private OrdersEntity testOrder;
    private ServicesEntity testService;

    @BeforeEach
    void setUp() {

        testService = new ServicesEntity();
        testService.setName("Laser Scanning");

        testOperator1 = UserEntity.builder()
                .id(UUID.randomUUID())
                .displayName("operator1")
                .email("operator1@test.com")
                .name("Jan")
                .surname("Kowalski")
                .role(UserRole.OPERATOR)
                .coordinates("52.2297, 21.0122")
                .radius(50)
                .build();

        testOperator2 = UserEntity.builder()
                .id(UUID.randomUUID())
                .displayName("operator2")
                .email("operator2@test.com")
                .name("Anna")
                .surname("Nowak")
                .role(UserRole.OPERATOR)
                .coordinates("52.2200, 21.0100")
                .radius(30)
                .build();

        testOrder = OrdersEntity.builder()
                .id(UUID.randomUUID())
                .title("Test Order")
                .description("Test description")
                .service(testService)
                .coordinates("52.2297, 21.0122")
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .status(OrderStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void givenOperator_whenSendNewOrderNotification_thenEmailIsSent() throws MessagingException, IOException {
        // When
        emailService.sendNewOrderNotification(testOperator1, testOrder);

        // Then
        assertThat(greenMail.getReceivedMessages()).hasSize(1);

        MimeMessage receivedMessage = greenMail.getReceivedMessages()[0];
        assertThat(receivedMessage.getAllRecipients()[0].toString()).isEqualTo("operator1@test.com");
        assertThat(receivedMessage.getSubject()).isEqualTo("Nowe zlecenie: Laser Scanning");
        assertThat(receivedMessage.getContent().toString()).contains("Witaj Jan");
        assertThat(receivedMessage.getContent().toString()).contains("Laser Scanning");
        assertThat(receivedMessage.getContent().toString()).contains("Test description");
    }

    @Test
    void givenOperator_whenSendEmail_thenEmailContainsGoogleMapsLink() throws MessagingException, IOException {
        // When
        emailService.sendNewOrderNotification(testOperator1, testOrder);

        // Then
        MimeMessage receivedMessage = greenMail.getReceivedMessages()[0];
        String emailContent = receivedMessage.getContent().toString();

        assertThat(emailContent).contains("https://www.google.com/maps/search/?api=1&query=52.2297,21.0122");
    }

    @Test
    void givenMultipleOperators_whenSendEmailsToAll_thenAllReceiveNotifications() throws MessagingException {
        // When
        emailService.sendNewOrderNotification(testOperator1, testOrder);
        emailService.sendNewOrderNotification(testOperator2, testOrder);

        // Then
        assertThat(greenMail.getReceivedMessages()).hasSize(2);

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages[0].getAllRecipients()[0].toString()).isEqualTo("operator1@test.com");
        assertThat(messages[1].getAllRecipients()[0].toString()).isEqualTo("operator2@test.com");
    }

    @Test
    void givenOperatorWithDifferentName_whenSendEmail_thenEmailContainsCorrectName() throws MessagingException, IOException {
        // When
        emailService.sendNewOrderNotification(testOperator2, testOrder);

        // Then
        MimeMessage receivedMessage = greenMail.getReceivedMessages()[0];
        String emailContent = receivedMessage.getContent().toString();

        assertThat(emailContent).contains("Witaj Anna");
        assertThat(receivedMessage.getAllRecipients()[0].toString()).isEqualTo("operator2@test.com");
    }

    @Test
    void givenInvalidEmail_whenSendEmail_thenNoExceptionIsThrown() {
        // Given
        UserEntity operatorWithInvalidEmail = UserEntity.builder()
                .id(UUID.randomUUID())
                .displayName("invalid_operator")
                .email("invalid-email")
                .name("Invalid")
                .surname("Operator")
                .role(UserRole.OPERATOR)
                .build();

        // When & Then - nie powinno rzucić wyjątku dzięki try-catch w EmailService
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
            emailService.sendNewOrderNotification(operatorWithInvalidEmail, testOrder)
        );
    }

    @Test
    void givenOrderWithDifferentService_whenSendEmail_thenSubjectContainsServiceName() throws MessagingException {
        // Given
        ServicesEntity differentService = new ServicesEntity();
        differentService.setName("Fotografia/Wideo");

        OrdersEntity orderWithDifferentService = OrdersEntity.builder()
                .id(UUID.randomUUID())
                .title("Photo Order")
                .service(differentService)
                .coordinates("52.2297, 21.0122")
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .status(OrderStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        // When
        emailService.sendNewOrderNotification(testOperator1, orderWithDifferentService);

        // Then
        MimeMessage receivedMessage = greenMail.getReceivedMessages()[0];
        assertThat(receivedMessage.getSubject()).isEqualTo("Nowe zlecenie: Fotografia/Wideo");
    }

    @Test
    void givenOrderWithSpacesInCoordinates_whenSendEmail_thenGoogleMapsUrlIsFormatted() throws MessagingException, IOException {
        // Given
        OrdersEntity orderWithSpaces = OrdersEntity.builder()
                .id(UUID.randomUUID())
                .title("Test Order")
                .service(testService)
                .coordinates("52.2297,   21.0122")  // Spacje w koordynatach
                .fromDate(LocalDateTime.now().plusDays(1))
                .toDate(LocalDateTime.now().plusDays(2))
                .status(OrderStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        // When
        emailService.sendNewOrderNotification(testOperator1, orderWithSpaces);

        // Then
        MimeMessage receivedMessage = greenMail.getReceivedMessages()[0];
        String emailContent = receivedMessage.getContent().toString();

        // URL powinien być bez spacji
        assertThat(emailContent).contains("https://www.google.com/maps/search/?api=1&query=52.2297,21.0122");
        assertThat(emailContent).doesNotContain("query=52.2297,   21.0122");
    }
}
