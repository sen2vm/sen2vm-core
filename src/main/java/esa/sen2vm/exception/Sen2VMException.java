package esa.sen2vm.exception;

/**
 * Sen2VMException
 */
public class Sen2VMException extends Exception {

    private static final long serialVersionUID = 1L;

	public Sen2VMException() {
        super();
    }

    /**
     * Constructor with cause
     * @param e cause
     */
    public Sen2VMException(Exception e) {
        super(e.getLocalizedMessage(), e);
    }

    public Sen2VMException(String message) throws Sen2VMException {
        super(message);
    }

    /**
     * Constructor with message and caused exception
     * @param msg the error message
     * @param e exception cause
     */
    public Sen2VMException(String msg, Exception e) {
        super(msg, e);
    }

}
