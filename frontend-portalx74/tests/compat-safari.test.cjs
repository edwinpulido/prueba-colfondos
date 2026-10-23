'use strict';

const test = require('node:test');
const assert = require('node:assert');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

/**
 * Ejecuta compat.js en un contexto sin crypto.randomUUID ni structuredClone,
 * para verificar los respaldos que se activan en navegadores que no exponen
 * esas APIs.
 */
function cargarCompatSinApisModernas() {
    const codigo = fs.readFileSync(path.join(__dirname, '..', 'src', 'compat.js'), 'utf8');
    const sandbox = {
        setTimeout,
        clearTimeout,
        Promise,
        Date,
        JSON,
        Array,
        Math,
        Error,
        Uint8Array
    };
    let llamada = 0;
    sandbox.crypto = {
        getRandomValues: (bytes) => {
            llamada++;
            for (let i = 0; i < bytes.length; i++) {
                bytes[i] = (i * 7 + llamada) % 256;
            }
            return bytes;
        }
    };
    const contexto = vm.createContext(sandbox);
    vm.runInContext(codigo, contexto);
    return sandbox.Compat;
}

test('generarId funciona sin crypto.randomUUID', () => {
    const Compat = cargarCompatSinApisModernas();
    const primero = Compat.generarId();
    const segundo = Compat.generarId();

    assert.match(primero, /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/);
    assert.strictEqual(primero.split('-').length, 5);
    assert.notStrictEqual(primero, segundo);
});

test('clonar funciona sin structuredClone', () => {
    const Compat = cargarCompatSinApisModernas();
    const original = { correo: 'ana@dominio.com', intentos: 0 };
    const copia = Compat.clonar(original);

    assert.deepStrictEqual(copia, original);
    assert.notStrictEqual(copia, original);
});

test('formatoFecha no depende de la configuracion regional del navegador', () => {
    const Compat = cargarCompatSinApisModernas();
    assert.strictEqual(Compat.formatoFecha(new Date(2026, 0, 5, 9, 7)), '05/01/2026 09:07');
});
