package com.canalesdigitales.contacto.domain;

import java.time.Instant;

/**
 * Transaccion del flujo de actualizacion de contacto. Guarda solo identificadores
 * tecnicos y resultado: nunca el codigo OTP ni datos biometricos.
 */
public class Transaccion {

    private final String id;
    private final String afiliadoId;
    private String nuevoCorreo;
    private EstadoActualizacion estado;
    private String referenciaIdentidad;
    private int intentosVerificacion;
    private String idempotencyKey;
    private String codigoRespuesta;
    private String mensajeRespuesta;
    private final Instant creadaEn;
    private Instant actualizadaEn;

    public Transaccion(String id, String afiliadoId, String nuevoCorreo) {
        this.id = id;
        this.afiliadoId = afiliadoId;
        this.nuevoCorreo = nuevoCorreo;
        this.estado = EstadoActualizacion.INICIADA;
        this.creadaEn = Instant.now();
        this.actualizadaEn = this.creadaEn;
    }

    public void cambiarEstado(EstadoActualizacion nuevoEstado) {
        this.estado = nuevoEstado;
        this.actualizadaEn = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getAfiliadoId() {
        return afiliadoId;
    }

    public String getNuevoCorreo() {
        return nuevoCorreo;
    }

    public void setNuevoCorreo(String nuevoCorreo) {
        this.nuevoCorreo = nuevoCorreo;
    }

    public EstadoActualizacion getEstado() {
        return estado;
    }

    public String getReferenciaIdentidad() {
        return referenciaIdentidad;
    }

    public void setReferenciaIdentidad(String referenciaIdentidad) {
        this.referenciaIdentidad = referenciaIdentidad;
    }

    public int getIntentosVerificacion() {
        return intentosVerificacion;
    }

    public void incrementarIntentosVerificacion() {
        this.intentosVerificacion++;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getCodigoRespuesta() {
        return codigoRespuesta;
    }

    public void setResultado(String codigoRespuesta, String mensajeRespuesta) {
        this.codigoRespuesta = codigoRespuesta;
        this.mensajeRespuesta = mensajeRespuesta;
        this.actualizadaEn = Instant.now();
    }

    public String getMensajeRespuesta() {
        return mensajeRespuesta;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }

    public Instant getActualizadaEn() {
        return actualizadaEn;
    }
}
