/*
 * Created on Sep 17, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package nie.sn;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import org.jdom.Element;
import org.jdom.Comment;
import nie.core.*;
import nie.sn.SearchTuningApp;

/**
 * @author mbennett
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class FileDispatcher
{

	final static String kClassName = "FileDispatcher";
	final static String kFullClassName = "nie.sn." + kClassName;


	public FileDispatcher(
		Element inConfigElem,
		nie.sn.SearchTuningConfig inMainConfig
		// nie.sn.SearchTuningApp inMainApp
		)
		//	throws ReportConfigException
	{
		final String kFName = "constructor";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// This will throw an exception if anything is
		// wrong, it also checks fMainApp for null
		// fReportsConfig = new SearchReportingConfig(
		//	inConfigElem, inMainApp
		//	);

		// Looking good

		// Save this
		// fMainApp = inMainApp;
		fMainConfig = inMainConfig;
	}


	// Tries to put a file in the output response
	void getFile( AuxIOInfo inRequestInfo, AuxIOInfo inResponseInfo ) throws IOException {
		final String kFName = "getFile";
		final String kExTag = kClassName + '.' + kFName + ": ";

		if( null==inRequestInfo || null==inResponseInfo ) {
			throw new IOException( kExTag + "Null object(s) passed in"
				+ ", inRequestInfo=" + inRequestInfo + ", inResponseInfo=" + inResponseInfo
				);
		}

		// Get the URL and chop off the parts we don't want
		String fileURI = inRequestInfo.getLocalURLPath();
		if( null==fileURI )
			throw new IOException( kExTag + "Null URL file path.");
		// drop any #ref suffix
		int poundMarkAt = fileURI.indexOf( '#' );
		if( 0 == poundMarkAt )
			throw new IOException( kExTag + "Invalid placement of pound sign in \"" + fileURI + "\"");
		if( poundMarkAt > 0 )
			fileURI = fileURI.substring( 0, poundMarkAt );
		// drop any CGI ?... stuff
		int questionMarkAt = fileURI.indexOf( '?' );
		if( 0 == questionMarkAt )
			throw new IOException( kExTag + "Invalid placement of question mark in \"" + fileURI + "\"");
		if( questionMarkAt > 0 )
			fileURI = fileURI.substring( 0, questionMarkAt );
		// Drop the prefix /files/
		if( fileURI.startsWith( SnRequestHandler.FILE_CONTEXT_CGI_PATH_PREFIX) ) {
			int prefixLen = SnRequestHandler.FILE_CONTEXT_CGI_PATH_PREFIX.length();
			if( prefixLen == fileURI.length() )
				throw new IOException( kExTag +
					"Empty local URI: Nothing would be left in \"" + fileURI + "\""
					+ " after removing prefix \"" +  SnRequestHandler.FILE_CONTEXT_CGI_PATH_PREFIX + "\""
					);
			fileURI = fileURI.substring( prefixLen );
		}
		else {
			warningMsg( kFName,
				"Local path \"" + fileURI + "\""
				+ " does not start with expected prefix \"" +  SnRequestHandler.FILE_CONTEXT_CGI_PATH_PREFIX + "\""
				+ ", file will probably not be found."
				);
		}

		// We need the extension to get the mime type
		int dotAt = fileURI.lastIndexOf( '.' );
		if( dotAt < 0 )
			throw new IOException( kExTag + "No extension to calculate mime type from in \"" + fileURI + "\"");
		if( 0 == dotAt || dotAt == fileURI.length()-1 )
			throw new IOException( kExTag + "Invalid placement of . in \"" + fileURI + "\"");
		String extension = fileURI.substring( dotAt+1 );
		// And the mime type
		String mimeType = getMimeTypeForExtension(extension);
		if( mimeType.equals(DEFAULT_MIME_TYPE) )
			warningMsg( kFName,
				"Unknown mime type for extension \"" + extension + "\" in requested file \"" + fileURI + "\"."
				+ " Returning default mime type of \"" + DEFAULT_MIME_TYPE + "\"."
				);
		inResponseInfo.setContentType( mimeType );
		boolean isBinary = inResponseInfo.getIsBinary();

		// Now we try to open the file
		// These guys will throw IO Exceptions if they are not happy

		// Try to open the stream, relative to this class
		AuxIOInfo tmpAuxInfo = new AuxIOInfo();
		tmpAuxInfo.setSystemRelativeBaseClassName(
			kFullClassName
			);

		// Get the content, binary or ASCII
		if( isBinary )
		{
			inResponseInfo.setBinContent(
				NIEUtil.fetchURIContentsBin(
					fileURI,	// String inBaseName,
					SnRequestHandler.STATIC_FILE_PREFIX, // was DEFAULT_LOCAL_FILE_URI_PREFIX,	// String optRelativeRoot,
					null,	// String optUsername,
					null,	// String optPassword,
					tmpAuxInfo,	// AuxIOInfo inoutAuxIOInfo
					false			// use POST
					)
				);
		}
		// Else it's character mode data
		else
		{
			inResponseInfo.setContent(
				NIEUtil.fetchURIContentsChar(
					fileURI,	// String inBaseName,
					SnRequestHandler.STATIC_FILE_PREFIX, // was DEFAULT_LOCAL_FILE_URI_PREFIX,	// String optRelativeRoot,
					null,	// String optUsername,
					null,	// String optPassword,
					tmpAuxInfo,	// AuxIOInfo inoutAuxIOInfo
					false			// use POST
					)
				);
		}

		// All done, if all went well and we got to here, response info will
		// have the new content and correct mime type
	}

	public String getMimeTypeForExtension( String inExtension ) {
		inExtension = NIEUtil.trimmedLowerStringOrNull(inExtension);
		if( null!=inExtension && mMimeTypesByExtension.containsKey(inExtension) )
			return (String) mMimeTypesByExtension.get(inExtension);
		else {
			return DEFAULT_MIME_TYPE;
		}
	}






	private static void ___Sep_Run_Logging__(){}
	//////////////////////////////////////////////////////////////////

	// Handling Errors and warning messages
	// ************* NEW LOGGING STUFF *************

	private static RunLogInterface getRunLogObject()
	// can't access some of impl's extensions with interface reference
	//private static RunLogBasicImpl getRunLogObject()
	{
		// return RunLogBasicImpl.getRunLogObject();
		return RunLogBasicImpl.getRunLogObject();
	}

	// New style
	private static boolean statusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg( kClassName, inFromRoutine,
			inMessage
			);
	}

	// New style
	private static boolean transactionStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().transactionStatusMsg( kClassName, inFromRoutine,
			inMessage
			);
	}

	// New style
	private static boolean infoMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().infoMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	private static boolean shouldDoInfoMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoInfoMsg( kClassName, inFromRoutine );
	}



	// Old style
	private void debugMsg( String inFromRoutine, String inMessage )
	{
		staticDebugMsg( inFromRoutine, inMessage );
	}
	private static void staticDebugMsg(
		String inFromRoutine, String inMessage
		)
	{

		getRunLogObject().debugMsg( kClassName, inFromRoutine,
			inMessage
			);

//		if( debug )
//		{
//			messageLogger( "Debug: " + kClassName + ":" + inFromRoutine + ":"
//				+ inMessage
//				);
//		}
	}

	// New style
	private static boolean traceMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().traceMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	private static boolean shouldDoTraceMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTraceMsg( kClassName, inFromRoutine );
	}

	// Old style
	private void warningMsg( String inFromRoutine, String inMessage )
	{
		staticWarningMsg( inFromRoutine, inMessage );
	}
	private static void staticWarningMsg(
		String inFromRoutine, String inMessage
		)
	{
		getRunLogObject().warningMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	private void errorMsg( String inFromRoutine, String inMessage )
	{
		staticErrorMsg( inFromRoutine, inMessage );
	}
	private static void staticErrorMsg(
		String inFromRoutine, String inMessage
		)
	{
		getRunLogObject().errorMsg( kClassName, inFromRoutine,
			inMessage
			);
	}

	// Newer style
	private static boolean fatalErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().fatalErrorMsg( kClassName, inFromRoutine,
			inMessage
			);
	}








	static Hashtable mMimeTypesByExtension;

	// NO, use SnRequestHandler.STATIC_FILE_PREFIX
	// public static final String _DEFAULT_LOCAL_FILE_URI_PREFIX = "system:static_files/";


	// private Element fConfigElem;
	// private nie.sn.SearchTuningApp fMainApp;
	private nie.sn.SearchTuningConfig fMainConfig;
	public static String DEFAULT_MIME_TYPE = "application/x-generic-binary-data";

	static {
		mMimeTypesByExtension = new Hashtable();

		// There are conflicting views on what these should be, I've tried to pick the "oldest"

		mMimeTypesByExtension.put( "txt", "text/plain" );
		mMimeTypesByExtension.put( "asc", "text/plain" );

		mMimeTypesByExtension.put( "htm", "text/html" );
		mMimeTypesByExtension.put( "html", "text/html" );
		mMimeTypesByExtension.put( "xhtml", "text/html" );

		mMimeTypesByExtension.put( "xml", "text/xml" );
		mMimeTypesByExtension.put( "css", "text/css" );

		mMimeTypesByExtension.put( "gif", "image/gif" );
		mMimeTypesByExtension.put( "jpg", "image/jpeg" );
		mMimeTypesByExtension.put( "jpeg", "image/jpeg" );
		mMimeTypesByExtension.put( "png", "image/png" );	// also x-png
		mMimeTypesByExtension.put( "tif", "image/tiff" );
		mMimeTypesByExtension.put( "tiff", "image/tiff" );
		mMimeTypesByExtension.put( "bmp", "image/x-bmp" );
		mMimeTypesByExtension.put( "ico", "image/x-ico" );
		mMimeTypesByExtension.put( "ps", "application/postscript" );
		mMimeTypesByExtension.put( "eps", "application/postscript" );

		mMimeTypesByExtension.put( "pdf", "application/pdf" );
		mMimeTypesByExtension.put( "zip", "application/zip" );

		mMimeTypesByExtension.put( "doc", "application/msword" );	// bunches of others
		mMimeTypesByExtension.put( "rtf", "application/rtf" );	// also text/rtf?
		mMimeTypesByExtension.put( "xls", "application/x-ms-excel" );	// also vnd...
		mMimeTypesByExtension.put( "ppt", "application/x-ms-powerpoint" );	// also vnd...

		mMimeTypesByExtension.put( "nb", "application/mathematica" );

	}

}
