package com.canalesdigitales.contacto.client.dto;

public class InicioVerificacionRespuesta {

    private String referencia;
    private String canal;
    private Integer expiraEnSegundos;

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getCanal() {
        return canal;
    }

    public void setCanal(String canal) {
        this.canal = canal;
    }

    public Integer getExpiraEnSegundos() {
        return expiraEnSegundos;
    }

    public void setExpiraEnSegundos(Integer expiraEnSegundos) {
        this.expiraEnSegundos = expiraEnSegundos;
    }
}
