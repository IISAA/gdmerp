package pe.edu.upeu.gdmerp.inventario.unidadmedida;

import pe.edu.upeu.gdmerp.inventario.unidadmedida.entity.CategoriaUnidadMedida;

import java.math.BigDecimal;
import java.util.List;

/**
 * Catálogo oficial SI / UN-ECE / SUNAT de unidades de medida precargadas.
 * <p>
 * Fuente de verdad única para el seeder y el endpoint de sincronización.
 * Toda nueva unidad estándar se agrega SOLO aquí.
 * </p>
 */
public final class CatalogoEstandarUnidades {

    private CatalogoEstandarUnidades() {}

    /**
     * Registro inmutable que describe una unidad de medida a insertar.
     *
     * @param codigo          Código interno único (PK lógica). Se normaliza a UPPER_CASE.
     * @param codigoSunat     Código UN/ECE requerido por SUNAT para e-facturación.
     * @param nombre          Denominación oficial.
     * @param simbolo         Símbolo tipográfico estándar.
     * @param categoria       Magnitud física.
     * @param factorConversion Factor respecto a la unidad base de su categoría.
     * @param permiteDecimales {@code true} si la unidad admite fracciones en formularios.
     */
    public record UnidadSeed(
            String codigo,
            String codigoSunat,
            String nombre,
            String simbolo,
            CategoriaUnidadMedida categoria,
            BigDecimal factorConversion,
            boolean permiteDecimales
    ) {}

    /** Lista canónica de las 18 unidades internacionales y agroindustriales. */
    public static List<UnidadSeed> getCatalogo() {
        return List.of(

                // ─── 1. MASA  (Base: KG = 1.0000) ───────────────────────────────────────
                seed("KG",  "KGM", "Kilogramo",        "kg",   CategoriaUnidadMedida.MASA,          "1.0000", true),
                seed("GR",  "GRM", "Gramo",             "g",    CategoriaUnidadMedida.MASA,          "0.0010", true),
                seed("MG",  "MGM", "Miligramo",         "mg",   CategoriaUnidadMedida.MASA,          "0.0001", true),
                seed("TON", "TNE", "Tonelada Métrica",  "t",    CategoriaUnidadMedida.MASA,       "1000.0000", true),
                seed("QQ",  "QQM", "Quintal (46 kg)",   "qq",   CategoriaUnidadMedida.MASA,         "46.0000", true),

                // ─── 2. LONGITUD  (Base: M = 1.0000) ────────────────────────────────────
                seed("M",   "MTR", "Metro",             "m",    CategoriaUnidadMedida.LONGITUD,      "1.0000", true),
                seed("CM",  "CMT", "Centímetro",        "cm",   CategoriaUnidadMedida.LONGITUD,      "0.0100", true),
                seed("MM",  "MMT", "Milímetro",         "mm",   CategoriaUnidadMedida.LONGITUD,      "0.0010", true),

                // ─── 3. VOLUMEN  (Base: LT = 1.0000) ────────────────────────────────────
                seed("LT",  "LTR", "Litro",             "L",    CategoriaUnidadMedida.VOLUMEN,       "1.0000", true),
                seed("ML",  "MLT", "Mililitro",         "ml",   CategoriaUnidadMedida.VOLUMEN,       "0.0010", true),
                seed("GAL", "GLL", "Galón (US)",        "gal",  CategoriaUnidadMedida.VOLUMEN,       "3.7854", true),

                // ─── 4. SUPERFICIE  (Base: M2 = 1.0000) ─────────────────────────────────
                seed("M2",  "MTK", "Metro Cuadrado",    "m²",   CategoriaUnidadMedida.SUPERFICIE,    "1.0000", true),

                // ─── 5. UNIDAD_EMPAQUE  (Discretas) ─────────────────────────────────────
                seed("UND", "NIU", "Unidad",            "und",  CategoriaUnidadMedida.UNIDAD_EMPAQUE,"1.0000", false),
                seed("CAJ", "BX",  "Caja",              "caj",  CategoriaUnidadMedida.UNIDAD_EMPAQUE,"1.0000", false),
                seed("SAC", "BG",  "Saco",              "sac",  CategoriaUnidadMedida.UNIDAD_EMPAQUE,"1.0000", false),
                seed("ROL", "RO",  "Rollo / Bobina",    "rol",  CategoriaUnidadMedida.UNIDAD_EMPAQUE,"1.0000", true),
                seed("PAQ", "PK",  "Paquete",           "paq",  CategoriaUnidadMedida.UNIDAD_EMPAQUE,"1.0000", false),
                seed("PAR", "PF",  "Parihuela / Pallet","par",  CategoriaUnidadMedida.UNIDAD_EMPAQUE,"1.0000", false)
        );
    }

    /** Número mínimo de registros esperados en la tabla. Si hay menos, se re-sincroniza. */
    public static final int UMBRAL_MINIMO = 10;

    // ── helper privado para reducir verbosidad ────────────────────────────────
    private static UnidadSeed seed(String codigo, String sunat, String nombre,
                                   String simbolo, CategoriaUnidadMedida cat,
                                   String factor, boolean decimales) {
        return new UnidadSeed(codigo, sunat, nombre, simbolo, cat,
                new BigDecimal(factor), decimales);
    }
}
