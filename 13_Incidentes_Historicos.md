# Incidentes historicos anonimizados

Area responsable: Operacion Digital  
Fecha del documento: 2026-01-30  
Estado: Base de conocimiento

## INC-001 - Safari
Descripcion: En una liberacion anterior, un grupo de usuarios Safari no logro finalizar un flujo del portal.
Sintoma: error JavaScript en navegador. APIs respondian exitosamente.
Causa raiz registrada: uso de funcionalidad JavaScript no soportada de forma consistente en Safari.
Accion aplicada: ajuste del componente, polyfill y pruebas de compatibilidad.

## INC-002 - Timeout Integracion
Descripcion: Flujo digital presentaba errores intermitentes.
Sintoma: timeout en capa de integracion.
Causa raiz registrada: dependencia sincrona con proveedor externo.
Accion aplicada: manejo de reintentos y ajuste de timeout.

## INC-003 - Codigo de verificacion
Descripcion: Usuarios no lograban finalizar validacion.
Sintoma: codigo expirado antes de finalizar el flujo.
Accion aplicada: ajuste de expiracion y mensajes de error.

## INC-004 - Despliegue nocturno
Descripcion: Rollback durante ventana de despliegue.
Sintoma: variable de ambiente faltante.
Accion aplicada: actualizacion de checklist.

## INC-005 - Proveedor de identidad
Descripcion: Degradacion de proveedor externo.
Sintoma: alta latencia y errores de validacion.
Accion aplicada: monitoreo, comunicacion preventiva y alternativa operativa temporal.
