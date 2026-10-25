package com.finantech.transacciones.dto;

import java.time.Instant;

public class ErrorResponse {
    private Instant timestamp;
    private int status;
    private String codigo;
    private String mensaje;
    private String path;
    private String traceId;

    public ErrorResponse() {
    }

    public ErrorResponse(Instant timestamp, int status, String codigo, String mensaje, String path, String traceId) {
        this.timestamp = timestamp;
        this.status = status;
        this.codigo = codigo;
        this.mensaje = mensaje;
        this.path = path;
        this.traceId = traceId;
    }

    public static ErrorResponse of(int status, String codigo, String mensaje, String path, String traceId) {
        return new ErrorResponse(Instant.now(), status, codigo, mensaje, path, traceId);
    }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
}
