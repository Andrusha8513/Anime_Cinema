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
    public ResponseEntity<ErrorResponse> handleUserExists(UserAlreadyExistsException ex , HttpServletRequest request){
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("USER_EXISTS" , ex.getMessage(), LocalDateTime.now() , request.getRequestURI()));
    }
}
