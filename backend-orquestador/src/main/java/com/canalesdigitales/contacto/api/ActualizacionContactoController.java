package com.canalesdigitales.contacto.api;

import com.canalesdigitales.contacto.api.dto.ActualizacionResponse;
import com.canalesdigitales.contacto.api.dto.ActualizarContactoRequest;
import com.canalesdigitales.contacto.api.dto.IniciarActualizacionRequest;
import com.canalesdigitales.contacto.api.dto.VerificacionRequest;
import com.canalesdigitales.contacto.domain.EstadoActualizacion;
import com.canalesdigitales.contacto.service.ActualizacionContactoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/v1/contacto/actualizacion")
public class ActualizacionContactoController {

    private final ActualizacionContactoService servicio;

    public ActualizacionContactoController(ActualizacionContactoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<ActualizacionResponse> iniciar(
            @Valid @RequestBody IniciarActualizacionRequest request) {
        ActualizacionResponse respuesta = servicio.iniciar(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(respuesta);
    }

    @PostMapping("/{transactionId}/verificacion")
    public ResponseEntity<ActualizacionResponse> verificar(
            @PathVariable String transactionId,
            @Valid @RequestBody VerificacionRequest request) {
        return ResponseEntity.ok(servicio.verificar(transactionId, request.getCodigo()));
    }

    @PutMapping("/{transactionId}")
    public ResponseEntity<ActualizacionResponse> aplicar(
            @PathVariable String transactionId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ActualizarContactoRequest request) {
        ActualizacionResponse respuesta = servicio.aplicar(
                transactionId, request.getNuevoCorreo(), idempotencyKey);
        HttpStatus estado = respuesta.getEstado() == EstadoActualizacion.APLICADA_NOTIFICACION_PENDIENTE
                ? HttpStatus.ACCEPTED
                : HttpStatus.OK;
        return ResponseEntity.status(estado).body(respuesta);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<ActualizacionResponse> consultar(@PathVariable String transactionId) {
        return ResponseEntity.ok(servicio.consultar(transactionId));
    }
}
