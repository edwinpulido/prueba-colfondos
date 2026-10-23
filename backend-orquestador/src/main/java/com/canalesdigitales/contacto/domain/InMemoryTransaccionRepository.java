package com.canalesdigitales.contacto.domain;

import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Implementacion en memoria para la prueba tecnica.
 * En el ambiente corporativo esta responsabilidad se reemplaza por el repositorio
 * transaccional del dominio (base de datos gobernada), manteniendo la misma interfaz.
 */
@Repository
public class InMemoryTransaccionRepository implements TransaccionRepository {

    private final ConcurrentMap<String, Transaccion> transacciones = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> indiceIdempotencia = new ConcurrentHashMap<>();

    @Override
    public Transaccion guardar(Transaccion transaccion) {
        transacciones.put(transaccion.getId(), transaccion);
        if (transaccion.getIdempotencyKey() != null) {
            indiceIdempotencia.put(transaccion.getIdempotencyKey(), transaccion.getId());
        }
        return transaccion;
    }

    @Override
    public Optional<Transaccion> buscarPorId(String id) {
        return Optional.ofNullable(transacciones.get(id));
    }

    @Override
    public Optional<Transaccion> buscarPorIdempotencyKey(String idempotencyKey) {
        String id = indiceIdempotencia.get(idempotencyKey);
        return id == null ? Optional.empty() : buscarPorId(id);
    }
}
