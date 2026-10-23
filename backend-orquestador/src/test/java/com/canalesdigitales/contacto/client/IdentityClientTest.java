package com.canalesdigitales.contacto.client;

import com.canalesdigitales.contacto.exception.CodigoExpiradoException;
import com.canalesdigitales.contacto.exception.DependenciaNoDisponibleException;
import com.canalesdigitales.contacto.exception.IdentidadRechazadaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class IdentityClientTest {

    private MockRestServiceServer servidor;
    private IdentityClient cliente;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        servidor = MockRestServiceServer.bindTo(restTemplate).build();
        cliente = new IdentityClient(restTemplate, "http://localhost:9091",
                "/v1/identity/verificacion", "/v1/identity/verificacion/validacion");
    }

    @Test
    @DisplayName("Inicia la verificacion y retorna la referencia")
    void iniciaVerificacion() {
        servidor.expect(requestTo("http://localhost:9091/v1/identity/verificacion"))
                .andRespond(withSuccess(
                        "{\"referencia\":\"REF-1\",\"canal\":\"SMS\",\"expiraEnSegundos\":300}",
                        MediaType.APPLICATION_JSON));

        assertEquals("REF-1", cliente.iniciarVerificacion("AFI-000123", "SMS").getReferencia());
    }

    @Test
    @DisplayName("Traduce el rechazo de identidad a IDV-401")
    void traduceRechazo() {
        servidor.expect(requestTo("http://localhost:9091/v1/identity/verificacion"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(IdentidadRechazadaException.class,
                () -> cliente.iniciarVerificacion("AFI-000123", "SMS"));
    }

    @Test
    @DisplayName("Traduce codigo expirado a OTP-410")
    void traduceCodigoExpirado() {
        servidor.expect(requestTo("http://localhost:9091/v1/identity/verificacion/validacion"))
                .andRespond(withStatus(HttpStatus.GONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"codigo\":\"OTP-EXPIRADO\"}"));

        assertThrows(CodigoExpiradoException.class,
                () -> cliente.validarCodigo("REF-1", "123456"));
    }

    @Test
    @DisplayName("Traduce caida del proveedor a SYS-503")
    void traduceCaidaProveedor() {
        servidor.expect(requestTo("http://localhost:9091/v1/identity/verificacion/validacion"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(DependenciaNoDisponibleException.class,
                () -> cliente.validarCodigo("REF-1", "123456"));
    }
}
