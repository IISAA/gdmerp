package pe.edu.upeu.gdmerp.exception;

/**
 * Excepción de negocio lanzada ante violaciones de invariantes o transiciones
 * inválidas en el ciclo de vida de los agregados DDD.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
