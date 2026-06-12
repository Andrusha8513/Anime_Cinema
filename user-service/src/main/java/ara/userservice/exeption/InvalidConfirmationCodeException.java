package ara.userservice.exeption;

public class InvalidConfirmationCodeException extends RuntimeException {
    public InvalidConfirmationCodeException(String message) {
        super(message);
    }
}
