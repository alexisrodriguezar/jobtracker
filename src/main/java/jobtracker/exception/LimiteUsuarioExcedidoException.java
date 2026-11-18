package jobtracker.exception;

public class LimiteUsuarioExcedidoException extends RuntimeException {
    public LimiteUsuarioExcedidoException(String message) {
        super(message);
    }
}