package com.canalesdigitales.contacto.exception;

import org.springframework.http.HttpStatus;

public class TimeoutDependenciaException extends ErrorNegocio {

    public TimeoutDependenciaException(String dependencia, String codigo) {
        super(codigo,
                "La validacion con " + dependencia + " esta tardando mas de lo esperado. Intente nuevamente.",
                HttpStatus.GATEWAY_TIMEOUT);
    }
}
