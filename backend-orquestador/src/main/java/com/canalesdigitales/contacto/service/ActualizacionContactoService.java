package com.canalesdigitales.contacto.service;

import com.canalesdigitales.contacto.api.dto.ActualizacionResponse;
import com.canalesdigitales.contacto.api.dto.IniciarActualizacionRequest;
import com.canalesdigitales.contacto.client.CustomerDataClient;
import com.canalesdigitales.contacto.client.IdentityClient;
import com.canalesdigitales.contacto.client.MessagingClient;
import com.canalesdigitales.contacto.client.dto.InicioVerificacionRespuesta;
import com.canalesdigitales.contacto.client.dto.ValidacionVerificacionRespuesta;
import com.canalesdigitales.contacto.domain.EstadoActualizacion;
import com.canalesdigitales.contacto.domain.Transaccion;
import com.canalesdigitales.contacto.domain.TransaccionRepository;
import com.canalesdigitales.contacto.exception.CodigoExpiradoException;
import com.canalesdigitales.contacto.exception.CodigoInvalidoException;
import com.canalesdigitales.contacto.exception.ErrorNegocio;
import com.canalesdigitales.contacto.exception.EstadoInvalidoException;
import com.canalesdigitales.contacto.exception.IdentidadRechazadaException;
import com.canalesdigitales.contacto.exception.IntentosExcedidosException;
import com.canalesdigitales.contacto.exception.TransaccionNoEncontradaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ActualizacionContactoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActualizacionContactoService.class);

    private final TransaccionRepository repositorio;
    private final IdentityClient identityClient;
    private final CustomerDataClient customerDataClient;
    private final MessagingClient messagingClient;
    private final int maxIntentos;

    public ActualizacionContactoService(
            TransaccionRepository repositorio,
            IdentityClient identityClient,
            CustomerDataClient customerDataClient,
            MessagingClient messagingClient,
            @Value("${contacto.otp.max-intentos:3}") int maxIntentos) {
        this.repositorio = repositorio;
        this.identityClient = identityClient;
        this.customerDataClient = customerDataClient;
        this.messagingClient = messagingClient;
        this.maxIntentos = maxIntentos;
    }

    public ActualizacionResponse iniciar(IniciarActualizacionRequest request) {
        String correo = CorreoValidator.normalizar(request.getNuevoCorreo());
        Transaccion transaccion = new Transaccion(
                UUID.randomUUID().toString(), request.getAfiliadoId().trim(), correo);
        repositorio.guardar(transaccion);
        LOGGER.info("evento=ACTUALIZACION_INICIADA transactionId={} afiliado={} correo={}",
                transaccion.getId(), transaccion.getAfiliadoId(), CorreoValidator.enmascarar(correo));

        try {
            InicioVerificacionRespuesta inicio = identityClient.iniciarVerificacion(
                    transaccion.getAfiliadoId(), request.getCanalPreferido());
            if (inicio == null || inicio.getReferencia() == null) {
                throw new EstadoInvalidoException(
                        "No fue posible iniciar la validacion de identidad. Intente nuevamente.");
            }
            transaccion.setReferenciaIdentidad(inicio.getReferencia());
            transaccion.cambiarEstado(EstadoActualizacion.PENDIENTE_VERIFICACION);
            repositorio.guardar(transaccion);

            ActualizacionResponse respuesta = respuestaBase(transaccion, "OTP-202",
                    "Enviamos un codigo de verificacion para confirmar su identidad.");
            respuesta.setCanal(inicio.getCanal());
            respuesta.setExpiraEnSegundos(inicio.getExpiraEnSegundos());
            return respuesta;
        } catch (ErrorNegocio error) {
            if (error instanceof IdentidadRechazadaException) {
                transaccion.cambiarEstado(EstadoActualizacion.RECHAZADA);
                transaccion.setResultado(error.getCodigo(), error.getMessage());
                repositorio.guardar(transaccion);
            } else {
                marcarFallida(transaccion, error);
            }
            throw error;
        }
    }

    public ActualizacionResponse verificar(String transactionId, String codigo) {
        Transaccion transaccion = obtener(transactionId);
        validarEstadoParaVerificacion(transaccion);

        if (transaccion.getIntentosVerificacion() >= maxIntentos) {
            transaccion.cambiarEstado(EstadoActualizacion.BLOQUEADA);
            repositorio.guardar(transaccion);
            throw new IntentosExcedidosException(
                    "Se supero el numero de intentos permitidos. Solicite un nuevo codigo.");
        }

        try {
            ValidacionVerificacionRespuesta resultado = identityClient.validarCodigo(
                    transaccion.getReferenciaIdentidad(), codigo);

            if (resultado == null || !resultado.isAprobado()) {
                registrarIntentoFallido(transaccion);
                throw new CodigoInvalidoException("El codigo de verificacion no es correcto.")
                        .conTransaccion(transaccion.getId());
            }

            transaccion.cambiarEstado(EstadoActualizacion.IDENTIDAD_VALIDADA);
            repositorio.guardar(transaccion);
            LOGGER.info("evento=IDENTIDAD_VALIDADA transactionId={}", transaccion.getId());
            return respuestaBase(transaccion, "IDV-200",
                    "Identidad validada. Confirme la actualizacion de su correo.");
        } catch (CodigoExpiradoException error) {
            transaccion.cambiarEstado(EstadoActualizacion.EXPIRADA);
            transaccion.setResultado(error.getCodigo(), error.getMessage());
            repositorio.guardar(transaccion);
            throw error.conTransaccion(transaccion.getId());
        } catch (CodigoInvalidoException error) {
            throw error.conTransaccion(transaccion.getId());
        } catch (IntentosExcedidosException error) {
            transaccion.cambiarEstado(EstadoActualizacion.BLOQUEADA);
            repositorio.guardar(transaccion);
            throw error.conTransaccion(transaccion.getId());
        } catch (ErrorNegocio error) {
            throw error.conTransaccion(transaccion.getId());
        }
    }

    public ActualizacionResponse aplicar(String transactionId, String nuevoCorreo, String idempotencyKey) {
        Transaccion transaccion = obtener(transactionId);

        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            Optional<Transaccion> previa = repositorio.buscarPorIdempotencyKey(idempotencyKey);
            if (previa.isPresent()) {
                LOGGER.info("evento=REINTENTO_IDEMPOTENTE transactionId={} key={}",
                        previa.get().getId(), idempotencyKey);
                return respuestaSegunEstado(previa.get());
            }
        }

        if (transaccion.getEstado() == EstadoActualizacion.APLICADA
                || transaccion.getEstado() == EstadoActualizacion.APLICADA_NOTIFICACION_PENDIENTE) {
            return respuestaSegunEstado(transaccion);
        }

        if (transaccion.getEstado() != EstadoActualizacion.IDENTIDAD_VALIDADA) {
            throw new EstadoInvalidoException(
                    "La transaccion no se encuentra en un estado valido para actualizar el contacto.")
                    .conTransaccion(transaccion.getId());
        }

        String correoNormalizado = CorreoValidator.normalizar(nuevoCorreo);
        transaccion.setNuevoCorreo(correoNormalizado);

        try {
            customerDataClient.actualizarCorreo(transaccion.getAfiliadoId(), correoNormalizado);
        } catch (ErrorNegocio error) {
            LOGGER.warn("evento=ACTUALIZACION_FALLIDA transactionId={} codigo={}",
                    transaccion.getId(), error.getCodigo());
            throw error.conTransaccion(transaccion.getId());
        }

        transaccion.setIdempotencyKey(idempotencyKey);
        try {
            messagingClient.notificarActualizacion(
                    transaccion.getAfiliadoId(), correoNormalizado, transaccion.getId());
            transaccion.cambiarEstado(EstadoActualizacion.APLICADA);
            transaccion.setResultado("OK-200",
                    "Su correo electronico fue actualizado correctamente. Enviamos la confirmacion.");
        } catch (RuntimeException error) {
            // La notificacion es recuperable: el cambio ya quedo aplicado.
            LOGGER.warn("evento=NOTIFICACION_PENDIENTE transactionId={} detalle={}",
                    transaccion.getId(), error.getMessage());
            transaccion.cambiarEstado(EstadoActualizacion.APLICADA_NOTIFICACION_PENDIENTE);
            transaccion.setResultado("MSG-503",
                    "Su correo fue actualizado. La confirmacion se enviara en los proximos minutos.");
        }

        repositorio.guardar(transaccion);
        LOGGER.info("evento=ACTUALIZACION_APLICADA transactionId={} estado={} correo={}",
                transaccion.getId(), transaccion.getEstado(),
                CorreoValidator.enmascarar(correoNormalizado));
        return respuestaSegunEstado(transaccion);
    }

    public ActualizacionResponse consultar(String transactionId) {
        return respuestaSegunEstado(obtener(transactionId));
    }

    private Transaccion obtener(String transactionId) {
        return repositorio.buscarPorId(transactionId)
                .orElseThrow(() -> new TransaccionNoEncontradaException(transactionId));
    }

    private void validarEstadoParaVerificacion(Transaccion transaccion) {
        EstadoActualizacion estado = transaccion.getEstado();
        if (estado == EstadoActualizacion.PENDIENTE_VERIFICACION) {
            return;
        }
        if (estado == EstadoActualizacion.BLOQUEADA) {
            throw new IntentosExcedidosException(
                    "Se supero el numero de intentos permitidos. Solicite un nuevo codigo.")
                    .conTransaccion(transaccion.getId());
        }
        if (estado == EstadoActualizacion.EXPIRADA) {
            throw new CodigoExpiradoException(
                    "El codigo de verificacion expiro. Solicite un nuevo codigo.")
                    .conTransaccion(transaccion.getId());
        }
        throw new EstadoInvalidoException(
                "La transaccion no se encuentra pendiente de verificacion.")
                .conTransaccion(transaccion.getId());
    }

    private void registrarIntentoFallido(Transaccion transaccion) {
        transaccion.incrementarIntentosVerificacion();
        if (transaccion.getIntentosVerificacion() >= maxIntentos) {
            transaccion.cambiarEstado(EstadoActualizacion.BLOQUEADA);
            repositorio.guardar(transaccion);
            throw new IntentosExcedidosException(
                    "Se supero el numero de intentos permitidos. Solicite un nuevo codigo.")
                    .conTransaccion(transaccion.getId());
        }
        repositorio.guardar(transaccion);
    }

    private void marcarFallida(Transaccion transaccion, ErrorNegocio error) {
        transaccion.cambiarEstado(EstadoActualizacion.FALLIDA);
        transaccion.setResultado(error.getCodigo(), error.getMessage());
        repositorio.guardar(transaccion);
    }

    private ActualizacionResponse respuestaSegunEstado(Transaccion transaccion) {
        if (transaccion.getCodigoRespuesta() != null) {
            return respuestaBase(transaccion,
                    transaccion.getCodigoRespuesta(), transaccion.getMensajeRespuesta());
        }
        switch (transaccion.getEstado()) {
            case PENDIENTE_VERIFICACION:
                return respuestaBase(transaccion, "OTP-202",
                        "Pendiente de validar el codigo de verificacion.");
            case IDENTIDAD_VALIDADA:
                return respuestaBase(transaccion, "IDV-200",
                        "Identidad validada. Pendiente confirmar la actualizacion.");
            default:
                return respuestaBase(transaccion, "SYS-200",
                        "Transaccion registrada.");
        }
    }

    private ActualizacionResponse respuestaBase(Transaccion transaccion, String codigo, String mensaje) {
        return new ActualizacionResponse(transaccion.getId(), transaccion.getEstado(),
                codigo, mensaje, correlationId());
    }

    private String correlationId() {
        String correlation = MDC.get("correlationId");
        return correlation != null ? correlation : "N/A";
    }
}
