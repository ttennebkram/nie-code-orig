package nie.sr;

import java.util.*;
import java.io.*;
import javax.servlet.jsp.*;
import javax.servlet.http.*;

public class SRView
{
    /**************************************************************
     * Constructor
     *************************************************************/
    
    public SRView( SRModel inModel ) throws SRException
    {
		commonInit();
		setModel( inModel );
	}
    
    public SRView() throws SRException
    {
		commonInit();
    }
    
    void commonInit() throws SRException
    {
    }
    
    /***************************************************************
     * Getters and Setters
     **************************************************************/
    
    public void setModel( SRModel inModel )
    {
    	fModel = inModel;
    }
    public void setWriter( PrintWriter inWriter )
    {
    	fWriter = inWriter;
    }
    public void setResponse( HttpServletResponse inResponse )
    {
    	fResponse = inResponse;
    }
    
    public SRModel getModel()
    {
    	return fModel;
    }
    public PrintWriter getWriter()
    {
    	return fWriter;
    }
    public HttpServletResponse getResponse()
    {
    	return fResponse;
    }


    //////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Display the "We have encountered an internal error" page.
    // It only requries the output writer
    // to actually show the page.
    //
    //////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void internalError( Writer inWriter ) throws SRException
    {
		try
		{
	    	inWriter.write( kSysErrorMsg );
		}
		catch( Exception e )
		{
	    	// Nothing we can do here.
		}
		return;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Display an error through the use of an external error file template
    //
    //////////////////////////////////////////////////////////////////////////////////////////////////////
    
    public static void displayErrorFile( PrintWriter inWriter,
					 String inTemplateFile,
					 String inErrorMessage ) throws SRException
    {
		Hashtable lHashtable = new Hashtable();
		lHashtable.put( kErrorMessageTemplateMarker, inErrorMessage );
		displayTemplateFile( inWriter, inTemplateFile, lHashtable );
    }
    
    public void displayErrorFile( String inTemplateFile, String inErrorMessage ) throws SRException
    {
		displayErrorFile( getWriter(), inTemplateFile, inErrorMessage );
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Display a template file.  This takes three arguments - the PriintWriter so that we can display
    // to the user, the name of the template file, and a hash whose keys are fields in the template file
    // and whose values should be substituted into the template file.
    //
    //////////////////////////////////////////////////////////////////////////////////////////////////////
    
    public static void displayTemplateFile( PrintWriter inWriter, String inTemplateFileName, Hashtable inHashtable ) throws SRException
    {
		File lFile = new File( SRConfig.getSoleInstance().getBaseDirectory()
			+ inTemplateFileName
			);

		// If the template file exists	
		if( lFile.exists() )
		{
		    int lFileSize = new Long(lFile.length()).intValue();
	    	try
	    	{
				FileReader lFileReader = new FileReader( lFile );
				if( lFileReader == null )
				{
				    Hashtable lHashtable = new Hashtable();
				    lHashtable.put( kErrorMessageTemplateMarker, "Template File '" + 
						    inTemplateFileName + 
						    "' could not be read.  Previous error was: '" + 
						    (String)(inHashtable.get(kErrorMessageTemplateMarker)) );
				    String lOutputString = createOutputStringFromTemplate( kSysErrorMsg, lHashtable );
				    inWriter.print( lOutputString );
				    return;
				}
		
				char[] lOutputCharArray = new char[ lFileSize + 10 ];
				lFileReader.read( lOutputCharArray, 0, lFileSize );
				inWriter.print( createOutputStringFromTemplate( new String( lOutputCharArray ), inHashtable ) );
	    	}
	    	catch( Exception e )
	    	{
				Hashtable lHashtable = new Hashtable();
				lHashtable.put( kErrorMessageTemplateMarker, e.toString() );
				try
				{
		    		inWriter.print( createOutputStringFromTemplate( kSysErrorMsg, lHashtable ) );
				}
				catch( Exception e2 )
				{

				    //
				    // If we get in here, then there's really nothing that we can do.
				    // I suppose that we COULD put some logging in here, but that's about it.
				    //
		
				    SRConfig.doErrorMsg( "SRView", "displayTemplateFile", "Multiple exceptions occurred: (most recent):" + 
							 e2 + "\n(previous): " + e );
				}
		    }
		}
		// Else the file does not exist
		else
		{
		    Hashtable lHashtable = new Hashtable();
//		    lHashtable.put( kErrorMessageTemplateMarker,
//		    		"Template File '"
//				    + inTemplateFileName
//				    + "' does not exist."
//					+ " Previous error was: \""
//					+ (String)(inHashtable.get(kErrorMessageTemplateMarker))
//					+ "\""
//				    );
		    String msg =
		    		"Template File '"
				    + inTemplateFileName
				    + "' does not exist."
				    ;
			// work past null pointer exception
			if( null != inHashtable
				&& null != kErrorMessageTemplateMarker
				&& inHashtable.containsKey(kErrorMessageTemplateMarker)
				)
			{
				msg += " Previous error was: \""
					+ (String)(inHashtable.get(kErrorMessageTemplateMarker))
					+ "\""
				    ;
			}
			else
			{
				msg += " Unable to show previous error."
					+ " inHashtable=" + inHashtable
					+ " kErrorMessageTemplateMarker=" + kErrorMessageTemplateMarker
					;
			}
		    lHashtable.put( kErrorMessageTemplateMarker, msg );



		    
		    String lOutputString = createOutputStringFromTemplate( kSysErrorMsg, lHashtable );
		    try
		    {
				inWriter.print( lOutputString );
	    	}
	    	catch( Exception e )
	    	{
				// There's nothing that we can do - the error is in communicating with the user.
				// I suppose that we COULD put some logging in here, but that's about it.
				//
	    	}
		}
	
		return;
    }
    
    public void displayTemplateFile( String inFileName, Hashtable inFields ) throws SRException
    {
		SRView.displayTemplateFile( getWriter(), inFileName, inFields );
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Given a template string and a hash of field name/value pairs, replace all the fields in the template
    // with the appropriate values.
    //
    //////////////////////////////////////////////////////////////////////////////////////////////////////
    
    public static String createOutputStringFromTemplate( String inTemplate, Hashtable inFieldValues )
    {
		// Get the field markers from the hash table
	
		if( inFieldValues == null )
	   		return inTemplate;
	
		String outTemplate = inTemplate;
	
		Enumeration lFields = inFieldValues.keys();
		while( lFields.hasMoreElements() )
	    {
			String lFieldName = (String)lFields.nextElement();
			outTemplate = applyField( outTemplate, lFieldName, (String)inFieldValues.get( lFieldName ) );
	    }
	
		return outTemplate;
    }
    
    static String applyField( String inString, String inFieldName, String inFieldValue )
    {
	String outString = inString;
	int lNameLength = inFieldName.length();
	int lPosition;
	while( (lPosition = outString.indexOf( inFieldName )) != -1 )
	    {
		String lPrefixString = outString.substring( 0, lPosition );
		String lPostfixString = outString.substring( lPosition + lNameLength );
		outString = lPrefixString + inFieldValue + lPostfixString;
	    }
	
	return outString;
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Do a redirection to a new page
    //
    //////////////////////////////////////////////////////////////////////////////////////////////////////

    public void doRedirect( String inURL ) throws SRException
    {
	try {
	    getResponse().sendRedirect( inURL );
	} catch( Exception e ) {
	    SRConfig.doErrorMsg( "SRView", "doRedirect(" + inURL + ")", "" + e + "" );
	}
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Constants of interest outside of our class
    //
    //////////////////////////////////////////////////////////////////////////////////////////////////////
    
    public static final String kErrorMessageTemplateMarker = "<error message goes here>";
    public static final String kErrorStringMarker = kErrorMessageTemplateMarker;
    
    // public static final String kUnavailableFileName = "html" + File.separator + "unavailable.html";
    public static final String kUnavailableFileName = "html/unavailable.html";
    
    static final String kSysErrorMsg = "<html>\n" +
	"\t<head>\n" +
	"\t\t<title>SearchTrack Reporting System Error</title>\n" +
	"\t</head>\n" +
	"\t<body>\n" +
	"\t\t<h1><center>Default SearchTrack Reporting System Error</center></h1>\n" +
	"\t\t<p>The SearchTrack Reporting module has encountered an internal configuration error and can not continue.</p>\n" +
	"\t\t<p>Please report this problem to your system administrator for further investigation.</p>\n" +
	"\t\t<p>The error returned was:</p>\n" +
	"\t\t<p>" + kErrorMessageTemplateMarker + "</p>\n" +
	"\t</body>\n" +
	"</html>";
    
    //////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Instance variables
    //
    //////////////////////////////////////////////////////////////////////////////////////////////////////
    
    SRModel fModel;
    PrintWriter fWriter;
    HttpServletResponse fResponse;
}
