package devkor.ontime_back.response;

public class InvalidAccessTokenException extends InvalidTokenException{
    public InvalidAccessTokenException(String message) {
        super(message);
    }
}
