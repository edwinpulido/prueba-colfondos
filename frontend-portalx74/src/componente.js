(function (global) {
    'use strict';

    var Validaciones = global.Validaciones;
    var Compat = global.Compat;

    var estadoInicial = function () {
        return {
            paso: 1,
            transactionId: null,
            idempotencyKey: null,
            correo: '',
            intervalo: null,
            segundosRestantes: 0
        };
    };

    var estado = estadoInicial();
    var cliente = null;

    function porId(id) {
        return global.document.getElementById(id);
    }

    function configurarCliente() {
        if (porId('modo-demo').checked) {
            cliente = global.ApiContactoMock.crearClienteMock();
            cliente.configurar({
                fallaNotificacion: porId('demo-falla-notificacion').checked
            });
        } else {
            cliente = global.ApiContacto.crearCliente({
                baseUrl: porId('base-url').value || ''
            });
        }
        return cliente;
    }

    function mostrarPaso(paso) {
        estado.paso = paso;
        ['paso-1', 'paso-2', 'paso-3'].forEach(function (id, indice) {
            porId(id).hidden = indice + 1 !== paso;
            var indicador = porId('indicador-' + (indice + 1));
            indicador.className = 'paso-indicador' +
                (indice + 1 === paso ? ' activo' : '') +
                (indice + 1 < paso ? ' completado' : '');
        });
        porId('bloque-confirmacion').hidden = false;
        porId('bloque-resultado').hidden = true;
        var foco = paso === 1 ? 'input-correo' : (paso === 2 ? 'input-codigo' : 'btn-confirmar');
        var elemento = porId(foco);
        if (elemento) {
            elemento.focus();
        }
    }

    function limpiarMensaje() {
        var contenedor = porId('mensaje');
        contenedor.hidden = true;
        contenedor.className = 'mensaje';
        contenedor.innerHTML = '';
    }

    function mostrarMensaje(tipo, titulo, detalle, codigo, correlationId) {
        var contenedor = porId('mensaje');
        contenedor.className = 'mensaje mensaje-' + tipo;
        contenedor.innerHTML = '';
        var encabezado = global.document.createElement('strong');
        encabezado.textContent = titulo;
        contenedor.appendChild(encabezado);
        if (detalle) {
            var parrafo = global.document.createElement('p');
            parrafo.textContent = detalle;
            contenedor.appendChild(parrafo);
        }
        if (codigo || correlationId) {
            var pie = global.document.createElement('p');
            pie.className = 'mensaje-pie';
            pie.textContent = (codigo ? 'Codigo: ' + codigo : '') +
                (correlationId ? '  |  CorrelationId: ' + correlationId : '');
            contenedor.appendChild(pie);
        }
        contenedor.hidden = false;
    }

    function mostrarError(error) {
        var esApi = error && error.esErrorApi;
        mostrarMensaje('error',
            esApi ? 'No pudimos completar la operacion' : 'Ocurrio un error inesperado',
            esApi ? error.message : 'Intente nuevamente. Si el problema continua, contacte al Contact Center.',
            esApi ? error.codigo : 'SYS-500',
            esApi ? error.correlationId : '');
    }

    function ocupado(boton, activo, texto) {
        boton.disabled = activo;
        if (texto) {
            if (!boton.dataset.textoOriginal) {
                boton.dataset.textoOriginal = boton.textContent;
            }
            boton.textContent = activo ? texto : boton.dataset.textoOriginal;
        }
    }

    function iniciarContador(segundos) {
        detenerContador();
        estado.segundosRestantes = segundos || 300;
        pintarContador();
        estado.intervalo = global.setInterval(function () {
            estado.segundosRestantes = Math.max(0, estado.segundosRestantes - 1);
            pintarContador();
            if (estado.segundosRestantes === 0) {
                detenerContador();
                mostrarMensaje('advertencia', 'El codigo expiro',
                    'Solicite un nuevo codigo para continuar con la validacion.', 'OTP-410', '');
            }
        }, 1000);
    }

    function detenerContador() {
        if (estado.intervalo) {
            global.clearInterval(estado.intervalo);
            estado.intervalo = null;
        }
    }

    function pintarContador() {
        var minutos = Math.floor(estado.segundosRestantes / 60);
        var segundos = estado.segundosRestantes % 60;
        porId('contador').textContent = ('0' + minutos).slice(-2) + ':' + ('0' + segundos).slice(-2);
    }

    function enviarCodigo(evento) {
        evento.preventDefault();
        limpiarMensaje();

        var revision = Validaciones.validarCorreo(porId('input-correo').value);
        if (!revision.valido) {
            mostrarMensaje('error', 'Revise el correo ingresado', revision.mensaje, 'VAL-001', '');
            porId('input-correo').focus();
            return;
        }

        var afiliadoId = porId('input-afiliado').value.trim();
        if (!afiliadoId) {
            mostrarMensaje('error', 'Falta informacion',
                'No fue posible identificar la sesion del afiliado.', 'VAL-001', '');
            return;
        }

        var boton = porId('btn-enviar');
        ocupado(boton, true, 'Enviando...');

        Compat.ejecutarSeguro(function () {
            return configurarCliente().iniciar({
                afiliadoId: afiliadoId,
                nuevoCorreo: revision.correo,
                canalPreferido: 'SMS'
            });
        }).then(function (respuesta) {
            estado.transactionId = respuesta.transactionId;
            estado.idempotencyKey = Compat.generarId();
            estado.correo = revision.correo;
            porId('correo-destino').textContent = Validaciones.enmascararCorreo(revision.correo);
            porId('canal-envio').textContent = respuesta.canal === 'EMAIL'
                ? 'correo electronico' : 'mensaje de texto';
            porId('correo-confirmacion').textContent = revision.correo;
            iniciarContador(respuesta.expiraEnSegundos || 300);
            mostrarPaso(2);
        }, mostrarError).then(function () {
            ocupado(boton, false);
        });
    }

    function verificarCodigo(evento) {
        evento.preventDefault();
        limpiarMensaje();

        var revision = Validaciones.validarCodigo(porId('input-codigo').value);
        if (!revision.valido) {
            mostrarMensaje('error', 'Revise el codigo', revision.mensaje, 'VAL-001', '');
            porId('input-codigo').focus();
            return;
        }

        var boton = porId('btn-verificar');
        ocupado(boton, true, 'Validando...');

        Compat.ejecutarSeguro(function () {
            return cliente.verificar(estado.transactionId, revision.codigo);
        }).then(function () {
            detenerContador();
            mostrarPaso(3);
        }, mostrarError).then(function () {
            ocupado(boton, false);
        });
    }

    function confirmarActualizacion() {
        limpiarMensaje();
        var boton = porId('btn-confirmar');
        ocupado(boton, true, 'Actualizando...');

        Compat.ejecutarSeguro(function () {
            return cliente.aplicar(estado.transactionId, estado.correo, estado.idempotencyKey);
        }).then(function (respuesta) {
            porId('resumen-correo').textContent = estado.correo;
            porId('resumen-estado').textContent = respuesta.estado === 'APLICADA'
                ? 'Actualizacion confirmada'
                : 'Actualizacion aplicada, notificacion en proceso';
            porId('resumen-fecha').textContent = Compat.formatoFecha(new Date());
            porId('bloque-confirmacion').hidden = true;
            porId('bloque-resultado').hidden = false;
            if (respuesta.estado === 'APLICADA_NOTIFICACION_PENDIENTE') {
                mostrarMensaje('advertencia', 'Actualizacion completada',
                    'La confirmacion se enviara en los proximos minutos. No es necesario repetir el proceso.',
                    respuesta.codigo, respuesta.correlationId);
            } else {
                mostrarMensaje('exito', 'Correo actualizado',
                    'Enviamos la confirmacion a ' + estado.correo + '.',
                    respuesta.codigo, respuesta.correlationId);
            }
            porId('btn-reiniciar').focus();
        }, mostrarError).then(function () {
            ocupado(boton, false);
        });
    }

    function reenviarCodigo() {
        limpiarMensaje();
        var boton = porId('btn-reenviar');
        ocupado(boton, true, 'Reenviando...');
        Compat.ejecutarSeguro(function () {
            return cliente.iniciar({
                afiliadoId: porId('input-afiliado').value.trim(),
                nuevoCorreo: estado.correo,
                canalPreferido: 'SMS'
            });
        }).then(function (respuesta) {
            estado.transactionId = respuesta.transactionId;
            estado.idempotencyKey = Compat.generarId();
            iniciarContador(respuesta.expiraEnSegundos || 300);
            mostrarMensaje('info', 'Codigo reenviado',
                'Le enviamos un nuevo codigo de verificacion.',
                respuesta.codigo, respuesta.correlationId);
        }, mostrarError).then(function () {
            ocupado(boton, false);
        });
    }

    function reiniciar() {
        detenerContador();
        estado = estadoInicial();
        porId('input-correo').value = '';
        porId('input-codigo').value = '';
        limpiarMensaje();
        mostrarPaso(1);
    }

    global.document.addEventListener('DOMContentLoaded', function () {
        porId('form-correo').addEventListener('submit', enviarCodigo);
        porId('form-codigo').addEventListener('submit', verificarCodigo);
        porId('btn-confirmar').addEventListener('click', confirmarActualizacion);
        porId('btn-reenviar').addEventListener('click', reenviarCodigo);
        porId('btn-reiniciar').addEventListener('click', reiniciar);
        porId('modo-demo').addEventListener('change', function () {
            porId('opciones-demo').hidden = !porId('modo-demo').checked;
            porId('opciones-backend').hidden = porId('modo-demo').checked;
        });
        porId('input-codigo').addEventListener('input', function (evento) {
            evento.target.value = evento.target.value.replace(/[^0-9]/g, '').slice(0, 6);
        });
        // Red de seguridad de la pantalla: ningun rechazo de promesa queda sin manejar.
        global.addEventListener('unhandledrejection', function (evento) {
            mostrarMensaje('error', 'No pudimos completar la operacion',
                'Intente nuevamente. Si el problema continua, contacte al Contact Center.',
                'SYS-500', '');
            if (evento && typeof evento.preventDefault === 'function') {
                evento.preventDefault();
            }
        });
        mostrarPaso(1);
    });
})(typeof globalThis !== 'undefined' ? globalThis : this);
