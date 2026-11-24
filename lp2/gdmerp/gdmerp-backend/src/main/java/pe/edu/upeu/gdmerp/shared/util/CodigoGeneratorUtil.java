package pe.edu.upeu.gdmerp.shared.util;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class CodigoGeneratorUtil {

    private static final int MAX_INTENTOS_COLISION = 10_000;

    private static final Set<String> CONECTORES = Set.of(
            "DE", "EL", "LA", "LOS", "LAS", "PARA", "Y", "DEL", "AL", "EN", "CON");

    private CodigoGeneratorUtil() {
        // Clase utilitaria: no instanciable
    }

    public static String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        return codigo.trim().toUpperCase();
    }

    public static String extraerCodigo(String texto, String valorPorDefecto, int maxIniciales) {
        if (valorPorDefecto == null || valorPorDefecto.isBlank()) {
            throw new IllegalArgumentException("El valor por defecto no puede ser nulo o vacío");
        }
        if (maxIniciales < 1) {
            throw new IllegalArgumentException("maxIniciales debe ser mayor o igual a 1");
        }

        String filtrado = limpiarYFiltrarConectores(texto);
        if (filtrado.isBlank()) {
            return valorPorDefecto.trim().toUpperCase();
        }

        String[] palabras = filtrado.split("\\s+");
        if (palabras.length >= 2) {
            StringBuilder sb = new StringBuilder();
            int limite = Math.min(palabras.length, maxIniciales);
            for (int i = 0; i < limite; i++) {
                sb.append(palabras[i].charAt(0));
            }
            return sb.toString();
        }

        String palabra = palabras[0];
        return palabra.length() >= 3
                ? palabra.substring(0, 3)
                : String.format("%-3s", palabra).replace(' ', 'X');
    }

    /**
     * Genera un código correlativo único garantizando que no colisione en BD.
     *
     * @param prefijo      Prefijo ya armado por el módulo (ej. "APF-ALM-BPCC" o
     *                     "BOM-BPCC")
     * @param conteoActual Cantidad actual de registros con ese prefijo (>= 0)
     * @param digitos      Cantidad de dígitos del correlativo (ej. 4 -> 0001, 2 ->
     *                     01)
     * @param existeEnBd   Función del repositorio del módulo para verificar
     *                     colisión
     */
    public static String generarCorrelativoUnico(
            String prefijo,
            long conteoActual,
            int digitos,
            Predicate<String> existeEnBd) {

        if (prefijo == null || prefijo.isBlank()) {
            throw new IllegalArgumentException("El prefijo del código no puede ser nulo o vacío");
        }
        if (conteoActual < 0) {
            throw new IllegalArgumentException("El conteo actual no puede ser negativo");
        }
        if (digitos < 1 || digitos > 10) {
            throw new IllegalArgumentException("La cantidad de dígitos debe estar entre 1 y 10");
        }
        Objects.requireNonNull(existeEnBd, "La función de verificación en BD es obligatoria");

        String prefijoLimpio = prefijo.trim().toUpperCase();
        String formato = "%s-%0" + digitos + "d";
        long correlativo = conteoActual + 1;

        for (int intento = 0; intento < MAX_INTENTOS_COLISION; intento++) {
            String candidato = String.format(formato, prefijoLimpio, correlativo);
            if (!existeEnBd.test(candidato)) {
                return candidato;
            }
            correlativo++;
        }

        throw new IllegalStateException(
                "No se pudo generar un código único para el prefijo '" + prefijoLimpio + "' tras "
                        + MAX_INTENTOS_COLISION + " intentos");
    }

    public static String limpiarYFiltrarConectores(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase()
                .replaceAll("[^A-Z0-9 ]", " ")
                .trim();

        return Arrays.stream(normalizado.split("\\s+"))
                .filter(w -> !w.isBlank() && !CONECTORES.contains(w))
                .collect(Collectors.joining(" "));
    }
}