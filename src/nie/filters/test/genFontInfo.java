package nie.filters.test;

import nie.filters.*;
import nie.filters.pdf.*;
import nie.filters.io.*;
import org.jdom.*;
import org.jdom.output.*;
import java.util.*;
import java.io.*;
import java.net.*;

class genFontInfo {
    public static void main(String[] args) {
        Vector positional = new Vector();
        String a[] = new String[2];
        a[0] = "input"; a[1] = "The file to parse";
        positional.addElement(a.clone());
        a[0] = "output"; a[1] = "The output file";
        positional.addElement(a.clone());

        Object o[] = new Object[2];
        Hashtable keyword = new Hashtable();
        o[0] = new Integer(1); o[1] = "Only generate the XML font info file, not the pdf file";
        keyword.put("xmlonly", o.clone());
        o[0] = new Integer(1); o[1] = "Only generate the PDF font info display, not the XMLfile";
        keyword.put("xmlonly", o.clone());
        o[0] = new Integer(1); o[1] = "The transformation matrix x value";
        keyword.put("transx", o.clone());
        o[0] = new Integer(1); o[1] = "The transformation matrix y value";
        keyword.put("transy", o.clone());
        
        Hashtable values;
        CommandLineParser cmdLine = new CommandLineParser(positional, keyword, args);
        
        try {
            values = cmdLine.parse();
        } catch (Exception e) {
            System.out.println(e.toString());
            cmdLine.printUsage();
            return;
        }

        String fileName = (String)values.get("input");
        SeekableStream rd;
        
        try {
            if (fileName.startsWith("http://")) {
                rd = new URLSeekableStream(new URL(fileName));
            } else {
                rd = new FileSeekableStream(fileName);
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return;
        }

        PDFDocWriter pdfOut = new PDFDocWriter();
        
        // Need these for the parser...
        Element doc = new Element("document");
        Document pdfDoc = new Document(doc);

        Element mapping = new Element("mapping");
        Document mapDoc = new Document(mapping);

        try {
            // Build the parser
            PDFParser p = new PDFParser(rd, doc);

            // Do the parsing
            p.getContents();
            
            // Everything should be populated now, so look at the
            // font info
            Enumeration iter = p.fontStructs.keys();
            while (iter.hasMoreElements()) {
                Long refId = (Long)iter.nextElement();
                PDFFont f = (PDFFont)p.fontStructs.get(refId);
                Hashtable font = (Hashtable)p.getObject((refId.longValue()));
            
                if (!f.getType().equals("Type3")) { continue; }
                    
                // Build the PDF object
                pdfOut.buildPageDisplay(p, font, mapping, values);   
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Write out the XML document
        try {
            String outputFile = (String)values.get("output") + ".xml";
            FileWriter writer = new FileWriter(outputFile);
            XMLOutputter outputter = new XMLOutputter("  ", true);
            outputter.output(mapDoc, writer);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        // Write out the pdf document
        try {
            String outputFile = (String)values.get("output") + ".pdf";
            byte[] data = pdfOut.getText();
            FileOutputStream writer = new FileOutputStream(outputFile);
            for (int x=0; x<data.length; x++) {
                writer.write(data[x]);
            }
            writer.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        
        // Free the file
        try {
            rd.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }        

    }
}


class PDFDocWriter {
    ByteArrayOutputStream contents;
    Vector objLoc;
    Vector pages;
    
    String CRLF = "\r\n";
    int baseFont;
    
    public PDFDocWriter() {
        contents = new ByteArrayOutputStream();
        addString("%PDF-1.3" + CRLF);
        objLoc = new Vector();
        pages = new Vector();
        addHeader();
    }
    
    public void addString(int txt) {
        addString(String.valueOf(txt));
    }
    
    public void addString(String txt) {
        try {
            contents.write(txt.getBytes());
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
    
    public int addObject(String obj) {
        return addObject(obj, false);
    }
    
    public int addObject(String obj, boolean isStream) {
        byte[] o = (obj.trim()).getBytes();
        return addObject(o, isStream);        
    }

    protected int addObject(byte[] obj, boolean isStream) {
        return addObject(obj, isStream, false);
    }
    
    protected int addObject(byte[] obj, boolean isStream, boolean isRaw) {
        objLoc.add(new Long(contents.size()));
        addString(objLoc.size()+2);
        addString(" 0 obj" + CRLF );
        if (!isRaw) { 
            addString("<< ");
        }
        try {
            contents.write(obj);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        addString(CRLF);
        if (!isStream) {
            addString(">>" + CRLF);
        }
        if (!isRaw) {
            addString("endobj" + CRLF);
        }
        return objLoc.size()+2;
    }
    
    public byte[] getText() {
        buildTrailer();
        return contents.toByteArray();
    }

    public void buildPageDisplay(PDFParser parser, Hashtable fontDef, Element mapDoc, Hashtable values) 
      throws java.io.IOException {
        // Build the font structure
        String font = "";
        int x, y;

        // Start the XML
        Element XMLFont = new Element("font");
        String fontName = (String)fontDef.get("Name");
        if (fontName.charAt(0) == '/') { fontName = fontName.substring(1); }
        
        XMLFont.setAttribute("name", fontName);

        font += "/Type /Font" + CRLF;
        font += "/Subtype /Type3" + CRLF;
        font += "/Name /" + fontName + CRLF;
        
        font += "/Encoding ";
        
        String encs = "";
        encs += "/Type /Encoding" + CRLF;
        encs += "/Differences [";
        
        Hashtable enc = (Hashtable)parser.getObject(((Long)fontDef.get("Encoding")).longValue());
        Vector diffs = (Vector)enc.get("Differences");
        Object elem;
        for (x=diffs.size()-1; x>=0; x--) {
            elem = diffs.elementAt(x);
            if (elem instanceof String) {
                encs += elem;
            } else {
                encs += ((Float)elem).intValue() + " ";
            }
            //font += " ";
        }
        encs += "]" + CRLF;
        int encRef = addObject(encs);
        font += encRef + " 0 R" + CRLF;
        
        int firstChar = ((Float)fontDef.get("FirstChar")).intValue();
        int lastChar = ((Float)fontDef.get("LastChar")).intValue();
        font += "/FirstChar " + firstChar + CRLF;
        font += "/LastChar " + lastChar + CRLF;
    
        font += "/CharProcs ";
        String cprocs = "";
        Hashtable procs = (Hashtable)fontDef.get("CharProcs");
        Iterator iter = (new TreeSet(procs.keySet())).iterator();
        while (iter.hasNext()) {
            String key = (String)iter.next();
            Hashtable glyph = (Hashtable)parser.getObject(((Long)procs.get(key)).longValue());

            int glyphRef = addObject(parser.getObjectRaw(((Long)procs.get(key)).longValue()), true, true);
            cprocs += "/" + key + " " + glyphRef + " 0 R" + CRLF;
        }
        int cRef = addObject(cprocs);
        font += cRef + " 0 R" + CRLF;

        Vector widths = new Vector();
        String arrs[] = {"FontBBox", "FontMatrix", "Widths"};
        for (x=0; x<arrs.length; x++) {
            font += "/" + arrs[x] + " [";
            Vector _v = (Vector)fontDef.get(arrs[x]);
            for (int z=_v.size()-1; z>=0; z--) {
                font += ((Float)_v.elementAt(z)).intValue();
                if (z != 0) { font += " "; }
                
                if (x==2) {
                    widths.add(_v.elementAt(z));
                }
            }
            font += "]" + CRLF;
        }

        // add the font object
        int fontRef = addObject(font);
        String stream = "";
                
        int pageHeight = 792;
        int pageWidth = 612;
        int margin = 10;
        Vector textSlots = new Vector();
        
        // Build the table display
        stream += "10 762 592 20 re" + CRLF + "S" + CRLF;
        
        // Build the columns        
        int colStart[] = {40, 130, 220, 310, 400, 490};
        int colWidth = 30;
        int colHeight = 16;
        for (x=0; x<colStart.length; x++) {
            int xstart = colStart[x];
            
            // Draw the outside box
            stream += String.valueOf(xstart) + " 20 " + String.valueOf(colWidth*2) + " 722 re" + CRLF + "S" + CRLF;
            
            // Draw the interior lines
            stream += "q" + CRLF;
            stream += "0 w" + CRLF;
            for (int z=742; z>=colHeight*1.5; z-=colHeight) {
                stream += String.valueOf(xstart) + " " + String.valueOf(z) + " m" + CRLF;
                stream += String.valueOf(xstart+(colWidth*2)) + " " + String.valueOf(z) + " l" + CRLF;
                stream += "s" + CRLF;
                
                int slot[] = {xstart+(colWidth/2), xstart+colWidth+(colWidth/2), z-(colHeight/2)};
                textSlots.add(slot);
            }
            stream += String.valueOf(xstart+colWidth) + " 742 m" + CRLF;
            stream += String.valueOf(xstart+colWidth) + " 20 l" + CRLF;
            stream += "s" + CRLF;
            stream += "Q" + CRLF;
        }
        
                
        // Build the stream contents
        stream += "0.1 0 0 0.1 0 0 cm" + CRLF + "q" + CRLF + "q" + CRLF + "10 0 0 10 0 0 cm" + CRLF;
        stream += "BT" + CRLF;

        // Add the header info
        stream += "/BASE 10 Tf" + CRLF;
        stream += "1 0 0 1 250 770 Tm" + CRLF;
        stream += "(Font: " + fontName + "    Glyphs: " + String.valueOf(lastChar-firstChar) + ") Tj" + CRLF;

        stream += "/BASE 10 Tf" + CRLF;
        for (x=firstChar, y=0; x<lastChar && y<textSlots.size(); x++, y++) {
            int slot[] = (int[])textSlots.elementAt(y);
            
            // Add the glyph code number reference
            String pos[] = {String.valueOf(slot[0]-4), String.valueOf(slot[2]-4)};
            stream += "1 0 0 1 " + pos[0] + " " + pos[1] + " Tm" + CRLF;
            stream += "(" + String.valueOf(x) + ") Tj" + CRLF;
            
            String pos2[] = {String.valueOf(slot[1]+20), String.valueOf(slot[2]-4)};
            stream += "1 0 0 1 " + pos2[0] + " " + pos2[1] + " Tm" + CRLF;
            int w = ((Float)widths.elementAt(x)).intValue();
            stream += "(" + String.valueOf(w) + ") Tj" + CRLF;
        }

        String transX = values.containsKey("transx") ? (String)values.get("transx") : "1";
        String transY = values.containsKey("transy") ? (String)values.get("transy") : "-1";

        char[] c = new char[1];
        stream += "/" + fontName + " 0.1 Tf" + CRLF;
        for (x=firstChar, y=0; x<lastChar && y<textSlots.size(); x++, y++) {
            // Get the glyph code
            c[0] = (char)x;
            String ch = new String(c);
            if (ch.equals("(")) ch = "\\(";
            else if (ch.equals(")")) ch = "\\)";
            else if (ch.equals("\\")) ch = "\\\\";
            
            int slot[] = (int[])textSlots.elementAt(y);
            String pos[] = {String.valueOf(slot[1]-4), String.valueOf(slot[2]+2)};
            
            // Add the glyph (the transformation matrix will probably need to be adjusted
            // for each producer, so check if anything was passed in
            //  Tm = <transx> 0 0 <transy> <x> <y>
            stream += transX + " 0 0 " + transY + " " + pos[0] + " " + pos[1] + " Tm" + CRLF;
            stream += "(" + ch + ") Tj" + CRLF;
            
            Element gelem = new Element("glyph");
            gelem.setAttribute("code", String.valueOf(x));
            gelem.addContent(" ");
            XMLFont.addContent(gelem);
        }
        
        stream += "ET" + CRLF;
        stream += "Q" + CRLF + "Q" + CRLF;

        // Build the contents
        String cs = "";
        cs += "/Length " + stream.length();
        cs += " >>" + CRLF + "stream" + CRLF;
        cs += stream;
        cs += "endstream" + CRLF;

        // Add the contents object
        int contRef = addObject(cs, true);

        String p = "";
        p += "/Type /Page" + CRLF;
        p += "/Parent 2 0 R" + CRLF;
        p += "/Contents " + contRef + " 0 R" + CRLF;
        p += "/Mediabox [0 0 612 792]" + CRLF;
        p += "/Resources << /ProcSet [/PDF /ImageB /Text]" + CRLF;
        p += "/Font << /" + fontName +  " " + fontRef + " 0 R /BASE " + this.baseFont + " 0 R >>" + CRLF;
        p += " >>" + CRLF;
                
        // Add the page object
        int pageRef = addObject(p);
        
        pages.add(new Integer(pageRef));
        mapDoc.addContent(XMLFont);
    }
    
    private void addHeader() {
        
        String h = new String();
        h += "/Producer (genFontInfo)" + CRLF;
        h += "/Title (Type 3 Font Information)" + CRLF;
        h += "/Creator (genFontInfo)" + CRLF;
        
        Calendar cal = Calendar.getInstance();

        String t = "D:";
        t += cal.get(Calendar.YEAR);
        t += cal.get(Calendar.MONTH);
        t += cal.get(Calendar.DAY_OF_MONTH);
        t += cal.get(Calendar.HOUR_OF_DAY);
        t += cal.get(Calendar.MINUTE);
        t += cal.get(Calendar.SECOND);
        
        //h += "/ModDate (" + t + ")\n";
        //h += "/CreationDate (" + t + ")\n";
        
        //addObject(h);
        
        String font = "";
        font += "/Type /Font" + CRLF;
        font += "/Subtype /Type1" + CRLF;
        font += "/Name /BASE" + CRLF;
        font += "/BaseFont /Helvetica" + CRLF;
        font += "/Encoding /MacRomanEncoding" + CRLF;
        this.baseFont = addObject(font);

    }
        
    private void buildTrailer() {
        // add the catalog object
        Long catLoc = new Long(contents.size());
        addString("1 0 obj" + CRLF + "<< ");
        addString("/Type /Catalog" + CRLF);
        addString("/Pages 2 0 R >>" + CRLF);
        addString("endobj" + CRLF);

        // add the page root
        Long rootLoc = new Long(contents.size());
        addString("2 0 obj" + CRLF + "<< ");
        addString("/Type /Pages" + CRLF);
        addString("/Kids [");
        Iterator i = pages.iterator();
        while (i.hasNext()) {
            int ref = ((Integer)i.next()).intValue();
            addString(ref);
            addString(" 0 R ");
        }
        addString("]" + CRLF);
        addString("/Count ");
        addString(pages.size());        
        addString(" >>" + CRLF + "endobj" + CRLF);
        
        // get the location of the start of the xref
        int xrefLoc = contents.size();
        
        // add the ref
        addString("xref" + CRLF + "0 ");
        addString(objLoc.size()+3);
        addString(CRLF + "0000000000 65535 f" + CRLF);

        // Add the catalog object
        for (int x=10-(catLoc.toString()).length(); x>0; x--) {
            addString("0");
        }
        addString(catLoc.toString());
        addString(" 00000 n " + CRLF);        
        // Add the root object
        for (int x=10-(rootLoc.toString()).length(); x>0; x--) {
            addString("0");
        }
        addString(rootLoc.toString());
        addString(" 00000 n " + CRLF);        

        Iterator iter = objLoc.iterator();
        while (iter.hasNext()) {
            String ind = String.valueOf(((Long)iter.next()).intValue()+2);
            for (int x=10-ind.length(); x>0; x--) {
                addString("0");
            }
            addString(ind);
            addString(" 00000 n " + CRLF);
        }
        
        // Build the trailer
        addString("trailer" + CRLF + "<<" + CRLF);
        addString("/Size ");
        addString(objLoc.size()+3);
        addString(CRLF);
        //addString("/Info 1 0 R\n");
        addString("/Root 1 0 R" + CRLF);
        addString(">>" + CRLF);
        
        // Add the xref position
        addString("startxref" + CRLF);
        addString(xrefLoc + CRLF);
        addString("%%EOF" + CRLF);
    }
}
