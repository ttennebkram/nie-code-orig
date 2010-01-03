
package nie.filters.io;

import java.io.*;
import java.util.*;
import org.jdom.*;

public class ErrWriter extends PrintWriter {
    Element errElement;
    String curText = "";
    boolean showWarnings = true;
    
    public ErrWriter(Element errElement) {
        this(errElement, true);
    }
    
    public ErrWriter(Element errElement, boolean showWarnings) {
        super(System.err);
        this.errElement = errElement;
        this.showWarnings = showWarnings;
    }
    
    /** Assume flush means check the current text */
    public void flush() {
        println();
    }
    
    /** Don't do anything on a close request */
    public void close() {
        
    }
    
    /** Not going to be any errors... */
    public boolean checkError() {
        return false;
    }

    /** Add a string to the current text */
    protected void addString(String s) {
        curText += s;
    }
    
    /** Write a single character. */
    public void write(int c) {
        addString(String.valueOf(c));
    }

    /** Write a portion of an array of characters. */
    public void write(char buf[], int off, int len) {
        addString(new String(buf, off, len));
    }

    /** Write a portion of a string. */
    public void write(String s, int off, int len) {
        addString(s.substring(off, len));
    }

    /** Newline means we're done with the entry... parse it for the error */
    public void println() {
        parseCurText();
        curText = "";
    }
    
    /** Parse the current text for an error message.   This is currently specific to the
        html/tidy error scheme, but could be adapted or overridden for other schemes */
    protected void parseCurText() {
        StringTokenizer st = new StringTokenizer(curText);

        Element err = new Element("exception");
        String token;
        boolean foundLine = false;
        while (st.hasMoreTokens()) {
            token = st.nextToken();
            
            if (token.equals("line")) {
                // The next token should be the line number
                err.setAttribute("line", st.nextToken());
                foundLine = true;
            } else if (token.equals("column")) {
                // The next token should be the column number
                err.setAttribute("column", st.nextToken());
            } else if (token.equalsIgnoreCase("warning:")) {
                err.setAttribute("type", "warning");
                if (!this.showWarnings) {
                    foundLine = false;
                    break;
                }
            } else if (token.equalsIgnoreCase("error:")) {
                err.setAttribute("type", "error");
            } else {
                err.addContent(token + " ");
            }
        }
        
        // If it's not an exception (didn't find a line), don't add it
        if (foundLine) {
            this.errElement.addContent(err);
        }
    }
}
