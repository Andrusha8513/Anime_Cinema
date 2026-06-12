package ara.exeption;

public class InvalidRegistrationTokenException extends RuntimeException {
    public InvalidRegistrationTokenException(String message) {
        super(message);
    }

    public InvalidRegistrationTokenException(String message , Throwable cause){
        super(message , cause);
    }
}
