package com.example.drones.orders;

import com.example.drones.user.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendNewOrderNotification(UserEntity operator, OrdersEntity order) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(operator.getEmail());
            message.setSubject("Nowe zlecenie: " + order.getService().getName());
            String googleMapsUrl = buildGoogleMapsUrl(order.getCoordinates());

            message.setText(String.format(
                    "Witaj %s,%n%nDostępne jest nowe zlecenie:%n" +
                            "Usługa: %s%n" +
                            "Lokalizacja: %s%n%n" +
                            "Zobacz na mapie: %s%n%n" +
                            "Zaloguj się, aby sprawdzić szczegóły.",
                    operator.getName(),
                    order.getService().getName(),
                    order.getCoordinates(),
                    googleMapsUrl
            ));

            mailSender.send(message);
            log.info("Email sent to operator {} for order {}", operator.getId(), order.getId());
        } catch (Exception e) {
            log.error("Failed to send email to operator {}", operator.getId(), e);
        }
    }

    private String buildGoogleMapsUrl(String coordinates) {
        String cleanCoords = coordinates.trim().replace(" ", "");
        return "https://www.google.com/maps/search/?api=1&query=" + cleanCoords;
    }
}
