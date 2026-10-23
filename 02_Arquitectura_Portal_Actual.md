# Arquitectura Portal Digital - Estado actual

Area responsable: Equipo Portal Digital  
Fecha del documento: 2025-11-10  
Estado: Vigente para operacion actual

## Plataforma actual
El portal de afiliados opera con componentes sobre PortalX 7.0. Los modulos principales asociados a autogestion son:

- Certificados.
- PQRS.
- Mi Perfil.
- Actualizacion de Datos.

## Modulo Actualizacion de Datos
El modulo utiliza componentes web modulares existentes del portal y consume servicios de integracion para consultar y actualizar informacion del afiliado.

## Consideraciones tecnicas registradas
- Existen dependencias con componentes legados.
- Algunas vistas usan librerias JavaScript incluidas en el portal actual.
- Los cambios sobre modulos legados requieren pruebas de regresion.
- Los empaquetamientos se realizan con el procedimiento vigente de portal.
