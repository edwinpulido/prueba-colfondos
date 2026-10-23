package com.canalesdigitales.contacto.service;

import com.canalesdigitales.contacto.exception.ValidacionException;

import java.util.regex.Pattern;

/**
 * Validacion de correo electronico aplicada en el borde del API (antes de invocar
 * cualquier servicio externo). El portal aplica la misma regla para dar
 * retroalimentacion inmediata al afiliado.
 */
public final class CorreoValidator {

    private static final Pattern FORMATO = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private CorreoValidator() {
    }

    public static String normalizar(String correo) {
        if (correo == null) {
            throw new ValidacionException("El correo electronico es obligatorio.");
        }
        String limpio = correo.trim().toLowerCase();
        if (limpio.isEmpty()) {
            throw new ValidacionException("El correo electronico es obligatorio.");
        }
        if (limpio.length() > 120) {
            throw new ValidacionException("El correo electronico no puede superar 120 caracteres.");
        }
        if (limpio.contains(" ")) {
            throw new ValidacionException("El correo electronico no puede contener espacios.");
        }
        if (limpio.contains("..")) {
            throw new ValidacionException("El correo electronico no es valido.");
        }
        if (!FORMATO.matcher(limpio).matches()) {
            throw new ValidacionException("Ingrese un correo electronico valido. Ejemplo: nombre@dominio.com");
        }
        return limpio;
    }

    /** Enmascara el correo para logs y trazabilidad: ana.lopez@mail.com -> a***@mail.com */
    public static String enmascarar(String correo) {
        if (correo == null || !correo.contains("@")) {
            return "***";
        }
        int arroba = correo.indexOf('@');
        String inicial = correo.substring(0, 1);
        return inicial + "***" + correo.substring(arroba);
    }
}
