package com.canalesdigitales.contacto.exception;

import org.springframework.http.HttpStatus;

public class DependenciaNoDisponibleException extends ErrorNegocio {

    public DependenciaNoDisponibleException(String dependencia) {
        super("SYS-503",
                "El servicio de " + dependencia + " no se encuentra disponible. Intente nuevamente en unos minutos.",
                HttpStatus.SERVICE_UNAVAILABLE);
    }
}
