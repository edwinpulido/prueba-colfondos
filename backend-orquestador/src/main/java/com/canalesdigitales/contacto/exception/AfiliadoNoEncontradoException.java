package com.canalesdigitales.contacto.exception;

import org.springframework.http.HttpStatus;

public class AfiliadoNoEncontradoException extends ErrorNegocio {

    public AfiliadoNoEncontradoException() {
        super("AFI-404", "No fue posible identificar el afiliado. Verifique los datos ingresados.",
                HttpStatus.NOT_FOUND);
    }
}
