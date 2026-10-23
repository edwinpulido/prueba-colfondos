package com.canalesdigitales.contacto.exception;

import org.springframework.http.HttpStatus;

public class ValidacionException extends ErrorNegocio {

    public ValidacionException(String mensaje) {
        super("VAL-001", mensaje, HttpStatus.BAD_REQUEST);
    }
}
