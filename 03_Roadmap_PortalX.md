# Roadmap PortalX Canales Digitales

Area responsable: Arquitectura Canales  
Fecha del documento: 2026-05-30  
Estado: Lineamiento objetivo

## Evolucion tecnologica
La estrategia de canales digitales contempla evolucionar capacidades del portal hacia PortalX 7.4.

## Lineamientos
- Nuevas capacidades digitales deben priorizar componentes desacoplados y reutilizables.
- Cuando sea viable, las nuevas funcionalidades deben construirse sobre PortalX 7.4.
- Las dependencias con componentes 7.0 deben identificarse durante el analisis tecnico.
- Las migraciones deben considerar compatibilidad funcional, navegadores y regresion.

## Riesgos recurrentes
- Dependencias no documentadas en componentes antiguos.
- Librerias JavaScript incompatibles entre navegadores.
- Diferencias de empaquetamiento y despliegue.
- Falta de evidencias de regresion sobre Safari.
