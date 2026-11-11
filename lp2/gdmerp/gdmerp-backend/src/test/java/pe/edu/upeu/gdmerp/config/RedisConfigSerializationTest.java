package pe.edu.upeu.gdmerp.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import pe.edu.upeu.gdmerp.inventario.almacen.dto.AlmacenResponse;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RedisConfigSerializationTest {

    @Test
    void serializadorRedisSoportaRecordsJava21YListasTipadasSinClassCastException() {
        GenericJacksonJsonRedisSerializer serializer = RedisConfig.createJsonSerializer();

        AlmacenResponse unico = new AlmacenResponse(1L, "Almacén Central", "Lima - Planta A", new BigDecimal("500.0000"));
        byte[] bytesUnico = serializer.serialize(unico);
        Object recuperadoUnico = serializer.deserialize(bytesUnico);

        AlmacenResponse castUnico = assertInstanceOf(AlmacenResponse.class, recuperadoUnico);
        assertEquals(1L, castUnico.id());
        assertEquals("Almacén Central", castUnico.nombre());
        assertEquals(0, new BigDecimal("500.0000").compareTo(castUnico.capacidad()));

        List<AlmacenResponse> lista = List.of(
                unico,
                new AlmacenResponse(2L, "Almacén Secundario", "Lima - Planta B", new BigDecimal("250.0000"))
        );
        byte[] bytesLista = serializer.serialize(lista);
        Object recuperadaLista = serializer.deserialize(bytesLista);

        List<?> castLista = assertInstanceOf(List.class, recuperadaLista);
        assertEquals(2, castLista.size());
        AlmacenResponse primero = assertInstanceOf(AlmacenResponse.class, castLista.get(0));
        AlmacenResponse segundo = assertInstanceOf(AlmacenResponse.class, castLista.get(1));
        assertNotNull(primero);
        assertEquals("Almacén Central", primero.nombre());
        assertEquals("Almacén Secundario", segundo.nombre());
    }
}
