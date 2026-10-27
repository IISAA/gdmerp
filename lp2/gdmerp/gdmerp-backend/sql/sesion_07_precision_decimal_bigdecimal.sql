-- ==================================================================================
-- GdmERP - Sesión 7: Migración de Precisión Decimal (12, 4) para Cadena Productiva
-- Arquitectura de Base de Datos Oracle (Oracle 19c / 21c / 23ai)
-- Esquemas objetivo: GDM_INVENTARIO y GDM_PRODUCCION
-- Usuario ejecutor: DBA (ej. SYSTEM o usuario con privilegios ALTER sobre los esquemas)
--
-- Objetivo:
-- Estandarizar todas las columnas de stock, cantidades de insumos, formulación y 
-- costos a NUMBER(12, 4) para soportar operaciones fraccionarias exactas sin pérdida 
-- de datos ni errores de redondeo.
-- ==================================================================================

-- ----------------------------------------------------------------------------------
-- 1. ESQUEMA GDM_INVENTARIO: Tabla PRODUCTOS
--    - STOCK_TOTAL: Precisión exacta para balanza y dosificación (kg, g, L, ml)
--    - STOCK_MINIMO: Umbral de alerta con soporte fraccionario
--    - COSTO_PROMEDIO: Valoración exacta de costo promedio ponderado
-- ----------------------------------------------------------------------------------
ALTER TABLE GDM_INVENTARIO.PRODUCTOS MODIFY (
    STOCK_TOTAL    NUMBER(12, 4) DEFAULT 0.0000 NOT NULL,
    STOCK_MINIMO   NUMBER(12, 4) DEFAULT 0.0000 NOT NULL,
    COSTO_PROMEDIO NUMBER(12, 4) DEFAULT 0.0000 NOT NULL
);

-- ----------------------------------------------------------------------------------
-- 2. ESQUEMA GDM_INVENTARIO: Tabla LOTES
--    - CANTIDAD_ACTUAL: Saldo disponible por lote para consumo FEFO fraccionario
-- ----------------------------------------------------------------------------------
ALTER TABLE GDM_INVENTARIO.LOTES MODIFY (
    CANTIDAD_ACTUAL NUMBER(12, 4) DEFAULT 0.0000 NOT NULL
);

-- ----------------------------------------------------------------------------------
-- 3. ESQUEMA GDM_INVENTARIO: Tabla MOVIMIENTOS_INVENTARIO (Kardex)
--    Crea la tabla si aún no existe, o modifica las columnas existentes a NUMBER(12, 4).
-- ----------------------------------------------------------------------------------
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM ALL_TABLES 
    WHERE OWNER = 'GDM_INVENTARIO' AND TABLE_NAME = 'MOVIMIENTOS_INVENTARIO';
    
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE TABLE GDM_INVENTARIO.MOVIMIENTOS_INVENTARIO (
                ID                NUMBER(19, 0) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                PRODUCTO_ID       NUMBER(19, 0) NOT NULL,
                TIPO_MOVIMIENTO   VARCHAR2(30 CHAR) NOT NULL,
                CANTIDAD          NUMBER(12, 4) NOT NULL,
                STOCK_ANTERIOR    NUMBER(12, 4) NOT NULL,
                STOCK_RESULTANTE  NUMBER(12, 4) NOT NULL,
                COSTO_UNITARIO    NUMBER(12, 4),
                FECHA_MOVIMIENTO  TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
                REFERENCIA        VARCHAR2(120 CHAR),
                CONSTRAINT FK_MOV_PRODUCTO FOREIGN KEY (PRODUCTO_ID)
                    REFERENCES GDM_INVENTARIO.PRODUCTOS (ID)
            )';
        EXECUTE IMMEDIATE 'GRANT SELECT, INSERT, UPDATE, DELETE ON GDM_INVENTARIO.MOVIMIENTOS_INVENTARIO TO GDMERP_APP';
    ELSE
        EXECUTE IMMEDIATE '
            ALTER TABLE GDM_INVENTARIO.MOVIMIENTOS_INVENTARIO MODIFY (
                CANTIDAD         NUMBER(12, 4) NOT NULL,
                STOCK_ANTERIOR   NUMBER(12, 4) NOT NULL,
                STOCK_RESULTANTE NUMBER(12, 4) NOT NULL,
                COSTO_UNITARIO   NUMBER(12, 4)
            )';
    END IF;
END;
/

-- ----------------------------------------------------------------------------------
-- 4. ESQUEMA GDM_PRODUCCION: Tabla CENTROS_TRABAJO
--    - CAPACIDAD: Capacidad operativa de planta o maquinaria (ej. kg/día, L/h)
-- ----------------------------------------------------------------------------------
ALTER TABLE GDM_PRODUCCION.CENTROS_TRABAJO MODIFY (
    CAPACIDAD NUMBER(12, 4)
);

-- ----------------------------------------------------------------------------------
-- 5. ESQUEMA GDM_PRODUCCION: Tabla ORDENES_PRODUCCION
--    - CANTIDAD_PLANIFICADA: Lote de producción planificado/fabricado fraccionario
-- ----------------------------------------------------------------------------------
ALTER TABLE GDM_PRODUCCION.ORDENES_PRODUCCION MODIFY (
    CANTIDAD_PLANIFICADA NUMBER(12, 4) NOT NULL
);

-- ----------------------------------------------------------------------------------
-- 6. ESQUEMA GDM_PRODUCCION: Tabla DETALLES_ORDEN_PRODUCCION
--    - CANTIDAD_REQUERIDA: Insumo fraccionario consumido en la orden
--    - COSTO_UNITARIO: Histórico unitario del insumo al momento del consumo
-- ----------------------------------------------------------------------------------
ALTER TABLE GDM_PRODUCCION.DETALLES_ORDEN_PRODUCCION MODIFY (
    CANTIDAD_REQUERIDA NUMBER(12, 4) NOT NULL,
    COSTO_UNITARIO     NUMBER(12, 4) NOT NULL
);

-- ----------------------------------------------------------------------------------
-- 7. ESQUEMA GDM_PRODUCCION: Tablas de Lista de Materiales (BOM)
--    Garantiza que la fórmula base y detalles soporten formulaciones fraccionarias
-- ----------------------------------------------------------------------------------
ALTER TABLE GDM_PRODUCCION.LISTAS_MATERIALES MODIFY (
    CANTIDAD_BASE NUMBER(12, 4) DEFAULT 1.0000 NOT NULL
);

ALTER TABLE GDM_PRODUCCION.DETALLES_LISTA_MATERIALES MODIFY (
    CANTIDAD_REQUERIDA NUMBER(12, 4) NOT NULL
);

-- ----------------------------------------------------------------------------------
-- 8. Permisos DML para el usuario de conexión de la aplicación (GDMERP_APP)
-- ----------------------------------------------------------------------------------
GRANT SELECT, INSERT, UPDATE, DELETE ON GDM_INVENTARIO.PRODUCTOS TO GDMERP_APP;
GRANT SELECT, INSERT, UPDATE, DELETE ON GDM_INVENTARIO.LOTES TO GDMERP_APP;
GRANT SELECT, INSERT, UPDATE, DELETE ON GDM_PRODUCCION.CENTROS_TRABAJO TO GDMERP_APP;
GRANT SELECT, INSERT, UPDATE, DELETE ON GDM_PRODUCCION.ORDENES_PRODUCCION TO GDMERP_APP;
GRANT SELECT, INSERT, UPDATE, DELETE ON GDM_PRODUCCION.DETALLES_ORDEN_PRODUCCION TO GDMERP_APP;
GRANT SELECT, INSERT, UPDATE, DELETE ON GDM_PRODUCCION.LISTAS_MATERIALES TO GDMERP_APP;
GRANT SELECT, INSERT, UPDATE, DELETE ON GDM_PRODUCCION.DETALLES_LISTA_MATERIALES TO GDMERP_APP;

COMMIT;
