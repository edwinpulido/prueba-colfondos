package com.canalesdigitales.contacto.exception;

import org.springframework.http.HttpStatus;

public class TransaccionNoEncontradaException extends ErrorNegocio {

    public TransaccionNoEncontradaException(String transactionId) {
        super("SYS-404", "La transaccion solicitada no existe o expiro.", HttpStatus.NOT_FOUND);
    }
}
