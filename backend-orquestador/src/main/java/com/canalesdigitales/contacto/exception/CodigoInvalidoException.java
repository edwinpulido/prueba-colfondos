package com.canalesdigitales.contacto.exception;

import org.springframework.http.HttpStatus;

public class CodigoInvalidoException extends ErrorNegocio {

    public CodigoInvalidoException(String mensaje) {
        super("OTP-401", mensaje, HttpStatus.UNAUTHORIZED);
    }
}
