package com.canalesdigitales.contacto.exception;

import org.springframework.http.HttpStatus;

public class CodigoExpiradoException extends ErrorNegocio {

    public CodigoExpiradoException(String mensaje) {
        super("OTP-410", mensaje, HttpStatus.GONE);
    }
}
