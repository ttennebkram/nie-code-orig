package nie.webui;

import java.util.*;
import java.io.*;
import java.net.*;
import java.lang.reflect.*;
import org.jdom.Element;
import org.jdom.Comment;
import nie.core.*;
import nie.sn.SearchTuningApp;
import nie.sn.SnRequestHandler;
import nie.sn.CSSClassNames;


public class UIRequestDispatcher
{
	private static final String kClassName = "UIRequestDispatcher";
	// We need this when referencing our class by name
	private final static String kFullClassName = "nie.webui." + kClassName;

	public UIRequestDispatcher(
		Element optConfigElem,
		nie.sn.SearchTuningConfig inMainConfig
		// nie.sn.SearchTuningApp inMainApp
		)
			throws UIException
	{
		final String kFName = "constructor";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// This will throw an exception if anything is
		// wrong, it also checks fMainApp for null
		if( null!=optConfigElem )
		{
			/***
			fUIConfig = new UIConfig(
				optConfigElem, inMainConfig // inMainApp
				);
			***/
			errorMsg( kFName, "UI config not supported at this time");
		}

		// Looking good

		// Save this
		// fMainApp = inMainApp;
		fMainConfig = inMainConfig;

		// Cache a couple things (they just use null check)
		getMainUiUrl();

		// Init some hashes
		fCachedXMLScreens = new Hashtable();
		fSearchedForXMLScreens = new HashSet();
		fCachedJavaScreens = new Hashtable();
		fSearchedForJavaScreens = new HashSet();

	}

	public String dispatch(
		AuxIOInfo inRequestInfo,
		AuxIOInfo inResponseInfo
		)
			throws UIException, UIConfigException
	{
		return dispatch( inRequestInfo, inResponseInfo, null );
	}

	public String dispatch(
		AuxIOInfo inRequestInfo,
		AuxIOInfo inResponseInfo,
		String optForceScreenName
		)
			throws UIException, UIConfigException
	{
		final String kFName = "dispatch";
		final String kExTag = kClassName + '.' + kFName + ": ";

		if( null == inRequestInfo || null == inResponseInfo )
			throw new UIException( kExTag
				+ "Null request/reponse object."
				);

		// Our eventual response, barring any exceptions
		Element outElem = null;

		// The name of the screen
		String screenName = optForceScreenName;
		if( null==screenName ) {
			screenName = extractScreenName( inRequestInfo );
			screenName = NIEUtil.trimmedStringOrNull( screenName );
		}

		// An overall display template
		// String screenTemplate = getScreenTemplate();
		String screenTemplate = null;

		/***
		// If no report, give the menu
		if( null == screenName )
		{
			// Todo: Give list of reports
			// throw new ReportException( kExTag
			//	+ "No report name given."
			//	);

			outElem = generateMainMenu(
				inRequestInfo,
				inResponseInfo,
				( null != screenTemplate )
				);

		}
		// Else a specific screen was requested
		else
		{
		***/

			// Track down the report
			// UIScreenInterface theScreen = locateScreen( screenName );
			UIScreenInterface theScreen = XMLDefinedScreen.screenFactory(
				getMainConfig(), screenName
				);
			if( null == theScreen )
				throw new UIException( kExTag
					+ "Unable to locate/load/configure UI screen \""
					+ screenName + "\". Error - got back a null screen."
					);


			if( ! theScreen.verifySecurityLevelAccess(inRequestInfo) )
				throw new UIException( kExTag
					+ "Insufficient priveledges to access this screen."
					+ " Please check that you are using the correct password for this screen."
					);


			// Now run it!
			// It may throw plain old report run time exceptions
			try {
				outElem = theScreen.processRequest(
					inRequestInfo,
					inResponseInfo,
					( null == screenTemplate )
					);
			}
			catch( Throwable e ) {
				String msg = null;
				try {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter( sw );
					e.printStackTrace(pw);
					sw.close();
					msg = e.toString() + " STACK: " + sw.toString();
				}
				catch( Exception e2 ) {
					msg = e.toString() + " - NO STACK TRACE, Error: " + e2;
				}
				throw new UIException( kExTag
					+ " Error processing UI screen \"" + screenName + "\", Error: " + msg
					);
			}
	
		// }

		// Sanity check
		if( null==outElem )
			throw new UIException( kExTag
				+ "Got back null XHTML screen element."
				);

		
		// By now we have a jdom tree that is either
		// a menu of reports, or the results of a specific report

		// Convert it to a String (XHTML)
		String outDoc = null;

		boolean isRedirect = false;

		if( outElem.getName().equals("redirect") ) {
			isRedirect = true;
			String newURL = outElem.getTextNormalize();
			SnRequestHandler.static_setupSpecificURLRedicect( newURL, inResponseInfo );
		}
		// If there's NO template
		if( null == screenTemplate )
		{
		 	// outDoc = JDOMHelper.JDOMToString( outElem, true );
			outDoc = JDOMHelper.JDOMToString( outElem, false );
			// statusMsg( kFName, "Raw screen = '" + outDoc + "'" );

		}
		// Else they ARE using a template
		else
		{
			outDoc = JDOMHelper.JDOMToString( outElem, true );
			errorMsg( kFName, "templates not supported in UI at tis time");

			/***
			// Format what we got back as a string to
			// substitute into the final page
		 	String snippet = JDOMHelper.JDOMToString( outElem, true );

			// get the marker text and options
			List patterns = getUIConfig().getMarkerLiteralText();
			if( null == patterns || patterns.size() < 1 )
			{
				errorMsg( kFName,
					"No patterns defined to mark substitution, returning null."
					);
				return null;
			}
	
			// Get some other flags from the Search Engine config
			boolean goesAfter =
				getReportingConfig().getIsMarkerNewTextInsertAfter();
			// statusMsg( kFName, "goesAfter=" + goesAfter );
			boolean doesReplace =
				getReportingConfig().getIsMarkerReplaced();
			boolean isCasen =
				getReportingConfig().getIsMarkerCaseSensitive();
			***/	
	
			/****
			debugMsg( kFName,
				"Will process input doc with " + inSourceDoc.length() + " chars"
				+ ", Snippet with " + inSnippet.length() + " chars"
				+ ", and " + patterns.size() + " pattern(s)."
				+ " Options:"
				+ " goesAfter=" + goesAfter
				+ " doesReplace=" + doesReplace
				+ " isCasen=" + isCasen
				);
			*******/

			/***	
			// call the substituion routine
			// Do the markup
			outDoc = NIEUtil.markupStringWtihString(
				reportTemplate, snippet, patterns,
				goesAfter, doesReplace, isCasen
				);
	
			// A final escape hatch to at least give them something
			if( null == outDoc )
			{
				errorMsg( kFName,
					"Did not find target pattern in report template."
					+ " Will return unformatted report."
					);
				// Just slap some HTML tags onto the snippet
				outDoc = "<html>" + snippet + "</html>";
			}
			***/

		}	// End if there was a template

		// Sanity check
		if( !isRedirect && null==outDoc )
			throw new UIException( kExTag
				+ "Got back null string from XHTML-to-String conversion."
				);

		// OK, we're done, return it!
		return outDoc;
	}

	String extractScreenName( AuxIOInfo inRequestInfo )
			throws UIException
	{
		final String kFName = "extractScreenName";
		final String kExTag = kClassName + '.' + kFName + ": ";

		if( null==inRequestInfo ) {
			throw new UIException( kExTag + "Null object passed in"
				+ ", inRequestInfo=" + inRequestInfo
				);
		}

		// Get the URL and chop off the parts we don't want
		String fileURI = inRequestInfo.getLocalURLPath();
		if( null==fileURI )
			throw new UIException( kExTag + "Null URL file path.");
		// drop any #ref suffix
		int poundMarkAt = fileURI.indexOf( '#' );
		if( 0 == poundMarkAt )
			throw new UIException( kExTag + "Invalid placement of pound sign in \"" + fileURI + "\"");
		if( poundMarkAt > 0 )
			fileURI = fileURI.substring( 0, poundMarkAt );
		// drop any CGI ?... stuff
		int questionMarkAt = fileURI.indexOf( '?' );
		if( 0 == questionMarkAt )
			throw new UIException( kExTag + "Invalid placement of question mark in \"" + fileURI + "\"");
		if( questionMarkAt > 0 )
			fileURI = fileURI.substring( 0, questionMarkAt );
		// Drop the prefix /files/
		if( fileURI.startsWith( SnRequestHandler.UI_CONTEXT_CGI_PATH_PREFIX) ) {
			int prefixLen = SnRequestHandler.UI_CONTEXT_CGI_PATH_PREFIX.length();
			if( prefixLen == fileURI.length() )
				throw new UIException( kExTag +
					"Empty local URI: Nothing would be left in \"" + fileURI + "\""
					+ " after removing prefix \"" +  SnRequestHandler.UI_CONTEXT_CGI_PATH_PREFIX + "\""
					);
			fileURI = fileURI.substring( prefixLen );
		}
		else {
			warningMsg( kFName,
				"Local path \"" + fileURI + "\""
				+ " does not start with expected prefix \"" +  SnRequestHandler.UI_CONTEXT_CGI_PATH_PREFIX + "\""
				+ ", file will probably not be found."
				);
		}

		// Now remove any extension
		int dotAt = fileURI.lastIndexOf( '.' );
		if( dotAt >= 0 ) {
			if( 0 == dotAt || dotAt == fileURI.length()-1 )
				throw new UIException( kExTag + "Invalid placement of . in \"" + fileURI + "\"");
			fileURI = fileURI.substring( 0, dotAt );
		}

		return fileURI;

	}


	String getCssStyleSheetURI()
	{
		return XMLDefinedScreen.DEFAULT_CSS_URI;
	}

	// Not getting a style sheet should not be a fatal error
	// though we will log error messages
	// Todo: let them load their own from elsewhere
	String getCssStyleText()
	{
		final String kFName = "getCssStyleText";

		String uri = getCssStyleSheetURI();
		if( null==uri )
		{
			infoMsg( kFName,
				"No CSS URI defined, returning null."
				);
			return null;
		}

		AuxIOInfo tmpAuxInfo = new AuxIOInfo();
		tmpAuxInfo.setSystemRelativeBaseClassName(
			kFullClassName
			);
		String answer = null;
		try
		{
			answer = NIEUtil.fetchURIContentsChar(
				uri,
				// getMainApp().getConfigFileURI(),
				getMainConfig().getConfigFileURI(),
				null, null,	// optUsername, optPassword,
				tmpAuxInfo, false
				);
		}
		catch( Exception e )
		{
			errorMsg( kFName,
				"Error opening CSS URI \"" + uri + "\"."
				+ " Returning null."
				+ " Error: " + e
				);
			return null;
		}
		// Normalize and check
		answer = NIEUtil.trimmedStringOrNull( answer );
		if( null==answer )
			errorMsg( kFName,
				"Null/empty default CSS style sheet contents read"
				+ " from URI \"" + uri + "\", returning null."
				);

		return answer;


		// Good resource on tables and CSS, from Nick Sayer
		// http://www.w3.org/TR/REC-CSS2/tables.html
		// And overall CSS info
		// http://www.w3.org/TR/REC-CSS2/cover.html#minitoc
		// Selectors / pattern matching
		// http://www.w3.org/TR/REC-CSS2/selector.html

//		Inside HTML:
//		<head>
//			...
//			<STYLE type="text/css">
//				H1 { color: blue }
//			</STYLE>
//		</head>

	}





	String getMainAppURL()
	{
		// return getMainApp().getSearchNamesURL();
		return getMainConfig().getSearchNamesURL();
	}

	public String getMainUiUrl()
		throws UIException
	{
		if( null==cMainUIURL ) {
			final String kFName = "getMainUiUrl";
			final String kExTag = kClassName + '.' + kFName + ": ";
			try {
				String baseURLStr = getMainConfig().getSearchNamesURL();
				URL baseURL = new URL( baseURLStr );
				URL newURL = null;
				// Can we use a simple constructor?
				if( baseURL.getProtocol().equals("http") && baseURL.getPort()==80 )
					newURL = new URL( "http", baseURL.getHost(),
						nie.sn.SnRequestHandler.UI_CONTEXT_CGI_PATH_PREFIX
						);
				else
					newURL = new URL( baseURL.getProtocol(),
						baseURL.getHost(), baseURL.getPort(),
						nie.sn.SnRequestHandler.UI_CONTEXT_CGI_PATH_PREFIX
						);
				cMainUIURL = newURL.toExternalForm();
			}
			catch( Throwable e ) {
				throw new UIException( kExTag + "Error getting UI URL: " + e );
			}
		}
			
		return cMainUIURL;
	}
	public Object /*SearchReportingConfig*/ _getReportingConfig()
	{
		return fReportsConfig;
	}



	UIScreenInterface _locateScreen(
			String inScreenName
			//, boolean inShowNotFoundErrors
		)
			throws UIConfigException, UIException
	{
		final String kFName = "locateScreen";
		final String kExTag = kClassName + '.' + kFName + ": ";

		inScreenName = NIEUtil.trimmedStringOrNull(inScreenName);
		if( null==inScreenName )
			throw new UIException( kExTag + "Null screen name given.");

		// Do we have an XML report?
		UIScreenInterface theScreen = null;
		try
		{
			// returns null if not found, not an exception
			theScreen = _locateXMLScreen(
				inScreenName,
				false	// inShowNotFoundErrors
				);
		}
		catch( Exception e1 )
		{
			throw new UIException( kExTag
				+ "Unable to configure XML defined screen \""
				+ inScreenName + "\". Error: " + e1
				);
		}

		// OR how about a Java report?
		if( null == theScreen )
		{
			try
			{
				theScreen = locateJavaScreen(
					inScreenName,
					false	// inShowNotFoundErrors
					);
			}
			catch( Exception e2 )
			{
				throw new UIException( kExTag
					+ "Unable to configure Java screen \""
					+ inScreenName + "\". Error: " + e2
					);
			}
		}

		return theScreen;
	}

	// This will return a null if not found, and maybe an error
	// If a report IS found, but has a PROBLEM, you will get an exception
	// I know it seems odd to have two failure modes, but one is
	// sometimes expected, whereras the other is ALWAYS a serious error
	// and we need to know the difference
	UIScreenInterface _locateXMLScreen(
		String inScreenName,
		boolean inShowNotFoundErrors
		)
			throws UIException, UIConfigException
	{
		final String kFName = "locateXMLScreen";
		final String kExTag = kClassName + '.' + kFName + ": ";


		return null;

		/***

		// fCachedXMLReports
		// fSearchedForXMLReports

		// Sanity check
		inScreenName = NIEUtil.trimmedStringOrNull( inScreenName );
		if( null == inScreenName ) {
			throw new UIException( kExTag
				+ "Null/empty report name passed in."
				);
		}

		debugMsg( kFName, "Looking for report " + inScreenName );

		// If it's in the cache, return it!
		if( fCachedXMLScreens.containsKey( inScreenName ) ) {
			debugMsg( kFName, "Returning previously cached screen." );
			return (UIScreenInterface)fCachedXMLScreens.get(
				inScreenName
				);
		}

		// If we've tried before, don't bother again
		// If it's in this hash, and not in the other, then
		// we have tried before and failed
		if( fSearchedForXMLScreens.contains( inScreenName ) ) {
			debugMsg( kFName, "We have tried to load this screen before." );
			if( inShowNotFoundErrors )
				throw new UIException( kExTag
					+ "Previously failed to load XML screen \""
					+ inScreenName + "\"."
					);
			// Else just quietly return null
			return null;
		}
		// Remember that at least we've tried before
		fSearchedForXMLScreens.add( inScreenName );

		UIScreenInterface newScreen = null;
		try
		{
			String screenURI = generateDefaultXMLScreenURI(
				inScreenName
				);
			debugMsg( kFName, "Screen URI is \"" + screenURI + "\"." );


			AuxIOInfo tmpAuxInfo = new AuxIOInfo();
			tmpAuxInfo.setSystemRelativeBaseClassName(
				kFullClassName
				);

			InputStream stream = NIEUtil.openURIReadBin(
				screenURI,
				getMainConfig().getConfigFileURI(), // optRelativeRoot
				null, // String optUsername,
				null, // String optPassword,
				tmpAuxInfo  // AuxIOInfo inoutAuxIOInfo
				);
			// Close it and reopen with recursive open
			stream.close();

			debugMsg( kFName, "Found it, will now try JDOM constructor" );

			// JDOMHelper elem = new JDOMHelper( reportURI, null, 0 );
			JDOMHelper elem = new JDOMHelper(
				screenURI,
				// getMainApp().getSearchTuningConfig().getConfigFileURI(),
				// getMainApp().getConfigFileURI(), // optRelativeRoot
				getMainConfig().getConfigFileURI(), // optRelativeRoot
				0,
				tmpAuxInfo
				);

			debugMsg( kFName,
				"Have created JDOM config tree, will instantiate report object."
				);


			// OK so far, so create the report
			newScreen = new XMLDefinedScreen(
				// fMainApp, inReportName, elem
				getMainConfig(), inScreenName, elem
				);
			newScreen = null;

			debugMsg( kFName,
				"Have created XML screen object, storing in cache."
				);

			// Save report in the cache
			fCachedXMLScreens.put( inScreenName, newScreen );


		}
		// OK, so we just couldn't locate it
		catch( IOException ioe )
		{
			if( inShowNotFoundErrors )
				errorMsg( kFName,
					"Error locating XML Screen \"" + inScreenName + "\"."
					+ " Returning null."
					+ " Error was: " + ioe
					);
			return null;
		}
		// This is always an error
		catch( JDOMHelperException jde )
		{
			throw new UIConfigException( kExTag
				+ "Error parsing XML in Screen \"" + inScreenName + "\"."
				+ " Returning null."
				+ " Error was: " + jde
				);
		}
		// catch( ReportConfigException e )
		// We let this bubble up the food chain

		return newScreen;

		***/

	}

	// If a report IS found, but has a PROBLEM, you will get an exception
	// I know it seems odd to have two failure modes, but one is
	// sometimes expected, whereras the other is ALWAYS a serious error
	// and we need to know the difference
	UIScreenInterface locateJavaScreen(
		String inScreenName,
		boolean inShowNotFoundErrors
		)
			throws UIException, UIConfigException
	{
		final String kFName = "locateJavaScreen";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// fCachedXMLReports
		// fSearchedForXMLReports

		// Sanity check
		inScreenName = NIEUtil.trimmedStringOrNull( inScreenName );
		if( null == inScreenName )
			throw new UIException( kExTag + "Null/empty screen name passed in." );

		debugMsg( kFName, "Looking for Java screen " + inScreenName );

		// If it's in the cache, return it!
		if( fCachedJavaScreens.containsKey( inScreenName ) ) {
			debugMsg( kFName, "Returning previously cached screen." );
			return (UIScreenInterface)fCachedJavaScreens.get( inScreenName );
		}

		// If we've tried before, don't bother again
		// If it's in this hash, and not in the other, then
		// we have tried before and failed
		if( fSearchedForJavaScreens.contains( inScreenName ) ) {
			debugMsg( kFName, "We have tried to load this Java screen before." );
			if( inShowNotFoundErrors )
				throw new UIException( kExTag + "Previously failed to load Java screen \"" + inScreenName + "\"." );
			// Else just quietly return null
			return null;
		}
		// Remember that at least we've tried before
		fSearchedForJavaScreens.add( inScreenName );

		UIScreenInterface newScreen = null;

		String screenURI = generateDefaultJavaScreenClassURI(
			inScreenName
			);
		debugMsg( kFName, "Screen class is \"" + screenURI + "\"." );


		// nie.sn.SearchTuningApp inMainApp,
		//		String
		Class lScreenClass;

		try {
			lScreenClass = Class.forName( screenURI );
		} catch( ClassNotFoundException cnfe ) {
			if( inShowNotFoundErrors )
				throw new UIException( "Can't load UI screen class \"" + screenURI + "\", error:" + cnfe );
			else
				return null;
		}

		// Class[] lConstructorSignature = new Class[3];
		// lConstructorSignature[0] = lQueueClassName.getClass();
		// lConstructorSignature[1] = this.getClass();
		// lConstructorSignature[2] = inElement.getClass();

		Class[] lConstructorSignature = new Class [] {
			// nie.sn.SearchTuningApp.class,
			nie.sn.SearchTuningConfig.class,
			String.class
		};


		Constructor lConstructor = null;

		try {
			lConstructor = lScreenClass.getConstructor( lConstructorSignature );
		} catch( NoSuchMethodException nsme ) {
			throw new UIException( "Could not find appropriate constructor for report class \"" + screenURI + "\"." );
		}

		try {
			// Create the args we will pass to the constructor
			// Object [] lParams = new Object[2];
			// lParams[0] = fMainApp;
			// lParams[1] = inReportName;
			Object [] lParams = new Object[] {
				// fMainApp,
				getMainConfig(),
				inScreenName
			};
			newScreen = (UIScreenInterface)lConstructor.newInstance( lParams );
		} catch( Exception ie ) {
			throw new UIConfigException( "Unable to instantiate object for UI screen class \"" + screenURI + "\". Error: " + ie );
		}

		// Save screen in the cache
		fCachedJavaScreens.put( inScreenName, newScreen );

		debugMsg( kFName,
			"Have created screen object, storing in cache."
			);

		return newScreen;

	}

	private static String generateDefaultXMLScreenURI( String inScreenName )
			throws UIException
	{
		final String kFName = "generateDefaultXMLScreenURI";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// somewhat redundant sanity checks
		// inTableName = NIEUtil.trimmedStringOrNull( inTableName );
		inScreenName = NIEUtil.trimmedStringOrNull( inScreenName );
		if( inScreenName == null )
			throw new UIException( kExTag
				+ "Null UI screen name passed in."
				);

		// Form the name and return it
		return DEFAULT_XML_SCREEN_PREFIX
			+ inScreenName
			+ DEFAULT_XML_SCREEN_SUFFIX
			;

	}

	private static String generateDefaultJavaScreenClassURI( String inScreenName )
		throws UIException
	{
		final String kFName = "generateDefaultJavaScreenClassURI";
		final String kExTag = kClassName + '.' + kFName + ": ";
		inScreenName = NIEUtil.trimmedStringOrNull(inScreenName);
		if( null==inScreenName )
			throw new UIException( kExTag + "Null UI screen name passed in." );
		return DEFAULT_JAVA_SCREEN_PREFIX + inScreenName;
	}


	// NO, use getMainConfig() instead
	nie.sn.SearchTuningApp _getMainApp()
	{
		return _fMainApp;
	}

	nie.sn.SearchTuningConfig getMainConfig()
	{
		return fMainConfig;
	}

	DBConfig getDBConfig()
	{
		// return getMainApp().getDBConfig();
		return getMainConfig().getDBConfig();
	}


	String _getReportTemplate()
	{
		/***
		if( null != getReportingConfig() )
			return getReportingConfig().getAndSetupOptionalReportTemplate();
		else
			return null;
		***/
		return null;
	}

	// private Element fConfigElem;
	private nie.sn.SearchTuningApp _fMainApp;
	private nie.sn.SearchTuningConfig fMainConfig;

	// Search reporting configuration
	Object /*SearchReportingConfig*/ fReportsConfig;





	// Holds JDOMHelper trees of reports we have located,
	private Hashtable fCachedXMLScreens;
	// Keeps track of whether we've checked for a particular
	// report or not, holds type Boolean
	private HashSet fSearchedForXMLScreens;

	// Holds ReportInterface objects of Java reports we have located,
	private Hashtable fCachedJavaScreens;
	// Keeps track of whether we've checked for a particular
	// report or not, holds type Boolean
	private HashSet fSearchedForJavaScreens;



	// This gets us to the logging object
	private static RunLogInterface getRunLogObject()
	{
		return RunLogBasicImpl.getRunLogObject();
	}
	private static boolean statusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	private static boolean transactionStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().transactionStatusMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	private static boolean shouldDoTransactionStatusMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTransactionStatusMsg( kClassName, inFromRoutine );
	}
	private static boolean infoMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().infoMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	private static boolean debugMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().debugMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
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
	private static boolean warningMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().warningMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	private static boolean errorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	private static boolean fatalErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().fatalErrorMsg( kClassName, inFromRoutine,
			inMessage
			);
	}



	String cMainUIURL;

	// public static final String REPORT_NAME_CGI_FIELD = "report";

	private final static String DEFAULT_XML_SCREEN_PREFIX =
		AuxIOInfo.SYSTEM_RESOURCE_PREFIX
		+ "xml_screens/"
		;
	private final static String DEFAULT_JAVA_SCREEN_PREFIX = "nie.webui.java_screens.";
	private final static String DEFAULT_XML_SCREEN_SUFFIX = ".xml";
	public final static String NIE_REPORT_LINK_CSS_CLASS =
		"nie_report_link";
	public final static String NIE_INACTIVE_REPORT_LINK_CSS_CLASS =
		"nie_inactive_report_link";

	public static final String LOGIN_SCREEN_NAME = "Login";


		// The name of the system XSLT template we will call
		// String templateName = "admin_showall";
		// The resulting document
		// Document formattedDoc = null;
		// Try to format it
//		try
//		{
//			formattedDoc = JDOMHelper.xsltDocToDoc(
//				mainDoc,
//				templateName,
//				null, true
//				);
//		}
//		catch (JDOMHelperException e)
//		{
//
//			String msg = "SnRequestHandler:adminShowCompleteConfig:"
//				+ " Got exception while formatting XML into HTML."
//				+ " Exception=\"" + e + "\""
//				;
//			errorMsg( kFName, msg );
//			setupTextErrorResponse( msg );
//			return;
//		}
//		String response = JDOMHelper.JDOMToString( formattedDoc, true );

}