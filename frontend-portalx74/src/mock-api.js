/**
 * API simulada para la demostracion local del componente (sin backend).
 * Mantiene el mismo contrato que src/api.js para poder intercambiarse sin
 * cambiar la logica de la pantalla.
 */
(function (global) {
    'use strict';

    var Compat = (typeof module !== 'undefined' && module.exports)
        ? require('./compat.js')
        : global.Compat;
    var Validaciones = (typeof module !== 'undefined' && module.exports)
        ? require('./validaciones.js')
        : global.Validaciones;

    var OTP_VALIDO = '123456';
    var OTP_EXPIRADO = '999999';
    var CORREO_DUPLICADO = 'duplicado@dominio.com';
    var LATENCIA = 400;

    function crearClienteMock() {
        var ApiContacto = (typeof module !== 'undefined' && module.exports)
            ? require('./api.js')
            : global.ApiContacto;
        var estado = {};
        var opciones = { fallaNotificacion: false, latencia: LATENCIA };

        function fallo(codigo, mensaje, estadoHttp) {
            return ApiContacto.crearErrorApi(codigo, mensaje, estadoHttp, Compat.generarId());
        }

        return {
            configurar: function (nuevas) {
                Object.keys(nuevas || {}).forEach(function (clave) {
                    opciones[clave] = nuevas[clave];
                });
            },
            iniciar: function (datos) {
                var correo = Validaciones.normalizarCorreo(datos.nuevoCorreo);
                var revision = Validaciones.validarCorreo(correo);
                return Compat.esperar(opciones.latencia).then(function () {
                    if (!revision.valido) {
                        throw fallo('VAL-001', revision.mensaje, 400);
                    }
                    estado = { id: Compat.generarId(), correo: correo, verificado: false };
                    return {
                        transactionId: estado.id,
                        estado: 'PENDIENTE_VERIFICACION',
                        codigo: 'OTP-202',
                        mensaje: 'Codigo enviado al numero registrado.',
                        canal: datos.canalPreferido || 'SMS',
                        expiraEnSegundos: 300,
                        correlationId: Compat.generarId()
                    };
                });
            },
            verificar: function (transactionId, codigo) {
                return Compat.esperar(opciones.latencia).then(function () {
                    if (!estado.id || estado.id !== transactionId) {
                        throw fallo('SYS-404', 'La transaccion no existe o expiro.', 404);
                    }
                    if (codigo === OTP_EXPIRADO) {
                        throw fallo('OTP-410', 'El codigo expiro. Solicite uno nuevo.', 410);
                    }
                    if (codigo !== OTP_VALIDO) {
                        throw fallo('OTP-401', 'El codigo de verificacion no es correcto.', 401);
                    }
                    estado.verificado = true;
                    return {
                        transactionId: transactionId,
                        estado: 'IDENTIDAD_VALIDADA',
                        codigo: 'IDV-200',
                        mensaje: 'Identidad validada.',
                        correlationId: Compat.generarId()
                    };
                });
            },
            aplicar: function (transactionId, correo, idempotencyKey) {
                return Compat.esperar(opciones.latencia).then(function () {
                    if (!estado.id || estado.id !== transactionId || !estado.verificado) {
                        throw fallo('CON-409', 'La transaccion no esta lista para actualizar.', 409);
                    }
                    if (!idempotencyKey) {
                        throw fallo('VAL-001', 'Falta la llave de idempotencia.', 400);
                    }
                    if (Validaciones.normalizarCorreo(correo) === CORREO_DUPLICADO) {
                        throw fallo('CD-409',
                            'No fue posible completar la actualizacion. Verifique los datos e intente nuevamente.',
                            409);
                    }
                    if (estado.aplicada) {
                        return estado.respuesta;
                    }
                    estado.aplicada = true;
                    estado.respuesta = opciones.fallaNotificacion
                        ? {
                            transactionId: transactionId,
                            estado: 'APLICADA_NOTIFICACION_PENDIENTE',
                            codigo: 'MSG-503',
                            mensaje: 'Su correo fue actualizado. La confirmacion se enviara en los proximos minutos.',
                            correlationId: Compat.generarId()
                        }
                        : {
                            transactionId: transactionId,
                            estado: 'APLICADA',
                            codigo: 'OK-200',
                            mensaje: 'Su correo electronico fue actualizado correctamente.',
                            correlationId: Compat.generarId()
                        };
                    return estado.respuesta;
                });
            },
            consultar: function (transactionId) {
                return Compat.esperar(opciones.latencia).then(function () {
                    if (!estado.id || estado.id !== transactionId) {
                        throw fallo('SYS-404', 'La transaccion no existe o expiro.', 404);
                    }
                    return {
                        transactionId: transactionId,
                        estado: estado.aplicada ? 'APLICADA' : (estado.verificado ? 'IDENTIDAD_VALIDADA' : 'PENDIENTE_VERIFICACION'),
                        codigo: 'SYS-200',
                        mensaje: 'Consulta de estado.',
                        correlationId: Compat.generarId()
                    };
                });
            }
        };
    }

    var api = { crearClienteMock: crearClienteMock };

    if (typeof module !== 'undefined' && module.exports) {
        module.exports = api;
    }
    global.ApiContactoMock = api;
})(typeof globalThis !== 'undefined' ? globalThis : this);
