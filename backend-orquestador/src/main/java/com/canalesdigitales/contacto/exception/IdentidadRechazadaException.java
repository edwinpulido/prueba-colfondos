package com.canalesdigitales.contacto.exception;

import org.springframework.http.HttpStatus;

public class IdentidadRechazadaException extends ErrorNegocio {

    public IdentidadRechazadaException(String mensaje) {
        super("IDV-401", mensaje, HttpStatus.FORBIDDEN);
    }
}
