package pe.edu.upeu.gdmerp.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // Reglas de negocio: incumplimiento de una invariante (p.ej. stock insuficiente).
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRule(IllegalStateException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Colisión de bloqueo optimista no resuelta tras agotar los reintentos del service.
    @ExceptionHandler(StockConcurrencyException.class)
    public ResponseEntity<Map<String, Object>> handleStockConcurrency(StockConcurrencyException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Colisión de bloqueo optimista (@Version) captada directamente por Spring Data JPA.
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLocking(OptimisticLockingFailureException ex) {
        log.warn("Colisión de bloqueo optimista: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT,
                "El registro fue modificado por otra operación concurrente. Intente nuevamente");
    }

    // Datos inválidos detectados en el servicio (cantidades, fechas), sin pasar por Bean Validation.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidArgument(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // Falla de Bean Validation en @RequestBody con @Valid.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> body = buildBody(HttpStatus.BAD_REQUEST, "Errores de validación en la petición");
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        body.put("errors", errors);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // Falla de Bean Validation en @PathVariable / @RequestParam (@Validated).
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> errors = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();
        Map<String, Object> body = buildBody(HttpStatus.BAD_REQUEST, "Errores de validación en la petición");
        body.put("errors", errors);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // Duplicados (unique) o relaciones en uso (FK) captadas a nivel de persistencia.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        return buildResponse(HttpStatus.CONFLICT,
                "No se puede completar la operación: existe un dato duplicado o el registro está en uso");
    }

    // JSON malformado o con tipos incompatibles.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido");
    }

    // @PathVariable / @RequestParam con tipo incorrecto.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "El valor del parámetro '" + ex.getName() + "' no es válido");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(Exception ex) {
        log.error("Error no controlado", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }

    private Map<String, String> toFieldError(FieldError fieldError) {
        Map<String, String> error = new HashMap<>();
        error.put("campo", fieldError.getField());
        error.put("mensaje", fieldError.getDefaultMessage());
        return error;
    }

    private Map<String, Object> buildBody(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("correlationId", MDC.get("correlationId"));
        return body;
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        return new ResponseEntity<>(buildBody(status, message), status);
    }
}