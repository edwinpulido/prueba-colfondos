package com.canalesdigitales.contacto.exception;

import org.springframework.http.HttpStatus;

public class ContactoDuplicadoException extends ErrorNegocio {

    public ContactoDuplicadoException() {
        super("CD-409",
                "No fue posible completar la actualizacion. Verifique los datos e intente nuevamente.",
                HttpStatus.CONFLICT);
    }
}
