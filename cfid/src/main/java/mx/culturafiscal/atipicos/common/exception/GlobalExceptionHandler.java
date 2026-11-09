package mx.culturafiscal.atipicos.common.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ApiError> r(HttpStatus s, String m, Map<String, String> d) {
        return ResponseEntity.status(s).body(new ApiError(LocalDateTime.now(), s.value(), s.getReasonPhrase(), m, d));
    }

    @ExceptionHandler({RecursoNoEncontradoException.class, NoResourceFoundException.class})
    ResponseEntity<ApiError> noEncontrado(Exception e) { return r(HttpStatus.NOT_FOUND, e.getMessage(), null); }

    @ExceptionHandler(ReglaNegocioException.class)
    ResponseEntity<ApiError> negocio(ReglaNegocioException e) { return r(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), null); }

    @ExceptionHandler(CfdiInvalidoException.class)
    ResponseEntity<ApiError> cfdi(CfdiInvalidoException e) { return r(HttpStatus.BAD_REQUEST, e.getMessage(), null); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validacion(MethodArgumentNotValidException e) {
        Map<String, String> d = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> d.put(f.getField(), f.getDefaultMessage()));
        return r(HttpStatus.BAD_REQUEST, "Datos inválidos", d);
    }

    @ExceptionHandler(DisabledException.class)
    ResponseEntity<ApiError> inactivo(DisabledException e) { return r(HttpStatus.UNAUTHORIZED, "Usuario inactivo", null); }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> auth(AuthenticationException e) { return r(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", null); }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> denegado(AccessDeniedException e) { return r(HttpStatus.FORBIDDEN, "Acceso denegado", null); }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> tamano(MaxUploadSizeExceededException e) { return r(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo excede el tamaño permitido", null); }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> general(Exception e) {
        log.error("Error no controlado", e);
        return r(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", null);
    }
}
