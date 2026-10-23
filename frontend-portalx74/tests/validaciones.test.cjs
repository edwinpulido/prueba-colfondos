'use strict';

const test = require('node:test');
const assert = require('node:assert');

const Validaciones = require('../src/validaciones.js');
const Compat = require('../src/compat.js');

test('validarCorreo acepta correos validos y normaliza a minusculas', () => {
    const resultado = Validaciones.validarCorreo('  Ana.Lopez@Dominio.COM  ');
    assert.strictEqual(resultado.valido, true);
    assert.strictEqual(resultado.correo, 'ana.lopez@dominio.com');
});

test('validarCorreo rechaza formatos invalidos', () => {
    ['ana@', 'ana@dominio', '@dominio.com', 'ana lopez@dominio.com',
        'ana..lopez@dominio.com', '', null].forEach((correo) => {
        assert.strictEqual(Validaciones.validarCorreo(correo).valido, false, String(correo));
    });
});

test('validarCorreo rechaza correos de mas de 120 caracteres', () => {
    const largo = 'a'.repeat(115) + '@dominio.com';
    assert.strictEqual(Validaciones.validarCorreo(largo).valido, false);
});

test('validarCodigo exige exactamente 6 digitos', () => {
    assert.strictEqual(Validaciones.validarCodigo('123456').valido, true);
    assert.strictEqual(Validaciones.validarCodigo('12345').valido, false);
    assert.strictEqual(Validaciones.validarCodigo('12345a').valido, false);
    assert.strictEqual(Validaciones.validarCodigo(' 123456 ').valido, true);
});

test('enmascararCorreo no expone el correo completo', () => {
    assert.strictEqual(Validaciones.enmascararCorreo('ana@dominio.com'), 'a***@dominio.com');
    assert.strictEqual(Validaciones.enmascararCorreo(''), '***');
});

test('generarId produce identificadores distintos', () => {
    const primero = Compat.generarId();
    const segundo = Compat.generarId();
    assert.ok(primero.length >= 20);
    assert.notStrictEqual(primero, segundo);
});

test('formatoFecha usa un formato estable entre navegadores', () => {
    assert.strictEqual(Compat.formatoFecha(new Date(2026, 9, 23, 14, 5)), '23/10/2026 14:05');
    assert.strictEqual(Compat.formatoFecha('no-es-fecha'), '');
});

test('seguro convierte un rechazo en error manejado', async () => {
    let capturado = null;
    await assert.rejects(
        Compat.seguro(Promise.reject(new Error('fallo')), (error) => { capturado = error; }),
        /fallo/
    );
    assert.ok(capturado instanceof Error);
});
