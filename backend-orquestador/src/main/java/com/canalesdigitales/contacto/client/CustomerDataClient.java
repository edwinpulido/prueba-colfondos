package com.canalesdigitales.contacto.client;

import com.canalesdigitales.contacto.config.CorrelationIdFilter;
import com.canalesdigitales.contacto.exception.AfiliadoNoEncontradoException;
import com.canalesdigitales.contacto.exception.ContactoDuplicadoException;
import com.canalesdigitales.contacto.exception.DependenciaNoDisponibleException;
import com.canalesdigitales.contacto.exception.TimeoutDependenciaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

/**
 * Adaptador hacia Customer Data (a traves de las interfaces gobernadas de IntegrationHub).
 */
@Component
public class CustomerDataClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerDataClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String pathContacto;

    public CustomerDataClient(
            @Qualifier("coreRestTemplate") RestTemplate restTemplate,
            @Value("${contacto.integracion.customer-data.base-url}") String baseUrl,
            @Value("${contacto.integracion.customer-data.path-contacto}") String pathContacto) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.pathContacto = pathContacto;
    }

    public void actualizarCorreo(String afiliadoId, String nuevoCorreo) {
        String url = baseUrl + String.format(pathContacto, afiliadoId);
        try {
            restTemplate.exchange(url, HttpMethod.PUT,
                    new HttpEntity<>(Collections.singletonMap("correo", nuevoCorreo), cabeceras()), Void.class);
            LOGGER.info("Correo actualizado para afiliado={}", afiliadoId);
        } catch (HttpClientErrorException ex) {
            int estado = ex.getRawStatusCode();
            if (estado == 409) {
                LOGGER.warn("Customer Data reporta correo duplicado para afiliado={}", afiliadoId);
                throw new ContactoDuplicadoException();
            }
            if (estado == 404) {
                throw new AfiliadoNoEncontradoException();
            }
            LOGGER.error("Customer Data rechazo la actualizacion. estado={} detalle={}",
                    estado, ex.getResponseBodyAsString());
            throw new DependenciaNoDisponibleException("actualizacion de datos");
        } catch (ResourceAccessException ex) {
            LOGGER.error("Timeout o error de red actualizando contacto: {}", ex.getMessage());
            throw new TimeoutDependenciaException("la actualizacion de datos", "SYS-408");
        } catch (HttpServerErrorException ex) {
            LOGGER.error("Customer Data no disponible. estado={}", ex.getRawStatusCode());
            throw new DependenciaNoDisponibleException("actualizacion de datos");
        }
    }

    private HttpHeaders cabeceras() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String correlation = org.slf4j.MDC.get(CorrelationIdFilter.MDC_KEY);
        headers.set(CorrelationIdFilter.HEADER, correlation != null ? correlation : "N/A");
        return headers;
    }
}
