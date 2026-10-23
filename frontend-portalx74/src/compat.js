/**
 * Utilidades de compatibilidad del componente.
 * El portal opera sobre PortalX 7.0 y 7.4 con librerias heredadas; estas funciones
 * evitan depender de APIs de JavaScript que no estan disponibles en todos los
 * navegadores soportados (Chrome, Edge y Safari).
 */
(function (global) {
    'use strict';

    function generarId() {
        if (global.crypto && typeof global.crypto.randomUUID === 'function') {
            return global.crypto.randomUUID();
        }
        var bytes = new Uint8Array(16);
        if (global.crypto && typeof global.crypto.getRandomValues === 'function') {
            global.crypto.getRandomValues(bytes);
        } else {
            for (var i = 0; i < bytes.length; i++) {
                bytes[i] = Math.floor(Math.random() * 256);
            }
        }
        bytes[6] = (bytes[6] & 0x0f) | 0x40;
        bytes[8] = (bytes[8] & 0x3f) | 0x80;
        var hex = Array.prototype.map.call(bytes, function (b) {
            return ('0' + b.toString(16)).slice(-2);
        }).join('');
        return hex.slice(0, 8) + '-' + hex.slice(8, 12) + '-' + hex.slice(12, 16) + '-' +
            hex.slice(16, 20) + '-' + hex.slice(20);
    }

    function clonar(valor) {
        if (typeof global.structuredClone === 'function') {
            return global.structuredClone(valor);
        }
        return JSON.parse(JSON.stringify(valor));
    }

    function formatoFecha(fecha) {
        var d = fecha instanceof Date ? fecha : new Date(fecha);
        if (isNaN(d.getTime())) {
            return '';
        }
        var pad = function (n) { return ('0' + n).slice(-2); };
        return pad(d.getDate()) + '/' + pad(d.getMonth() + 1) + '/' + d.getFullYear() +
            ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes());
    }

    function esperar(ms) {
        return new Promise(function (resolve) {
            global.setTimeout(resolve, ms);
        });
    }

    /** Envuelve una promesa para que ningun rechazo quede sin manejar. */
    function seguro(promesa, alFallar) {
        return Promise.resolve(promesa).catch(function (error) {
            if (typeof alFallar === 'function') {
                alFallar(error);
            }
            throw error;
        });
    }

    /** Ejecuta una funcion async y garantiza manejo explicito del error. */
    function ejecutarSeguro(fn, alFallar) {
        var resultado;
        try {
            resultado = fn();
        } catch (error) {
            if (typeof alFallar === 'function') { alFallar(error); }
            return Promise.reject(error);
        }
        return seguro(resultado, alFallar);
    }

    var api = {
        generarId: generarId,
        clonar: clonar,
        formatoFecha: formatoFecha,
        esperar: esperar,
        seguro: seguro,
        ejecutarSeguro: ejecutarSeguro
    };

    if (typeof module !== 'undefined' && module.exports) {
        module.exports = api;
    }
    global.Compat = api;
})(typeof globalThis !== 'undefined' ? globalThis : this);
