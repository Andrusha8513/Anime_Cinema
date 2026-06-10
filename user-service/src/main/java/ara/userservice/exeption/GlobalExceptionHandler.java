package ara.userservice.exeption;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {
 @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex ,
                                                                 HttpServletRequest request){
     ErrorResponse body = ErrorResponse.of(
             "USER_EXISTS",
             ex.getMessage(),
             request.getRequestURI()
     );
     return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
 }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException ex ,
                                                                     HttpServletRequest request){
     ErrorResponse body = ErrorResponse.of(
             "USER_NOT_FOUND",
             ex.getMessage(),
             request.getRequestURI()
     );
     return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex ,
                                                                     HttpServletRequest request){
     ErrorResponse body = ErrorResponse.of(
             "BAD_REQUEST",
             ex.getMessage(),
             request.getRequestURI()
     );
     return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex,
                                                         HttpServletRequest request){
     ErrorResponse body = ErrorResponse.of(
             "INTERNAL_ERROR",
             "Внутрення ошибка сервера" + ex.getMessage(),
             request.getRequestURI()
     );
     return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

}
