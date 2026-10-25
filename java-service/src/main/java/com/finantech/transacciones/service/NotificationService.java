package com.finantech.transacciones.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void notificarExito(String destinatario, String asunto, String mensaje) {
        enviar("[FINANTECH] " + asunto, destinatario, mensaje);
    }

    @Async
    public void notificarFallido(String destinatario, String asunto, String mensaje) {
        enviar("[ALERTA FINANTECH] " + asunto, destinatario, mensaje);
    }

    private void enviar(String asunto, String destinatario, String mensaje) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(destinatario);
            msg.setSubject(asunto);
            msg.setText(mensaje);
            mailSender.send(msg);
            log.info("Correo enviado: {}", asunto);
        } catch (Exception e) {
            log.error("No se pudo enviar el correo: {}", e.getMessage());
        }
    }
}
