package com.canalesdigitales.contacto.api;

import com.canalesdigitales.contacto.api.dto.ApiErrorResponse;
import com.canalesdigitales.contacto.config.CorrelationIdFilter;
import com.canalesdigitales.contacto.exception.ErrorNegocio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ErrorNegocio.class)
    public ResponseEntity<ApiErrorResponse> manejarErrorNegocio(ErrorNegocio error) {
        LOGGER.warn("evento=ERROR_CONTROLADO codigo={} transactionId={}",
                error.getCodigo(), error.getTransactionId());
        ApiErrorResponse cuerpo = new ApiErrorResponse(error.getCodigo(), error.getMessage(),
                error.getTransactionId(), correlationId());
        return ResponseEntity.status(error.getEstadoHttp()).body(cuerpo);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> manejarValidacion(MethodArgumentNotValidException error) {
        String mensaje = error.getBindingResult().getFieldErrors().isEmpty()
                ? "Los datos enviados no son validos."
                : error.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        ApiErrorResponse cuerpo = new ApiErrorResponse("VAL-001", mensaje, null, correlationId());
        return ResponseEntity.badRequest().body(cuerpo);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> manejarCuerpoIlegible(HttpMessageNotReadableException error) {
        ApiErrorResponse cuerpo = new ApiErrorResponse("VAL-001",
                "La solicitud no tiene un formato valido.", null, correlationId());
        return ResponseEntity.badRequest().body(cuerpo);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> manejarErrorNoControlado(Exception error) {
        LOGGER.error("evento=ERROR_NO_CONTROLADO correlationId={}", correlationId(), error);
        ApiErrorResponse cuerpo = new ApiErrorResponse("SYS-500",
                "Se presento un error inesperado. Intente nuevamente o contacte al Contact Center.",
                null, correlationId());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(cuerpo);
    }

    private String correlationId() {
        String correlation = MDC.get(CorrelationIdFilter.MDC_KEY);
        return correlation != null ? correlation : "N/A";
    }
}
