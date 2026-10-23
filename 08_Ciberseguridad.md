# Lineamientos de Ciberseguridad para canales digitales

Area responsable: Ciberseguridad  
Fecha del documento: 2026-03-12  
Estado: Vigente

## Aplicaciones expuestas a internet
Las aplicaciones o componentes expuestos a internet deben pasar por revision de ciberseguridad y escaneo de codigo seguro antes de salida a produccion.

Tiempo promedio de escaneo: 3 dias habiles.

## Controles minimos
- TLS 1.2 o superior.
- Gestion segura de secretos.
- Validacion de entradas.
- Proteccion contra vulnerabilidades OWASP Top 10.
- Registro de eventos de seguridad.

## Datos biometricos
No se deben almacenar fotografias, videos o imagenes faciales en los componentes del canal digital. Para efectos de trazabilidad transaccional se permite conservar identificadores de transaccion, resultado, proveedor, fecha y hora.

## Documentacion requerida
Para iniciativas con validacion de identidad se debe diligenciar el formato de evaluacion de ciberseguridad con flujos, datos tratados, componentes expuestos, controles y responsables.
