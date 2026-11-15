package jobtracker.exception;

public class LlmRespuestaInvalidaException extends RuntimeException {
    public LlmRespuestaInvalidaException(String message) {
        super(message);
    }
}