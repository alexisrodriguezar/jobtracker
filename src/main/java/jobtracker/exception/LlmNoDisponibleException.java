package jobtracker.exception;

public class LlmNoDisponibleException extends RuntimeException {
    public LlmNoDisponibleException(String message) {
        super(message);
    }
}