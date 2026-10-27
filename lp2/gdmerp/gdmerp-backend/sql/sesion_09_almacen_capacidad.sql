-- GdmERP - Sesión 9: Añadir campo capacidad a la tabla ALMACENES
-- Migración: el proyecto NO usa Flyway/Liquibase; los scripts se ejecutan manualmente.
-- Ejecutar como usuario con privilegios sobre el esquema GDM_INVENTARIO.
--
-- Se agrega la columna CAPACIDAD con precisión decimal estándar (NUMBER(12, 4))
-- para registrar el aforo volumétrico o métrico (m3, ton o ubicaciones).

ALTER TABLE GDM_INVENTARIO.ALMACENES ADD capacidad NUMBER(12, 4);

-- Comentario descriptivo en el diccionario de datos de Oracle
COMMENT ON COLUMN GDM_INVENTARIO.ALMACENES.capacidad IS 'Capacidad máxima de almacenamiento (volumen, peso o unidades métricas estandarizadas)';
