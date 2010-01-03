
package nie.filters.pdf;

import java.util.*;
import nie.filters.PDFParser;
import nie.filters.io.*;

/**
 * PDF parsing format exception.
 * 
 * @author Steve Halliburton for NIE
 * @version 0.9
 * @see PDFException
 */
class FormatException extends PDFException {
    String excName = "FormatException";
    FormatException(String msg) {
        super(msg);
    }
}

/**
 * Static class that holds functions for parsing basic pdf objects.
 * 
 * @author Steve Halliburton for NIE
 * @version 0.9
 * @see PDFConst
 */
public class PDFBasicObjParser implements PDFConst {
    
    /**
     * Check to see whether or not a given token contains a basic object parseable by this class's functions
     * 
     * @param token The token to check
     * @return True or False depending on whether or not the token is a basic object
     */
    public static boolean isBasicObject(String token) {
        if (PDFBasicObjParser.isWhiteSpace(token.charAt(0))) { return true; }
        else if ((Character.isDigit(token.charAt(0))) || (token.charAt(0) == '-') || (token.charAt(0) == '.')) {
            return true;
        } else if (token.charAt(0) == '(' || token.equals(")")) {
            return true;
        } else if (token.equals("]") || token.equals("[")) {
            return true;
        } else if (token.equals("<") || token.equals(">")) {
            return true;
        } else if (token.equals("true") || token.equals("false")) {
            return true;
        }
        return false;
    }
    
    /**
     * Given a stack of parsed commands and a token, parse a basic object.   Presumably isBasicObject
     * has already been called on the token.
     * 
     * @param st The StringTokenizer2 being used to parse the data stream.   Depending on the token being parsed
     * more data may be extracted from the tokenizer
     * @param stack A stack object containing already been parsed objects.   The basic object represented in token
     * will be added to the stack.
     * @param token The basic object
     * @return True if parsing went ok, False otherwise
     * @exception com.nie.parser.pdf.FormatException
     */
    public static boolean parseBasicObject(PDFParser parent, int refNum, int genNum, StringTokenizer2 st, Stack stack, String token) 
      throws FormatException {
        if (PDFBasicObjParser.isWhiteSpace(token.charAt(0))) { 
            // Whitespace, just keep going
        } else if ((Character.isDigit(token.charAt(0))) || (token.charAt(0) == '-') || (token.charAt(0) == '.')) {
            // Number... push a float
            try {
                stack.push(new Float(token));
            } catch (NumberFormatException e) {
                // Must really be a string?
                String s = new String(token);
                stack.push(s);
            }
        } else if (token.charAt(0) == '(') {
            // String, we should never get the ending ) because it's consumed below
            String s = new String("");
            int openParens = 0;
            while (st.hasMoreTokens()) {
                token = st.nextToken();
            
                // See if this is the start of an balanced parenthesis pair
                if (token.equals("(")) {
                    // If it is escaped, it's not necessarily part of a balanced pair
                    if (s.charAt(s.length()-1) == '\\') {
                        s = s.substring(0, s.length()-1);
                    } else {
                        openParens++;
                    }
                // See if it's the end of the string, or end of a balanced pair
                } else if (token.equals(")")) {
                    // If it's escaped don't bothing looking for stuff
                    if (s.charAt(s.length()-1) == '\\') {
                        s = s.substring(0, s.length()-1);
                    } else if (openParens >= 1) {
                        openParens--;
                    } else {
                        // Done with the string...
                        break;
                    }
                }   
                
                s += token;
            }
            Object _t = decodePDFString(s);
            if ((parent.decrypter != null) && (_t instanceof String))
                _t = parent.decrypter.decryptString((String)_t, refNum, genNum);
            stack.push(_t);
            //stack.push(decodePDFString(s));
            
        } else if (token.equals("]")) {
            // Array 
            Object o;
            Vector v = new Vector();
            while (!stack.isEmpty()) {
                o = stack.pop();
                if ((o instanceof String) && ((String)o).equals("[")) {
                    break;
                } else {
                    v.addElement(o);
                }
            }
            stack.push(v);
        } else if (token.equals("[")) {
            stack.push(token);
        } else if (token.equals("<")) {
            // Need to look at the next one to see if it's another <
            token = st.nextToken();
            if (token.equals("<")) {
                // Need to put them both onto the stack, and they'll be dealt with later
                // when the dictionary is built
                stack.push(token);
                stack.push(token);                
            } else if (token.equals(">")) {
                // OK... nothing in there... just bail
            } else {
                // Token should be the string, keep reading until a closing
                String s = new String(token);
                while (st.hasMoreTokens()) {
                    token = st.nextToken();
                    if (token.equals(">")) { break; }
                    s += token;
                }      
                
                s = decodeHexString(s);
                if (parent.decrypter != null)
                    s = parent.decrypter.decryptString(s, refNum, genNum);
                stack.push(s);
            }
        } else if (token.equals(">")) {
            // Dictionary.   Could be the end of a string, but that shouldn't ever happen
            token = st.nextToken();
            if (token.equals(">")) {
                // Dictionary...
                Hashtable t = new Hashtable();
                Object o;
                while (!stack.isEmpty()) {
                    o = stack.pop();
                    // See if we're back at the start yet
                    if ((o instanceof String) && ((String)o).equals("<")) {
                        // pop off the other one
                        stack.pop();
                        break;
                    } else {
                        // Must be an item, name should be the next item on the stack...
                        t.put(((String)stack.pop()).substring(1), o);
                    }
                }
                // Put the new dictionary back on the stack
                stack.push(t);
            } else {
                // If this get's hit, that means there wasn't a starting <
                throw new FormatException("Malformed hex string, beginning < not found");
            }
        } else if (token.equals("true") || token.equals("false")) {
            stack.push(new Boolean(token));
        } else {
            throw new FormatException("Not a basic object: " + token);
        }
        
        return RETURN_OK;        
    }   

    /**
     * Private function to tell whether or not a given character is whitespace.
     * 
     * @param c The character to check
     * @return True or False
     * @see PDFConst.PDF_WHITESPACE
     */
    private static boolean isWhiteSpace(char c) {
        for (int i=0; i<PDF_WHITESPACE.length; i++) {
            if (PDF_WHITESPACE[i] == c) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper function to decode hex strings
     * 
     * @param todo The string to decode
     * @return The decoded string
     */
    public static String decodeHexString(String todo) {
        StringBuffer decoded = new StringBuffer();
        int len = todo.length();
        char hstring[] = new char[2];
        int pos = 0;
        char thisChar;
        
        for (int x=0; x < len; x++) {
            thisChar = todo.charAt(x);
            switch (Character.toUpperCase(thisChar)) {
			case ' ':
			case '\t':
			case '\r':
			case '\n':
			case '\f':
				// ignore whitespace
				break;
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
			case 'A':
			case 'B':
			case 'C':
			case 'D':
			case 'E':
			case 'F':
                // It's a valid character
                hstring[pos] = thisChar;
                pos++;
                // See if it's at the end and needs to be balanced out with an extra 0
                if ((x == (len-1)) && (pos == 1)) {
                    hstring[pos] = '0';
                    pos++;
                }

                if (pos == 2) {
                    decoded.append((char)Integer.parseInt(new String(hstring), 16));
                    pos = 0;
                }
                break;
            default:
                // Not a valid character?
                
            }        
        }
        
        return decoded.toString();
    }
    
    /**
     * Helper function to decode PDF strings
     * 
     * @param todo The string to decode
     * @return The decoded string
     */
    public static Object decodePDFString(String todo) {
        if (PDFDate.isDate(todo)) {
            return new PDFDate(todo);
        }
        
        return todo;   
    }


}