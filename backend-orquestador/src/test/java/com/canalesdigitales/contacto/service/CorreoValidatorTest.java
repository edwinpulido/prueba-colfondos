package com.canalesdigitales.contacto.service;

import com.canalesdigitales.contacto.exception.ValidacionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CorreoValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"ana@", "ana@dominio", "@dominio.com", "ana lopez@dominio.com",
            "ana@@dominio.com", "ana..lopez@dominio.com", "ana@dominio..com", "ana@.com"})
    @DisplayName("Rechaza correos con formato invalido")
    void rechazaCorreosInvalidos(String correo) {
        assertThrows(ValidacionException.class, () -> CorreoValidator.normalizar(correo));
    }

    @Test
    @DisplayName("Rechaza correo nulo o vacio")
    void rechazaCorreoVacio() {
        assertThrows(ValidacionException.class, () -> CorreoValidator.normalizar(null));
        assertThrows(ValidacionException.class, () -> CorreoValidator.normalizar("   "));
    }

    @Test
    @DisplayName("Rechaza correos de mas de 120 caracteres")
    void rechazaCorreoLargo() {
        String local = new String(new char[115]).replace('\0', 'a');
        assertThrows(ValidacionException.class,
                () -> CorreoValidator.normalizar(local + "@dominio.com"));
    }

    @Test
    @DisplayName("Normaliza correo valido a minusculas y sin espacios")
    void normalizaCorreoValido() {
        assertEquals("ana.lopez@dominio.com",
                CorreoValidator.normalizar("  Ana.Lopez@Dominio.COM  "));
        assertEquals("ana+prueba@dominio.com.co",
                CorreoValidator.normalizar("ana+prueba@dominio.com.co"));
    }

    @Test
    @DisplayName("Enmascara el correo para trazabilidad")
    void enmascaraCorreo() {
        assertEquals("a***@dominio.com", CorreoValidator.enmascarar("ana@dominio.com"));
        assertEquals("***", CorreoValidator.enmascarar(null));
    }
}
