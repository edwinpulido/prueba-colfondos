package com.canalesdigitales.contacto.api.dto;

import com.canalesdigitales.contacto.domain.EstadoActualizacion;

public class ActualizacionResponse {

    private String transactionId;
    private EstadoActualizacion estado;
    private String codigo;
    private String mensaje;
    private String canal;
    private Integer expiraEnSegundos;
    private String correlationId;

    public ActualizacionResponse() {
    }

    public ActualizacionResponse(String transactionId, EstadoActualizacion estado, String codigo,
                                 String mensaje, String correlationId) {
        this.transactionId = transactionId;
        this.estado = estado;
        this.codigo = codigo;
        this.mensaje = mensaje;
        this.correlationId = correlationId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public EstadoActualizacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoActualizacion estado) {
        this.estado = estado;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
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

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}
