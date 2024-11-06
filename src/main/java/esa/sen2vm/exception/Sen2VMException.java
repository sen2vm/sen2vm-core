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
     * Constructor with exception cause
     * @param e exception cause
     */
    public Sen2VMException(Exception e) {
        super(e.getLocalizedMessage(), e);
    }

    /**
     * Constructor with message
     * @param msg the error message
     */
    public Sen2VMException(String msg) {
        super(msg);
    }

    /**
     * Constructor with message and exception cause
     * @param msg the error message
     * @param e exception cause
     */
    public Sen2VMException(String msg, Exception e) {
        super(msg, e);
    }
}
