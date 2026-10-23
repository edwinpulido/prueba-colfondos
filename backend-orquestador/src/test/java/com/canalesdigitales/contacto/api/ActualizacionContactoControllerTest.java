package com.canalesdigitales.contacto.api;

import com.canalesdigitales.contacto.api.dto.ActualizacionResponse;
import com.canalesdigitales.contacto.domain.EstadoActualizacion;
import com.canalesdigitales.contacto.exception.ContactoDuplicadoException;
import com.canalesdigitales.contacto.service.ActualizacionContactoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActualizacionContactoController.class)
class ActualizacionContactoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ActualizacionContactoService servicio;

    @Test
    @DisplayName("POST /v1/contacto/actualizacion responde 202 y propaga correlationId")
    void iniciaFlujo() throws Exception {
        ActualizacionResponse respuesta = new ActualizacionResponse(
                "TX-1", EstadoActualizacion.PENDIENTE_VERIFICACION, "OTP-202",
                "Codigo enviado", "corr-123");
        when(servicio.iniciar(any())).thenReturn(respuesta);

        Map<String, String> cuerpo = new HashMap<>();
        cuerpo.put("afiliadoId", "AFI-000123");
        cuerpo.put("nuevoCorreo", "ana@dominio.com");

        mockMvc.perform(post("/v1/contacto/actualizacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-ID", "corr-123")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isAccepted())
                .andExpect(header().string("X-Correlation-ID", "corr-123"))
                .andExpect(jsonPath("$.transactionId").value("TX-1"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE_VERIFICACION"));
    }

    @Test
    @DisplayName("POST responde 400 cuando el correo viene vacio")
    void validaCuerpo() throws Exception {
        Map<String, String> cuerpo = new HashMap<>();
        cuerpo.put("afiliadoId", "AFI-000123");
        cuerpo.put("nuevoCorreo", "");

        mockMvc.perform(post("/v1/contacto/actualizacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VAL-001"));
    }

    @Test
    @DisplayName("PUT responde 202 cuando la actualizacion queda con notificacion pendiente")
    void aplicaConNotificacionPendiente() throws Exception {
        ActualizacionResponse respuesta = new ActualizacionResponse(
                "TX-1", EstadoActualizacion.APLICADA_NOTIFICACION_PENDIENTE, "MSG-503",
                "Correo actualizado, notificacion en cola", "corr-123");
        when(servicio.aplicar(eq("TX-1"), anyString(), eq("KEY-1"))).thenReturn(respuesta);

        Map<String, String> cuerpo = new HashMap<>();
        cuerpo.put("nuevoCorreo", "ana@dominio.com");

        mockMvc.perform(put("/v1/contacto/actualizacion/TX-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "KEY-1")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.codigo").value("MSG-503"));
    }

    @Test
    @DisplayName("PUT responde 409 cuando el correo ya existe")
    void respondeConflicto() throws Exception {
        when(servicio.aplicar(anyString(), anyString(), any()))
                .thenThrow(new ContactoDuplicadoException());

        Map<String, String> cuerpo = new HashMap<>();
        cuerpo.put("nuevoCorreo", "ana@dominio.com");

        mockMvc.perform(put("/v1/contacto/actualizacion/TX-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CD-409"));
    }
}
