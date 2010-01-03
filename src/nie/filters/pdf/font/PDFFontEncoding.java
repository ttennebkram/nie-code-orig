
package nie.filters.pdf.font;

import java.io.*;
import java.util.*;
import nie.filters.*;
import nie.filters.pdf.*;
import org.jdom.*;
import org.jdom.input.SAXBuilder;

/**
 * PDF encoding object wrapper.   Complements the PDFDesc object
 * 
 * @author Steve Halliburton for NIE
 * @version 0.9
 * @see nie.filters.pdf.font.PDFFontDesc
 */
public class PDFFontEncoding {
    
    static protected Hashtable charMapping;
    
    static protected Hashtable XMLMapping;
    static protected boolean XMLMappingParsed;
    
    protected Hashtable encoding;
    protected String encodingName;
 
    public PDFFontEncoding() {
        this.encoding = new Hashtable();
    }
 
    public PDFFontEncoding(PDFParser parent, PDFFontDesc fontDesc, long ref) 
      throws PDFException, IOException {
        this();
        parseEncodingDict(parent, fontDesc, ref);
    }

    /**
     * Given a "raw" font object, get the correspondig encoding object
     * 
     * @param parent The PDFParser creating the font
     * @param fontDesc The PDFFontDesc object corresponding to this font
     * @param rawFont The Hashtable read form the pdf stream
     * @return A suitable PDFFontEncoding object
     * @exception com.nie.parser.pdf.PDFException
     * @exception java.io.IOException
     */
    public static PDFFontEncoding getEncoding(PDFParser parent, PDFFontDesc fontDesc, Hashtable rawFont) 
      throws PDFException, IOException {
        String key = "No Encoding";
        if (rawFont.containsKey("Encoding")) { key = "Encoding"; }
        else if (rawFont.containsKey("/Encoding")) { key = "/Encoding"; }
            
        if (rawFont.containsKey(key)) {
            Object edef = rawFont.get(key);
            if (edef instanceof Long) {
                return new PDFFontEncoding(parent, fontDesc, ((Long)edef).longValue());
            } else if (edef instanceof String) {
                return getEncodingByName(parent, fontDesc, (String)edef);
            } else {
                throw new PDFException("Unknown encoding type " + edef.toString());
            }
        } 
        return new PDFEncodingStandard();
    }

    /**
     * Given the name of an encoding scheme, return a suitable encoding object
     * 
     * @param parent The PDFParser creating the font
     * @param fontDesc The PDFFontDesc object corresponding to this font
     * @param name The name to look up
     * @return A suitable PDFFontEncoding object
     */
    public static PDFFontEncoding getEncodingByName(PDFParser parent, PDFFontDesc fontDesc, String name) {
        name = fixNameString(name);
        if (name.equals("StandardEncoding")) {
            return new PDFEncodingStandard();
        } else if (name.equals("PDFDocEncoding")) {
            return new PDFEncodingPDF();
        } else if (name.equals("MacRomanEncoding")) {
            return new PDFEncodingMacRoman();
        } else if (name.equals("WinAnsiEncoding")) {
            return new PDFEncodingWinAscii();
        }

        return new PDFEncodingStandard();
    }

    private static String fixNameString(String name) {
        if (name.charAt(0) == '/') { name = name.substring(1); }
        return name;
    }

    protected void parseEncodingDict(PDFParser parent, PDFFontDesc fontDesc, long refNum) 
      throws IOException {
        Hashtable enc = (Hashtable)parent.getObject(refNum);

        if (enc.containsKey("BaseEncoding")) {
            String baseEncoding = (String)enc.get("BaseEncoding");
            if (baseEncoding.charAt(0) == '/') { baseEncoding = baseEncoding.substring(1); }
            this.encoding = (getEncodingByName(parent, fontDesc, baseEncoding)).getEncoding();
        } else {
            this.encoding = (getEncodingByName(parent, fontDesc, fontDesc.baseEncoding)).getEncoding();
        }

        Hashtable mappingDict = getXMLMapping(parent, fontDesc);
        
        // See if the character codes should be included
        boolean includeCharCodes = parent.getSetting("inccharcodes", "0").equals("0") ? false : true;

        if (enc.containsKey("Differences")) {
            Vector diffs = (Vector)enc.get("Differences");
            int charPos=0;
            Object elem;
            String token;
            StringTokenizer st;
                    
            // Need to go in reverse order
            for (int x=diffs.size()-1; x>=0; x--) {
                elem = diffs.elementAt(x);
                if (elem instanceof String) {
                    // Gotta be a more lightweight way to do this...
                    st = new StringTokenizer((String)elem, "/");
                    while (st.hasMoreElements()) {
                        token = (String)st.nextElement();
                                
                        if (Character.isDigit(token.charAt(0))) {
                            // New position
                            charPos = Integer.parseInt(token);
                        } else {
                            // A reference
                            Integer p = new Integer(charPos);
                            String v = "";
                            
                            if (mappingDict.containsKey(p)) {
                                v = (String)mappingDict.get(p);
                            } else if (this.charMapping.containsKey(token)) {
                                v = (String)charMapping.get(token);
                            } else {
                                // use the first letter
                                v = String.valueOf(token.charAt(0));
                            }

                            if (includeCharCodes) {
                                v += "(" + String.valueOf(charPos) + ")";
                            }
                            this.encoding.put(p, v);
                            
                            charPos++;
                        }
                    }
                } else if (elem instanceof Float) {
                    charPos = ((Float)elem).intValue();
                }
            }
        }
    }

    // Initialize the XMLMapping Hashtable
    static {
        XMLMapping = new Hashtable();
        XMLMappingParsed = false;
    }

    protected void parseXMLMapping(PDFParser parent) {
        // Flag that we've been here
        this.XMLMappingParsed = true;
        
        // Do the actual parsing
        Object mapping = parent.getSetting("mapping", null);
        
        if (mapping != null) {
            if (mapping instanceof String) {
                // Must be a filename?   Try and get an XML object
                SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
                try {
                    mapping = builder.build(new File((String)mapping));
                // } catch (JDOMException e) {
				} catch ( Exception e ) {
                    e.printStackTrace();
                    return;
                }
            }
            Document doc = (Document)mapping;
            
            Element root = doc.getRootElement();
            for (Iterator i=root.getChildren().iterator(); i.hasNext(); ) {
                Element child = (Element)i.next();
                if (child.getName().equalsIgnoreCase("font")) {
                    String fontName = child.getAttribute("name").getValue();
                    Hashtable fontMap = new Hashtable();
                    
                    for (Iterator z=child.getChildren().iterator(); z.hasNext(); ) {
                        Element glyph = (Element)z.next();
                        Integer code = new Integer(glyph.getAttribute("code").getValue());
                        String charCode = glyph.getText();
                        fontMap.put(code, charCode);
                    }
                    
                    XMLMapping.put(fontName, fontMap);
                }
            }
        }
    }

    protected Hashtable getXMLMapping(PDFParser parent, PDFFontDesc fontDesc) {

        if (!XMLMappingParsed) {
            parseXMLMapping(parent);
        }

        if (XMLMapping.containsKey(fontDesc.getName())) {
            return (Hashtable)XMLMapping.get(fontDesc.getName());
        }
        
        return new Hashtable();
    }

    public Hashtable getEncoding() {
        return this.encoding;
    }

    /**
     * Given an integer character code, return the corresponding display string
     * 
     * @param charCode The code to look up
     * @return The string to be displayed
     */
    public String getCharCode(Integer charCode) {
        if (this.encoding.containsKey(charCode)) {
            return (String)this.encoding.get(charCode);
        } else {
            return (String)charMapping.get(".notdef");
        }        
    }
 
    static protected String getCharMapping(String code) {
        if (charMapping.containsKey(code)) {
            return (String)charMapping.get(code);
        } else {
            return String.valueOf(code.charAt(0));
        }
    }
    
    static {
        // The vast majority of the character mappings have the character they are mapping
        // to as the first letter (including case).   So, all we need here is the ones that
        // don't follow that rule
        charMapping = new Hashtable();
        charMapping.put("AE", "AE");            charMapping.put("Eth", "D");
        charMapping.put("Euro", "$");           charMapping.put("OE", "OE");
        charMapping.put("acute", "'");          charMapping.put("ae", "ae");
        charMapping.put("ampersand", "&");       
        
        charMapping.put("asciicircum", "^");    charMapping.put("asciitilde", "~");
        charMapping.put("asterisk", "*");       charMapping.put("at", "@");
        charMapping.put("backslash", "\\");     charMapping.put("bar", "|");
        charMapping.put("braceleft", "{");      charMapping.put("braceright", "}");
        charMapping.put("bracketleft", "[");    charMapping.put("bracketright", "]");
        charMapping.put("breve", "~");          charMapping.put("brokenbar", "|");
        charMapping.put("bullet", "* ");        charMapping.put("caron", "'");
        charMapping.put("cedilla", ",");        charMapping.put("circumflex", "^");
        charMapping.put("colon", ":");          charMapping.put("comma", ",");
        charMapping.put("copyright", " ");      charMapping.put("currency", "$");
        charMapping.put("dagger", "*");         charMapping.put("daggerdbl", "*");
        charMapping.put("degree", "o");         charMapping.put("dieresis", "~");
        charMapping.put("divide", "/");         charMapping.put("dollar", "$");
        charMapping.put("dotaccent", "'");      charMapping.put("dotlessi", "*");
        charMapping.put("eight", "8");          charMapping.put("ellipsis", "...");
        charMapping.put("emdash", "-");         charMapping.put("endash", "-");
        charMapping.put("equal", "=");          charMapping.put("exclam", "!");
        charMapping.put("exclamdown", "!");     charMapping.put("five", "5");
        charMapping.put("fi", "fi");            charMapping.put("fl", "fl");
        charMapping.put("four", "4");           charMapping.put("fraction", "/");
        charMapping.put("grave", "`");          charMapping.put("greater", ">");
        charMapping.put("guillemotleft", "<<"); charMapping.put("guillemotright", ">>");
        charMapping.put("guilsinglleft", "<");  charMapping.put("guilsinglright", ">");
        charMapping.put("hungarumlat", "~");    charMapping.put("hyphen", "-");
        
        charMapping.put("less", "<");           charMapping.put("logicalnot", "|");
        charMapping.put("macron", "-");         charMapping.put("minus", "-");
        charMapping.put("mu", "u");             charMapping.put("multiply", "x");
        charMapping.put("nine", "9");           charMapping.put("numbersign", "#");
        charMapping.put("oe", "oe");            charMapping.put("oganek", ",");
        charMapping.put("one", "1");            charMapping.put("onehalf", " 1/2");
        charMapping.put("onequarter", " 1/4");  charMapping.put("onesuperior", "1");
        charMapping.put("ordfeminine", "a");    charMapping.put("ordmasculine", "o");
        charMapping.put("parenleft", "(");      charMapping.put("parentright", ")");
        charMapping.put("percent", "%");        charMapping.put("period", ".");
        charMapping.put("periodcenter", ".");   charMapping.put("perthousand", " 0/00");
        charMapping.put("plus", "+");           charMapping.put("plusminus", "+/-");
        charMapping.put("question", "?");       charMapping.put("quotedbl", "\"");
        charMapping.put("quotedblbase", "\"");  charMapping.put("quotedblleft", "\"");
        charMapping.put("quotedblright", "\""); charMapping.put("quoteleft", "'");
        charMapping.put("quoteright", "'");     charMapping.put("quotesinglbase", "'");
        charMapping.put("quotesingle", "'");    charMapping.put("registered", "R");
        charMapping.put("ring", "");            charMapping.put("semicolon", ";");
        charMapping.put("seven", "7");          charMapping.put("six", "6");
        charMapping.put("slash", "/");          charMapping.put("space", " ");
        charMapping.put("three", "3");          charMapping.put("threequarters", " 3/4");
        charMapping.put("threesuperior", "3");  charMapping.put("tilde", "~");
        charMapping.put("trademark", " TM");    charMapping.put("two", "2");
        charMapping.put("twosuperior", "2");
        
        charMapping.put("underscore", "_");     charMapping.put("zero", "0");
        charMapping.put(".notdef", " ");
    }        
}
