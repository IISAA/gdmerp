package pe.edu.upeu.gdmerp.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import pe.edu.upeu.gdmerp.inventario.almacen.dto.AlmacenResponse;
import pe.edu.upeu.gdmerp.inventario.categoria.dto.CategoriaResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RedisSerializationTest {

    @Test
    void testSerializationAlmacenList() {
        GenericJacksonJsonRedisSerializer serializer = RedisConfig.createJsonSerializer();
        List<AlmacenResponse> list = List.of(
                new AlmacenResponse(1L, "Almacen Central", "Calle 1", new BigDecimal("100.0000"))
        );

        byte[] bytes = serializer.serialize(list);
        assertNotNull(bytes);
        System.out.println("JSON Almacenes: " + new String(bytes));

        Object deserialized = serializer.deserialize(bytes);
        assertNotNull(deserialized);
        System.out.println("Deserialized type: " + deserialized.getClass());
    }

    @Test
    void testSerializationCategoriaList() {
        GenericJacksonJsonRedisSerializer serializer = RedisConfig.createJsonSerializer();
        List<CategoriaResponse> list = List.of(
                new CategoriaResponse(1L, "Derivados", "Desc")
        );

        byte[] bytes = serializer.serialize(list);
        assertNotNull(bytes);
        System.out.println("JSON Categorias: " + new String(bytes));

        Object deserialized = serializer.deserialize(bytes);
        assertNotNull(deserialized);
        System.out.println("Deserialized type: " + deserialized.getClass());
    }
}
