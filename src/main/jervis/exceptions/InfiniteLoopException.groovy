package jervis.exceptions


class InfiniteLoopException extends ValidationException
{
    def InfiniteLoopException(String message) {
        super("Infinite loop detected.  Last known key: " + message)
    }
}
