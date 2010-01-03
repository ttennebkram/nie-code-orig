
package nie.filters.pdf.font;

import java.io.*;
import java.util.*;
import nie.filters.*;
import nie.filters.pdf.*;

/**
 * PDF Font descriptor wrapper class.   Complements the font encoding wrapper
 * 
 * @author Steve Halliburton for NIE
 * @version 0.9
 * @see nie.filters.pdf.font.PDFFontEncoding
 */
public class PDFFontDesc {
    protected Hashtable fontDescriptor;
    protected Hashtable fontWidths;
    
    public String fontName;
    public String baseEncoding;
    
    // Override for the standard font definitions
    protected boolean isStandardFont = false;
 
    // For defined fonts to override
    public PDFFontDesc() { 
        fontName = "Unknown font";
        baseEncoding = "StandardEncoding";
    }
    
    // For parsed fonts
    public PDFFontDesc(PDFParser parent, Hashtable rawFont) throws PDFException, IOException {
        this();
        this.parse(parent, rawFont);
    }

    /**
     * Given a "raw" font (Hashtable built from the pdf stream), build a corresponding
     * PDFFontDesc object.   Since all the standard fonts have their own schemes, need
     * to have a big if statement and return the right type of object
     * 
     * @param parent The parser that is creating the font object
     * @param rawFont The Hashtable read from the PDF stream
     * @return A suitable descriptor object
     * @exception com.nie.parser.pdf.PDFException
     * @exception java.io.IOException
     */
    static public PDFFontDesc getFontDesc(PDFParser parent, Hashtable rawFont) throws PDFException, IOException {
        // Get the name
        String fName = getBaseFontName(rawFont);
        
        if (fName.equals("Courier")) {
            return new PDFFontCourier();
        } else if (fName.equals("Courier-Bold")) {
            return new PDFFontCourierBold();
        } else if (fName.equals("Courier-Oblique")) {
            return new PDFFontCourierOblique();
        } else if (fName.equals("Courier-BoldOblique")) {
            return new PDFFontCourierBoldOblique();
        } else if (fName.equals("Helvetica")) {
            return new PDFFontHelvetica();
        } else if (fName.equals("Helvetica-Bold")) {
            return new PDFFontHelveticaBold();
        } else if (fName.equals("Helvetica-Oblique")) {
            return new PDFFontHelveticaOblique();
        } else if (fName.equals("Helvetica-BoldOblique")) {
            return new PDFFontHelveticaBoldOblique();
        } else if (fName.equals("Times-Bold")) {
            return new PDFFontTimesBold();
        } else if (fName.equals("Times-BoldItalic")) {
            return new PDFFontTimesBoldItalic();
        } else if (fName.equals("Times-Italic")) {
            return new PDFFontTimesItalic();
        } else if (fName.equals("Times-Roman")) {
            return new PDFFontTimesRoman();
        } else if (fName.equals("Symbol")) {
            return new PDFFontSymbol();
        } else if (fName.equals("ZapfDingbats")) {
            return new PDFFontZapfDingbats();
        } else {
            return new PDFFontDesc(parent, rawFont);
        }
    }

    /**
     * Given a "raw" font, get the name of the actual font
     * 
     * @param rawFont The hashtable read from the PDF stream
     * @return The font name.   <b>Note:</b> This is the actual name of the font as recorded in the
     * raw font data.   This does not take into account multiple master fonts, subfonts, or
     * anything other than standard Type1 fonts.
     */
    static protected String getBaseFontName(Hashtable rawFont) {
        if (rawFont.containsKey("BaseFont")) {
            String f = (String)rawFont.get("BaseFont");
            if (f.charAt(0) == '/') { f = f.substring(1); }
            return f;
        }
        return "Unknown base font";
    }
     
    /**
     * Get the font name
     * 
     * @return The font name
     */
    public String getName() {
        return this.fontName;
    }
        
    /**
     * Get the font descriptor object
     * 
     * @return The font descriptor object
     */
    public Hashtable getFontDescriptor() {
        return this.fontDescriptor;
    }
    
    /**
     * Get the font widths object
     * 
     * @return The font widths object
     */
    public Hashtable getFontWidths() {
        return this.fontWidths;
    }
    
    /**
     * Given a character, get the width of that character for this font as specified
     * in the font widths dictionary
     * 
     * @param c The character to look up
     * @return The font width
     */
    public double getCharWidth(char c) {
        Integer key = new Integer((int)c);
        
        if (this.fontWidths.containsKey(key)) {
            return ((Float)this.fontWidths.get(key)).doubleValue();
        }
        // Try a space character
        key = new Integer(40);
        if (this.fontWidths.containsKey(key)) {
            return ((Float)this.fontWidths.get(key)).doubleValue();
        }
        // Go with a default, 250 sounds good
        return 250.0;
    }
    
       
    private void parse(PDFParser parent, Hashtable rawFont) throws PDFException, IOException {
        // Get the name
        this.parseFontName(parent, rawFont);
        
        // Get the descriptor
        this.parseDescriptor(parent, rawFont);
        
        // Get the widths
        this.parseFontWidths(parent, rawFont);
    }
    
    private void parseFontName(PDFParser parent, Hashtable rawFont) {
        if (rawFont.containsKey("BaseFont")) {
            String f = (String)rawFont.get("BaseFont");
            if (f.charAt(0) == '/') { f = f.substring(1); }
            this.fontName = f;
        } else if (rawFont.containsKey("Name")) {
            String f = (String)rawFont.get("Name");
            if (f.charAt(0) == '/') { f = f.substring(1); }
            this.fontName = f;
        } else {
            this.fontName = new String("Unknown base font");
        }            
    }        
    
    private void parseDescriptor(PDFParser parent, Hashtable rawFont) throws PDFException, IOException {
        if (rawFont.containsKey("FontDescriptor")) {
            long refNum = ((Long)rawFont.get("FontDescriptor")).longValue();
            this.fontDescriptor = (Hashtable)parent.getObject(refNum);
        } else {
            // Signal an error, but keep going
            parent.signalError(new PDFException("No font descriptor: " + this.fontName));
            this.fontDescriptor = new Hashtable();
        }
    }        
    
    private void parseFontWidths(PDFParser parent, Hashtable rawFont) throws PDFException {
        int firstChar, lastChar;
        Vector widths;
        if (rawFont.containsKey("Widths")) {
            if (!rawFont.containsKey("FirstChar") || !rawFont.containsKey("LastChar")) {
                throw new PDFException("No FirstChar/LastChar: " + this.fontName);
            }
            firstChar = ((Float)rawFont.get("FirstChar")).intValue();
            lastChar = ((Float)rawFont.get("LastChar")).intValue();
            
            widths = (Vector)rawFont.get("Widths");
        } else {
            parent.signalError(new PDFException("No widths for font: " + this.fontName));
            widths = new Vector();
            firstChar = 0;
            lastChar = 0;
        }
         
        this.fontWidths = new Hashtable();
        for (int x=widths.size()-1; x>=0; x--) {
            this.fontWidths.put(new Integer(x+firstChar), widths.elementAt(x));
        }
        
        
    }
}