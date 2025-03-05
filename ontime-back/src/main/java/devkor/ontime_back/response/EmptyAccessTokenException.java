package devkor.ontime_back.response;

public class EmptyAccessTokenException extends InvalidTokenException {
    public EmptyAccessTokenException(String message) {
        super(message);
    }
}
