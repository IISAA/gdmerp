package pe.edu.upeu.gdmerp.exception;

/**
 * Se lanza cuando una operación sobre el stock (consumo FEFO o ingreso de lote)
 * no se pudo completar tras agotar los reintentos disponibles, a causa de una
 * colisión de bloqueo optimista (@Version) con otra transacción concurrente.
 */
public class StockConcurrencyException extends RuntimeException {

    public StockConcurrencyException(String message) {
        super(message);
    }

    public StockConcurrencyException(String message, Throwable cause) {
        super(message, cause);
    }
}