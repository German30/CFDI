package mx.culturafiscal.atipicos.common.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiError(LocalDateTime timestamp, int status, String error, String mensaje, Map<String, String> detalles) {
    
}
