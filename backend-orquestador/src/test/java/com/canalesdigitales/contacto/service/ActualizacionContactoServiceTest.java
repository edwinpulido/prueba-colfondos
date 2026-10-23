package com.canalesdigitales.contacto.service;

import com.canalesdigitales.contacto.api.dto.ActualizacionResponse;
import com.canalesdigitales.contacto.api.dto.IniciarActualizacionRequest;
import com.canalesdigitales.contacto.client.CustomerDataClient;
import com.canalesdigitales.contacto.client.IdentityClient;
import com.canalesdigitales.contacto.client.MessagingClient;
import com.canalesdigitales.contacto.client.dto.InicioVerificacionRespuesta;
import com.canalesdigitales.contacto.client.dto.ValidacionVerificacionRespuesta;
import com.canalesdigitales.contacto.domain.EstadoActualizacion;
import com.canalesdigitales.contacto.domain.InMemoryTransaccionRepository;
import com.canalesdigitales.contacto.domain.TransaccionRepository;
import com.canalesdigitales.contacto.exception.CodigoExpiradoException;
import com.canalesdigitales.contacto.exception.CodigoInvalidoException;
import com.canalesdigitales.contacto.exception.ContactoDuplicadoException;
import com.canalesdigitales.contacto.exception.DependenciaNoDisponibleException;
import com.canalesdigitales.contacto.exception.EstadoInvalidoException;
import com.canalesdigitales.contacto.exception.IntentosExcedidosException;
import com.canalesdigitales.contacto.exception.ValidacionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActualizacionContactoServiceTest {

    private TransaccionRepository repositorio;
    private IdentityClient identityClient;
    private CustomerDataClient customerDataClient;
    private MessagingClient messagingClient;
    private ActualizacionContactoService servicio;

    @BeforeEach
    void setUp() {
        repositorio = new InMemoryTransaccionRepository();
        identityClient = mock(IdentityClient.class);
        customerDataClient = mock(CustomerDataClient.class);
        messagingClient = mock(MessagingClient.class);
        servicio = new ActualizacionContactoService(
                repositorio, identityClient, customerDataClient, messagingClient, 3);
    }

    @Test
    @DisplayName("Inicia la transaccion y deja el flujo pendiente de verificacion")
    void iniciaTransaccion() {
        when(identityClient.iniciarVerificacion("AFI-000123", "SMS")).thenReturn(inicioOk());

        ActualizacionResponse respuesta = servicio.iniciar(request("Ana@Dominio.com"));

        assertEquals(EstadoActualizacion.PENDIENTE_VERIFICACION, respuesta.getEstado());
        assertEquals("OTP-202", respuesta.getCodigo());
        assertEquals("SMS", respuesta.getCanal());
        assertEquals(300, respuesta.getExpiraEnSegundos());
    }

    @Test
    @DisplayName("No invoca servicios externos cuando el correo es invalido")
    void noInvocaServiciosConCorreoInvalido() {
        assertThrows(ValidacionException.class, () -> servicio.iniciar(request("ana@")));
        verify(identityClient, never()).iniciarVerificacion(anyString(), any());
    }

    @Test
    @DisplayName("Valida identidad con codigo correcto")
    void validaIdentidad() {
        String id = iniciarFlujo();
        when(identityClient.validarCodigo("REF-1", "123456")).thenReturn(validacion(true));

        ActualizacionResponse respuesta = servicio.verificar(id, "123456");

        assertEquals(EstadoActualizacion.IDENTIDAD_VALIDADA, respuesta.getEstado());
        assertEquals("IDV-200", respuesta.getCodigo());
    }

    @Test
    @DisplayName("Bloquea la transaccion al tercer intento fallido")
    void bloqueaTrasTresIntentos() {
        String id = iniciarFlujo();
        when(identityClient.validarCodigo("REF-1", "000000")).thenReturn(validacion(false));

        assertThrows(CodigoInvalidoException.class, () -> servicio.verificar(id, "000000"));
        assertThrows(CodigoInvalidoException.class, () -> servicio.verificar(id, "000000"));
        assertThrows(IntentosExcedidosException.class, () -> servicio.verificar(id, "000000"));
        assertEquals(EstadoActualizacion.BLOQUEADA, servicio.consultar(id).getEstado());
    }

    @Test
    @DisplayName("Marca la transaccion como expirada cuando el codigo vence")
    void marcaExpirada() {
        String id = iniciarFlujo();
        when(identityClient.validarCodigo(eq("REF-1"), anyString()))
                .thenThrow(new CodigoExpiradoException("expirado"));

        assertThrows(CodigoExpiradoException.class, () -> servicio.verificar(id, "123456"));
        assertEquals(EstadoActualizacion.EXPIRADA, servicio.consultar(id).getEstado());
    }

    @Test
    @DisplayName("Aplica la actualizacion y envia la notificacion")
    void aplicaActualizacion() {
        String id = flujoValidado();

        ActualizacionResponse respuesta = servicio.aplicar(id, "ana@dominio.com", "KEY-1");

        assertEquals(EstadoActualizacion.APLICADA, respuesta.getEstado());
        assertEquals("OK-200", respuesta.getCodigo());
        verify(customerDataClient).actualizarCorreo("AFI-000123", "ana@dominio.com");
        verify(messagingClient).notificarActualizacion("AFI-000123", "ana@dominio.com", id);
    }

    @Test
    @DisplayName("La actualizacion queda aplicada aunque falle la notificacion")
    void aplicaAunqueFalleNotificacion() {
        String id = flujoValidado();
        doThrow(new DependenciaNoDisponibleException("notificaciones"))
                .when(messagingClient).notificarActualizacion(anyString(), anyString(), anyString());

        ActualizacionResponse respuesta = servicio.aplicar(id, "ana@dominio.com", "KEY-2");

        assertEquals(EstadoActualizacion.APLICADA_NOTIFICACION_PENDIENTE, respuesta.getEstado());
        assertEquals("MSG-503", respuesta.getCodigo());
        verify(customerDataClient).actualizarCorreo("AFI-000123", "ana@dominio.com");
    }

    @Test
    @DisplayName("No aplica la actualizacion si el correo ya existe")
    void noAplicaCorreoDuplicado() {
        String id = flujoValidado();
        doThrow(new ContactoDuplicadoException())
                .when(customerDataClient).actualizarCorreo(anyString(), anyString());

        assertThrows(ContactoDuplicadoException.class,
                () -> servicio.aplicar(id, "ana@dominio.com", "KEY-3"));
        assertEquals(EstadoActualizacion.IDENTIDAD_VALIDADA, servicio.consultar(id).getEstado());
        verify(messagingClient, never()).notificarActualizacion(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Reintento con la misma Idempotency-Key no duplica la actualizacion")
    void reintentoIdempotente() {
        String id = flujoValidado();

        servicio.aplicar(id, "ana@dominio.com", "KEY-4");
        ActualizacionResponse segundo = servicio.aplicar(id, "ana@dominio.com", "KEY-4");

        assertEquals(EstadoActualizacion.APLICADA, segundo.getEstado());
        verify(customerDataClient, times(1)).actualizarCorreo(anyString(), anyString());
        verify(messagingClient, times(1)).notificarActualizacion(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("No permite aplicar sin identidad validada")
    void exigeIdentidadValidada() {
        String id = iniciarFlujo();

        assertThrows(EstadoInvalidoException.class,
                () -> servicio.aplicar(id, "ana@dominio.com", "KEY-5"));
        verify(customerDataClient, never()).actualizarCorreo(anyString(), anyString());
    }

    @Test
    @DisplayName("Registra estado fallido si Identity no responde al iniciar")
    void registraFalloAlIniciar() {
        when(identityClient.iniciarVerificacion(anyString(), any()))
                .thenThrow(new DependenciaNoDisponibleException("validacion de identidad"));

        assertThrows(DependenciaNoDisponibleException.class, () -> servicio.iniciar(request("ana@dominio.com")));
    }

    private String iniciarFlujo() {
        when(identityClient.iniciarVerificacion("AFI-000123", "SMS")).thenReturn(inicioOk());
        return servicio.iniciar(request("ana@dominio.com")).getTransactionId();
    }

    private String flujoValidado() {
        String id = iniciarFlujo();
        when(identityClient.validarCodigo("REF-1", "123456")).thenReturn(validacion(true));
        servicio.verificar(id, "123456");
        return id;
    }

    private IniciarActualizacionRequest request(String correo) {
        IniciarActualizacionRequest request = new IniciarActualizacionRequest();
        request.setAfiliadoId("AFI-000123");
        request.setNuevoCorreo(correo);
        request.setCanalPreferido("SMS");
        return request;
    }

    private InicioVerificacionRespuesta inicioOk() {
        InicioVerificacionRespuesta inicio = new InicioVerificacionRespuesta();
        inicio.setReferencia("REF-1");
        inicio.setCanal("SMS");
        inicio.setExpiraEnSegundos(300);
        return inicio;
    }

    private ValidacionVerificacionRespuesta validacion(boolean aprobado) {
        ValidacionVerificacionRespuesta respuesta = new ValidacionVerificacionRespuesta();
        respuesta.setAprobado(aprobado);
        return respuesta;
    }
}
