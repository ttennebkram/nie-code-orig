
package nie.filters;

import nie.filters.io.*;
import nie.filters.html.*;
import org.w3c.tidy.Tidy;

// Have to be careful about what gets imported since there are 2 DOM implementations
// here that have overlapping names (org.jdom and org.w3c.dom)
// We should be ok here, importing Attr, NamedNodeMap, Node and NodeList from jtidy
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

// Importing just Element from JDOM
import org.jdom.Element;


public class HTMLParser extends ToXMLParser implements HTMLConst {
    
    private boolean inBody = false;

    public HTMLParser(SeekableStream data, Element rootElement) {
        super(data, rootElement);
    }

    public boolean getContents() {
        Tidy tidy = new Tidy();
        
        // If clean is set, tell Tidy to clean it
        if (this.getSettingInt("clean", new Integer(-1)).intValue() != -1) {
            tidy.setMakeClean(true);
        }

        tidy.setTidyMark(false);

        // Set an error capture writer that will redirect output errors to the
        // XML element
        boolean showWarning = this.getSettingInt("nowarn", new Integer(-1)).intValue() == -1 ? true : false;
        ErrWriter err = new ErrWriter(this.errElement, showWarning);
        tidy.setErrout(err);
        
        // Start parsing it
        return this.getNodeInfo(tidy.parseDOM(this.data, null), this.bodyElement);
    }   

    protected boolean getNodeInfo(Node node, Element parent) {
        if (node == null) { return RETURN_OK; }
     
        int type = node.getNodeType();
        switch (type) {
        case Node.DOCUMENT_NODE:
            // Ignore the document node for now
            this.getNodeInfo(((org.w3c.dom.Document)node).getDocumentElement(), parent);
            break;
        case Node.ELEMENT_NODE:
            String nodeName = node.getNodeName();
            Element e;
            boolean doAdd = false;
            if (nodeName.equalsIgnoreCase("head")) {
                e = this.infoElement;
            } else if (nodeName.equalsIgnoreCase("body")) {
                e = this.bodyElement;
            } else {
                doAdd = true;
                if (skipNode(nodeName)) { break; }
                e = new Element(nodeName);
            }

            NamedNodeMap attrs = node.getAttributes();
                
            for (int i=0; i<attrs.getLength(); i++) {
				// e.setAttribute(attrs.item(i).getNodeName(), attrs.item(i).getNodeValue());
				// mbennett: the attribute name xmlns is causing problems
				String tmpName = attrs.item(i).getNodeName();
				if( null!=tmpName && tmpName.startsWith("xmlns") )
					tmpName = "x_" + tmpName;
				// mbennett: colons are causing problems
				if( null!=tmpName && tmpName.indexOf(':') >= 0 )
					tmpName = nie.core.NIEUtil.simpleSubstitution( tmpName, ":", "_" );
				String tmpValue = attrs.item(i).getNodeValue();
				e.setAttribute( tmpName, tmpValue );
            }

            NodeList children = node.getChildNodes();
            if (children != null) {
                int len = children.getLength();
                for (int i=0; i<len; i++) {
                    this.getNodeInfo(children.item(i), e);
                }
            }

            if (doAdd) {
                parent.addContent(e);
            }

            break;
 
        case Node.TEXT_NODE:
        	// mbennett: sometimes get junk
        	try {
	            parent.addContent(node.getNodeValue());
			}
			catch( org.jdom.IllegalDataException ex ) {
				String tmpData = node.getNodeValue();
				if( null!=tmpData && tmpData.length() > 1 )
					System.err.println(
						"Warning: nie.filters.HTMLParser: getNodeInfo:"
						+ " JDOM Date error for string \"" + tmpData + "\""
						+ " Error: " + ex
						+ " Ignoring data."
						);
			}
            break;
        }
     
        return RETURN_OK;
    }
    
    
    protected boolean skipNode(String nodeName) {
        if (nodeName.equalsIgnoreCase("style")) {
            // If nostyle was set, return true, otherwise false
            if (this.getSettingInt("nostyle", new Integer(-1)).intValue() != -1)
                return true;
            return false;
        } else if (nodeName.equalsIgnoreCase("script")) {
            // If noscript was set, return true, otherwise false
            if (this.getSettingInt("noscript", new Integer(-1)).intValue() != -1)
                return true;
            return false;
        }
        return false;
    }
}
