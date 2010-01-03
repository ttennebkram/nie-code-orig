package nie.lucene;

import java.util.*;
import java.io.*;
import java.net.*;
import java.lang.reflect.*;

import org.jdom.Element;
import org.jdom.Comment;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.*;
import org.apache.lucene.document.*;
import org.apache.lucene.search.*;
import org.apache.lucene.queryParser.*;

import nie.core.*;
import nie.sn.SearchTuningApp;
import nie.sn.SnRequestHandler;
import nie.sn.CSSClassNames;


public class LuceneRequestDispatcher
{
	private static final String kClassName = "LuceneRequestDispatcher";
	// We need this when referencing our class by name
	private final static String kFullClassName = "nie.lucene." + kClassName;

	public LuceneRequestDispatcher(
		Element inConfigElem,
		nie.sn.SearchTuningConfig inMainConfig
		// nie.sn.SearchTuningApp inMainApp
		)
			throws LuceneConfigException
	{
		final String kFName = "constructor";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// statusMsg( kFName, "inConfigElem=" + inConfigElem );

		// This will throw an exception if anything is
		// wrong, it also checks fMainApp for null
		if( null==inConfigElem )
			throw new LuceneConfigException( kExTag + "Null config passed in." );

		fLuceneConfig = new LuceneConfig(
			inConfigElem, inMainConfig
			);

		// Looking good

		// Save this
		// fMainApp = inMainApp;
		fMainConfig = inMainConfig;

		// Cache a couple things (they just use null check)
		getMainSearchUrl();


	}

	public String dispatch(
		AuxIOInfo inRequestInfo,
		AuxIOInfo inResponseInfo
		)
			throws LuceneException, LuceneConfigException
	{
		final String kFName = "dispatch";
		final String kExTag = kClassName + '.' + kFName + ": ";

		if( null == inRequestInfo || null == inResponseInfo )
			throw new LuceneException( kExTag
				+ "Null request/reponse object."
				);

		// Our eventual response, barring any exceptions
		Element outElem = null;

		// An overall display template
		// String screenTemplate = getScreenTemplate();
		String screenTemplate = null;

		// Now run it!
		// It may throw plain old report run time exceptions
		try {
			outElem = processRequest(
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
			throw new LuceneException( kExTag
				+ " Error processing Lucene search, Error: " + msg
				);
		}

		// Sanity check
		if( null==outElem )
			throw new LuceneException( kExTag
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
			throw new LuceneException( kExTag
				+ "Got back null string from XHTML-to-String conversion."
				);

		// OK, we're done, return it!
		return outDoc;
	}

	LuceneConfig getLuceneConfig()
		throws LuceneConfigException
	{
		final String kFName = "getLuceneConfig";
		final String kExTag = kClassName + '.' + kFName + ": ";
		if( null==fLuceneConfig )
			throw new LuceneConfigException( kExTag
				+ "Lucene is not configured properly; null config."
				);
		return fLuceneConfig;
	}

	List getSearchFieldsAsList()
		throws LuceneConfigException
	{
		return getLuceneConfig().getSearchFieldsAsList();
	}
	String [] getSearchFieldsAsArray()
		throws LuceneConfigException
	{
		return getLuceneConfig().getSearchFieldsAsArray();
	}
	List getDisplayFields()
		throws LuceneConfigException
	{
		return getLuceneConfig().getDisplayFields();
	}
	/***
	String getContentField()
		throws LuceneConfigException
	{
		return getLuceneConfig().getDocumentContentField();
	}
	***/

	IndexSearcher getLuceneIndex()
		throws LuceneConfigException, LuceneException
	{
		return getLuceneConfig().getLuceneIndex();
	}
	Analyzer getLuceneAnalyzer()
		throws LuceneConfigException, LuceneException
	{
		return getLuceneConfig().getLuceneAnalyzer();
	}
	FieldSelector getLuceneFieldFilterOrNull()
		throws LuceneConfigException, LuceneException
	{
		return getLuceneConfig().getLuceneFieldFilterOrNull();
	}

	Element processRequest(
		AuxIOInfo inRequestInfo, AuxIOInfo inResponseInfo, boolean _templateFlag
		)
			throws LuceneException, LuceneConfigException
	{
		final String kFName = "processRequest";
		final String kExTag = kClassName + '.' + kFName;
		
		// Setup the output
		Element outElem = null;
		try {
			outElem = new JDOMHelper( RESULTS_SKEL, null ).getJdomElement();
		}
		catch( JDOMHelperException e ) {
			throw new LuceneException( kExTag
				+ "Got exception from JDOMHelper: " + e
				);
		}
		Element contentHanger = JDOMHelper.findElementByPath( outElem, "body/table" );
		if( null==contentHanger )
			throw new LuceneException( kExTag
				+ "Problem building skeleton results page"
				);

		// Setup the Query
		String queryText = inRequestInfo.getScalarCGIField( QUERY_CGI_FIELD );
		queryText = NIEUtil.trimmedStringOrNull( queryText );
		if( null!=queryText )
		{

			// Analyzer analyzer = new StandardAnalyzer();
			try {
				QueryParser parse = null;
				// Multi-field query
				if( getSearchFieldsAsList().size() > 1 ) {
					parse = new MultiFieldQueryParser(
							getSearchFieldsAsArray(),
							getLuceneAnalyzer()
							);
				}
				// Single-field query
				else {
					parse = new QueryParser(
							getSearchFieldsAsArray()[0],
							getLuceneAnalyzer()
							);
				}
				// Change default from OR to AND
				parse.setDefaultOperator( QueryParser.Operator.AND );

				// compile and run the search
				Query query = parse.parse( queryText );
				// ^^^ ParseException caught separately
				Hits hits = getLuceneIndex().search( query );

				// For each match
				if( hits.length() > 0 ) {
					for( int i = 0; i < hits.length() && i < DEFAULT_DOCS_PER_PAGE ; i++ )
					{
						Document doc = null;
						// Careful about OutOfMemory issue
						// use specifically named reasonable sized fields
						// if needed
						if( null!=getLuceneFieldFilterOrNull() ) {
							doc = getLuceneIndex().doc(
								hits.id(i), getLuceneFieldFilterOrNull()
								);					
						}
						// Or just use the old way
						else {
							doc = hits.doc(i);
						}
	
						// Start building this row
						Element newRow = new Element( "tr" );
						contentHanger.addContent( newRow );
	
						// Figure out which set of fields we will use
						Iterator fit = null;
						if( getDisplayFields().size() > 0 ) {
							List tmpFields = new Vector();
							for( Iterator fn=getDisplayFields().iterator(); fn.hasNext() ; ) {
								String name = (String)fn.next();
								org.apache.lucene.document.Field fld =
									doc.getField( name );
								// We actually need to check
								if( null!=fld ) {
									tmpFields.add( fld );
								}
								else {
									warningMsg( kFName,
										"No field '"+name+"' in this record"
										);
								}
							}
							fit = tmpFields.iterator();
						}
						else {
							fit = doc.getFields().iterator();
						}

						boolean shownAField = false;
						// For each Field
						while( fit.hasNext() ) {
							// Type "Field" is ambiguous
							org.apache.lucene.document.Field fld =
								(org.apache.lucene.document.Field) fit.next();
							String name = fld.name();
							String value = fld.stringValue();
							// Special handling for numeric fields
							if( ( name.indexOf("count")>=0 || name.indexOf("num")>=0 )
								&& value.startsWith("000")
							) {
									long tmpLong = NumberTools.stringToLong( value );
									value = ""+tmpLong;
							}
							// HTML Escape
							// value = NIEUtil.htmlEscapeString( value, true );
							// ^^^ NO, handled by later XML tree output
							// Add in the field name for now

							// Add "name="
							Element labelTag = new Element( "span" );
							labelTag.addContent( name + '=' );
							// and Bold the value
							Element valueTag = new Element( "b" );
							valueTag.addContent( value );
							// The table cell
							Element cell1 = new Element( "td" );
							cell1.addContent( labelTag );
							cell1.addContent( valueTag );
							newRow.addContent( cell1 );
							shownAField = true;
	
							/***
							String uri = doc.get("url");
							String title = doc.get("title");
							if( null==title )
								title = "URL: " + uri;
							Element link = new Element( "a" );
							link.setAttribute( "href", uri );
							link.addContent( title );
							cell1.addContent( link );
							***/
						}
						if( ! shownAField ) {
							Element cell1 = new Element( "td" );
							newRow.addContent( cell1 );
							cell1.addContent( "Sorry, no fields found in this record." );						
						}
					}
				}
				// Else no results found
				else {
					Element newRow = new Element( "tr" );
					contentHanger.addContent( newRow );
					Element cell1 = new Element( "td" );
					newRow.addContent( cell1 );
					cell1.addContent( "Sorry, no results found." );
				}

			}
			catch( org.apache.lucene.queryParser.ParseException pe ) {
				Element newRow = new Element( "tr" );
				contentHanger.addContent( newRow );
				Element cell1 = new Element( "td" );
				cell1.addContent( pe.toString() );
				newRow.addContent( cell1 );
				// Element preTag = new Element( "pre" );
				// preTag.addContent( pe.toString() );
				// preTag.addContent( ""+pe );
				// cell1.addContent( preTag );
				
			}
			catch( Exception e ) {
				e.printStackTrace( System.err );
				throw new LuceneException( kExTag
					+ "Lucene exception: " + e
					);
			}
		}
		// Else no search
		else {
			Element newRow = new Element( "tr" );
			contentHanger.addContent( newRow );
			Element cell1 = new Element( "td" );
			newRow.addContent( cell1 );
			cell1.addContent( "Empty search - no search terms entered - so no results." );
			statusMsg( kFName,
				inRequestInfo.displayCGIFieldsIntoBuffer()
				);
		}

		return outElem;
	}

	String getCssStyleSheetURI()
	{
		return DEFAULT_CSS_URI;
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



	public String getMainSearchUrl()
		throws LuceneConfigException
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
				throw new LuceneConfigException( kExTag + "Error getting built-in search URL: " + e );
			}
		}
			
		return cMainUIURL;
	}



	private static String generateDefaultSearchURI( String inScreenName )
			throws LuceneException
	{
		final String kFName = "generateDefaultSearchURI";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// somewhat redundant sanity checks
		// inTableName = NIEUtil.trimmedStringOrNull( inTableName );
		inScreenName = NIEUtil.trimmedStringOrNull( inScreenName );
		if( inScreenName == null )
			throw new LuceneException( kExTag
				+ "Null UI screen name passed in."
				);

		// Form the name and return it
		return DEFAULT_XML_SCREEN_PREFIX
			+ inScreenName
			+ DEFAULT_XML_SCREEN_SUFFIX
			;

	}

	private static String generateDefaultJavaScreenClassURI( String inScreenName )
		throws LuceneException
	{
		final String kFName = "generateDefaultJavaScreenClassURI";
		final String kExTag = kClassName + '.' + kFName + ": ";
		inScreenName = NIEUtil.trimmedStringOrNull(inScreenName);
		if( null==inScreenName )
			throw new LuceneException( kExTag + "Null UI screen name passed in." );
		return DEFAULT_JAVA_SCREEN_PREFIX + inScreenName;
	}



	nie.sn.SearchTuningConfig getMainConfig()
	{
		return fMainConfig;
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

	private nie.sn.SearchTuningConfig fMainConfig;

	private LuceneConfig fLuceneConfig;

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

	public static int DEFAULT_DOCS_PER_PAGE = 25;

	public static final String LOGIN_SCREEN_NAME = "Login";

	// Where CSS style sheets come from
	public static final String DEFAULT_CSS_URI =
		AuxIOInfo.SYSTEM_RESOURCE_PREFIX
		+ "style_sheets/default_xml_defined_report.css"
		;

	// public static final String QUERY_CGI_FIELD = "query";
	public static final String QUERY_CGI_FIELD = "q";

	static final String NL = NIEUtil.NL;
	static final String TITLE = "Lucene Search Results";
	static final String RESULTS_SKEL =
		"<html>" + NL +
		"<head>" + NL +
		"	<title>" + TITLE + "</title>" + NL +
		"</head>" + NL +
		"<body>" + NL +
		"	<h2>" + TITLE + "</h2>" + NL +
		"	<form>" + NL +
		"		New Search: " + NL +
		"		<input name=\"" + QUERY_CGI_FIELD + "\" />" + NL +
		"		<input type=\"submit\" value=\"Search\" />" + NL +
		"	</form>" + NL +
		"	<table cellpadding=\"2\">" + NL +
		"	</table>" + NL +
		"</body>" + NL +
		"</html>" + NL
		;


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