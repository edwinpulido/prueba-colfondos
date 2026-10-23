/**
 * Cliente del API de actualizacion de contacto.
 * Centraliza timeout, correlationId, Idempotency-Key y traduccion de errores
 * para que el componente de UI no manipule detalles de red.
 */
(function (global) {
    'use strict';

    var Compat = (typeof module !== 'undefined' && module.exports)
        ? require('./compat.js')
        : global.Compat;

    var TIMEOUT_MS = 20000;

    function crearErrorApi(codigo, mensaje, estadoHttp, correlationId) {
        var error = new Error(mensaje);
        error.codigo = codigo;
        error.estadoHttp = estadoHttp || 0;
        error.correlationId = correlationId || '';
        error.esErrorApi = true;
        return error;
    }

    function leerCuerpo(respuesta, correlationId) {
        return respuesta.text().then(function (texto) {
            if (!texto) {
                return {};
            }
            try {
                return JSON.parse(texto);
            } catch (error) {
                return {};
            }
        }, function () {
            return {};
        }).then(function (datos) {
            if (!respuesta.ok) {
                throw crearErrorApi(
                    datos.codigo || 'SYS-' + respuesta.status,
                    datos.mensaje || 'No fue posible completar la operacion.',
                    respuesta.status,
                    datos.correlationId || correlationId
                );
            }
            return datos;
        });
    }

    function crearCliente(opciones) {
        var config = opciones || {};
        var baseUrl = String(config.baseUrl || '').replace(/\/+$/, '');
        var fetchImpl = config.fetchImpl || function (url, init) {
            return global.fetch(url, init);
        };
        var timeoutMs = config.timeoutMs || TIMEOUT_MS;

        function solicitar(metodo, ruta, cuerpo, cabecerasExtra) {
            var correlationId = Compat.generarId();
            var controlador = typeof global.AbortController !== 'undefined'
                ? new global.AbortController()
                : null;
            var cabeceras = {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
                'X-Correlation-ID': correlationId
            };
            if (cabecerasExtra) {
                Object.keys(cabecerasExtra).forEach(function (clave) {
                    cabeceras[clave] = cabecerasExtra[clave];
                });
            }

            var opcionesFetch = {
                method: metodo,
                headers: cabeceras,
                cache: 'no-store'
            };
            if (cuerpo !== undefined && cuerpo !== null) {
                opcionesFetch.body = JSON.stringify(cuerpo);
            }
            if (controlador) {
                opcionesFetch.signal = controlador.signal;
            }

            var temporizador = null;
            var limite = new Promise(function (resolve, reject) {
                temporizador = global.setTimeout(function () {
                    if (controlador) {
                        controlador.abort();
                    }
                    reject(crearErrorApi('GEN-408',
                        'La operacion tardo mas de lo esperado. Intente nuevamente.',
                        0, correlationId));
                }, timeoutMs);
            });

            // La carrera con el temporizador garantiza el timeout incluso si el
            // navegador no cancela la peticion al abortar.
            var peticion = Promise.race([fetchImpl(baseUrl + ruta, opcionesFetch), limite]);

            return Compat.seguro(peticion.then(function (respuesta) {
                var correlationRespuesta = respuesta.headers && typeof respuesta.headers.get === 'function'
                    ? respuesta.headers.get('X-Correlation-ID')
                    : '';
                return leerCuerpo(respuesta, correlationRespuesta || correlationId);
            })).then(function (datos) {
                return datos;
            }, function (error) {
                if (error && error.esErrorApi) {
                    throw error;
                }
                if (error && error.name === 'AbortError') {
                    throw crearErrorApi('GEN-408',
                        'La operacion tardo mas de lo esperado. Intente nuevamente.',
                        0, correlationId);
                }
                throw crearErrorApi('GEN-503',
                    'No fue posible conectar con el servicio. Verifique su conexion e intente nuevamente.',
                    0, correlationId);
            }).then(function (resultado) {
                global.clearTimeout(temporizador);
                return resultado;
            }, function (error) {
                global.clearTimeout(temporizador);
                throw error;
            });
        }

        return {
            iniciar: function (datos) {
                return solicitar('POST', '/v1/contacto/actualizacion', {
                    afiliadoId: datos.afiliadoId,
                    nuevoCorreo: datos.nuevoCorreo,
                    canalPreferido: datos.canalPreferido || 'SMS'
                });
            },
            verificar: function (transactionId, codigo) {
                return solicitar('POST', '/v1/contacto/actualizacion/' +
                    encodeURIComponent(transactionId) + '/verificacion', { codigo: codigo });
            },
            aplicar: function (transactionId, correo, idempotencyKey) {
                return solicitar('PUT', '/v1/contacto/actualizacion/' +
                    encodeURIComponent(transactionId),
                    { nuevoCorreo: correo },
                    { 'Idempotency-Key': idempotencyKey });
            },
            consultar: function (transactionId) {
                return solicitar('GET', '/v1/contacto/actualizacion/' +
                    encodeURIComponent(transactionId));
            }
        };
    }

    var api = { crearCliente: crearCliente, crearErrorApi: crearErrorApi };

    if (typeof module !== 'undefined' && module.exports) {
        module.exports = api;
    }
    global.ApiContacto = api;
})(typeof globalThis !== 'undefined' ? globalThis : this);
