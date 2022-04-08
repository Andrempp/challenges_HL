package utils;

/**The convenience exception, which can be used to wrap up checked general {@link Exception}.
 * @author Rui Henriques
 */
public class BicException extends RuntimeException {

	private static final long serialVersionUID = -2784759893280383334L;

	/**Creates a new instance of with detail mesage.
     * @param message the detail message
     */
    public BicException(String message) {
        super(message);
    }

    /**Creates a new instance of with detail message and nested exception.
     * @param message the detail message
     * @param cause the nested exception
     */
    public BicException(String message, Throwable cause) {
        super(message, cause);
    }
}
