package com.canalesdigitales.contacto.domain;

import java.util.Optional;

public interface TransaccionRepository {

    Transaccion guardar(Transaccion transaccion);

    Optional<Transaccion> buscarPorId(String id);

    Optional<Transaccion> buscarPorIdempotencyKey(String idempotencyKey);
}
