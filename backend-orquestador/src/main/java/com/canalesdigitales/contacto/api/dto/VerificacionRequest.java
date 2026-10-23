package com.canalesdigitales.contacto.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class VerificacionRequest {

    @NotBlank(message = "El codigo de verificacion es obligatorio")
    @Pattern(regexp = "^[0-9]{6}$", message = "El codigo de verificacion debe tener 6 digitos")
    private String codigo;

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
}
