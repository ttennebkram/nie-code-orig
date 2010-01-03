/*
 * Created on Jun 9, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nie.core;

import java.io.StringReader;

import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.Element;

/**
 * @author Mark Bennett
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class String2Dom {

    static String myString =
        "<html>"
        + "<body>"
        + "Hello world!"
        + "</body>"
        + "</html>"
        ;

        
    public static void main(String[] args) {

        SAXBuilder jdomBuilder = new SAXBuilder();

        Document jdomDocument = null;
		StringReader sr = new StringReader( myString );
		try
		{
			jdomDocument = jdomBuilder.build(sr);
		}
		catch( Exception e )
		{
			System.err.println( "Exception: " + e );
		}
		finally {
		    try { sr.close(); } catch (Exception e2) { }
		}
    
    }
}
