'use strict';

const test = require('node:test');
const assert = require('node:assert');

const Api = require('../src/api.js');
const Mock = require('../src/mock-api.js');

function respuesta(estado, cuerpo, correlationId) {
    return Promise.resolve({
        ok: estado >= 200 && estado < 300,
        status: estado,
        headers: {
            get: (nombre) => (nombre === 'X-Correlation-ID' ? (correlationId || null) : null)
        },
        text: () => Promise.resolve(cuerpo === undefined ? '' : JSON.stringify(cuerpo))
    });
}

test('enviar solicitud incluye correlationId y devuelve el cuerpo', async () => {
    let capturado = null;
    const cliente = Api.crearCliente({
        baseUrl: 'http://localhost:8081',
        fetchImpl: (url, init) => {
            capturado = { url, init };
            return respuesta(202, { transactionId: 'TX-1', estado: 'PENDIENTE_VERIFICACION' });
        }
    });

    const resultado = await cliente.iniciar({ afiliadoId: 'AFI-1', nuevoCorreo: 'ana@dominio.com' });

    assert.strictEqual(resultado.transactionId, 'TX-1');
    assert.ok(capturado.init.headers['X-Correlation-ID']);
    assert.strictEqual(capturado.url, 'http://localhost:8081/v1/contacto/actualizacion');
});

test('traduce el error del backend a un error de negocio', async () => {
    const cliente = Api.crearCliente({
        fetchImpl: () => respuesta(409, {
            codigo: 'CD-409',
            mensaje: 'No fue posible completar la actualizacion.',
            correlationId: 'corr-9'
        })
    });

    await assert.rejects(
        cliente.aplicar('TX-1', 'ana@dominio.com', 'KEY-1'),
        (error) => error.esErrorApi === true && error.codigo === 'CD-409' && error.correlationId === 'corr-9'
    );
});

test('soporta respuestas 200 sin cuerpo', async () => {
    const cliente = Api.crearCliente({
        fetchImpl: () => respuesta(200, undefined)
    });

    const resultado = await cliente.consultar('TX-1');
    assert.deepStrictEqual(resultado, {});
});

test('aplica timeout cuando el servicio no responde', async () => {
    const cliente = Api.crearCliente({
        timeoutMs: 30,
        fetchImpl: () => new Promise(() => {})
    });

    await assert.rejects(
        cliente.consultar('TX-1'),
        (error) => error.codigo === 'GEN-408'
    );
});

test('flujo completo con la API simulada', async () => {
    const cliente = Mock.crearClienteMock();
    cliente.configurar({ latencia: 0 });

    const inicio = await cliente.iniciar({ afiliadoId: 'AFI-1', nuevoCorreo: 'Ana@Dominio.com' });
    assert.strictEqual(inicio.estado, 'PENDIENTE_VERIFICACION');

    const validacion = await cliente.verificar(inicio.transactionId, '123456');
    assert.strictEqual(validacion.estado, 'IDENTIDAD_VALIDADA');

    const aplicada = await cliente.aplicar(inicio.transactionId, 'ana@dominio.com', 'KEY-1');
    assert.strictEqual(aplicada.estado, 'APLICADA');
    assert.strictEqual(aplicada.codigo, 'OK-200');
});

test('la API simulada reporta correo duplicado y notificacion pendiente', async () => {
    const cliente = Mock.crearClienteMock();
    cliente.configurar({ latencia: 0 });

    const inicio = await cliente.iniciar({ afiliadoId: 'AFI-1', nuevoCorreo: 'ana@dominio.com' });
    await cliente.verificar(inicio.transactionId, '123456');

    await assert.rejects(
        cliente.aplicar(inicio.transactionId, 'duplicado@dominio.com', 'KEY-2'),
        (error) => error.codigo === 'CD-409'
    );

    const otro = await cliente.iniciar({ afiliadoId: 'AFI-1', nuevoCorreo: 'ana@dominio.com' });
    await cliente.verificar(otro.transactionId, '123456');
    cliente.configurar({ fallaNotificacion: true });
    const pendiente = await cliente.aplicar(otro.transactionId, 'ana@dominio.com', 'KEY-3');
    assert.strictEqual(pendiente.estado, 'APLICADA_NOTIFICACION_PENDIENTE');
    assert.strictEqual(pendiente.codigo, 'MSG-503');
});
