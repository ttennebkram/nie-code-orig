/*
 * Created on Feb 17, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package nie.exp;

import java.util.*;
import java.io.*;

//import org.jdom.Element;
//import org.jdom.Document;
//import org.jdom.Attribute;
import org.jdom.*;
import org.jdom.xpath.*;

import nie.core.*;
import nie.sn.*;
import nie.sr2.*;

/**
 * @author mbennett
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class FindLinksAndForms
{
	public static final String kClassName = "FindLinksAndForms";

	public static void main(String[] args)
	{
		final String kFName = "main";
		/***
		if( args.length < 2 ) {
			fatalErrorMsg( kFName, "syntax: give config file on command line, then searches..." );
			System.exit(1);
		}
		***/

		List stuffToTry = new Vector();
		boolean doForms = false;
		boolean doLinks = false;
		boolean doShowSource = false;
		String pattern = null;
		String config = null;
		try {
			// FindLinksAndForms finder = new FindLinksAndForms( args[0] );
			FindLinksAndForms finder = new FindLinksAndForms();

			for( int i = 0; i<args.length ; i++ ) {
				String arg = args[i];
				arg = NIEUtil.trimmedStringOrNull( arg );
				if( null==arg || arg.equals("-") ) {
					errorMsg( kFName, "Null arg" );
					continue;
				}
				// statusMsg( kFName, "arg=" + arg );
				if( arg.startsWith("-") ) {
					arg = arg.substring( 1 ).toLowerCase();
					if( arg.indexOf("form") >= 0 ) {
						doForms = true;
						// statusMsg( kFName, "do forms" );
					}
					else if( arg.indexOf("link") >= 0 ) {
						doLinks = true;
						// statusMsg( kFName, "do links" );
					}
					else if( arg.indexOf("show") >= 0 || arg.indexOf("source") >= 0 || arg.indexOf("html") >= 0 ) {
						doShowSource = true;
						// statusMsg( kFName, "show html" );
					}
					else if( arg.indexOf("pattern") >= 0 || arg.indexOf("path") >= 0 ) {
						if( i==args.length-1 )
							errorMsg( kFName, "-" + arg + " requires an argument" );
						else
							pattern = args[++i];
					}
					else if( arg.indexOf("config") >= 0 ) {
						if( i==args.length-1 )
							errorMsg( kFName, "-" + arg + " requires an argument" );
						else
							config = args[++i];
					}
					else
						errorMsg( kFName, "Unknown option -" + arg );
				}
				else {
					stuffToTry.add( arg );
				}
			}


			if( null!=config )
				finder.setMainConfig( config );

			if( null==pattern ) {
				if( doForms )
					pattern = "//form";
				else if( doLinks )
					pattern = "//a";
				else
					throw new Exception( kFName + "Syntax: No pattern (or -links or -forms)" );
			}

			if( stuffToTry.isEmpty() )
				throw new Exception( kFName + "Syntax: No URLs or terms to try" );

			// For each thing to try
			for( Iterator it = stuffToTry.iterator(); it.hasNext() ; ) {
				String theString = (String) it.next();

				String theURL = (null!=theString && theString.indexOf(":/") >= 0)
					? theString : finder.expandTermToTestDriveURL( theString )
					;

				System.out.println();
				System.out.println( "Checking URL " + theURL );

				Document doc = fetchUrlAsDom( theURL );

				if( ! doForms )
					findAndShowPathSearch( doc, pattern );
				else
					analyzeForms( doc, pattern, doShowSource );

			}


		}
		catch( Throwable t ) {
			fatalErrorMsg( kFName, "Error: " + t );
			t.printStackTrace( System.out );
			System.exit(1);
		}


	}

	void setMainConfig( String inConfigURI )
		throws Exception
	{
		fMainConfig = new SearchTuningConfig(
			inConfigURI,
			new SearchTuningApp()
			);
		initSearchTestDriveLink();
	}
	void setMainConfig( SearchTuningConfig inConfig )
		throws Exception
	{
		if( null==inConfig )
			throw new Exception( "Null config pased in." );
		fMainConfig = inConfig;
		initSearchTestDriveLink();
	}

	void initSearchTestDriveLink()
		throws Exception
	{
		// Links to the search engine for a test drive
		// Don't need try/catch because it also throws ReportException
		fSearchLink = new SearchEngineLink(
			getMainConfig(),
			"_blank",										// String optWindowTarget,
			"Run this search now (in a new window)",	// String optLinkTitleText,
			null,											// String optCssClass
			// false
			// Gnerally in this case we're trying to bypass our proxy
			// However, it's only safe to skip our proxy if using GETs
			! getMainConfig().getSearchEngineMethod().equalsIgnoreCase("GET")
			);
	}


	// URL's or just search terms
	String expandTermToTestDriveURL( String inTerm )
		throws Exception
	{
		final String kFName = "expandTermToTestDriveURL";
		final String kExTag = kClassName + '.' + kFName + ": ";

		if( null==fSearchLink )
			throw new Exception( kExTag + "No search link configured." );

		AuxIOInfo newRequest = new AuxIOInfo();
		// Add it to the hash
		newRequest.addHTTPHeaderField(
			AuxIOInfo.HTTP_USER_AGENT_FIELD_SPELLING,
			AuxIOInfo.DEFAULT_HTTP_USER_AGENT_FIELD
			);
		// genLink Takes care of nulls and ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE
		Element linkElem = fSearchLink.generateLinkElement(
			newRequest, "foo", inTerm
			);
		if( null==linkElem )
			throw new Exception( kExTag +
				"Got back null link element."
				);
		String outURL = linkElem.getAttributeValue( "href" );
		outURL = NIEUtil.trimmedStringOrNull( outURL );
		if( null==outURL )
			throw new Exception( kExTag +
				"Got back null/empty href in the link element."
				);
		return outURL;
	}

	// URL's or just search terms
	void _examineAndReportByTerm( String inQuery )
		throws Exception
	{
		final String kFName = "examineAndReport";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// Generate a search engine search
		// we are NOT going through our proxy at this time
		String theURL = null;
		if( null!=inQuery && inQuery.indexOf(":/") >= 0 )
			theURL = inQuery;
		else {
			theURL = expandTermToTestDriveURL( inQuery );
		}

		// Get the content
		Document doc = fetchUrlAsDom( theURL );
		Element root = doc.getRootElement();

		// String targetPath = "//div/font/a"; // "//a";
		// String targetPath = "//title | //div/font/a"; // "//a";
		// String targetPath = "//title | //meta/@content | //a"; // "//a";
		// String targetPath = "//title | //meta/@content/[@name=Description]"; // "//a";
		// String targetPath = "//meta[@name=Description]";
		// String targetPath = "//meta/@name";
		// String targetPath = "//meta[@name='description']/@content";
		// String targetPath = "//meta[ str:to-lower(@name) ='description']/@content";
		// String targetPath = "//meta[ lower-case(@name) = 'description']/@content";

		// String targetPath = "//title | //meta[lower-case(@name)='description']/@content | //a";
		// NOTE: using the UNION (|) operator breaks document-ordering of returned nodes
		// but not doing so also seems to cause a problem...
		String targetPath = "//a";

		// findAndShowPath( doc, targetPath );

		// JDOMHelper.writeToFile( root, "tmp.html", true );
	}

	public static void findAndShowPathSearch( Document inDoc, String inPath )
		throws JDOMException
	{
		if( null==inDoc )
			throw new JDOMException( "Null input document passed in." );
		findAndShowPathSearch( inDoc.getRootElement(), inPath );
	}

	// public static void findAndShowPath( Document inDoc, String inPath )
	public static void findAndShowPathSearch( Element inTop, String inPath )
			throws JDOMException
	{
		final String kFName = "findAndShowPath";

		if( null==inTop )
			throw new JDOMException( "Null input element passed in." );
		inPath = NIEUtil.trimmedStringOrNull( inPath );
		if( null==inTop )
			throw new JDOMException( "Null search path passed in." );

		XPath xpath = XPath.newInstance( inPath );

		// List results = xpath.selectNodes( inDoc );
		List results = xpath.selectNodes( inTop );

		statusMsg( kFName,
			"Looking for \"" + inPath + "\" from node \"" + inTop.getName() + "\""
			// "Looking for \"" + inPath + "\" from node \"" + inDoc.getRootElement().getName() + "\""
			);

		for( Iterator it = results.iterator() ; it.hasNext() ; ) {
			Object currObj = it.next();
			if( currObj instanceof org.jdom.Element ) {
				Element currElem = (Element) currObj;
				// org.JDOM.Content currElem = (Content) it.next();
				String fieldName = currElem.getAttributeValue( "name" );
				String currPath = JDOMHelper.getPathToElement( currElem );
				statusMsg( kFName,
					"Name/path="
					+ NIEUtil.NL + "  " // '\t'
					// + "\"" + fieldName + "\" @ "
					// + "\""
					+ currPath
					// + "\""
					+ NIEUtil.NL + "  " // '\t'
					// + "url = \""
					+ JDOMHelper.getStringFromAttributeTrimOrNull( currElem, "href" )
					// + "\""
					// + " = "
					+ NIEUtil.NL + "  " // '\t'
					+ currElem.getTextNormalize()
					+ NIEUtil.NL
					);
			}
			else {
				Attribute currAttr = (Attribute) currObj;
				statusMsg( kFName,
					"Attr " 
					+ NIEUtil.NL + "  " // '\t'
					// + currAttr.toString()
					+ currAttr.getValue()
					);
			}


		}	// End for each matching node


	}

	public static void analyzeForms( Document inDoc, String inPath, boolean inShowSource )
		throws JDOMException
	{
		if( null==inDoc )
			throw new JDOMException( "Null input document passed in." );
		analyzeForms( inDoc.getRootElement(), inPath, inShowSource );
	}


	// public static void findAndShowPath( Document inDoc, String inPath )
	public static void analyzeForms( Element inTop, String inPath, boolean inShowSource )
			throws JDOMException
	{
		final String kFName = "analyzeForms";

		if( null==inTop )
			throw new JDOMException( "Null input element passed in." );
		inPath = NIEUtil.trimmedStringOrNull( inPath );
		if( null==inTop )
			throw new JDOMException( "Null search path passed in." );

		XPath xpath = XPath.newInstance( inPath );

		// List results = xpath.selectNodes( inDoc );
		List results = xpath.selectNodes( inTop );

		statusMsg( kFName,
			"Looking for \"" + inPath + "\" from node \"" + inTop.getName() + "\""
			// "Looking for \"" + inPath + "\" from node \"" + inDoc.getRootElement().getName() + "\""
			);

		int formCounter = 0;
		for( Iterator it = results.iterator() ; it.hasNext() ; ) {
			Object currObj = it.next();
			if( currObj instanceof org.jdom.Element ) {
				formCounter++;
				Element currElem = (Element) currObj;
				// org.JDOM.Content currElem = (Content) it.next();
				String fieldName = currElem.getAttributeValue( "name" );
				String currPath = JDOMHelper.getPathToElement( currElem );
				// statusMsg( kFName,
				System.out.println(
					"Form # " + formCounter + " path = "
					// + NIEUtil.NL + "  " // '\t'
					// + "\"" + fieldName + "\" @ "
					// + "\""
					+ currPath
					// + "\""
					+ NIEUtil.NL + "  " // '\t'
					// + "url = \""
					+ JDOMHelper.getStringFromAttributeTrimOrNull( currElem, "method" )
					+ " to "
					+ JDOMHelper.getStringFromAttributeTrimOrNull( currElem, "action" )
					// + "\""
					// + " = "
					// + NIEUtil.NL + "  " // '\t'
					// + currElem.getTextNormalize()
					// + NIEUtil.NL
					);

				if( inShowSource ) {
					String theText = JDOMHelper.getTreeText( currElem, true );
					System.out.println( "Text: "
						+ (null==theText ? 0 : theText.length() ) + " long"
						);
					System.out.println( theText );

					String theHTML = JDOMHelper.JDOMToString( currElem, true );
					System.out.println( "HTML: "
						+ (null==theHTML ? 0 : theHTML.length() ) + " long"
						);
					System.out.println( theHTML );
				}

				XPath xpath2 = XPath.newInstance( ".//input | .//select" );

				// List results = xpath.selectNodes( inDoc );
				List widgets = xpath2.selectNodes( currElem );

				statusMsg( kFName,
					"Looking for form widgets"
					);

				for( Iterator it2 = widgets.iterator() ; it2.hasNext() ; ) {
					Element widget = (Element) it2.next();

					String wigName = widget.getAttributeValue( "name" );

					String wigType = widget.getName();
					if( ! wigType.equals("select") ) {
						wigType = widget.getAttributeValue( "type" );
						wigType = NIEUtil.trimmedLowerStringOrNull( wigType );
						if( null==wigType )
							wigType = "text";
					}


					// statusMsg( kFName,
					System.out.println(
						"  Widget="
						// + NIEUtil.NL + "    " // 2 indents
						+ wigType + " / " + wigName
						// + NIEUtil.NL
						);


				}

			}
			else {
				Attribute currAttr = (Attribute) currObj;
				statusMsg( kFName,
					"Attr " 
					+ NIEUtil.NL + "  " // '\t'
					// + currAttr.toString()
					+ currAttr.getValue()
					);
			}


		}	// End for each matching node


	}




	public static Document fetchUrlAsDom( String inURL )
			throws IOException, JDOMException, Exception
	{
		final String kFName = "fetchUrlAsDom";

		// Fetch and Convert the content into an XHTML tree, including
		// the handling of mangled HTML
		Document doc = JDOMHelper.readHTML( inURL );
		Element root = doc.getRootElement();
		// Rename from document to html
		root.setName( "html" );
		// Rename "info" to head
		List children = root.getChildren( "info" );
		if( children!=null && ! children.isEmpty() ) {
			for( Iterator it = children.iterator() ; it.hasNext() ; ) {
				Element elem = (Element) it.next();
				elem.setName( "head" );
			}
		}


		// final String kPre = "/html/head/";
		final String kPre = "| head/";
		String cleanPath1 =
			"error"
			+ kPre + "expires"
			+ kPre + "location"
			+ kPre + "last-modified"
			+ kPre + "type"
			+ kPre + "length"
			+ kPre + "meta[ lower-case(@http-equiv) ='content-type' ]"
			;

		debugMsg( kFName, "Cleaning out " + cleanPath1 );

		XPath clean1 = XPath.newInstance( cleanPath1 );
		// List cleanList = clean1.selectNodes( doc );
		List cleanList = clean1.selectNodes( root );
		for( Iterator cl = cleanList.iterator() ; cl.hasNext() ; ) {
			Element badElem = (Element) cl.next();
			debugMsg( kFName, "cleaned " + badElem.getName() );
			badElem.detach();
			badElem = null;
		}

		// <head>
		//	<meta content="text/html;charset=iso-8859-1" http-equiv="content-type"
		// <base href="http://ideaeng.com/search/">

		// Fix the encoding
		Element meta = JDOMHelper.findOrCreateElementByPath(
			root,
			"head/meta[+]/@http-equiv=content-type",
			true
			);
		if( null!=meta )
			meta.setAttribute( "content", "text/html;charset=utf8" );
		// fix the relative tag
		Element base = JDOMHelper.findOrCreateElementByPath(
			root,
			"head/base",
			true
			);
		if( null!=base )
			base.setAttribute( "href", inURL );

		return doc;
	}


	SearchTuningConfig getMainConfig() {
		return fMainConfig;
	}

	SearchTuningConfig fMainConfig;
	SearchEngineLink fSearchLink;

	private static RunLogInterface getRunLogObject()
	// can't access some of impl's extensions with interface reference
	//private static RunLogBasicImpl getRunLogObject()
	{
		// return RunLogBasicImpl.getRunLogObject();
		return RunLogBasicImpl.getRunLogObject();
	}

	// Return the same thing casted to allow access to impl extensions
	private static RunLogBasicImpl getRunLogImplObject()
	{
		// return RunLogBasicImpl.getRunLogObject();
		return RunLogBasicImpl.getRunLogImplObject();
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





}
