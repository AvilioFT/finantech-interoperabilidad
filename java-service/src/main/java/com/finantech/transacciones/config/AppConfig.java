package com.finantech.transacciones.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${app.mail.host}")
    private String mailHost;

    @Value("${app.mail.port}")
    private int mailPort;

    public String getJwtSecret() { return jwtSecret; }
    public long getJwtExpirationMs() { return jwtExpirationMs; }
    public String getMailHost() { return mailHost; }
    public int getMailPort() { return mailPort; }
}
