package com.canalesdigitales.contacto.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class IniciarActualizacionRequest {

    @NotBlank(message = "El identificador del afiliado es obligatorio")
    private String afiliadoId;

    @NotBlank(message = "El correo electronico es obligatorio")
    @Size(max = 120, message = "El correo electronico no puede superar 120 caracteres")
    private String nuevoCorreo;

    private String canalPreferido;

    public String getAfiliadoId() {
        return afiliadoId;
    }

    public void setAfiliadoId(String afiliadoId) {
        this.afiliadoId = afiliadoId;
    }

    public String getNuevoCorreo() {
        return nuevoCorreo;
    }

    public void setNuevoCorreo(String nuevoCorreo) {
        this.nuevoCorreo = nuevoCorreo;
    }

    public String getCanalPreferido() {
        return canalPreferido;
    }

    public void setCanalPreferido(String canalPreferido) {
        this.canalPreferido = canalPreferido;
    }
}
