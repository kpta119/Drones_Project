package com.example.drones.orders;

import com.example.drones.user.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${app.frontend_url}")
    private String frontendUrl;

    public void sendNewOrderNotification(UserEntity operator, OrdersEntity order) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(operator.getEmail());
            message.setSubject("Nowe zlecenie: " + order.getService().getName());
            String googleMapsUrl = buildGoogleMapsUrl(order.getCoordinates());

            message.setText(String.format(
                    "Witaj %s,%n%nDostępne jest nowe zlecenie:%n" +
                            "Tytuł zlecenia: %s%n" +
                            "Opis zlecenia: %s%n" +
                            "Usługa: %s%n" +
                            "Lokalizacja: %s%n%n" +
                            "Zaloguj się, aby sprawdzić szczegóły: %s",
                    operator.getName(),
                    order.getTitle(),
                    order.getDescription(),
                    order.getService().getName(),
                    googleMapsUrl,
                    frontendUrl
            ));

            mailSender.send(message);
            log.info("New Order email sent to operator {} for order {}", operator.getId(), order.getId());
        } catch (Exception e) {
            log.error("Failed to send email to operator {}", operator.getId(), e);
        }
    }

    private String buildGoogleMapsUrl(String coordinates) {
        String cleanCoords = coordinates.trim().replace(" ", "");
        return "https://www.google.com/maps/search/?api=1&query=" + cleanCoords;
    }

    @Async
    public void sendOrderAcceptedByOperatorNotification(UserEntity client, OrdersEntity order) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(client.getEmail());
            message.setSubject("Operator zaakceptował Twoje zlecenie");
            message.setText(String.format(
                    "Witaj %s,%n%nOperator zaakceptował Twoje zlecenie:%n" +
                            "Tytuł zlecenia: %s%n" +
                            "Opis zlecenia: %s%n" +
                            "Usługa: %s%n%n" +
                            "Zaloguj się, aby sprawdzić szczegóły: %s",
                    client.getName(),
                    order.getTitle(),
                    order.getDescription(),
                    order.getService().getName(),
                    frontendUrl
            ));

            mailSender.send(message);
            log.info("Order accepted by operator email sent to client {} for order {}", client.getId(), order.getId());
        } catch (Exception e) {
            log.error("Failed to send email to client {}", client.getId(), e);
        }

    }

    @Async
    public void sendOperatorAcceptedByClientNotification(UserEntity operator, OrdersEntity order) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(operator.getEmail());
            message.setSubject("Zostałeś zaakceptowany przez klienta do nowego zlecenia");
            String googleMapsUrl = buildGoogleMapsUrl(order.getCoordinates());
            message.setText(String.format(
                    "Witaj %s,%n%nKlient zaakceptował Cię do zlecenia:%n" +
                            "Tytuł zlecenia: %s%n" +
                            "Opis zlecenia: %s%n" +
                            "Usługa: %s%n%n" +
                            "Lokalizacja: %s%n%n" +
                            "Zaloguj się, aby sprawdzić szczegóły: %s",
                    operator.getName(),
                    order.getTitle(),
                    order.getDescription(),
                    order.getService().getName(),
                    googleMapsUrl,
                    frontendUrl
            ));

            mailSender.send(message);
            log.info("Operator accepted by client email sent to operator {} for order {}", operator.getId(), order.getId());
        } catch (Exception e) {
            log.error("Failed to send email to operator {}", operator.getId(), e);
        }

    }
}
