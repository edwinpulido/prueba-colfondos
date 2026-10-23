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
        try {
            var modoDemo = porId('modo-demo');
            if (modoDemo && modoDemo.checked) {
                if (global.ApiContactoMock && typeof global.ApiContactoMock.crearClienteMock === 'function') {
                    cliente = global.ApiContactoMock.crearClienteMock();
                    var fallaNotif = porId('demo-falla-notificacion');
                    if (cliente && typeof cliente.configurar === 'function') {
                        cliente.configurar({
                            fallaNotificacion: fallaNotif ? fallaNotif.checked : false
                        });
                    }
                    return cliente;
                }
            }
        } catch (e) {
            // continuar con fallback
        }

        // Fallback: si por alguna razon no se usa el modo demo, forzarlo para evitar recarga
        try {
            if (global.ApiContactoMock && typeof global.ApiContactoMock.crearClienteMock === 'function') {
                cliente = global.ApiContactoMock.crearClienteMock();
                if (cliente && typeof cliente.configurar === 'function') {
                    cliente.configurar({ fallaNotificacion: false });
                }
                return cliente;
            }
        } catch (e) {
            // ignorar
        }

        try {
            cliente = global.ApiContacto.crearCliente({
                baseUrl: porId('base-url') ? porId('base-url').value || '' : ''
            });
            return cliente;
        } catch (e) {
            // último intento con mock
            if (global.ApiContactoMock && typeof global.ApiContactoMock.crearClienteMock === 'function') {
                cliente = global.ApiContactoMock.crearClienteMock();
                if (cliente && typeof cliente.configurar === 'function') {
                    cliente.configurar({ fallaNotificacion: false });
                }
                return cliente;
            }
        }
        return cliente;
    }

    function mostrarPaso(paso) {
        estado.paso = paso;
        ['paso-1', 'paso-2', 'paso-3'].forEach(function (id, indice) {
            var pasoElem = porId(id);
            if (pasoElem) {
                pasoElem.hidden = indice + 1 !== paso;
            }
            var indicador = porId('indicador-' + (indice + 1));
            if (indicador) {
                indicador.className = 'paso-indicador' +
                    (indice + 1 === paso ? ' activo' : '') +
                    (indice + 1 < paso ? ' completado' : '');
            }
        });
        var bloqueConfirmacion = porId('bloque-confirmacion');
        var bloqueResultado = porId('bloque-resultado');
        if (bloqueConfirmacion) {
            bloqueConfirmacion.hidden = false;
        }
        if (bloqueResultado) {
            bloqueResultado.hidden = true;
        }
        var foco = paso === 1 ? 'input-correo' : (paso === 2 ? 'input-codigo' : 'btn-confirmar');
        var elemento = porId(foco);
        if (elemento) {
            elemento.focus();
        }
    }

    function limpiarMensaje() {
        var contenedor = porId('mensaje');
        if (!contenedor) {
            return;
        }
        contenedor.hidden = true;
        contenedor.className = 'mensaje';
        contenedor.innerHTML = '';
    }

    function mostrarMensaje(tipo, titulo, detalle) {
        var contenedor = porId('mensaje');
        if (!contenedor) {
            return;
        }
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
        contenedor.hidden = false;
    }

    function mostrarError(error) {
        var esApi = error && error.esErrorApi;
        var codigo = esApi ? error.codigo : 'SYS-500';
        var mensajeDetalle = esApi
            ? error.message
            : 'Intente nuevamente. Si el problema continua, contacte al Contact Center.';

        var contenedor = porId('mensaje');
        if (!contenedor) {
            return;
        }
        contenedor.className = 'mensaje mensaje-' + (codigo === 'CD-409' ? 'advertencia' : 'error');
        contenedor.innerHTML = '';

        var encabezado = global.document.createElement('strong');
        encabezado.textContent = codigo === 'CD-409'
            ? 'Este correo ya está registrado'
            : (esApi ? 'No pudimos completar la operacion' : 'Ocurrio un error inesperado');
        contenedor.appendChild(encabezado);

        var parrafo = global.document.createElement('p');
        parrafo.textContent = codigo === 'CD-409'
            ? 'El correo que intentas registrar ya pertenece a otra cuenta. Prueba con uno diferente.'
            : mensajeDetalle;
        contenedor.appendChild(parrafo);

        if (codigo === 'CD-409') {
            var acciones = global.document.createElement('div');
            acciones.style.marginTop = '10px';
            var botonVolver = global.document.createElement('button');
            botonVolver.type = 'button';
            botonVolver.className = 'boton secundario';
            botonVolver.textContent = 'Volver al inicio';
            botonVolver.addEventListener('click', function () {
                reiniciar();
            });
            acciones.appendChild(botonVolver);
            contenedor.appendChild(acciones);
        }

        contenedor.hidden = false;
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
                    'Solicite un nuevo codigo para continuar con la validacion.');
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
            mostrarMensaje('error', 'Revise el correo ingresado', revision.mensaje);
            porId('input-correo').focus();
            return;
        }

        var afiliadoId = porId('input-afiliado').value.trim();
        if (!afiliadoId) {
            mostrarMensaje('error', 'Falta informacion',
                'No fue posible identificar la sesion del afiliado.');
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
            mostrarMensaje('error', 'Revise el codigo', revision.mensaje);
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
                    'La confirmacion se enviara en los proximos minutos. No es necesario repetir el proceso.');
            } else {
                mostrarMensaje('exito', 'Correo actualizado',
                    'Enviamos la confirmacion a ' + estado.correo + '.');
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
                'Le enviamos un nuevo codigo de verificacion.');
        }, mostrarError).then(function () {
            ocupado(boton, false);
        });
    }

    function reiniciar() {
        detenerContador();
        estado = estadoInicial();
        var inputCorreo = porId('input-correo');
        if (inputCorreo) {
            inputCorreo.value = '';
        }
        var inputCodigo = porId('input-codigo');
        if (inputCodigo) {
            inputCodigo.value = '';
        }
        var modoDemo = porId('modo-demo');
        if (modoDemo) {
            modoDemo.checked = true;
        }
        var optDemo = porId('opciones-demo');
        var optBack = porId('opciones-backend');
        if (optDemo) {
            optDemo.hidden = false;
        }
        if (optBack) {
            optBack.hidden = true;
        }
        limpiarMensaje();
        mostrarPaso(1);
        if (inputCorreo) {
            inputCorreo.focus();
        }
    }

    global.document.addEventListener('DOMContentLoaded', function () {
        var formCorreo = porId('form-correo');
        if (formCorreo) {
            formCorreo.addEventListener('submit', function (e) {
                e.preventDefault();
                enviarCodigo(e);
            });
        }

        var btnEnviar = porId('btn-enviar');
        if (btnEnviar) {
            btnEnviar.addEventListener('click', function (e) {
                enviarCodigo(e);
            });
        }

        var formCodigo = porId('form-codigo');
        if (formCodigo) {
            formCodigo.addEventListener('submit', function (e) {
                e.preventDefault();
                verificarCodigo(e);
            });
        }

        var btnVerificar = porId('btn-verificar');
        if (btnVerificar) {
            btnVerificar.addEventListener('click', function (e) {
                verificarCodigo(e);
            });
        }

        var btnConfirmar = porId('btn-confirmar');
        if (btnConfirmar) {
            btnConfirmar.addEventListener('click', confirmarActualizacion);
        }

        var btnReenviar = porId('btn-reenviar');
        if (btnReenviar) {
            btnReenviar.addEventListener('click', reenviarCodigo);
        }

        var btnReiniciar = porId('btn-reiniciar');
        if (btnReiniciar) {
            btnReiniciar.addEventListener('click', reiniciar);
        }

        var modoDemo = porId('modo-demo');
        if (modoDemo) {
            modoDemo.addEventListener('change', function () {
                var optDemo = porId('opciones-demo');
                var optBack = porId('opciones-backend');
                if (optDemo) {
                    optDemo.hidden = !modoDemo.checked;
                }
                if (optBack) {
                    optBack.hidden = modoDemo.checked;
                }
            });
        }

        var inputCodigo = porId('input-codigo');
        if (inputCodigo) {
            inputCodigo.addEventListener('input', function (evento) {
                evento.target.value = evento.target.value.replace(/[^0-9]/g, '').slice(0, 6);
            });
        }

        // Red de seguridad de la pantalla: ningun rechazo de promesa queda sin manejar.
        global.addEventListener('unhandledrejection', function (evento) {
            mostrarMensaje('error', 'No pudimos completar la operacion',
                'Intente nuevamente. Si el problema continua, contacte al Contact Center.');
            if (evento && typeof evento.preventDefault === 'function') {
                evento.preventDefault();
            }
        });

        mostrarPaso(1);
    });
})(typeof globalThis !== 'undefined' ? globalThis : this);
