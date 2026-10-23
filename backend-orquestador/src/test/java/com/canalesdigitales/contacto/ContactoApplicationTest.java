package com.canalesdigitales.contacto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class ContactoApplicationTest {

    @Autowired
    private ApplicationContext contexto;

    @Test
    @DisplayName("El contexto de la aplicacion carga con la configuracion por defecto")
    void cargaContexto() {
        assertNotNull(contexto.getBean(com.canalesdigitales.contacto.service.ActualizacionContactoService.class));
        assertNotNull(contexto.getBean(com.canalesdigitales.contacto.api.ActualizacionContactoController.class));
    }
}
