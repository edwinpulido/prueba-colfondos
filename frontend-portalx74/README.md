# Componente de actualizacion de contacto (PortalX 7.4)

Componente de interfaz para la HU-001. No depende de librerias del portal legado ni de un
proceso de compilacion: se integra como recurso estatico del portal.

## Ejecutar

Abrir `index.html` en el navegador. El modo demostracion viene activo, por lo que funciona
sin backend.

Para probar contra el orquestador:

1. Desmarcar "Usar API simulada".
2. Confirmar la URL del backend (por defecto `http://localhost:8081`).

## Datos de demostracion

| Escenario | Valor |
|---|---|
| Codigo correcto | `123456` |
| Codigo expirado | `999999` |
| Correo duplicado | `duplicado@dominio.com` |
| Falla de notificacion | Casilla del panel de prueba |

## Estructura

| Archivo | Responsabilidad |
|---|---|
| `index.html` | Estructura de los tres pasos y panel de prueba |
| `styles.css` | Estilos aislados, sin afectar el portal |
| `src/compat.js` | Respaldos para APIs no disponibles en todos los navegadores |
| `src/validaciones.js` | Reglas de correo y codigo |
| `src/api.js` | Cliente HTTP con timeout, `correlationId` e idempotencia |
| `src/mock-api.js` | API simulada con el mismo contrato |
| `src/componente.js` | Controlador de la pantalla |

## Pruebas

```bash
node --test "tests/*.test.cjs"
```

Cubren validaciones, utilidades de compatibilidad, cliente HTTP (error, timeout, respuesta
sin cuerpo) y flujo completo contra la API simulada.

## Decisiones de compatibilidad

- No se usan APIs sin respaldo (`crypto.randomUUID`, `structuredClone`, `AbortController`
  opcional, formato de fecha regional).
- Toda promesa tiene manejador de rechazo y existe una red de seguridad global.
- El cuerpo de la respuesta se lee como texto antes de interpretarlo como JSON.
- Los mensajes de error se muestran por codigo, con `correlationId` para soporte.

## Integracion con el portal

1. Publicar la carpeta como recurso estatico del portal 7.4.
2. Registrar el componente en la vista "Mi Perfil" o en la ruta definida por el portal.
3. Activar con la bandera de funcionalidad para permitir rollback inmediato.
4. Retirar el panel de prueba local en el empaquetamiento de produccion.
