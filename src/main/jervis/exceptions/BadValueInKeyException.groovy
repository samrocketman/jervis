package jervis.exceptions


class BadValueInKeyException extends ValidationException
{
    def BadValueInKeyException(String message) {
        super("Bad value in key: " + message)
    }
}
