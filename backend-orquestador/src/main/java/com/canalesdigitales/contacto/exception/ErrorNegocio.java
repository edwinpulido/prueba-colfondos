package com.canalesdigitales.contacto.exception;

import org.springframework.http.HttpStatus;

/**
 * Error controlado del dominio. El codigo es el contrato que comparten
 * portal, backend y QA (ver docs/02_Diseno_Tecnico.md, catalogo de errores).
 */
public class ErrorNegocio extends RuntimeException {

    private final String codigo;
    private final HttpStatus estadoHttp;
    private String transactionId;

    public ErrorNegocio(String codigo, String mensaje, HttpStatus estadoHttp) {
        super(mensaje);
        this.codigo = codigo;
        this.estadoHttp = estadoHttp;
    }

    public String getCodigo() {
        return codigo;
    }

    public HttpStatus getEstadoHttp() {
        return estadoHttp;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public ErrorNegocio conTransaccion(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }
}
