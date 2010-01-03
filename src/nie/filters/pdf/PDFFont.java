
package nie.filters.pdf;

import java.io.*;
import java.util.*;
import nie.filters.PDFParser;
import nie.filters.pdf.font.*;

/**
 * PDF Font object wrapper class, delegates most of the work
 * to the descriptor and encoding objects
 * 
 * @author Steve Halliburton for NIE
 * @version 0.9
 * @see nie.filters.pdf.font.PDFFontDesc
 * @see nie.filters.pdf.font.PDFFontEncoding
 */
public class PDFFont implements PDFConst {
    PDFFontDesc fontDesc;
    PDFFontEncoding encoding;

    String size;
    String color;
    String subType;

    public PDFFont(PDFParser parent, Hashtable font) throws java.io.IOException, PDFException {

        // Get the font description
        this.fontDesc = PDFFontDesc.getFontDesc(parent, font);
        
        // Get the encoding information
        this.encoding = PDFFontEncoding.getEncoding(parent, this.fontDesc, font);
        
        this.getInfo(font);
    }
    
    private void getInfo(Hashtable font) {
        if (font.containsKey("Subtype")) {
            this.subType = (String)font.get("Subtype");
        } else if (font.containsKey("/Subtype")) {
            this.subType = (String)font.get("/Subtype");
        } else {
            this.subType = "Unknown";
        }
    }
     
    public String getType() {
        if (this.subType.charAt(0) == '/') {
            return this.subType.substring(1);
        }
        return this.subType;
    }
        
    
    /**
     * Given an integer character code, get the actual string
     * that should be displayed
     * 
     * @param charCode The character code to get
     * @return A string representing the display string
     */
    public String getCharCode(Integer charCode) {
        return this.encoding.getCharCode(charCode);
    }

    /**
     * Given a character, get the font's horizontal displacement
     * for that character
     * 
     * @param c The character to get the width for
     * @return The width of the character
     */
    public double getCharWidth(char c) {
        return this.fontDesc.getCharWidth(c);
    }

    /**
     * Get the name of the font object
     * 
     * @return The name of the font object
     */
    public String getFontName() {
        return this.fontDesc.getName();
    }
    
    /**
     * Set the font size
     * 
     * @param size The font size
     */
    public void setSize(String size) {
        this.size = size;
    }
    
    /**
     * Get the current font size
     * 
     * @return The current font size
     */
    public String getSize() {
        return this.size;
    }
    
    /**
     * Set the current color (currently color information is unimplemented)
     * 
     * @param color The color to set to
     */
    public void setColor(String color) {
        this.color = color;
    }
    
    /**
     * Get the current color (currently unimplemented)
     * 
     * @return The current color string
     */
    public String getColor() {
        return this.color;
    }
    
}
