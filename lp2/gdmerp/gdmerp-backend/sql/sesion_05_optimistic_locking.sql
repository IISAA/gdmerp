-- GdmERP - Sesión 5: Bloqueo optimista (@Version) para control de concurrencia de stock
-- Migración: el proyecto NO usa Flyway/Liquibase; los scripts se ejecutan manualmente.
-- Ejecutar como usuario con privilegios sobre el esquema GDM_INVENTARIO.
--
-- Se agrega la columna VERSION a las dos tablas que se modifican al consumir o
-- ingresar stock:
--   * GDM_INVENTARIO.PRODUCTOS : stock_total se actualiza en cada operación.
--   * GDM_INVENTARIO.LOTES     : cantidad_actual se actualiza en el consumo FEFO.
--
-- DEFAULT 0 + NOT NULL:
--   Hibernate garantiza que la versión nunca sea null y que los registros
--   existentes queden inicializados en 0 sin migrar datos.

ALTER TABLE GDM_INVENTARIO.PRODUCTOS ADD (
    VERSION NUMBER(10, 0) DEFAULT 0 NOT NULL
);

ALTER TABLE GDM_INVENTARIO.LOTES ADD (
    VERSION NUMBER(10, 0) DEFAULT 0 NOT NULL
);