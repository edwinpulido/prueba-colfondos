package com.canalesdigitales.contacto.exception;

import org.springframework.http.HttpStatus;

public class EstadoInvalidoException extends ErrorNegocio {

    public EstadoInvalidoException(String mensaje) {
        super("CON-409", mensaje, HttpStatus.CONFLICT);
    }
}
