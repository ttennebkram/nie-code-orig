
package nie.filters;

import java.io.*;
import java.util.*;
import org.jdom.*;
import nie.filters.io.*;

abstract class ToXMLParser {
    
    // The input source
    protected SeekableStream data;
    
    // JDOM output elements
    protected Element infoElement;
    protected Element bodyElement;
    protected Element errElement;
    
    protected Hashtable settings;

    public ToXMLParser(SeekableStream data, Element rootElement) {
        this.data = data;
        
        this.settings = new Hashtable();
        
        this.infoElement = new Element("info");
        this.bodyElement = new Element("body");
        this.errElement = new Element("error");
        
        rootElement.addContent(this.infoElement);
        rootElement.addContent(this.bodyElement);
        rootElement.addContent(this.errElement);
        
        grabInfoFromStream();
    }

    private void grabInfoFromStream() {
        Hashtable info = this.data.getInfo();
        Enumeration keys = info.keys();
        String key, value;
        while (keys.hasMoreElements()) {
            key = (String)keys.nextElement();
            value = (String)info.get(key);
            
            Element e = new Element(key);
            e.addContent(value);
            addInfo(e);
        }
    }

    protected void addInfo(Element info) {
        this.infoElement.addContent(info);
    }
    
    protected void addInfo(String info) {
        this.infoElement.addContent(info);
    }
    
    protected void addInfo(String key, String value) {
        Element e = new Element(key);
        e.addContent(value);
        this.addInfo(e);
    }

    protected void addBodyContent(Element content) {
        this.bodyElement.addContent(content);
    }

    public void setSettings(Hashtable settings) {
        this.settings = settings;
    }

    public Object getSetting(String name, Object def) {
        if (this.settings.containsKey(name)) {
            return this.settings.get(name);
        } else {
            return def;
        }
    }

    public Integer getSettingInt(String name, Integer def) {
        Object theVal = this.getSetting(name, def);
        if (theVal instanceof String) {
            return new Integer((String)theVal);
        }
        return (Integer)theVal;
    }

    public void signalError(Exception e) {
        this.signalError("", "", -1, e.toString());
    }

    public void signalError(String errText, String token) {
        this.signalError(errText, token, -1, "");
    }
    
    public void signalError(String errText, String token, Exception e) {
        this.signalError(errText, token, -1, e.toString());
    }
    
    public void signalError(String errText, String token, long filePos, Exception e) {
        this.signalError(errText, token, filePos, e.toString());
    }
    
    public void signalError(String errText, String token, long filePos, String expText) {
        Element err = new Element("exception");
        err.setAttribute("token", token);
        if (filePos >= 0) {
            err.setAttribute("pos", new Long(filePos).toString());
        }
        if (expText.length() > 0) {
            Element traceback = new Element("traceback");
            traceback.addContent(expText);
            err.addContent(traceback);
        }
        err.addContent(errText.trim());
        this.errElement.addContent(err);
    }
    
    public abstract boolean getContents();

    protected byte[] readUntilToken(String endToken) throws java.io.IOException {
        ByteArrayOutputStream ok = new ByteArrayOutputStream();
        int curChar;
        int endTokenLen = endToken.length();
        StringBuffer toCheck = new StringBuffer("");
        
        for ( ;; ) {
            curChar = this.data.readUnsignedByte();
            
            // Tokens must be on a line by themselves, so if this is a newline\linefeed
            // go ahead and check if this is the token
            if ((curChar == '\r') || (curChar == '\n')) {
                // If we haven't gotten enough characters yet, don't bother checking....
                if (toCheck.length() >= endTokenLen) {
                    if ((toCheck.toString()).equals(endToken)) {
                        ok.write(curChar);
                        
                        // Got the end...  make sure the correct linefeed\newline combination is there
                        //      Dos is \r\n
                        //      Mac is \r
                        //      Unix is \n
                        if (curChar == '\r') {
                            try {
                                curChar = this.data.readUnsignedByte();
                            } catch (java.io.IOException e) {
                                // we're finished anyways, just ignore it...
                                break;
                            }
                            if (curChar == '\n') {
                                //ok.append(curChar);
                                ok.write(curChar);
                            } else {
                                // Need to set it back
                                this.data.seek(this.data.getFilePointer()-1);
                            }
                        }
                        break;                        
                    }
                }
            }
            
            if (toCheck.length() == endTokenLen) {
                toCheck.deleteCharAt(0);
            }
            toCheck.append((char)curChar);
            
            // Add this to the ok stuff
            ok.write(curChar);
        }
        
        return ok.toByteArray();
    }
   
}