
package nie.filters.pdf;

/**
 * Class representing a PDF date.   Takes as input an unparsed
 * string from a pdf file, and on a toString() request returns
 * a properly formatted date string
 * 
 * @author Steve Halliburton for NIE
 * @version 0.9
 */
public class PDFDate {

    /**
     * The parsed date structure
     */
    public int d[];
    
    /**
     * Constructor function.   If passed another PDFDate instance,
     * just grabs the parsed date structure from the other one.  If
     * a string is passed, parses the string into the date structure.
     * 
     * @param date The pdf date string to parse or another PDFDate object.
     */
    public PDFDate(Object date) {
        if (date instanceof PDFDate) {
            this.d = ((PDFDate)date).d;
        } else {
            // Better be a string...
            this.d = this.parse((String)date);    
        }
    }

    /**
     * Helper function to decide whether or not a given string
     * corresponds to a pdf date object
     * 
     * @param toCheck The string to check
     * @return true or false
     */
    public static boolean isDate(Object toCheck) {
        if (toCheck instanceof PDFDate) { return true; }
        
        // See if it's a date.   The D: on the front isn't a requirement but its VERY strongly recommended
        // and every file I've seen uses it, so go for that for the time being
        if (((String)toCheck).length() >= 2 && (((String)toCheck).substring(0, 2)).equals("D:")) {
            return true;
        }
        return false;
    }

    /**
     * Return a properly formatted date string
     * 
     * @return The date string
     */
    public String toString() {
        // Target output format: YYYY-MM-DDTHH:mm:ss
        return new String(d[0]+"-"+d[1]+"-"+d[2] + "T" + d[3]+":"+d[4]+":"+d[5]);
    }

    /**
     * Parse a given string into an array
     * 
     * @param date The date string to parse
     * @return An array of date parts
     */
    private int[] parse(String date) {
        if (date.substring(0, 2).equals("D:")) { date = date.substring(2); }
        
        // Format YYYYMMDDHHmmSSOHH' mm'
        int d[] = {0, 1, 1, 0, 0, 0, 0, 0, 0}; 
        int len = date.length();
        
        // Just get whatever we can...  if something goes wrong, go with whatever we got to that point
        try {
            // Year, first 4 digits
            d[0] = Integer.parseInt(date.substring(0, 4));
            if (len <= 4) { return d; }
            // Month, next 2 digits
            d[1] = Integer.parseInt(date.substring(4, 6));
            if (len <= 6) { return d; }
            // Day, next 2 digits
            d[2] = Integer.parseInt(date.substring(6, 8));
            if (len <= 8) { return d; }
            // Hour, next 2 digits
            d[3] = Integer.parseInt(date.substring(8, 10));
            if (len <= 10) { return d; }
            // Minute, next 2 digits
            d[4] = Integer.parseInt(date.substring(10, 12));
            if (len <= 12) { return d; }
            // Seconds, next 2 digits
            d[5] = Integer.parseInt(date.substring(12, 14));
            if (len <= 14) { return d; }
            // UTC direction (one of -/+/Z ==> -1/0/1)
            char t = date.charAt(15);
            switch (t) {
            case '-':
                d[6] = -1;
                break;
            case '+':
                d[6] = 1;
                break;
            default:
                d[6] = 0;
                break;            
            }
            if (len <= 15) { return d; }
            // UTC hour offset, next 2 digits
            d[7] = Integer.parseInt(date.substring(15, 17));
            if (len <= 18) { return d; }
            // UTC minute offset, skip the ', then next 2 digits
            d[8] = Integer.parseInt(date.substring(18, 20));
        } catch (Exception e) {
        }
        return d;        
    }
    
    
}