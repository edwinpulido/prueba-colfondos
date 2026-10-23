# E6 - Incidente Safari 17

## 1. Reporte de QA

- Navegador: Safari 17.
- Flujo: confirmacion de actualizacion de datos de contacto.
- Sintoma: la API responde HTTP 200, pero la pantalla no avanza y la consola muestra
  `Promise rejected - Unhandled JavaScript exception`.
- Chrome y Edge: funcionan sin novedad.

## 2. Reproduccion

1. Cuenta de prueba `AFI-000123`, correo `ana@dominio.com`.
2. Completar OTP con `123456`.
3. Pulsar "Confirmar actualizacion".
4. Observar la consola y la pestaña Network.

Condiciones controladas:

- Safari 17 estable, ventana privada y cache limpia.
- El mismo `transactionId` se prueba despues en Chrome para comparar.
- Se conserva el `X-Correlation-ID` de la peticion.

Evidencia a registrar: video de la reproduccion, HAR de la sesion, captura de consola y
`correlationId`. El backend debe mostrar la transaccion en estado `APLICADA`, lo que
confirmaria que el fallo esta en el navegador y no en el servicio.

## 3. Hipotesis

| # | Hipotesis | Como se comprueba | Resultado esperado |
|---|---|---|---|
| H-01 | Promesa rechazada sin `catch` en el manejador de la confirmacion | Revisar la cadena de la promesa y agregar `unhandledrejection` temporal | La consola muestra el stack real |
| H-02 | Uso de `crypto.randomUUID` no disponible | Ejecutar `typeof crypto.randomUUID` en la consola de Safari | `undefined` en versiones sin soporte |
| H-03 | `structuredClone` no disponible | `typeof structuredClone` | `undefined` en navegadores antiguos |
| H-04 | `Response.json()` sobre cuerpo vacio o 204 | Revisar Network y el codigo de lectura | Excepcion de parseo |
| H-05 | Formato de fecha dependiente de la region | Comparar `toLocaleString` en los tres navegadores | Salida distinta |
| H-06 | Libreria del portal 7.0 empaquetada de forma distinta | Comparar el bundle servido a Safari | Version distinta del componente |

Se ordenan por probabilidad segun el antecedente INC-001, que ya registro un fallo de
JavaScript en Safari con APIs exitosas.

## 4. Aislamiento

1. **Biseccion funcional:** se separa "llamar al API" de "pintar el resultado". Con la
   respuesta simulada (200 y cuerpo conocido), el fallo se mantiene: el problema esta en el
   manejo del resultado, no en la red.
2. **Instrumentacion:** se agrega `window.onunhandledrejection` temporal para capturar el
   stack. El error apunta a la confirmacion, no al formulario previo.
3. **Prueba de navegador:** se ejecuta el mismo componente en Chrome y Edge; el flujo
   termina. Se confirma que es una diferencia de motor.
4. **Prueba de API disponible:** se evalua `crypto.randomUUID`, `structuredClone` y
   `AbortController` en Safari. Los dos primeros no estan disponibles en el entorno
   reportado.
5. **Correccion por partes:** se aplican respaldos uno a uno y se repite el flujo hasta
   que la consola queda limpia.

## 5. Causa raiz

Concurren dos causas, coherentes con el sintoma "API 200 + excepcion no manejada":

1. El componente usaba APIs de JavaScript no disponibles en el entorno de Safari
   (`crypto.randomUUID`, `structuredClone`), lo que producia una excepcion antes de
   terminar de pintar la confirmacion.
2. La promesa de la confirmacion no tenia un manejador de rechazo en el camino de error,
   por lo que el fallo se reportaba como `Unhandled JavaScript exception` y la pantalla
   quedaba bloqueada aunque el backend hubiera respondido 200.

## 6. Correccion aplicada

### 6.1 Utilidades con respaldo (`src/compat.js`)

```javascript
function generarId() {
    if (global.crypto && typeof global.crypto.randomUUID === 'function') {
        return global.crypto.randomUUID();
    }
    // Respaldo con getRandomValues o Math.random, formato UUID v4
}

function clonar(valor) {
    if (typeof global.structuredClone === 'function') {
        return global.structuredClone(valor);
    }
    return JSON.parse(JSON.stringify(valor));
}
```

### 6.2 Lectura de respuesta tolerante

`src/api.js` lee el cuerpo como texto y solo intenta `JSON.parse` si hay contenido. Una
respuesta 200 sin cuerpo ya no genera excepcion.

### 6.3 Timeout sin depender de `AbortController`

La peticion compite con un temporizador propio. Si el navegador no cancela la peticion al
abortar, el flujo igual recibe `GEN-408` y muestra un mensaje.

### 6.4 Manejo explicito de promesas

```javascript
Compat.ejecutarSeguro(function () {
    return cliente.aplicar(estado.transactionId, estado.correo, estado.idempotencyKey);
}).then(function (respuesta) {
    // pintar resultado
}, mostrarError).then(function () {
    ocupado(boton, false);
});
```

Ademas se registra un manejador global `unhandledrejection` como red de seguridad de la
pantalla: muestra el mensaje controlado y evita que el flujo quede bloqueado.

### 6.5 Formato de fecha estable

`Compat.formatoFecha` construye la fecha con `dd/mm/aaaa hh:mm` sin depender de la
configuracion regional del navegador.

## 7. Pruebas asociadas

| Prueba | Archivo | Que verifica |
|---|---|---|
| `generarId funciona sin crypto.randomUUID` | `frontend-portalx74/tests/compat-safari.test.cjs` | Respaldo de identificadores |
| `clonar funciona sin structuredClone` | idem | Respaldo de clonado |
| `formatoFecha no depende de la configuracion regional` | idem | Formato estable |
| `soporta respuestas 200 sin cuerpo` | `frontend-portalx74/tests/api.test.cjs` | Lectura tolerante |
| `aplica timeout cuando el servicio no responde` | idem | Timeout sin `AbortController` |
| `no duplica la actualizacion con la misma llave` | `backend/.../ActualizacionContactoServiceTest` | Reintento seguro |
| Regresion manual Chrome, Edge y Safari | Pendiente en laboratorio QA | Flujo completo |

## 8. Estado de la evidencia

- Ajuste implementado y cubierto por pruebas automaticas que simulan la ausencia de las
  APIs conflictivas.
- La ejecucion en Safari 17 real debe hacerse en el laboratorio de QA. Evidencia a
  adjuntar: video antes/despues, HAR, consola limpia y `transactionId` de la transaccion.
- No se declara el caso como cerrado hasta contar con esa evidencia.

## 9. Prevencion

1. Incluir Safari en la matriz de regresion de cada liberacion del portal.
2. Revisar en el PR cualquier API nueva de JavaScript y su soporte en los tres navegadores.
3. Mantener el manejador global de rechazos como red de seguridad, no como solucion.
4. Medir errores por navegador en observabilidad (documento 11).
5. Registrar el incidente en la base de conocimiento con la causa y el ajuste.
