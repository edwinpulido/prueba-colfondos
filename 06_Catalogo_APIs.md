# Catalogo de APIs Corporativas

Area responsable: Integracion Empresarial  
Fecha del documento: 2025-11-18  
Estado: En revision

## Customer Data API
Descripcion: API utilizada para consultar informacion basica del afiliado y gestionar datos de contacto.

Capacidades documentadas:
- Consulta de perfil del afiliado.
- Actualizacion de datos de contacto.
- Consulta de estado general del afiliado.

Ambientes registrados:
- Desarrollo.
- QA.
- Produccion.

Observaciones:
La actualizacion de datos de contacto requiere una validacion previa de identidad. Este documento no describe el flujo exacto de validacion.

## Identity Capability API
Descripcion: API utilizada en procesos digitales que requieren validacion de identidad.

Capacidades documentadas:
- Generacion de codigo de verificacion.
- Validacion de codigo de verificacion.
- Registro de resultado de validacion.

Observaciones:
El catalogo no incluye detalle de contrato para validacion facial. Existen referencias en documentos de integracion a capacidades de validacion de identidad.

## Messaging API
Descripcion: API utilizada para envio de notificaciones transaccionales.

Canales documentados:
- Correo electronico.
- SMS.

Observaciones:
No se incluyen en este catalogo tiempos garantizados de entrega ni politica de reintentos.
