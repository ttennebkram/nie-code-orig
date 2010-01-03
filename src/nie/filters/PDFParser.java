
package nie.filters;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.jdom.*;
import nie.filters.io.*;
import nie.filters.pdf.*;

public class PDFParser extends ToXMLParser implements PDFConst {
    
    // PDF information
    private long startXrefPos = -1;
    private long infoRef = -1;
    private long rootRef = -1;
    private long encryptRef = -1;
    private Hashtable xref;
    private Hashtable _objects;
    public Hashtable fontStructs;
    public Vector fileId;
    public PDFDecrypter decrypter = null;

    public int charCount = 0;
    public int pageCharCount = 0;
    
    public PDFParser(SeekableStream data, Element rootElement) {
        super(data, rootElement);
        
        this.xref = new Hashtable();
        this._objects = new Hashtable();
        this.fontStructs = new Hashtable();
    }

    public boolean getContents() {
        // Get the startxref position
        if (!this.getStartXref()) { return RETURN_ERR; }
        
        // Get all the xrefs and trailers
        try {
            this.getXref(this.startXrefPos);
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return RETURN_ERR;
        }

        // Setup the decryption stuff
        Hashtable encDict;
        if (this.encryptRef >= 0) {
            try {
                encDict = (Hashtable)getObject(this.encryptRef);
            } catch (java.io.IOException e) {
                e.printStackTrace();
                return RETURN_ERR;
            }
        } else {
            encDict = new Hashtable();
        }
        
        try {
            this.decrypter = new PDFDecrypter(fileId, encDict, "", "");
        } catch (PDFException e) {
            e.printStackTrace();
            return RETURN_ERR;
        }

        // Get the info dictionary if it exists
        if (this.infoRef >= 0) {
            try {
                this.getInfo();
            } catch (java.io.IOException e) {
                e.printStackTrace();
                return RETURN_ERR;
            }
        }
        
        // Get the page tree
        try {
            this.getPageTree();
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return RETURN_ERR;
        }
        
        return RETURN_OK;
    }   

    private boolean getStartXref() {
        int offset = PDF_STARTXREF_INCR;
        for (int r=0; r<PDF_STARTXREF_MAXINCR; r++, offset+=PDF_STARTXREF_INCR) {
            byte[] buf;
            try {
                this.data.seek(this.data.length() - offset);
                buf = this.readUntilToken(PDF_TOKEN_STARTXREF);
                // Read until the end of the file...
                buf = readUntilToken(PDF_TOKEN_EOF);
            } catch (java.io.IOException e) {
                this.signalError(PDF_ERR_IOERROR, PDF_TOKEN_STARTXREF, e);
                e.printStackTrace();
                return RETURN_ERR;
            }

            // If we found anything, parse it
            if (buf.length != 0) {
                StringBuffer val = new StringBuffer();
                for (int i=0; i<buf.length; i++) {
                    if (!Character.isDigit((char)buf[i])) { break; }
                    val.append((char)buf[i]);
                }
                if (val.length() > 0) {
                    this.startXrefPos = (new Long(new String(val))).longValue();
                    return RETURN_OK;
                }
            }
        }
        
        // Anything else is an error condition...
        this.signalError(PDF_ERR_EOF, PDF_TOKEN_STARTXREF);
        return RETURN_ERR;
    }

    private boolean getXref(long startPos) 
      throws java.io.IOException {
        this.data.seek(startPos);
        
        byte[] xrefData = this.readUntilToken(PDF_TOKEN_TRAILER);
        byte[] trailerData = this.readUntilToken(PDF_TOKEN_STARTXREF);
        
        Hashtable trailer = (Hashtable)this.parseObject(trailerData, 0);

        if (trailer.containsKey("Root")) {
            this.rootRef = ((Long)trailer.get("Root")).longValue();
        }
        if (trailer.containsKey("Info")) {
            this.infoRef = ((Long)trailer.get("Info")).longValue();
        }
        
        if (trailer.containsKey("Prev")) {
            long pos = ((Float)trailer.get("Prev")).longValue();
            // Call this function again with the new location
            if (this.getXref(pos) != RETURN_OK) {
                return RETURN_ERR;
            }
        }

        if (trailer.containsKey("Encrypt")) {
            this.encryptRef = ((Long)trailer.get("Encrypt")).longValue();
        }

        if (trailer.containsKey("ID")) {
            this.fileId = (Vector)trailer.get("ID");
        }

        // Do we want anything from the trailer in the XML?   For now just Size
        Enumeration keys = trailer.keys();
        while (keys.hasMoreElements()) {
            String key = (String)keys.nextElement();
            String lkey = key.toLowerCase();
            if (lkey.equals("size")) {
                Element s = new Element("numobjects");
                s.addContent(String.valueOf(((Float)trailer.get(key)).intValue()));
                this.addInfo(s);
            }
        }
        
        // Parse the xref data
        if (this.parseXref(xrefData) != RETURN_OK) { return RETURN_ERR; }
        
        return RETURN_OK;
    }

    private boolean getInfo() throws java.io.IOException {
        Hashtable info = (Hashtable)this.getObject(this.infoRef);
        Enumeration keys = info.keys();
        while (keys.hasMoreElements()) {
            String key = (String)keys.nextElement();
            Element elem = new Element(key.toLowerCase());
            Object value = info.get(key);
            if (PDFDate.isDate(value)) {
                // Two ways to do this, either have it as an attribute of the current element
                // or have a subelement named date with the value as the text.   For now go with
                // the first option
                value = new PDFDate(value);  // Just to be safe
                elem.setAttribute("dateTimeValue", ((PDFDate)value).toString());
                
                /*
                Element date = new Element("date");
                date.addContent(((PDFDate)value).toString());
                elem.addContent(date);
                */
                
            } else {
                elem.addContent((String)value);
            }
            this.addInfo(elem);
        }

        return RETURN_OK;
    }

    private boolean getPageTree() throws java.io.IOException {
        // Get the catalog object
        Hashtable catalog = (Hashtable)this.getObject(this.rootRef);
        Hashtable props = new Hashtable();
        
        // Get the page structure(s)
        Vector pages = new Vector();
        if (this.getPageNode(((Long)catalog.get("Pages")).longValue(), pages, props) != RETURN_OK) { 
            return RETURN_ERR; 
        } 
        
        // Add the number of pages to the info element
        this.addInfo("numpages", (new Integer(pages.size())).toString());
        
        int maxPages = (this.getSettingInt("maxpages", new Integer(-1))).intValue();
        
        // Go through each page and add everything to the appropriate place
        Iterator iter = pages.iterator();
        long charOffset = 0;
        int pageNum = 1;
        Hashtable page;
        for ( ; iter.hasNext(); pageNum++) {
            Element pageElem = new Element("page");
            pageElem.setAttribute("num", (new Integer(pageNum)).toString());
            page = (Hashtable)iter.next();
            this.pageCharCount = 0;
                        
            if (page.containsKey("Contents")) {
                Vector content;
                Object o = page.get("Contents");
                if (o instanceof Vector) {
                    content = (Vector)o;
                } else {
                    content = new Vector(1);
                    content.addElement(o);
                }
                    
                Iterator contentRefs = content.iterator();
                while (contentRefs.hasNext()) {
                    Long refNum = (Long)contentRefs.next();
                    Long genNum = new Long(0);
                    Object co = this.getObject(refNum.longValue());
                    Vector ccontent = new Vector();
                    if (co instanceof Vector) {
                        Iterator citer = ((Vector)co).iterator();
                        while (citer.hasNext()) {
                            refNum = (Long)citer.next();
                            Hashtable h = (Hashtable)this.getObject(refNum.longValue());
                            h.put("refNum", refNum);
                            h.put("genNum", genNum);
                            ccontent.addElement(h);
                        }
                    } else {
                        ((Hashtable)co).put("refNum", refNum);
                        ((Hashtable)co).put("genNum", genNum);
                        ccontent.addElement(co);
                    }
                    
                    Iterator ccontiter = ccontent.iterator();
                    while (ccontiter.hasNext()) {
                        Hashtable c = (Hashtable)ccontiter.next();
                        
                        try {
                            if ((new PDFStreamParser(this, page, c, pageElem)).parse() != RETURN_OK) {
                                return RETURN_ERR;
                            }
                        } catch (PDFException e) {
                            this.signalError(e);
                            return RETURN_ERR;
                        }
                    }
                }
            }
            
            this.addBodyContent(pageElem);

            if ((maxPages > 0) && (pageNum >= maxPages)) { break; }
        }
        
        return RETURN_OK;
    }

    private boolean getPageNode(long refNum, Vector pages, Hashtable props) throws java.io.IOException {
        Hashtable page = (Hashtable)this.getObject(refNum);
        Hashtable thisProps = (Hashtable)props.clone();
        
        // For now only propogate the resource dictionary
        if (page.containsKey("Resources")) {
            Object o = page.get("Resources");
            // Apparently this can either be a reference or a dictionary.   If it's a reference, the object
            // should contain the dictionary
            if (o instanceof Long) {
                // It's a reference... get the object
                Hashtable p = (Hashtable)this.getObject(((Long)o).longValue());
                // Just override o with p
                o = p;
            }                

            Enumeration keys = ((Hashtable)o).keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object val = ((Hashtable)o).get(key);
                // If this is a font, get the actual font definition
                if ((key instanceof String) && (key.equals("Font")) && (val instanceof Hashtable)) {
                    // This should be a hashtable of name/reference mappings, change that to name/object mappings
                    Enumeration fonts = ((Hashtable)val).keys();
                    while (fonts.hasMoreElements()) {
                        String fontName = (String)fonts.nextElement();
                        Object fontRef = ((Hashtable)val).get(fontName);
                        if (fontRef instanceof Long) {
                            Hashtable font = (Hashtable)this.getObject(((Long)fontRef).longValue());
                            //((Hashtable)val).put(fontName, font);
                            try {
                                PDFFont f = new PDFFont(this, font);
                                ((Hashtable)val).put(fontName, f);
                                
                                if (!this.fontStructs.containsKey(fontRef)) {
                                    this.fontStructs.put(fontRef, f);
                                }
                            } catch (PDFException e) {
                                this.signalError(e);
                                return RETURN_ERR;
                            }
                        }
                    }                            
                }
                thisProps.put(key, val);
            }
        }
        
        if (((String)page.get("Type")).equals("/Pages")) {
            // It's another tree...  (need to reverse the order to get the descension correct)
            int maxPages = (this.getSettingInt("maxpages", new Integer(-1))).intValue();
            if ((maxPages > 0) && (pages.size() >= maxPages)) {
                return RETURN_OK;
            }
            Vector v = (Vector)page.get("Kids");
            for (int i=v.size()-1; i>=0; i--) {
                if (!this.getPageNode(((Long)v.elementAt(i)).longValue(), pages, thisProps) == RETURN_OK) { 
                    return RETURN_ERR; 
                }
            }
        } else {
            // An actual page...
            page.put("refNum", new Long(refNum));
            // If it has a resources already, overwrite it
            page.put("Resources", thisProps);
            pages.addElement(page);
        }
         
        return RETURN_OK;
    }

    public Object getObject(long refNum) throws java.io.IOException {
        if (this._objects.containsKey(new Long(refNum))) {
            return this._objects.get(new Long(refNum));
        } else {
            byte[] data = this.getObjectRaw(refNum);
            
            Object o = this.parseObject(data, refNum);
            this._objects.put(new Long(refNum), o);
            return o;
        }
    }

    public byte[] getObjectRaw(long refNum) throws java.io.IOException {
        long pos = ((Long)this.xref.get(new Integer((int)refNum))).longValue();
        this.data.seek(pos);
        
        byte[] trash = this.readUntilToken(PDF_TOKEN_STARTOBJ);
        byte[] data = this.readUntilToken(PDF_TOKEN_ENDOBJ);
        return data;
    }

    private Object parseObject(byte[] data, long refNum) {
        StringTokenizer2 st;
        st = new StringTokenizer2(BytesToString.convert(data), " \t\r\n[]()<>/", true);
        
        //try { 
        //    st = new StringTokenizer2(new String(data, "UTF-16"), " \t\r\n[]()<>/", true);
        //} catch (java.io.UnsupportedEncodingException e) {
        //    st = new StringTokenizer2(new String(data), " \t\r\n[]()<>/", true);
        //}
        
        Stack stack = new Stack();
        String token = new String("");
        String carryOver = "";
        boolean getToken = true;
        Hashtable followRefs = new Hashtable();
        followRefs.put("/Length", new Integer(1));
        followRefs.put("Length", new Integer(1));
                
        while (st.hasMoreTokens()) {
            // Basically just keep pushing things onto the stack until we find something 
            // recognizable (that will presumably consume all of the stack that it knows about)
            // This allows objects to be seemlessly nested as long as the top level objects
            // consume the stack with the sub objects on it
            if (getToken) { token = st.nextToken(); }
            else { getToken = true; }
            
            if (carryOver.length() > 0) {
                token = carryOver + token;
                carryOver = "";
            }
            
            // Hack: Since Ghostscript doesn't include spaces between the tokens and instead just
            // uses the / to delimit stuff, the / character has to be included as a seperator, but
            // it can't work on it's own, so just carry it over and prepend it to the next token
            if (token.equals("/")) {
                carryOver = token;
            } else if (PDFBasicObjParser.isBasicObject(token)) {
                try { PDFBasicObjParser.parseBasicObject(this, (int)refNum, 0, st, stack, token); }
                catch (PDFException e) {
                    this.signalError(e);
                }
            } else if (token.equals(PDF_TOKEN_STREAMSTART)) {
                // It's a stream... need to go ahead and consume it all so it doesn't confuse anything else
                Hashtable stream = new Hashtable();
                // The attributes dictionary should be the last thing on the stack, grab that
                stream.put("attrs", stack.pop());
                Hashtable attrs = (Hashtable)stream.get("attrs");

                // Need to look at the first couple of characters, the spec states:
                //      The keyword stream that follows the stream dictionary should be 
                //      followed by either a carriage return and a line feed or by just a 
                //      line feed, and not by a carriage return alone. 
                //      ...
                //      Note: Without the restriction against following the keyword stream 
                //      a carriage re-turn alone, it would be impossible to differentiate a 
                //      stream that uses carriage return as its end-of-line marker and has a 
                //      line feed as its first byte of data from one that uses a carriage 
                //      return–line feed sequence to denote end-of-line.
                int startPos = st.getCurrentPosition();
                String tok1, tok2;
                tok1 = st.nextToken(); tok2 = st.nextToken();
                if (tok1.equals("\r") && tok2.equals("\n")) { 
                    startPos += 2; 
                } else if (tok1.equals("\n")) { 
                    startPos++; 
                }
                
                // Read in the stream, what we really want to do here is just grab the position of the stream start
                // tag and the position of the endstream tag so we can read that directly out of the data[] array
                while (st.hasMoreTokens()) {
                    token = st.nextToken();
                    if (token.equals(PDF_TOKEN_STREAMEND)) { break; }
                }
                int endPos = st.getCurrentPosition()-PDF_TOKEN_STREAMEND.length();
                int len = endPos-startPos;
                
                if (attrs.containsKey("Length")) {
                    int sLen = ((Long)attrs.get("Length")).intValue();

                    // From the spec:
                    //    Every stream dictionary has a Length entry that indicates how many bytes of the
                    //    PDF file are used for the stream’s data. (If the stream has a filter, Length is the
                    //    number of bytes of encoded data.) In addition, most filters are defined so that the
                    //    data is self-limiting; that is, they use an encoding scheme in which an explicit
                    //    end-of-data (EOD) marker delimits the extent of the data. Finally, streams are
                    //    used to represent many objects from whose attributes a length can be inferred. All
                    //    of these constraints must be consistent.
                    //    It is also an error if the stream contains too much data, with the exception that
                    //    there may be an extra end-of-line marker in the PDF file before the keyword end-stream.                    
                    
                    // See if there is too much or too little data
                    if (len > sLen) {
                        // Just use the sLen and the remaining stuff will be chopped off
                        len = sLen;
                    } else if (len < sLen) {
                        // Hmm... this is a problem.   There is not enough data to decode, so we need to just bail.
                        stream.put("stream", "");
                        stack.push(stream);
                        continue;
                    }
                }
                
                byte[] sData = new byte[len];
                System.arraycopy(data, startPos, sData, 0, len);
                // Need to decrypt it
                if (decrypter != null) {
                    sData = decrypter.decryptBytes(sData, (int)refNum, 0);
                }
                stream.put("stream", sData);
                stack.push(stream);
            } else if (token.equals("R")) {
                // It's a reference, ignore the generation number for now
                stack.pop();
                Long i = new Long((long)((Float)stack.pop()).floatValue());
                Object key = stack.pop();
                stack.push(key);
                if ((key instanceof String) && (followRefs.containsKey(key))) {
                    // Need to get the object at the reference (not sure this will work with anything
                    // but the length)
                    try {
                        byte[] rdata = getObjectRaw(i.longValue());
                        String rsdata = new String(rdata);
                        StringTokenizer rst = new StringTokenizer(rsdata);
                        rsdata = rst.nextToken();
                        i = Long.valueOf(rsdata);
                    } catch (java.io.IOException e) {
                    }
                }
                stack.push(i);
            } else if (token.equals(PDF_TOKEN_STARTXREF)) {
                break;
            } else if (token.equals(PDF_TOKEN_TRAILER)) {
                break;
            } else if (token.equals(PDF_TOKEN_ENDOBJ)) {
                break;
            } else {
                // Just add it to the stack
                stack.push(token);
            }
        }
        
        // Return whatever is on the stack...
        return stack.pop();
    }

    private boolean parseXref(byte[] data) {
        StringTokenizer st = new StringTokenizer(new String(data), " \t\r\n", false);
        String token;

        // Pull off the initial xref tag
        token = st.nextToken();
        if (!token.equals(PDF_TOKEN_XREF)) {
            // Hmmm, should be xref but isn't
            this.signalError(PDF_ERR_FORMATERR, PDF_TOKEN_XREF);
            return RETURN_ERR;
        }
        
        int index, count, i;
        Long pos, gen;
        String type;

        while (st.hasMoreTokens()) {
            token = st.nextToken();
            if ((token instanceof String) && (token.equals(PDF_TOKEN_TRAILER))) {
                break;
            }
            
            // This line should be the first index number followed by the number of objects in this
            // subsection
            index = Integer.parseInt(token);
            count = Integer.parseInt(st.nextToken());

            // Loop through count items and build the references
            for (i=0; i<count; i++, index++) {
                pos = new Long(st.nextToken());
                gen = new Long(st.nextToken());
                type = st.nextToken();
                
                // Don't deal with freed entries since those don't matter in the resulting XML
                if (type.equals("n")) {
                    this.xref.put(new Integer(index), pos);
                }
            }
        }
        
        return RETURN_OK;
    }

}
