package com.canalesdigitales.contacto.client;

import com.canalesdigitales.contacto.config.CorrelationIdFilter;
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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adaptador hacia Messaging. La notificacion no es critica para el cambio:
 * si falla, la actualizacion queda aplicada y la notificacion se encola.
 */
@Component
public class MessagingClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessagingClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String pathNotificacion;

    public MessagingClient(
            @Qualifier("coreRestTemplate") RestTemplate restTemplate,
            @Value("${contacto.integracion.messaging.base-url}") String baseUrl,
            @Value("${contacto.integracion.messaging.path-notificacion}") String pathNotificacion) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.pathNotificacion = pathNotificacion;
    }

    public void notificarActualizacion(String afiliadoId, String correo, String transactionId) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("afiliadoId", afiliadoId);
        cuerpo.put("plantilla", "ACTUALIZACION_CORREO");
        cuerpo.put("canal", "EMAIL");
        cuerpo.put("destino", correo);
        cuerpo.put("referencia", transactionId);

        try {
            restTemplate.exchange(baseUrl + pathNotificacion, HttpMethod.POST,
                    new HttpEntity<>(cuerpo, cabeceras()), Void.class);
            LOGGER.info("Notificacion enviada para transactionId={}", transactionId);
        } catch (HttpClientErrorException ex) {
            LOGGER.warn("Messaging rechazo la notificacion. estado={}", ex.getRawStatusCode());
            throw new DependenciaNoDisponibleException("notificaciones");
        } catch (ResourceAccessException ex) {
            LOGGER.warn("Timeout enviando notificacion: {}", ex.getMessage());
            throw new TimeoutDependenciaException("la notificacion", "MSG-408");
        } catch (HttpServerErrorException ex) {
            LOGGER.warn("Messaging no disponible. estado={}", ex.getRawStatusCode());
            throw new DependenciaNoDisponibleException("notificaciones");
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
