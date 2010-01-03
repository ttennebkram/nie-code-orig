/*-- 

 $Id$

 Copyright (C) 2000 Brett McLaughlin & Jason Hunter.
 All rights reserved.
 
 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions, and the following disclaimer.
 
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions, and the disclaimer that follows 
    these conditions in the documentation and/or other materials 
    provided with the distribution.

 3. The name "JDOM" must not be used to endorse or promote products
    derived from this software without prior written permission.  For
    written permission, please contact license@jdom.org.
 
 4. Products derived from this software may not be called "JDOM", nor
    may "JDOM" appear in their name, without prior written permission
    from the JDOM Project Management (pm@jdom.org).
 
 In addition, we request (but do not require) that you include in the 
 end-user documentation provided with the redistribution and/or in the 
 software itself an acknowledgement equivalent to the following:
     "This product includes software developed by the
      JDOM Project (http://www.jdom.org/)."
 Alternatively, the acknowledgment may be graphical using the logos 
 available at http://www.jdom.org/images/logos.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED.  IN NO EVENT SHALL THE JDOM AUTHORS OR THE PROJECT
 CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 This software consists of voluntary contributions made by many 
 individuals on behalf of the JDOM Project and was originally 
 created by Brett McLaughlin <brett@jdom.org> and 
 Jason Hunter <jhunter@jdom.org>.  For more information on the 
 JDOM Project, please see <http://www.jdom.org/>.
 
 */

package org.jdom.input;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;


/**
 * <p><code>HTMLBuilder</code> builds a JDOM tree using SAX.</p>
 *
 * @author Brett McLaughlin
 * @author Jason Hunter
 * @author Dan Schaffer
 * @author Laurent Rom�o
 * @version 1.0
 */
public class HTMLBuilder {

    org.w3c.tidy.Tidy tidy;
   
    /**
     * <p>
     * Creates a new HTMLBuilder 
     * </p>
     */

    public HTMLBuilder() {
	tidy = new org.w3c.tidy.Tidy();
	tidy.setMakeClean( true );
	tidy.setXHTML( true );
    }
	    
    /**
     * <p>
     * This builds a document from the supplied
     *   input stream.
     * </p>
     *
     * @param in <code>InputStream</code> to read from.
     * @return <code>Document</code> - resultant Document object.
     * @throws JDOMException when errors occur in parsing.
     */
    
    public Document build(InputStream in) throws JDOMException {
	System.out.println( "Parsing the HTML document" );
	
	org.w3c.dom.Document w3cDocument = tidy.parseDOM( in , System.out );
	
	if ( w3cDocument == null )
	    throw new JDOMException( "Error while parsing HTML document" );
	
	// Creating a DOM with DOMBuilder	    
	DOMBuilder domBuilder = new DOMBuilder();
	
	return  domBuilder.build( w3cDocument );
    }

    /**
     * <p>
     * This builds a document from the supplied
     *   filename.
     * </p>
     *
     * @param file <code>File</code> to read from.
     * @return <code>Document</code> - resultant Document object.
     * @throws JDOMException when errors occur in parsing.
     */

    public Document build(File file) throws JDOMException {
	FileInputStream fis;
	try {
	    fis =  new FileInputStream( file );
	} catch ( java.io.FileNotFoundException e ) {
	    throw new JDOMException("Unable to parse file : ", e );
	}
	return build( fis );
    }

    /**
     * <p>
     * This builds a document from the supplied
     *   URL.
     * </p>
     *
     * @param url <code>URL</code> to read from.
     * @return <code>Document</code> - resultant Document object.
     * @throws JDOMException when errors occur in parsing.
     */

    public Document build(URL url) throws JDOMException {
	InputStream in;
	try {
	    in = url.openStream();
	} catch ( java.io.IOException e ) {
	    throw new JDOMException("Unable to parse file : ", e );
	}
	return build( in );
    }

   
  
}