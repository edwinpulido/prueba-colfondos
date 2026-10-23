package com.canalesdigitales.contacto.client;

import com.canalesdigitales.contacto.client.dto.InicioVerificacionRespuesta;
import com.canalesdigitales.contacto.client.dto.ValidacionVerificacionRespuesta;
import com.canalesdigitales.contacto.exception.CodigoExpiradoException;
import com.canalesdigitales.contacto.exception.CodigoInvalidoException;
import com.canalesdigitales.contacto.exception.DependenciaNoDisponibleException;
import com.canalesdigitales.contacto.exception.IdentidadRechazadaException;
import com.canalesdigitales.contacto.exception.IntentosExcedidosException;
import com.canalesdigitales.contacto.exception.TimeoutDependenciaException;
import com.canalesdigitales.contacto.config.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adaptador hacia Identity Capability / Identity Orchestration.
 * Traduce las fallas tecnicas a codigos de negocio del catalogo de errores.
 */
@Component
public class IdentityClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdentityClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String pathInicio;
    private final String pathValidacion;

    public IdentityClient(
            @Qualifier("identityRestTemplate") RestTemplate restTemplate,
            @Value("${contacto.integracion.identity.base-url}") String baseUrl,
            @Value("${contacto.integracion.identity.path-inicio}") String pathInicio,
            @Value("${contacto.integracion.identity.path-validacion}") String pathValidacion) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.pathInicio = pathInicio;
        this.pathValidacion = pathValidacion;
    }

    public InicioVerificacionRespuesta iniciarVerificacion(String afiliadoId, String canalPreferido) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("afiliadoId", afiliadoId);
        if (canalPreferido != null && !canalPreferido.trim().isEmpty()) {
            cuerpo.put("canalPreferido", canalPreferido);
        }
        try {
            ResponseEntity<InicioVerificacionRespuesta> respuesta = restTemplate.exchange(
                    baseUrl + pathInicio, HttpMethod.POST, new HttpEntity<>(cuerpo, cabeceras()),
                    InicioVerificacionRespuesta.class);
            return respuesta.getBody();
        } catch (HttpClientErrorException ex) {
            LOGGER.warn("Identity rechazo el inicio de verificacion. estado={}", ex.getRawStatusCode());
            throw new IdentidadRechazadaException(
                    "No fue posible validar su identidad con los datos registrados. Puede acercarse a una oficina.");
        } catch (ResourceAccessException ex) {
            LOGGER.error("Timeout o error de red invocando Identity: {}", ex.getMessage());
            throw new TimeoutDependenciaException("la validacion de identidad", "IDV-408");
        } catch (HttpServerErrorException ex) {
            LOGGER.error("Identity no disponible. estado={}", ex.getRawStatusCode());
            throw new DependenciaNoDisponibleException("validacion de identidad");
        }
    }

    public ValidacionVerificacionRespuesta validarCodigo(String referencia, String codigo) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("referencia", referencia);
        cuerpo.put("codigo", codigo);
        try {
            ResponseEntity<ValidacionVerificacionRespuesta> respuesta = restTemplate.exchange(
                    baseUrl + pathValidacion, HttpMethod.POST, new HttpEntity<>(cuerpo, cabeceras()),
                    ValidacionVerificacionRespuesta.class);
            return respuesta.getBody();
        } catch (HttpClientErrorException ex) {
            throw traducirRechazo(ex);
        } catch (ResourceAccessException ex) {
            LOGGER.error("Timeout o error de red validando codigo: {}", ex.getMessage());
            throw new TimeoutDependenciaException("la validacion de identidad", "IDV-408");
        } catch (HttpServerErrorException ex) {
            LOGGER.error("Identity no disponible en validacion. estado={}", ex.getRawStatusCode());
            throw new DependenciaNoDisponibleException("validacion de identidad");
        }
    }

    private RuntimeException traducirRechazo(HttpClientErrorException ex) {
        int estado = ex.getRawStatusCode();
        if (estado == 429) {
            return new IntentosExcedidosException(
                    "Se supero el numero de intentos permitidos. Solicite un nuevo codigo.");
        }
        String cuerpo = ex.getResponseBodyAsString();
        if (cuerpo != null && (cuerpo.contains("OTP-EXPIRADO") || cuerpo.contains("EXPIRADO"))) {
            return new CodigoExpiradoException(
                    "El codigo de verificacion expiro. Solicite un nuevo codigo.");
        }
        return new CodigoInvalidoException("El codigo de verificacion no es correcto.");
    }

    private HttpHeaders cabeceras() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(CorrelationIdFilter.HEADER, correlationActual());
        return headers;
    }

    private String correlationActual() {
        String correlation = org.slf4j.MDC.get(CorrelationIdFilter.MDC_KEY);
        return correlation != null ? correlation : "N/A";
    }
}
