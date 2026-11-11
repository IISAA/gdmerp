package pe.edu.upeu.gdmerp.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import tools.jackson.databind.jsontype.impl.DefaultTypeResolverBuilder;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;

/**
 * Configuración de caché distribuida Redis exclusiva para datos maestros de baja mutabilidad.
 * Desactivada automáticamente en perfiles donde {@code spring.cache.type} sea distinto de {@code redis}.
 */
@Configuration
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class RedisConfig {

    private static final Duration TTL_POR_DEFECTO = Duration.ofMinutes(15);
    private static final Duration TTL_CATALOGOS_MAESTROS = Duration.ofMinutes(30);
    private static final Duration TTL_CENTROS_TRABAJO = Duration.ofMinutes(20);

    @Bean
    public RedisCacheConfiguration defaultRedisCacheConfiguration() {
        return construirConfiguracionBase(TTL_POR_DEFECTO);
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(
            RedisCacheConfiguration defaultRedisCacheConfiguration) {
        Map<String, RedisCacheConfiguration> cachePolicies = Map.of(
                "almacenes", construirConfiguracionBase(TTL_CATALOGOS_MAESTROS),
                "categorias", construirConfiguracionBase(TTL_CATALOGOS_MAESTROS),
                "unidadesMedida", construirConfiguracionBase(TTL_CATALOGOS_MAESTROS),
                "centrosTrabajo", construirConfiguracionBase(TTL_CENTROS_TRABAJO)
        );
        return builder -> builder
                .cacheDefaults(defaultRedisCacheConfiguration)
                .withInitialCacheConfigurations(cachePolicies);
    }

    public static GenericJacksonJsonRedisSerializer createJsonSerializer() {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("pe.edu.upeu.gdmerp.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.math.")
                .allowIfSubType("java.time.")
                .allowIfSubType("org.springframework.cache.support.")
                .build();
        DefaultTypeResolverBuilder typeResolver = new DefaultTypeResolverBuilder(
                ptv,
                DefaultTyping.NON_FINAL_AND_RECORDS,
                JsonTypeInfo.As.PROPERTY,
                JsonTypeInfo.Id.CLASS,
                "@class"
        ) {
            @Override
            public DefaultTypeResolverBuilder withDefaultImpl(Class<?> defaultImpl) {
                return this;
            }

            @Override
            public boolean useForType(JavaType t) {
                if (t.isPrimitive()) {
                    return false;
                }
                JavaType base = _unwrapArrayType(_unwrapReferenceType(t));
                if (Collection.class.isAssignableFrom(base.getRawClass())
                        || Map.class.isAssignableFrom(base.getRawClass())) {
                    return true;
                }
                return super.useForType(t);
            }
        };
        return GenericJacksonJsonRedisSerializer.builder()
                .enableSpringCacheNullValueSupport()
                .customize(builder -> builder
                        .configure(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY, false)
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .setDefaultTyping(typeResolver))
                .build();
    }

    private RedisCacheConfiguration construirConfiguracionBase(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(createJsonSerializer()));
    }
}
