package com.canalesdigitales.contacto.exception;

import org.springframework.http.HttpStatus;

public class IntentosExcedidosException extends ErrorNegocio {

    public IntentosExcedidosException(String mensaje) {
        super("IDV-429", mensaje, HttpStatus.TOO_MANY_REQUESTS);
    }
}
