package devkor.ontime_back.response;

public class InvalidRefreshTokenException extends InvalidTokenException{
    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
