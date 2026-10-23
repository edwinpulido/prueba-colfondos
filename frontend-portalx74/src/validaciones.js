/**
 * Validaciones del formulario de actualizacion de contacto.
 * La misma regla esta implementada en el backend (CorreoValidator) para que el
 * afiliado reciba retroalimentacion inmediata sin depender de la red.
 */
(function (global) {
    'use strict';

    var FORMATO = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

    function normalizarCorreo(valor) {
        return String(valor == null ? '' : valor).trim().toLowerCase();
    }

    function validarCorreo(valor) {
        var correo = normalizarCorreo(valor);
        if (!correo) {
            return { valido: false, mensaje: 'El correo electronico es obligatorio.' };
        }
        if (correo.length > 120) {
            return { valido: false, mensaje: 'El correo electronico no puede superar 120 caracteres.' };
        }
        if (correo.indexOf(' ') !== -1) {
            return { valido: false, mensaje: 'El correo electronico no puede contener espacios.' };
        }
        if (correo.indexOf('..') !== -1 || !FORMATO.test(correo)) {
            return { valido: false, mensaje: 'Ingrese un correo valido. Ejemplo: nombre@dominio.com' };
        }
        return { valido: true, mensaje: '', correo: correo };
    }

    function validarCodigo(valor) {
        var codigo = String(valor == null ? '' : valor).trim();
        if (!/^[0-9]{6}$/.test(codigo)) {
            return { valido: false, mensaje: 'El codigo debe tener 6 digitos.' };
        }
        return { valido: true, mensaje: '', codigo: codigo };
    }

    function enmascararCorreo(correo) {
        var texto = normalizarCorreo(correo);
        var arroba = texto.indexOf('@');
        if (arroba < 1) {
            return '***';
        }
        return texto.slice(0, 1) + '***' + texto.slice(arroba);
    }

    var api = {
        normalizarCorreo: normalizarCorreo,
        validarCorreo: validarCorreo,
        validarCodigo: validarCodigo,
        enmascararCorreo: enmascararCorreo
    };

    if (typeof module !== 'undefined' && module.exports) {
        module.exports = api;
    }
    global.Validaciones = api;
})(typeof globalThis !== 'undefined' ? globalThis : this);
