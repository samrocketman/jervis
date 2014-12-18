package jervis.exceptions


class MissingKeyException extends ValidationException
{
    def MissingKeyException(String message) {
        super("Missing key: " + message)
    }
}
