
package nie.filters.pdf;

import nie.filters.*;

/**
 * Interface representing PDF constants.   All PDF parsing classes should "implement" this interface
 * 
 * @author Steve Halliburton for NIE
 * @version 0.9
 */
public interface PDFConst extends ParserConstants {


    /**
     * Amount to look back each sucessive time until the startxref tag is found.
     */
    final int PDF_STARTXREF_INCR = 100;   

    /**
     * Number of times to try looking further back in the file for the startxref tag
     */
    final int PDF_STARTXREF_MAXINCR = 11;

    /**
     * Not used?
     */
    final int PDF_MAXTOKEN_LINES = -1;
    
    /**
     * The token signifying the file trailer
     */
    final String PDF_TOKEN_TRAILER = "trailer";

    /**
     * The token signifying the start of a cross reference table
     */
    final String PDF_TOKEN_XREF = "xref";

    /**
     * Token signifying the initial startxref
     */
    final String PDF_TOKEN_STARTXREF = "startxref";

    /**
     * Toekn signifying the end of an object definition
     */
    final String PDF_TOKEN_ENDOBJ = "endobj";
    final String PDF_TOKEN_STARTOBJ = " obj";

    /**
     * Token signifying the type of a page tree node
     */
    final String PDF_TOKEN_PAGETREENODE = "Pages";

    /**
     * Token signifying the type of a page leaf node
     */
    final String PDF_TOKEN_PAGETREEOBJ = "Page";

    /**
     * Token signifying the start of a content stream
     */
    final String PDF_TOKEN_STREAMSTART = "stream";

    /**
     * Token signifying the end of a content stream
     */
    final String PDF_TOKEN_STREAMEND = "endstream";

    /**
     * Token signifying the end of the document
     */
    final String PDF_TOKEN_EOF = "%%EOF";

    /**
     * An array of all characters considered by PDF to be whitespace
     */
    final char[] PDF_WHITESPACE = {' ', '\t', '\r', '\n'};
    
    final String PDF_ERR_EOF = "Unexpected end of file";
    final String PDF_ERR_IOERROR = "IOError";
    final String PDF_ERR_FORMATERR = "Format error";
}

