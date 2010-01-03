
package nie.filters.pdf;

/**
 * Base exception class for PDF based errors
 * 
 * @author Steve Halliburton for NIE
 * @version 0.9
 * @see Exception
 */
public class PDFException extends Exception {

    /**
     * The error message
     */
    String msg;

    /**
     * The name of the exception.   Should be overridden in derived classes
     */
    String excName = "PDFException";

    /**
     * The position within the file where the error occurred
     */
    int filePos = -1;
    
    /**
     * Basic constructor for an unknown error.   Should rarely be used.
     */
    public PDFException() {
        this("Unknown error", -1);
    }

    /**
     * Error with a string message
     * 
     * @param msg The error message
     */
    public PDFException(String msg) {
        this(msg, -1);
    }

    /**
     * Error with a string message and a file position of where the error occurred
     * 
     * @param msg The error message
     * @param filePos The position within the file where the error occurred
     */
    public PDFException(String msg, int filePos) {
        this.msg = msg;
        this.filePos = filePos;
    }

    /**
     * Return a string representation of the error message
     */
    public String toString() {
        String s = new String(this.excName + "[" + this.msg);
        if (this.filePos >= 0) {
            s += "(" + this.filePos + ")";
        }
        s += "]";
        return s;
    }
}
        
