package com.canalesdigitales.contacto.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class ActualizarContactoRequest {

    @NotBlank(message = "El correo electronico es obligatorio")
    @Size(max = 120, message = "El correo electronico no puede superar 120 caracteres")
    private String nuevoCorreo;

    public String getNuevoCorreo() {
        return nuevoCorreo;
    }

    public void setNuevoCorreo(String nuevoCorreo) {
        this.nuevoCorreo = nuevoCorreo;
    }
}
