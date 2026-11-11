package pe.edu.upeu.gdmerp.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Manejador global de excepciones de GdmERP bajo el estándar RFC 9457 ({@link ProblemDetail}).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatusException(
            ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = resolveStatus(ex.getStatusCode());
        String detail = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        return buildProblem(status, status.getReasonPhrase(), detail, request, null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return buildProblem(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage(), request, null);
    }

    @ExceptionHandler(BusinessConflictException.class)
    public ResponseEntity<ProblemDetail> handleBusinessConflict(
            BusinessConflictException ex, HttpServletRequest request) {
        return buildProblem(HttpStatus.CONFLICT, "Conflicto de negocio", ex.getMessage(), request, null);
    }

    @ExceptionHandler(VentaYaAnuladaException.class)
    public ResponseEntity<ProblemDetail> handleVentaYaAnulada(
            VentaYaAnuladaException ex, HttpServletRequest request) {
        return buildProblem(HttpStatus.CONFLICT, "Comprobante ya anulado", ex.getMessage(), request, null);
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ProblemDetail> handleAccesoDenegado(
            AccesoDenegadoException ex, HttpServletRequest request) {
        return buildProblem(HttpStatus.FORBIDDEN, "Acceso denegado", ex.getMessage(), request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleSecurityAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Acceso denegado por Spring Security/Keycloak en [{}]: {}",
                obtenerUri(request), ex.getMessage());
        return buildProblem(
                HttpStatus.FORBIDDEN,
                "Permisos insuficientes",
                "No cuentas con los roles o privilegios necesarios para realizar esta operación",
                request,
                null
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        return buildProblem(
                HttpStatus.UNAUTHORIZED,
                "Autenticación requerida",
                "Token de acceso inválido, expirado o ausente",
                request,
                null
        );
    }

    // Regla de Negocio: Solvencia / saldo insuficiente en caja chica o rendiciones (RN-FIN-01).
    @ExceptionHandler(SolvenciaInsuficienteException.class)
    public ResponseEntity<ProblemDetail> handleSolvenciaInsuficiente(
            SolvenciaInsuficienteException ex, HttpServletRequest request) {
        return buildProblem(HttpStatus.BAD_REQUEST, "Solvencia insuficiente", ex.getMessage(), request, null);
    }

    // Regla de Negocio: Stock Físico insuficiente para consumos de recetas / órdenes / ventas.
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ProblemDetail> handleInsufficientStock(
            InsufficientStockException ex, HttpServletRequest request) {
        return buildProblem(HttpStatus.BAD_REQUEST, "Stock insuficiente", ex.getMessage(), request, null);
    }

    // Reglas de negocio: incumplimiento de una invariante de dominio.
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleBusinessRule(
            IllegalStateException ex, HttpServletRequest request) {
        return buildProblem(HttpStatus.CONFLICT, "Estado inválido para la operación", ex.getMessage(), request, null);
    }

    // Colisión de bloqueo optimista no resuelta tras agotar los reintentos del service.
    @ExceptionHandler(StockConcurrencyException.class)
    public ResponseEntity<ProblemDetail> handleStockConcurrency(
            StockConcurrencyException ex, HttpServletRequest request) {
        return buildProblem(HttpStatus.CONFLICT, "Concurrencia de inventario", ex.getMessage(), request, null);
    }

    // Colisión de bloqueo optimista (@Version) captada directamente por Spring Data JPA.
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLocking(
            OptimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("Colisión de bloqueo optimista: {}", ex.getMessage());
        return buildProblem(
                HttpStatus.CONFLICT,
                "Conflicto de concurrencia",
                "El registro fue modificado por otra operación concurrente. Intente nuevamente",
                request,
                null
        );
    }

    // Datos inválidos detectados en el servicio sin pasar por Bean Validation.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleInvalidArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return buildProblem(HttpStatus.BAD_REQUEST, "Argumento inválido", ex.getMessage(), request, null);
    }

    // Falla de Bean Validation en @RequestBody con @Valid.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return buildProblem(
                HttpStatus.BAD_REQUEST,
                "Error de validación",
                "Errores de validación en la petición",
                request,
                errors
        );
    }

    // Falla de Bean Validation en @PathVariable / @RequestParam (@Validated).
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        List<String> errors = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();
        return buildProblem(
                HttpStatus.BAD_REQUEST,
                "Error de validación de parámetros",
                "Errores de validación en la petición",
                request,
                errors
        );
    }

    // Duplicados (ORA-00001 / UNIQUE) o violaciones de integridad referencial (ORA-02291/ORA-02292 / FK).
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        Throwable rootCause = NestedExceptionUtils.getMostSpecificCause(ex);
        String rawMessage = rootCause != null ? rootCause.getMessage() : ex.getMessage();
        String detail = resolverMensajeIntegridad(rawMessage);
        log.warn("Violación de integridad de datos en [{}]: {}", obtenerUri(request), rawMessage);
        return buildProblem(HttpStatus.CONFLICT, "Violación de integridad de datos", detail, request, null);
    }

    // JSON malformado o con tipos incompatibles.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildProblem(
                HttpStatus.BAD_REQUEST,
                "Cuerpo de petición inválido",
                "El cuerpo JSON de la petición no es legible o contiene tipos incompatibles",
                request,
                null
        );
    }

    // @PathVariable / @RequestParam con tipo incorrecto.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return buildProblem(
                HttpStatus.BAD_REQUEST,
                "Tipo de parámetro inválido",
                "El valor del parámetro '" + ex.getName() + "' no es válido",
                request,
                null
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        return buildProblem(
                HttpStatus.BAD_REQUEST,
                "Parámetro obligatorio faltante",
                "Falta el parámetro requerido '" + ex.getParameterName() + "'",
                request,
                null
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return buildProblem(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Método HTTP no permitido",
                "El método '" + ex.getMethod() + "' no está soportado para este endpoint",
                request,
                null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGlobalException(
            Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en [{}]", obtenerUri(request), ex);
        return buildProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor",
                "Error interno del servidor",
                request,
                null
        );
    }

    private ResponseEntity<ProblemDetail> buildProblem(
            HttpStatus status,
            String title,
            String detail,
            HttpServletRequest request,
            Object errors) {
        String safeDetail = detail != null ? detail : status.getReasonPhrase();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, safeDetail);
        problem.setTitle(title);
        if (request != null && request.getRequestURI() != null && !request.getRequestURI().isBlank()) {
            problem.setInstance(URI.create(request.getRequestURI()));
        }
        problem.setProperty("timestamp", LocalDateTime.now());
        problem.setProperty("error", status.getReasonPhrase());
        problem.setProperty("message", safeDetail);
        String correlationId = MDC.get("correlationId");
        if (correlationId != null && !correlationId.isBlank()) {
            problem.setProperty("correlationId", correlationId);
        }
        if (errors != null) {
            problem.setProperty("errors", errors);
        }
        return ResponseEntity.status(status).body(problem);
    }

    private Map<String, String> toFieldError(FieldError fieldError) {
        Map<String, String> error = new LinkedHashMap<>();
        error.put("campo", fieldError.getField());
        error.put("mensaje", fieldError.getDefaultMessage());
        error.put("field", fieldError.getField());
        error.put("defaultMessage", fieldError.getDefaultMessage());
        return error;
    }

    private String resolverMensajeIntegridad(String rawMessage) {
        if (rawMessage == null) {
            return "No se puede completar la operación: existe un dato duplicado o el registro está en uso";
        }
        String normalized = rawMessage.toLowerCase(Locale.ROOT);
        if (normalized.contains("ora-00001")
                || normalized.contains("unique constraint")
                || normalized.contains("unique index")
                || normalized.contains("23505")
                || normalized.contains("duplicate")) {
            return "No se puede completar la operación: ya existe un registro con el mismo identificador o valor único";
        }
        if (normalized.contains("ora-02291")
                || normalized.contains("ora-02292")
                || normalized.contains("foreign key")
                || normalized.contains("referential integrity")
                || normalized.contains("23503")) {
            return "No se puede completar la operación: el registro tiene dependencias activas o referencia un elemento inexistente";
        }
        if (normalized.contains("ora-01400") || normalized.contains("not-null") || normalized.contains("23502")) {
            return "No se puede completar la operación: falta un campo obligatorio en base de datos";
        }
        return "No se puede completar la operación: existe un dato duplicado o el registro está en uso";
    }

    private HttpStatus resolveStatus(HttpStatusCode statusCode) {
        HttpStatus resolved = HttpStatus.resolve(statusCode.value());
        return resolved != null ? resolved : HttpStatus.BAD_REQUEST;
    }

    private String obtenerUri(HttpServletRequest request) {
        return request != null ? request.getRequestURI() : "N/A";
    }
}
