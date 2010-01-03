
package nie.filters.pdf;

import java.util.*;
import org.jdom.*;
import nie.filters.io.*;
import nie.filters.PDFParser;

class StreamException extends PDFException {
    String excName = "StreamException";
    
    StreamException(String msg, int streamPos) {
        super(msg, streamPos);
    }
    StreamException(String msg) {
        super(msg);
    }
}


public class PDFStreamParser implements PDFConst {
    PDFParser parent;
    Hashtable page;
    Hashtable contents;
    Element pageElem;
    int refNum, genNum;

    public PDFStreamParser(PDFParser parent, Hashtable page, Hashtable contents, Element pageElem) {
        this.parent = parent;
        this.page = page;
        this.contents = contents;
        this.pageElem = pageElem;
        this.refNum = ((Long)contents.get("refNum")).intValue();
        this.genNum = ((Long)contents.get("genNum")).intValue();
    }

    public boolean parse() throws PDFException {
        Hashtable attrs = (Hashtable)contents.get("attrs");       
        byte[] data = (byte[])contents.get("stream");
        
        // Check for compression
        if (attrs.containsKey("Filter")) {
            Vector filters;
            Object f = attrs.get("Filter");
            if (f instanceof Vector) {
                filters = (Vector)f;
            } else {
                filters = new Vector(1);
                filters.addElement(f);
            }

            // Need to apply these in reverse order
            for (int x=filters.size()-1; x>=0; x--) {
                String filter = (String)filters.elementAt(x);
                if (filter.equals("/FlateDecode")) {
                    data = (new PDFDecompressorFlate(attrs, data)).decompress();
                } else if (filter.equals("/LZWDecode")) {
                    data = (new PDFDecompressorLZW(attrs, data)).decompress();
                } else {
                    throw new StreamException("Unsupported stream compression: " + filter);
                }
            }
        }

        Hashtable props;
        if (page.containsKey("Resources")) {
            props = (Hashtable)page.get("Resources");
        } else {
            props = new Hashtable();
        }

        if ((this.parent.getSettingInt("stream", new Integer(0))).intValue() != 0) {
            Element stream = new Element("stream");
            stream.addContent(new String(data));
            pageElem.addContent(stream);
        }

        return this.parseStreamContents(data, props);
    }
    
    private boolean parseStreamContents(byte[] data, Hashtable props) throws PDFException {
        Stack stack = new Stack();
        StringTokenizer2 st = new StringTokenizer2(new String(data), " \t\r\n[]()<>", true);
        String token;
        Hashtable textState = new Hashtable();
        
        textState.put("color", "");
        textState.put("style", "");
        textState.put("length", new Integer(0));
        Element curText = new Element("text");
        Element pathElement = new Element("path");
        Element fontElement = null;
        PDFFont curFont = null;
        double Tm[] = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        double Tc = 0.0;
        double Tw = 0.0;
        Float curPathPoint[] = new Float[2];
        double bbox[] = {0.0, 0.0, 0.0, 0.0};
        //double startpos[] = {0.0, 0.0}, endpos[] = {0.0, 0.0};
        int streamLen = 0;
        double Tl = 0.0;
        boolean doPathInfo = (this.parent.getSettingInt("nopath", new Integer(0))).intValue() == 1 ? false : true;
        
        // 2 options here for the layout, either we need to absolutely preserve the character positioning in the 
        // file, or we can inject some characters to make it a little more friendly.   The default is absolute
        // positioning
        boolean doAbsolute = (this.parent.getSettingInt("layout", new Integer(1))).intValue() == 1 ? true : false;
        
        Vector doTokens = new Vector();
        // Add all the text operators
        doTokens.add("Tm");  doTokens.add("TJ");  doTokens.add("Tj");
        doTokens.add("'");   doTokens.add("\"");  doTokens.add("TD");
        doTokens.add("Td");  doTokens.add("Tc");  doTokens.add("Tw");
        doTokens.add("TL");  doTokens.add("Tf");
        // Add all the path operators
        if (doPathInfo) {
        doTokens.add("m");   doTokens.add("l");   doTokens.add("c");
        doTokens.add("v");   doTokens.add("y");   doTokens.add("re");
        doTokens.add("h");   doTokens.add("f*");  doTokens.add("n");
        doTokens.add("f");   doTokens.add("s");   doTokens.add("b");
        }
        
        // Since we only want a pretty small subset of the operators, there's going to be a lot
        // of extra stuff left over in the stack... 
        while (st.hasMoreTokens()) {
            token = st.nextToken();
            
            // Check if it's a basic object
            if (PDFBasicObjParser.isBasicObject(token)) {
                PDFBasicObjParser.parseBasicObject(this.parent, refNum, genNum, st, stack, token);
                continue;
            // If not, see if it is a token, if not just add it to the stack and skip
            // all the checks below
            } else if (!doTokens.contains(token)) {
                stack.push(token);
                continue;
            }
                
            // At this point it's guaranteed to be a token, just find the right one
            if (token.equals("Tm")) {
                double[] newTm = new double[7];
                for (int x=0; x<6; x++) {
                    newTm[x] = ((Float)stack.pop()).doubleValue();
                }
                // Tack the text character spacing on the end to make sure it stays around
                newTm[6] = Tc;
                
                // Need to check the translation between the last text matrix and this one and see where
                // we're at.   If the y coordinate has changed at all, start a new line.   If the x 
                // coordinate has moved over to the right more than 1 space's worth of position, add some
                // spacing
                
                // First, figure out the x/y coords of each of the matrices (assume for now the font width
                // is constant)
                double newpos[] = getTransPos(newTm);
                double oldpos[] = getTransPos(Tm);

                if (newpos[1] != oldpos[1]) {
                    // Y changed, add a line
                    if (doAbsolute) {
                        curText = this._setStreamTextElem(curText, fontElement, textState, bbox, newpos);
                    } else {
                        addText(Tm, curFont, curText, textState, "\r\n", false, false, 0);
                    }
                } else if (getTransdCharWidth(Tm, curFont, ' ') <= (newpos[0] - oldpos[0])) {
                    // X changed more than a space, add a space
                    if (doAbsolute) {
                        curText = this._setStreamTextElem(curText, fontElement, textState, bbox, newpos);
                    } else {
                        addText(Tm, curFont, curText, textState, " ", false, false, 0);
                    }
                }

                // Set the text matrix to be the new one
                Tm = newTm;
                //startpos = getTransPos(Tm);
            } else if (token.equals("TJ")) {
                // Text item, should be a vector at the head of the stack
                if (!(stack.peek() instanceof Vector)) {
                    throw new StreamException("No vector before TJ", st.getCurrentPosition());
                }
                Vector v = (Vector)stack.pop();
                String s = new String();
                int adjust = 0;
                double spaceSize = (curFont.getCharWidth(' ')*0.5)/1000.0;
                for (int i=v.size()-1; i>=0; i--) {
                    Object o = v.elementAt(i);
                    if (o instanceof String) {
                        s += (String)o;
                    } else if ((o instanceof Float)) {
                        // The formula for deciding how far to move over is:
                        //
                        //  tx = (w0 - Tj/1000) + Tc
                        //
                        // where w0 is the horizontal displacement of the glyph, Tj is the float currently
                        // in scope, and Tc is the text character spacing.   And then it's applied to the
                        // text transformation matrix:
                        //
                        // [ 1  0 0
                        //   0  1 0    x  Tm = Trm
                        //   tx 0 1 ] 
                        //
                        // So basically what this means is that if the horizontal displacement plus the scaled
                        // adjustment, plus the character spacing is positive, we should move away from the current
                        // character, otherwise, move closer.   For our purposes, if we're moving away the size 
                        // of a space (or pretty close, say 50%), add a space.   So we can just forget about the
                        // horizontal displacement, and just look at the relation of the spacing and the character
                        // spacing
                        double cSpacing = ((Float)o).doubleValue();
                        double move = ((-cSpacing)/1000)+Tc;
                        if ( ((-cSpacing)/1000)+Tc >= spaceSize) {
                            // Add a space
                            if (doAbsolute) {
                                if (s.length() > 0) {
                                    addText(Tm, curFont, curText, textState, s, true, true, adjust);
//                                    endpos = getTransPos(Tm);
                                    curText = this._setStreamTextElem(curText, fontElement, textState, bbox, getTransPos(Tm));
                                }
                                s = "";
                                adjust = 0;
                            } else {
                                s += " ";
                                adjust++;
                            }
                        }                        
                    }
                }
                if (s.length() > 0)
                    addText(Tm, curFont, curText, textState, s, true, true, adjust);
            } else if (token.equals("Tj")) {
                // Text item, there should be a string item on the stack
                if (!(stack.peek() instanceof String)) {
                    throw new StreamException("No string before Tj", st.getCurrentPosition());
                }
                String s = (String)stack.pop();
                addText(Tm, curFont, curText, textState, s, true, true, 0);
            } else if (token.equals("T*")) {
                // New line
                //
                // T* - Move to the start of the next line. This operator has the same effect as the code
                //
                //       0 Tl Td
                //
                //      where Tl is the leading parameter in the text state.

                double[] change = {Tl, 0.0, 1, 0, 0, 1, Tc};
                Tm = updateTransFormMatrix(Tm, change);
                
                if (doAbsolute) {
                    //endpos = getTransPos(Tm);
                    curText = this._setStreamTextElem(curText, fontElement, textState, bbox, getTransPos(Tm));
                } else {
                    addText(Tm, curFont, curText, textState, "\r\n", false, false, 0);
                }
            } else if (token.equals("'") || token.equals("\"")) {
                // New line then some text
                //
                // ' - Move to the next line and show a text string. This operator has the same effect as
                //
                //      T*
                //      string Tj
                //
                // " - Move to the next line and show a text string, using a w as the word spacing and a c
                //      as the character spacing (setting the corresponding parameters in the text state).
                //      a w and a c are numbers expressed in unscaled text space units. This operator has
                //      the same effect as
                //
                //      aw Tw
                //      ac Tc
                //      string '                
                //
                
                if (!(stack.peek() instanceof String)) {
                    throw new StreamException("No string before quote operator", st.getCurrentPosition());
                }

                String s = (String)stack.pop();

                // If it's a " pull off the extra two parameters
                if (token.equals("\"")) {
                    Float ac = (Float)stack.pop();
                    Float aw = (Float)stack.pop();
                
                    Tc = ac.doubleValue();
                    Tw = aw.doubleValue();
                }

                double[] change = {Tl, 0.0, 1, 0, 0, 1, Tc};
                Tm = updateTransFormMatrix(Tm, change);
                
                if (doAbsolute) {
                    //endpos = getTransPos(Tm);
                    curText = this._setStreamTextElem(curText, fontElement, textState, bbox, getTransPos(Tm));
                    addText(Tm, curFont, curText, textState, s, true, true, 2);
                } else {
                    addText(Tm, curFont, curText, textState, "\r\n" + s, true, true, 2);
                }
            } else if (token.equals("TD") || token.equals("Td")) {
                // Offset next line, make sure it's really going to the next line though
                //
                // Td - Move to the start of the next line, offset from the start of the current line by
                //      (tx, ty). tx and ty are numbers expressed in unscaled text space units. More precisely,
                //      this operator performs the following assignments:
                //
                //                   [ 1  0  0
                //        Tm = Tlm =   0  1  0   x Tlm
                //                     tx ty 1 ] 
                //
                // TD - Move to the start of the next line, offset from the start of the current line by
                //      (tx, ty). As a side effect, this operator sets the leading parameter in the text state.
                //      This operator has the same effect as the code
                //
                //          -ty TL
                //          tx ty Td
                //
                
                Float yoffset = (Float)stack.pop();
                Float xoffset = (Float)stack.pop();

                double[] change = {yoffset.doubleValue(), xoffset.doubleValue(), 1, 0, 0, 1, Tc};
                Tm = updateTransFormMatrix(Tm, change);
                
                if (yoffset.intValue() != 0) {
                    if (doAbsolute) {
                        //endpos = getTransPos(Tm);
                        curText = this._setStreamTextElem(curText, fontElement, textState, bbox, getTransPos(Tm));
                    } else {
                        addText(Tm, curFont, curText, textState, "\r\n", false, false, 0);
                    }
                }
            } else if (token.equals("Tc")) {
                // Change the text character spacing
                Tc = ((Float)stack.pop()).doubleValue();
            } else if (token.equals("Tw")) {
                // Change the text word spacing
                Tw = ((Float)stack.pop()).doubleValue();
            } else if (token.equals("TL")) {
                // Change the text leading spacing
                Tl = ((Float)stack.pop()).doubleValue();
            } else if (token.equals("Tf")) {
                // Switch fonts (text state change)
                if (fontElement != null) {
                    //endpos = getTransPos(Tm);
                    curText = this._setStreamTextElem(curText, fontElement, textState, bbox, getTransPos(Tm));
                }
                
                String size = ((Float)stack.pop()).toString();
                String fontRefName = (String)stack.pop();
                
                PDFFont newFont = this.getFontPropsFromPageProps(props, fontRefName.substring(1));
                newFont.setSize(size);
                
                boolean doReset = true;
                if (curFont != null) {
                    if (!curFont.getFontName().equals(newFont.getFontName()) || !curFont.getSize().equals(size)) {
                        // Something changed, go ahead and add it
                        pageElem.addContent(fontElement);
                    } else {
                        // Nothing changed, don't reset
                        doReset = false;
                    }
                } 
                    
                if (doReset) {
                    curFont = newFont;
                    
                    fontElement = new Element("font");
                    fontElement.setAttribute("size", curFont.getSize());
                    fontElement.setAttribute("font", curFont.getFontName());
                }
            } else if (token.equals("m") && doPathInfo) {
                // Start a new subpath
                Float my = (Float)stack.pop();
                Float mx = (Float)stack.pop();
                
                pathElement = new Element("path");
                curPathPoint[0] = mx;
                curPathPoint[1] = my;
                
            } else if (token.equals("l") && doPathInfo) {
                // Straight line
                Float ly = (Float)stack.pop();
                Float lx = (Float)stack.pop();
                
                Element line = new Element("line");
                line.setAttribute("x1", curPathPoint[0].toString());
                line.setAttribute("y1", curPathPoint[1].toString());
                line.setAttribute("x2", lx.toString());
                line.setAttribute("y2", ly.toString());
                pathElement.addContent(line);
                
                curPathPoint[0] = lx;
                curPathPoint[1] = ly;
                
            } else if (token.equals("c") && doPathInfo) {
                // Curve
                Float cy3 = (Float)stack.pop();
                Float cx3 = (Float)stack.pop();
                Float cy2 = (Float)stack.pop();
                Float cx2 = (Float)stack.pop();
                Float cy1 = (Float)stack.pop();
                Float cx1 = (Float)stack.pop();
                
                Element curve = new Element("curve");
                curve.setAttribute("x1", curPathPoint[0].toString());
                curve.setAttribute("y1", curPathPoint[1].toString());
                curve.setAttribute("x2", cx3.toString());
                curve.setAttribute("y2", cy3.toString());
                curve.setAttribute("cpx1", cx1.toString());
                curve.setAttribute("cpy1", cy1.toString());
                curve.setAttribute("cpx2", cx2.toString());
                curve.setAttribute("cpy2", cy2.toString());
                pathElement.addContent(curve); 
                
                curPathPoint[0] = cx3;
                curPathPoint[1] = cy3;
                
            } else if (token.equals("v") && doPathInfo) {
                Float cy3 = (Float)stack.pop();
                Float cx3 = (Float)stack.pop();
                Float cy2 = (Float)stack.pop();
                Float cx2 = (Float)stack.pop();

                Element curve = new Element("curve");
                curve.setAttribute("x1", curPathPoint[0].toString());
                curve.setAttribute("y1", curPathPoint[1].toString());
                curve.setAttribute("x2", cx3.toString());
                curve.setAttribute("y2", cy3.toString());
                curve.setAttribute("cpx1", cx2.toString());
                curve.setAttribute("cpy1", cy2.toString());
                pathElement.addContent(curve); 
                
                curPathPoint[0] = cx3;
                curPathPoint[1] = cy3;
                
            } else if (token.equals("y") && doPathInfo) {
                Float cy3 = (Float)stack.pop();
                Float cx3 = (Float)stack.pop();
                Float cy1 = (Float)stack.pop();
                Float cx1 = (Float)stack.pop();

                Element curve = new Element("curve");
                curve.setAttribute("x1", curPathPoint[0].toString());
                curve.setAttribute("y1", curPathPoint[1].toString());
                curve.setAttribute("x2", cx3.toString());
                curve.setAttribute("y2", cy3.toString());
                curve.setAttribute("cpx1", cx1.toString());
                curve.setAttribute("cpy1", cy1.toString());
                curve.setAttribute("cpx2", cx3.toString());
                curve.setAttribute("cpy2", cy3.toString());
                pathElement.addContent(curve); 
                
                curPathPoint[0] = cx3;
                curPathPoint[1] = cy3;

            } else if (token.equals("re") && doPathInfo) {
                // Append a complete rectangle to the subpath (ends the path)
                Float rh = (Float)stack.pop();
                Float rw = (Float)stack.pop();
                Float ry = (Float)stack.pop();
                Float rx = (Float)stack.pop();
                
                Element rect = new Element("rectangle");
                rect.setAttribute("x1", rx.toString());
                rect.setAttribute("y1", ry.toString());
                rect.setAttribute("width", rw.toString());
                rect.setAttribute("height", rh.toString());
                pathElement.addContent(rect);
                
                pageElem.addContent(pathElement);
                pathElement = new Element("path");
                curPathPoint[0] = new Float(0);
                curPathPoint[1] = new Float(0);
               
            } else if (token.equals("h")  || token.equalsIgnoreCase("f") ||
                       token.equals("f*") || token.equalsIgnoreCase("s") ||
                       token.equals("n")  || token.equalsIgnoreCase("b") ||
                       token.equalsIgnoreCase("b*")) {
                // End the current path  (Not sure if this should ignore non-stroking operators, for
                // example, updating the clipping path).
                if (!doPathInfo) { continue; }
                
                if (curPathPoint[0].floatValue() == 0.0f && curPathPoint[1].floatValue() == 0.0f) {
                    // Already got added... just skip it
                    continue;
                }
                pageElem.addContent(pathElement);
                pathElement = new Element("path");
                curPathPoint[0] = new Float(0);
                curPathPoint[1] = new Float(0);
                
            } else {
                stack.push(token);
            }
        }
        
        // Get the last set
        if (fontElement != null) {
            //endpos = getTransPos(Tm);
            curText = this._setStreamTextElem(curText, fontElement, textState, bbox, getTransPos(Tm));
            pageElem.addContent(fontElement);
        }
        
        return RETURN_OK;
    }


    private double[] addText(double[] Tm, PDFFont curFont, Element curText, Hashtable textState, String text, boolean escape, boolean doLen, int sizeAdjust) {
        if (curFont == null) {
            this.parent.signalError(new PDFException("Text defined before font selection, ignoring"));
            return Tm;
        }
        if (escape == true) {
            // If it's being escaped, it's probably a raw string, so go ahead and adjust the transformation matrix
            // along the way
            Tm = updateTransFormMatrix(Tm, curFont, text);
            
            Object[] vals = this.unescapeString(text, curFont);
            text = (String)vals[0];
            sizeAdjust += ((Integer)vals[1]).intValue();
        }

        if (doLen == true) {
            // Add size of the string to the length in textstate
            int len = ((Integer)textState.get("length")).intValue() + text.length();
            if (sizeAdjust != 0) { len -= sizeAdjust; }
            textState.put("length", new Integer(len));
        }        
        
        // Add it to curText
        curText.addContent(text);
        
        return Tm;
    }

    private Element _setStreamTextElem(Element curText, Element parent, Hashtable textState, double[] bbox, double[] pos) {
        if ((curText.getText()).length() == 0) { return new Element("text"); }
        
        Enumeration keys = textState.keys();
        while (keys.hasMoreElements()) {
            String key = (String)keys.nextElement();
            Object val = textState.get(key);
            if (val instanceof Integer) {
                val = ((Integer)val).toString();
            }
            if (((String)val).length() > 0) {
                curText.setAttribute(key, (String)val);
            }
        }
        
        // Deal with the character offset
        curText.setAttribute("charoffset", (new Integer(this.parent.charCount)).toString());
        this.parent.charCount += ((Integer)textState.get("length")).intValue();
        curText.setAttribute("pageoffset", (new Integer(this.parent.pageCharCount)).toString());
        this.parent.pageCharCount += ((Integer)textState.get("length")).intValue();

        // Store the info about the bounding box
/*        double bbox[] = this.getTmBBox(startpos, endpos);
        curText.setAttribute("bbx", (new Double(bbox[0])).toString());
        curText.setAttribute("bby", (new Double(bbox[1])).toString());
        curText.setAttribute("bbwidth", (new Double(bbox[2])).toString());
        curText.setAttribute("bbheight", (new Double(bbox[3])).toString());
        
        // Set the contents of the startpos to endpos
        startpos[0] = endpos[0];
        startpos[1] = endpos[1];
  */      
        // Reset the length
        textState.put("length", new Integer(0));
        
        parent.addContent(curText);
        return new Element("text");
    }
    
    private PDFFont getFontPropsFromPageProps(Hashtable pageProps, String fontName) throws StreamException {
        Hashtable fonts = (Hashtable)pageProps.get("Font");
        if (fonts.containsKey(fontName)) {
            return (PDFFont)fonts.get(fontName);
        }
        throw new StreamException("Undefined font reference " + fontName);
    }
    
    private Object[] unescapeString(String s, PDFFont font) {
        Object ret[] = new Object[2];
/*        
        if (s.indexOf("\\") == -1) {
            // Nothing there, just return it
            ret[0] = s;
            ret[1] = new Integer(0);
            return ret;
        }
  */      
        StringBuffer s2 = new StringBuffer();
        StringBuffer charCode = new StringBuffer();
        int pos=0, lastPos=0;
        int adjust=0;
        Integer code;
        boolean force=false;
        
        while (pos < s.length()) {
            char curChar = s.charAt(pos);
            
            if (curChar == '\\') {
                // Go past the backslash
                pos++;
                
                if (Character.isDigit(s.charAt(pos))) {
                    while (pos < s.length() && Character.isDigit(s.charAt(pos)) && (charCode.length() < 3)) {
                        charCode.append(s.charAt(pos));
                        pos++;
                    }
                    String t = "";
                    code = new Integer(new String(charCode));
                    charCode = new StringBuffer();
                    force=true;
                } else {
                    // Put the backslash back and go back to the last position
                    curChar = '\\';
                    //code = new Integer(Character.getNumericValue(curChar));
                    code = new Integer((int)curChar);
                }
                pos--;
            } else {
                //code = new Integer(Character.getNumericValue(curChar));                
                code = new Integer((int)curChar);
            }
            
            // For Type3 fonts we need to resolve EVERY character going through, otherwise just
            // go with whatever is in the curChar
            if (force || font.getType().equals("Type3")) { 
                String repl = font.getCharCode(code);
                s2.append(repl);

                // No matter what, this should only consume 1 character's worth of space in the offset, so
                // set the adjust accordingly
                if (repl.length() > 1) { 
                    adjust += repl.length() - 1;
                }
            } else {
                s2.append(curChar);
            }
            
            force = false;
            pos++;
        }
        
        ret[0] = new String(s2);
        ret[1] = new Integer(adjust);
        return ret;
    }
 
    // We could use a Point here instead of a double[2], but Point only accepts ints, and we
    // need to keep the precision of the double
    private double[] getTransPos(double[] Tm) {
        double ret[] = new double[2];
        
        ret[0] = Tm[1] + Tm[6];
        ret[1] = Tm[0];
        
        return ret;
    }
    
    // We could use a Rectangle here instead of a double[4], but Rectangle only accepts ints,
    // and we need to keep the precision of the double
    private double[] getTmBBox(double[] startpos, double[] endpos) {
        double ret[] = new double[4];

        // Start x and y is just startpos
        ret[0] = startpos[0];
        ret[1] = startpos[1];
        
        // Width and height is just endpos[x] - startpos[x]
        ret[2] = endpos[0] - startpos[0];
        ret[3] = endpos[1] - startpos[1];
        
        return ret;
    }
    
    private double[] updateTransFormMatrix(double[] Tm, PDFFont font, String text) {
        char c;
        double offset;
        for (int x=0; x<text.length(); x++) {
            c = text.charAt(x);
            
            // Get the offset
            offset = getTransdCharWidth(Tm, font, c);
            
            // Adjust the text matrix to be at this current x position
            Tm[1] += offset;
        }
        
        return Tm;
    }
    
    private double[] updateTransFormMatrix(double[] Tm, double[] lm) {
        Tm[1] = lm[1];
        Tm[2] += lm[2];
        
        return Tm;
    }
    
    private double getTransdCharWidth(double[] Tm, PDFFont font, char c) {
        // Get the glyph width (in glyph units and convert to user units)
        double width = font.getCharWidth(c)/1000.0;
        // For now assume the font width is the width of the space character
        double fWidth = font.getCharWidth(' ')/1000.0;
        
        // Figure out the actual width, that's the font width attribute of the text matrix
        // times the glyph width plus the text spacing plus the font width
        double offset = (Tm[5]*width)+Tm[6]+fWidth;
        
        return offset;
    }        
}