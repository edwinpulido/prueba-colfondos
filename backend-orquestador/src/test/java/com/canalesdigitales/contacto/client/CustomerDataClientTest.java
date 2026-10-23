package com.canalesdigitales.contacto.client;

import com.canalesdigitales.contacto.exception.ContactoDuplicadoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.springframework.http.HttpStatus;

class CustomerDataClientTest {

    private MockRestServiceServer servidor;
    private CustomerDataClient cliente;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        servidor = MockRestServiceServer.bindTo(restTemplate).build();
        cliente = new CustomerDataClient(restTemplate, "http://localhost:9092",
                "/v1/afiliados/%s/contacto");
    }

    @Test
    @DisplayName("Actualiza el contacto usando la interfaz gobernada")
    void actualizaContacto() {
        servidor.expect(requestTo("http://localhost:9092/v1/afiliados/AFI-000123/contacto"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        cliente.actualizarCorreo("AFI-000123", "ana@dominio.com");
        servidor.verify();
    }

    @Test
    @DisplayName("Traduce el 409 de Customer Data a correo duplicado")
    void traduceConflicto() {
        servidor.expect(requestTo("http://localhost:9092/v1/afiliados/AFI-000123/contacto"))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThrows(ContactoDuplicadoException.class,
                () -> cliente.actualizarCorreo("AFI-000123", "ana@dominio.com"));
    }
}
