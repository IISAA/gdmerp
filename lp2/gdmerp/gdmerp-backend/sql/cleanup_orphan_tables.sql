-- =============================================================================
-- GdmERP - Script de Limpieza de Tablas Huérfanas y Consolidación de Catálogo
-- Base de Datos: Oracle Database 23ai / 19c (PDB: FREEPDB1)
-- Usuario Ejecutor: GDMERP_APP (o SYSTEM con privilegios DBA)
-- 
-- Reglas de Negocio Aplicadas:
-- 1. Única Fuente de Verdad (Single Source of Truth) para Producto en GDM_CATALOGO.
-- 2. Migración segura de productos y stock desde GDM_INVENTARIO antes del drop.
-- 3. Eliminación de tablas huérfanas sin entidad JPA:
--    - GDM_INVENTARIO.PRODUCTOS (eliminado a favor de GDM_CATALOGO.PRODUCTOS)
--    - GDM_FINANZAS.ASIGNACIONES_CAJA_CENTRO_COSTO (eliminado a favor de CajaChica.centroCostoId)
--    - Tablas duplicadas residuales en GDMERP_APP (creadas previamente sin schema)
-- 4. Preservación estricta de GDMERP_APP.EVENT_PUBLICATION (Spring Modulith).
-- =============================================================================

SET SERVEROUTPUT ON SIZE UNLIMITED;
SET DEFINE OFF;

-- -----------------------------------------------------------------------------
-- PASO 1: Migrar datos maestros de productos a GDM_CATALOGO.PRODUCTOS si aún no existen
-- -----------------------------------------------------------------------------
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM ALL_TABLES 
    WHERE OWNER = 'GDM_INVENTARIO' AND TABLE_NAME = 'PRODUCTOS';

    IF v_count > 0 THEN
        DBMS_OUTPUT.PUT_LINE('===> Migrando productos de GDM_INVENTARIO a GDM_CATALOGO.PRODUCTOS...');
        
        FOR r IN (
            SELECT p.ID, p.SKU, p.NOMBRE, p.TIPO_PRODUCTO, p.UNIDAD_MEDIDA,
                   p.COSTO_PROMEDIO, p.STOCK_MINIMO, p.STOCK_TOTAL, p.CATEGORIA_ID
            FROM GDM_INVENTARIO.PRODUCTOS p
            WHERE NOT EXISTS (
                SELECT 1 FROM GDM_CATALOGO.PRODUCTOS c WHERE c.ID = p.ID OR c.SKU = p.SKU
            )
        ) LOOP
            INSERT INTO GDM_CATALOGO.PRODUCTOS (
                ID, SKU, NOMBRE, TIPO_PRODUCTO, UNIDAD_MEDIDA, COSTO_BASE, PRECIO,
                STOCK, STOCK_MINIMO, ACTIVO, ID_CATEGORIA, DESCRIPCION
            ) VALUES (
                r.ID,
                r.SKU,
                r.NOMBRE,
                r.TIPO_PRODUCTO,
                r.UNIDAD_MEDIDA,
                r.COSTO_PROMEDIO,
                CASE WHEN r.TIPO_PRODUCTO = 'TERMINADO' THEN 10.00 ELSE ROUND(r.COSTO_PROMEDIO * 1.30, 2) END,
                r.STOCK_TOTAL,
                r.STOCK_MINIMO,
                TRUE,
                r.CATEGORIA_ID,
                r.NOMBRE
            );
            DBMS_OUTPUT.PUT_LINE('  + Producto migrado: ' || r.SKU || ' (' || r.NOMBRE || ')');
        END LOOP;
        COMMIT;
    ELSE
        DBMS_OUTPUT.PUT_LINE('===> GDM_INVENTARIO.PRODUCTOS ya no existe. Omitiendo migración.');
    END IF;
END;
/

-- -----------------------------------------------------------------------------
-- PASO 2: Población de existencias en GDM_INVENTARIO.STOCK_INVENTARIO
-- -----------------------------------------------------------------------------
DECLARE
    v_inv_prod_exists NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_inv_prod_exists FROM ALL_TABLES 
    WHERE OWNER = 'GDM_INVENTARIO' AND TABLE_NAME = 'PRODUCTOS';

    IF v_inv_prod_exists > 0 THEN
        DBMS_OUTPUT.PUT_LINE('===> Verificando stock inicial en GDM_INVENTARIO.STOCK_INVENTARIO...');
        FOR r IN (
            SELECT p.ID AS PRODUCTO_ID, p.ALMACEN_ID, p.CATEGORIA_ID, 
                   p.STOCK_TOTAL, p.STOCK_MINIMO, p.COSTO_PROMEDIO
            FROM GDM_INVENTARIO.PRODUCTOS p
            WHERE NOT EXISTS (
                SELECT 1 FROM GDM_INVENTARIO.STOCK_INVENTARIO s 
                WHERE s.PRODUCTO_ID = p.ID AND s.ALMACEN_ID = p.ALMACEN_ID
            )
        ) LOOP
            INSERT INTO GDM_INVENTARIO.STOCK_INVENTARIO (
                PRODUCTO_ID, ALMACEN_ID, CATEGORIA_ID, STOCK_ACTUAL, STOCK_MINIMO, COSTO_PROMEDIO, VERSION
            ) VALUES (
                r.PRODUCTO_ID,
                r.ALMACEN_ID,
                r.CATEGORIA_ID,
                r.STOCK_TOTAL,
                r.STOCK_MINIMO,
                r.COSTO_PROMEDIO,
                0
            );
            DBMS_OUTPUT.PUT_LINE('  + Stock registrado para Producto ID: ' || r.PRODUCTO_ID || ' en Almacén: ' || r.ALMACEN_ID);
        END LOOP;
        COMMIT;
    END IF;
END;
/

-- -----------------------------------------------------------------------------
-- PASO 3: Consolidar Centro de Costo en GDM_FINANZAS.CAJAS_CHICAS
-- -----------------------------------------------------------------------------
DECLARE
    v_asig_exists NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_asig_exists FROM ALL_TABLES 
    WHERE OWNER = 'GDM_FINANZAS' AND TABLE_NAME = 'ASIGNACIONES_CAJA_CENTRO_COSTO';

    IF v_asig_exists > 0 THEN
        DBMS_OUTPUT.PUT_LINE('===> Consolidando centro de costo en GDM_FINANZAS.CAJAS_CHICAS...');
        
        -- Asignar el primer centro de costo activo si la caja chica no tiene uno configurado
        UPDATE GDM_FINANZAS.CAJAS_CHICAS c
        SET c.CENTRO_COSTO_ID = (
            SELECT MIN(a.CENTRO_COSTO_ID) 
            FROM GDM_FINANZAS.ASIGNACIONES_CAJA_CENTRO_COSTO a 
            WHERE a.CAJA_CHICA_ID = c.ID
        )
        WHERE c.CENTRO_COSTO_ID IS NULL
          AND EXISTS (
            SELECT 1 FROM GDM_FINANZAS.ASIGNACIONES_CAJA_CENTRO_COSTO a 
            WHERE a.CAJA_CHICA_ID = c.ID
        );

        DBMS_OUTPUT.PUT_LINE('  + Cajas chicas actualizadas con centro_costo_id: ' || SQL%ROWCOUNT);
        COMMIT;
    END IF;
END;
/

-- -----------------------------------------------------------------------------
-- PASO 4: Eliminar FKs residuales en GDM_INVENTARIO que apuntaban a GDM_INVENTARIO.PRODUCTOS
-- -----------------------------------------------------------------------------
DECLARE
    PROCEDURE drop_constraint_safe(p_owner VARCHAR2, p_table VARCHAR2, p_constraint VARCHAR2) IS
    BEGIN
        EXECUTE IMMEDIATE 'ALTER TABLE ' || p_owner || '.' || p_table || ' DROP CONSTRAINT ' || p_constraint;
        DBMS_OUTPUT.PUT_LINE('  - FK eliminada: ' || p_owner || '.' || p_table || '.' || p_constraint);
    EXCEPTION
        WHEN OTHERS THEN
            NULL;
    END;
BEGIN
    DBMS_OUTPUT.PUT_LINE('===> Eliminando constraints residuales apuntando a GDM_INVENTARIO.PRODUCTOS...');
    FOR c IN (
        SELECT owner, table_name, constraint_name
        FROM all_constraints
        WHERE r_constraint_name IN (
            SELECT constraint_name 
            FROM all_constraints 
            WHERE owner = 'GDM_INVENTARIO' AND table_name = 'PRODUCTOS'
        )
    ) LOOP
        drop_constraint_safe(c.owner, c.table_name, c.constraint_name);
    END LOOP;
END;
/

-- -----------------------------------------------------------------------------
-- PASO 5: Eliminar Tablas Huérfanas de GDM_INVENTARIO y GDM_FINANZAS
-- -----------------------------------------------------------------------------
DECLARE
    PROCEDURE drop_table_safe(p_owner VARCHAR2, p_table VARCHAR2) IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_count FROM ALL_TABLES WHERE OWNER = p_owner AND TABLE_NAME = p_table;
        IF v_count > 0 THEN
            EXECUTE IMMEDIATE 'DROP TABLE ' || p_owner || '.' || p_table || ' CASCADE CONSTRAINTS PURGE';
            DBMS_OUTPUT.PUT_LINE('  - Tabla eliminada: ' || p_owner || '.' || p_table);
        END IF;
    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('  ! Error al eliminar ' || p_owner || '.' || p_table || ': ' || SQLERRM);
    END;
BEGIN
    DBMS_OUTPUT.PUT_LINE('===> Eliminando tablas huérfanas en GDM_INVENTARIO y GDM_FINANZAS...');
    drop_table_safe('GDM_INVENTARIO', 'PRODUCTOS');
    drop_table_safe('GDM_FINANZAS', 'ASIGNACIONES_CAJA_CENTRO_COSTO');
END;
/

-- -----------------------------------------------------------------------------
-- PASO 6: Eliminar Tablas Residuales/Duplicadas en el esquema GDMERP_APP
-- (Conservando únicamente EVENT_PUBLICATION requerida por Spring Modulith)
-- -----------------------------------------------------------------------------
DECLARE
    PROCEDURE drop_table_safe(p_owner VARCHAR2, p_table VARCHAR2) IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_count FROM ALL_TABLES WHERE OWNER = p_owner AND TABLE_NAME = p_table;
        IF v_count > 0 THEN
            EXECUTE IMMEDIATE 'DROP TABLE ' || p_owner || '.' || p_table || ' CASCADE CONSTRAINTS PURGE';
            DBMS_OUTPUT.PUT_LINE('  - Tabla eliminada en GDMERP_APP: ' || p_table);
        END IF;
    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('  ! Error al eliminar ' || p_owner || '.' || p_table || ': ' || SQLERRM);
    END;
BEGIN
    DBMS_OUTPUT.PUT_LINE('===> Eliminando tablas duplicadas residuales en GDMERP_APP...');
    drop_table_safe('GDMERP_APP', 'ABONOS_CUENTA_POR_COBRAR');
    drop_table_safe('GDMERP_APP', 'ASIGNACIONES_CAJA_CENTRO_COSTO');
    drop_table_safe('GDMERP_APP', 'CAJAS_CHICAS');
    drop_table_safe('GDMERP_APP', 'CAJA_CHICA_CUSTODIOS');
    drop_table_safe('GDMERP_APP', 'CATEGORIAS_FINANCIERAS');
    drop_table_safe('GDMERP_APP', 'CENTROS_COSTO');
    drop_table_safe('GDMERP_APP', 'CUENTAS_POR_COBRAR');
    drop_table_safe('GDMERP_APP', 'DENOMINACIONES_MONEDA');
    drop_table_safe('GDMERP_APP', 'DETALLES_ARQUEO_EFECTIVO');
    drop_table_safe('GDMERP_APP', 'DETALLES_RENDICION_CAJA_CHICA');
    drop_table_safe('GDMERP_APP', 'MOVIMIENTOS_CAJA');
    drop_table_safe('GDMERP_APP', 'RENDICIONES_CAJA_CHICA');
    drop_table_safe('GDMERP_APP', 'SESIONES_CAJA');
    drop_table_safe('GDMERP_APP', 'TRANSACCIONES_FINANCIERAS');
END;
/

-- -----------------------------------------------------------------------------
-- PASO 7: Limpiar tablas temporales de Hibernate (HTE_*) si existen
-- -----------------------------------------------------------------------------
DECLARE
BEGIN
    DBMS_OUTPUT.PUT_LINE('===> Limpiando tablas temporales residuales de Hibernate (HTE_*)...');
    FOR t IN (
        SELECT owner, table_name 
        FROM all_tables 
        WHERE owner LIKE 'GDM%' AND table_name LIKE 'HTE_%'
    ) LOOP
        BEGIN
            EXECUTE IMMEDIATE 'DROP TABLE ' || t.owner || '.' || t.table_name || ' CASCADE CONSTRAINTS PURGE';
            DBMS_OUTPUT.PUT_LINE('  - Temporal eliminada: ' || t.owner || '.' || t.table_name);
        EXCEPTION
            WHEN OTHERS THEN NULL;
        END;
    END LOOP;
END;
/

PROMPT
PROMPT =============================================================================
PROMPT Resumen final de tablas activas por esquema en Oracle:
PROMPT =============================================================================
COLUMN owner FORMAT A20;
COLUMN table_name FORMAT A35;

SELECT owner, table_name 
FROM all_tables 
WHERE owner IN ('GDM_CATALOGO','GDM_COMPRAS','GDM_CRM','GDM_VENTAS','GDM_INVENTARIO','GDM_PRODUCCION','GDM_FINANZAS','GDMERP_APP') 
ORDER BY owner, table_name;

EXIT;
