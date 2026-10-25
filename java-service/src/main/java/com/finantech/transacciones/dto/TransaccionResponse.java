package com.finantech.transacciones.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class TransaccionResponse {
    private String idTransaccion;
    private String cuentaOrigen;
    private String cuentaDestino;
    private BigDecimal monto;
    private String moneda;
    private String estado;
    private Instant fechaHora;
    private String mensaje;

    public TransaccionResponse() {
    }

    public TransaccionResponse(String idTransaccion, String cuentaOrigen, String cuentaDestino, BigDecimal monto,
                               String moneda, String estado, Instant fechaHora, String mensaje) {
        this.idTransaccion = idTransaccion;
        this.cuentaOrigen = cuentaOrigen;
        this.cuentaDestino = cuentaDestino;
        this.monto = monto;
        this.moneda = moneda;
        this.estado = estado;
        this.fechaHora = fechaHora;
        this.mensaje = mensaje;
    }

    public String getIdTransaccion() { return idTransaccion; }
    public void setIdTransaccion(String idTransaccion) { this.idTransaccion = idTransaccion; }
    public String getCuentaOrigen() { return cuentaOrigen; }
    public void setCuentaOrigen(String cuentaOrigen) { this.cuentaOrigen = cuentaOrigen; }
    public String getCuentaDestino() { return cuentaDestino; }
    public void setCuentaDestino(String cuentaDestino) { this.cuentaDestino = cuentaDestino; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Instant getFechaHora() { return fechaHora; }
    public void setFechaHora(Instant fechaHora) { this.fechaHora = fechaHora; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}
