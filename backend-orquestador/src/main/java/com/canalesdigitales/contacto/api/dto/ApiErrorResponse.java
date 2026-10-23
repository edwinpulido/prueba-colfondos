package com.canalesdigitales.contacto.api.dto;

import java.time.Instant;

public class ApiErrorResponse {

    private final String codigo;
    private final String mensaje;
    private final String transactionId;
    private final String correlationId;
    private final Instant timestamp;

    public ApiErrorResponse(String codigo, String mensaje, String transactionId, String correlationId) {
        this.codigo = codigo;
        this.mensaje = mensaje;
        this.transactionId = transactionId;
        this.correlationId = correlationId;
        this.timestamp = Instant.now();
    }

    public String getCodigo() {
        return codigo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
