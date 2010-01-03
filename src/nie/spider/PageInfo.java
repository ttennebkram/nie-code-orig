package nie.spider;

import java.io.*;
import java.util.*;
import java.net.*;

import org.jdom.*;
import org.jdom.xpath.*;
import nie.core.*;


// Track information about an HTML form
public class PageInfo
{

	private static final String kClassName = "PageInfo";

//	// private static boolean debug = true;
//	// Don't specifically set it if you want FALSE, it'll default to that
//	private static boolean debug;
//	public static void setDebug( boolean flag )
//	{
//		debug = flag;
//	}


	public static void main(String[] args)
	{
		final String kFName = "main";

		debugMsg( kFName, "Start" );
		/***
		if( args.length < 2 ) {
			fatalErrorMsg( kFName, "syntax: give config file on command line, then searches..." );
			System.exit(1);
		}
		***/

		List stuffToTry = new Vector();
		// boolean doForms = false;
		// boolean doLinks = false;
		boolean doShowSummary = false;
		boolean doShowSource = false;
		boolean doShowConfig = false;
		boolean doShowText = false;
		boolean doShowWords = false;
		boolean doShowVendor = false;
		// String pattern = null;
		// String config = null;
		String targetVendor = null;

		String cacheDir = null;

		String listFile = null;
		try {
			// FindLinksAndForms finder = new FindLinksAndForms( args[0] );
			// FindLinksAndForms finder = new FindLinksAndForms();

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
					debugMsg( kFName, "-arg=" + arg );
					if( arg.indexOf("summary") >= 0 || arg.indexOf("info") >= 0 ) {
						doShowSummary = true;
						// statusMsg( kFName, "show html" );
					}
					/***
					if( arg.indexOf("form") >= 0 ) {
						doForms = true;
						// statusMsg( kFName, "do forms" );
					}
					else if( arg.indexOf("link") >= 0 ) {
						doLinks = true;
						// statusMsg( kFName, "do links" );
					}
					***/
					else if( /*arg.indexOf("show") >= 0 ||*/
							arg.indexOf("source") >= 0 || arg.indexOf("html") >= 0
						) {
						doShowSource = true;
						// statusMsg( kFName, "show html" );
					}
					else if( arg.indexOf("config") >= 0 ) {
						doShowConfig = true;
						// statusMsg( kFName, "show html" );
					}
					else if( arg.indexOf("words") >= 0 ) {
						doShowWords = true;
						// statusMsg( kFName, "show html" );
					}
					else if( arg.indexOf("text") >= 0 ) {
						doShowText = true;
						// statusMsg( kFName, "do links" );
					}
					else if(
							arg.indexOf("vendor_filter") >= 0
							|| arg.indexOf("filter_vendor") >= 0
							|| arg.indexOf("target_vendor") >= 0
							|| arg.indexOf("vendor_target") >= 0
						) {
						doShowVendor = true;
						if( i==args.length-1 )
							errorMsg( kFName, "-" + arg + " requires an argument" );
						else {
							if( null!=targetVendor )
								errorMsg( kFName, "already have target vendor." );
							else
								targetVendor = args[++i];
						}
						// statusMsg( kFName, "do links" );
					}
					else if( arg.indexOf("vendor") >= 0 ) {
						doShowVendor = true;
						// statusMsg( kFName, "do links" );
					}
					/***
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
					***/
					else if( arg.indexOf("list") >= 0 ) {
						if( i==args.length-1 )
							errorMsg( kFName, "-" + arg + " requires an argument" );
						else {
							if( null!=listFile )
								errorMsg( kFName, "already have list file." );
							else
								listFile = args[++i];
						}
					}
					else if( arg.indexOf("cache") >= 0 ) {
						if( i==args.length-1 )
							errorMsg( kFName, "-" + arg + " requires an argument" );
						else {
							if( null!=cacheDir )
								errorMsg( kFName, "already have cache dir." );
							else
								cacheDir = args[++i];
						}
					}
					else
						errorMsg( kFName, "Unknown option -" + arg );
				}
				else {
					stuffToTry.add( arg );
				}
			}

			/***
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
			***/

			if( null!=listFile ) {
				try {
					List tmpList = NIEUtil.fetchURIContentsLines(
						listFile,	// String inBaseName,
						null,	// String optRelativeRoot,
						null,	// String optUsername,
						null,	// String optPassword,
						true,	// boolean inDoTrim,
						true,	// boolean inDoSkipBlankLines,
						null,	// AuxIOInfo inoutAuxIOInfo
						false	// use POST
						);
					if( null!=tmpList && ! tmpList.isEmpty() )
						stuffToTry.addAll( tmpList );
					else
						errorMsg( kFName, "Got back null/empty list after reading list file \"" + listFile + "\"" );
				}
				catch( IOException e ) {
					errorMsg( kFName, "Error reading list file \"" + listFile + "\", error: " + e );
				}
			}

			if( stuffToTry.isEmpty() )
				throw new Exception( kFName + "Syntax: No URLs or terms to try" );

			// Force something to happen
			if( ! doShowSummary )
				if( ! doShowConfig && ! doShowSource && ! doShowText
						&& ! doShowWords && ! doShowVendor
					)
					doShowSummary = true;
			boolean hushMode = doShowVendor
				&& ! doShowSummary && ! doShowConfig && ! doShowSource
				&& ! doShowText && ! doShowWords
				;


			// For each thing to try
			for( Iterator it = stuffToTry.iterator(); it.hasNext() ; ) {
				String theURL = (String) it.next();
				theURL = NIEUtil.trimmedStringOrNull( theURL );
				// Skip blank lines
				if( null==theURL )
					continue;
				// Skip commented lines
				if( null!=theURL && theURL.startsWith("#") )
					continue;
				String origURL = theURL;
				theURL = normalizeDomainToURL( theURL );

				// String theURL = (null!=theString && theString.indexOf(":/") >= 0)
				//	? theString : finder.expandTermToTestDriveURL( theString )
				//	;

				if( ! hushMode ) {
					System.out.println();
					System.out.println( "Checking URL " + theURL );
				}

				// Get the forms from that URL
				try {
					PageInfo page = new PageInfo( theURL, cacheDir );

					List forms = page.findForms();
					if( ! hushMode )
						System.out.println( "Found " + forms.size() + " forms." );
					int formCounter = 0;
					for( Iterator fit = forms.iterator() ; fit.hasNext() ; ) {
						FormInfo form = (FormInfo) fit.next();
						formCounter++;
						if( doShowSummary )
							form.printFormSummary();
						if( doShowVendor /*&& ! doShowSummary*/ )
							form.printVendor( targetVendor );
						if( doShowText )
							System.out.println(
								"Text:" + NIEUtil.NL +
								form.getFormTreeTextOrNull()
								+ NIEUtil.NL
								);
						if( doShowConfig )
							System.out.println(
								"Config:" + NIEUtil.NL +
								JDOMHelper.JDOMToString(
									form.generateSearchEngineConfigTree(), true
									)
								+ NIEUtil.NL
								);
						if( doShowSource )
							System.out.println(
								"Souce:" + NIEUtil.NL +
								JDOMHelper.JDOMToString(
									form.getFormElem(), true
									)
								+ NIEUtil.NL
								);
						if( doShowWords && 1==formCounter )
							System.out.println(
								"Top Words:" + NIEUtil.NL
								// + form.topWordsReport( 5 )
								+ page.topWordsReport( 5 )
								+ NIEUtil.NL
								);
					}
				}
				catch( IOException ioe ) {
					errorMsg( kFName,
						"IO Exception for URL: " + theURL
						+ " Error: " + ioe
						);
				}
				catch( Exception e ) {
					e.printStackTrace( System.out );
					errorMsg( kFName,
						"Error analyzing URL \"" + theURL + "\""
						+ " Orig URL = \"" + origURL + "\""						+ " Error: " + e
						);
				}

				/***
				Document doc = fetchUrlAsDom( theURL );

				if( ! doForms )
					findAndShowPathSearch( doc, pattern );
				else
					analyzeForms( doc, pattern, doShowSource );
				***/

			}


		}
		catch( Throwable t ) {
			fatalErrorMsg( kFName, "Error: " + t );
			t.printStackTrace( System.out );
			System.exit(1);
		}


	}

	public PageInfo()
	{
		initVars();
	}

	public PageInfo( String inURL )
		throws PageInfoException, IOException
	{
		this( inURL, null );
	}
	public PageInfo( String inURL, String optCacheDir )
		throws PageInfoException, IOException
	{
		final String kFName = "constructor(2)";
		final String kExTag = kClassName + '.' + kFName + ": ";
		if( null==inURL )
			throw new PageInfoException( kExTag + "Null/empty URL passed in." );
		mPageURL = inURL;

		if( null!=optCacheDir ) {
			if( null==mCacheObj || null==mCacheDirName || ! mCacheDirName.equals(optCacheDir) ) {
				mCacheDirName = optCacheDir;
				mCacheObj = new CachingRetriever( mCacheDirName, true );
			}
		}

		// If there's a cache, use it
		if( null!=mCacheObj ) {
			byte [] content = mCacheObj.fetchContents( inURL );
			mPageTreeElem = bytesToDom( content );
		}
		else {
			// Will complain if there's a problem
			mPageTreeElem = fetchUrlAsDom( inURL );
		}

		finishInit();
	}

	public PageInfo( byte [] inPageBuffer )
		throws PageInfoException, IOException
	{
		final String kFName = "constructor(3)";
		final String kExTag = kClassName + '.' + kFName + ": ";
		if( null==inPageBuffer )
			throw new PageInfoException( kExTag + "Null/empty page buffer passed in." );
		// mPageURL = inURL;
		// Will complain if there's a problem
		// mPageTreeElem = fetchUrlAsDom( inURL );
		mPageTreeElem = bytesToDom( inPageBuffer );

		finishInit();
	}

	public PageInfo(
		Element inPageElem,
		String inURL
		)
			throws PageInfoException
	{
		final String kFName = "constructor(4)";
		final String kExTag = kClassName + '.' + kFName + ": ";
		if( null==inPageElem )
			throw new PageInfoException( kExTag + "Null page element passed in." );
		mPageURL = inURL;
		mPageTreeElem = inPageElem;

		finishInit();
	}

	void finishInit()
		throws PageInfoException
	{
		initVars();
		// analyzePage();
	}

	void initVars() {
		mUseCache = false;
		getDeclaredBaseUrlOrNull( false );
		getTitle(true);
		getDescription(true);
		mUseCache = true;
	}

	private static void ___High_Level_Analysis___() {}
	////////////////////////////////////////////////////////////

	////////////////////////////////////////////////////////////

	// public static void findAndShowPath( Document inDoc, String inPath )
	public List findForms()
		throws PageInfoException
	{
		final String kFName = "findForms";
		final String kExTag = kClassName + '.' + kFName + ": ";
		boolean trace = shouldDoTraceMsg( kFName );
		boolean debug = shouldDoDebugMsg( kFName );

		Element pageRoot = getPageElem();
		if( null==pageRoot )
			throw new PageInfoException( kExTag + "Page root Element is Null." );

		List outList = new Vector();

		// final String kPath = "//form";
		final String kPath = "//form";

		// A special hash used for sorting
		// The key is actually a list of numbers, the numeric path to the node
		// the value is the Element / node itself
		Hashtable holdingHash = new Hashtable();
		Hashtable pathHash = new Hashtable();

		try {
			XPath xpath = XPath.newInstance( kPath );
			// xpath.

			debugMsg( kFName,
				"path=" + xpath.getXPath()
				+ " (url: " + getPageUrlOrNull() + ")"
				);
	
			// List results = xpath.selectNodes( inDoc );
			List results = xpath.selectNodes( pageRoot );
	
			debugMsg( kFName,
				"Looking for \"" + kPath + "\" from node \"" + pageRoot.getName() + "\""
				// "Looking for \"" + inPath + "\" from node \"" + inDoc.getRootElement().getName() + "\""
				+ " Found " + results.size() + " forms."
				+ " (url: " + getPageUrlOrNull() + ")"
				);
	
			int formCounter = 0;
			// For each form
			for( Iterator it = results.iterator() ; it.hasNext() ; ) {
				Object currObj = it.next();
				if( currObj instanceof org.jdom.Element ) {
					formCounter++;
					Element currElem = (Element) currObj;
					// org.JDOM.Content currElem = (Content) it.next();
					String fieldName = currElem.getAttributeValue( "name" );
					String currPath = JDOMHelper.getPathToElement( currElem );
					String currNPath = JDOMHelper.getPathToElementNumericStr( currElem );
					List currNPathBits = JDOMHelper.getPathToElementNumericList( currElem );

					debugMsg( kFName, "our path=" + currPath );
					debugMsg( kFName, "our npath=" + currNPath );
					debugMsg( kFName, "npath bits=" + currNPathBits );

					holdingHash.put( currNPathBits, currElem );
					pathHash.put( currNPathBits, currPath );

					/***
					// Will throw exception if it's not happy
					FormInfo newForm = new FormInfo(
						currElem, getPageUrlOrNull(), formCounter, pageRoot, currPath
						);

					outList.add( newForm );
					***/

				}
				else {
					Attribute currAttr = (Attribute) currObj;
					errorMsg( kFName,
						"Don't know how to handle Attr " 
						+ NIEUtil.NL + "  " // '\t'
						// + currAttr.toString()
						+ currAttr.getValue()
						+ ", skipping."
						+ " (url: " + getPageUrlOrNull() + ")"
						);
				}
			}	// End for each matching node

			// Now we sort them in document order and create them
			Set keys = holdingHash.keySet();
			// List sortedKeys = NIEUtil.sortAsc( keys );
			List sortedKeys = new Vector( keys );

			Comparator comp = new Comparator() {
				public int compare(Object o1, Object o2) {
					if( null==o1 || null==o2 )
						return 0;
					List l1 = (List) o1;
					Iterator i1 = l1.iterator();
					List l2 = (List) o2;
					Iterator i2 = l2.iterator();
					while( i1.hasNext() || i2.hasNext() ) {
						Object obj1 = i1.hasNext() ? i1.next() : null;
						Object obj2 = i2.hasNext() ? i2.next() : null;
						if( null==obj1 )
							return -1;
						if( null==obj2 )
							return 1;
						Comparable c1 = (Comparable) obj1;
						Comparable c2 = (Comparable) obj2;
						int tmpResult = c1.compareTo( c2 );
						if( 0 != tmpResult )
							return tmpResult;
					}
					return 0;
				}
				public boolean equals(Object obj) { return false; }
			};

			Collections.sort( sortedKeys, comp );

			// Vector v = new Vector();
			// Comparable c = v;



			for( Iterator it2 = sortedKeys.iterator() ; it2.hasNext() ; ) {
				List compositeKey = (List) it2.next();

				debugMsg( kFName, "ckey=" + compositeKey );
				Element currElem = (Element) holdingHash.get( compositeKey );
				String currPath = (String) pathHash.get( compositeKey );
				// Will throw exception if it's not happy
				FormInfo newForm = new FormInfo(
					currElem, getPageUrlOrNull(), formCounter, pageRoot, currPath
					);

				outList.add( newForm );

			}


		}
		catch( JDOMException e ) {
			throw new PageInfoException( kExTag + "Got JDOM/XML exception: " + e );
		}
		catch( FormInfoException ef ) {
			throw new PageInfoException( kExTag + "Got Form Info exception: " + ef );
		}
		catch( Throwable t ) {
			t.printStackTrace( System.err );
			throw new PageInfoException( kExTag + "General exception: " + t );
		}

		return outList;

	}






	// public static void findAndShowPath( Document inDoc, String inPath )
	public List findFormsOBS()
		throws PageInfoException
	{
		final String kFName = "findFormsOBS";
		final String kExTag = kClassName + '.' + kFName + ": ";
		boolean trace = shouldDoTraceMsg( kFName );
		boolean debug = shouldDoDebugMsg( kFName );

		Element pageRoot = getPageElem();
		if( null==pageRoot )
			throw new PageInfoException( kExTag + "Page root Element is Null." );

		List outList = new Vector();

		// final String kPath = "//form";
		final String kPath = "//form";

		try {
			XPath xpath = XPath.newInstance( kPath );
			// xpath.

statusMsg( kFName, "path=" + xpath.getXPath() );
	
			// List results = xpath.selectNodes( inDoc );
			List results = xpath.selectNodes( pageRoot );
	
			debugMsg( kFName,
				"Looking for \"" + kPath + "\" from node \"" + pageRoot.getName() + "\""
				// "Looking for \"" + inPath + "\" from node \"" + inDoc.getRootElement().getName() + "\""
				+ " Found " + results.size() + " forms."
				);
	
			int formCounter = 0;
			// For each form
			for( Iterator it = results.iterator() ; it.hasNext() ; ) {
				Object currObj = it.next();
				if( currObj instanceof org.jdom.Element ) {
					formCounter++;
					Element currElem = (Element) currObj;
					// org.JDOM.Content currElem = (Content) it.next();
					String fieldName = currElem.getAttributeValue( "name" );
					String currPath = JDOMHelper.getPathToElement( currElem );
					String currNPath = JDOMHelper.getPathToElementNumericStr( currElem );
					List currNPathBits = JDOMHelper.getPathToElementNumericList( currElem );

statusMsg( kFName, "our path=" + currPath );
statusMsg( kFName, "our npath=" + currNPath );
statusMsg( kFName, "npath bits=" + currNPathBits );

					// Will throw exception if it's not happy
					FormInfo newForm = new FormInfo(
						currElem, getPageUrlOrNull(), formCounter, pageRoot, currPath
						);

					outList.add( newForm );

					/***
					if(debug) {
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
					}
	
					if(trace) {
						String theText = JDOMHelper.getTreeText( currElem );
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
	
					debugMsg( kFName,
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
					***/


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
		catch( JDOMException e ) {
			throw new PageInfoException( kExTag + "Got JDOM/XML exception: " + e );
		}
		catch( FormInfoException ef ) {
			throw new PageInfoException( kExTag + "Got Form Info exception: " + ef );
		}

		return outList;

	}


	public Hashtable getTopNPageWords( int inRankCount ) {
		final String kFName = "getTopNPageWords";
		Hashtable candiateHash = getPageWords();
		Hashtable byCountHash = null;
		if( null!=candiateHash ) {
	
			// Get rid of stop words
			Set stopWords = NIEUtil.getEnglishStopWords();
			Set tmpKeys = new HashSet();
			tmpKeys.addAll( candiateHash.keySet() );
			for( Iterator tmpIt = tmpKeys.iterator() ; tmpIt.hasNext() ; ) {
				Object tmpKey = tmpIt.next();
				if( stopWords.contains(tmpKey) )
					candiateHash.remove( tmpKey );
			}
	
			byCountHash = NIEUtil.reverseHashKeysAndValues( candiateHash );
			// Set origKeys = candiateHash.keySet();
			Set origKeys = byCountHash.keySet();
			// statusMsg( kFName, "orig keys = " + origKeys );
			HashSet copyKeys = new HashSet();
			copyKeys.addAll( origKeys );
			List topKeys = NIEUtil.getTopNItems( origKeys, inRankCount );
			// statusMsg( kFName, "top keys = " + topKeys );
			copyKeys.removeAll( topKeys );
			// statusMsg( kFName, "remove keys = " + copyKeys );
			for( Iterator it = copyKeys.iterator() ; it.hasNext() ; ) {
				Object key = it.next();
				// candiateHash.remove( key );
				byCountHash.remove( key );
			}
		}
		// return candiateHash;
		return byCountHash;
	}

	private static void ___Lower_Level_Logic___() {}
	///////////////////////////////////////////////////////

	public static String normalizeDomainToURL( String inURL ) {
		final String kFName = "normalizeDomainToURL";
		inURL = NIEUtil.trimmedStringOrNull( inURL );
		if( null==inURL ) {
			errorMsg( kFName, "Null/empty URL passed in, returning null." );
			return null;
		}
		String origDomain = inURL;
		// Sanity
		if( NIEUtil.isStringAURL( inURL ) )
			return inURL;
		if( inURL.indexOf('/') >=0 ) {
			warningMsg( kFName, "Suspicious domain name \"" + inURL + "\" has a slash in it, returning it as-is." );
			return inURL;
		}
		if( inURL.toLowerCase().startsWith("www.") )
			return "http://" + inURL + '/';

		// TLD
		int lastDotAt = inURL.lastIndexOf( '.' );
		if( lastDotAt < 0 || lastDotAt==(inURL.length()-1) ) {
			errorMsg( kFName, "Invalid domain name \"" + inURL + "\" passed in, returning null." );
			return null;
		}
		String tld = inURL.substring( lastDotAt+1 );
		tld = NIEUtil.trimmedStringOrNull( tld );
		if( null==tld ) {
			errorMsg( kFName, "Null/empty top level domain in the domain name \"" + inURL + "\" passed in, returning null." );
			return null;
		}

		// Obsess about dots
		int dotsInDomain = 0;
		int fromIndex = 0;
		while( true ) {
			int dotAt = inURL.indexOf( '.', fromIndex );
			if( dotAt < 0 )
				break;
			dotsInDomain++;
			fromIndex = dotAt+1;
			if( fromIndex >= inURL.length() )
				break;
		}
		int numSections = dotsInDomain + 1;

		// NOT counting any www. prefix
		int goalNumSections = (3==tld.length()) ? 2 : 3;

		// How much to trim off
		int trimNumSections = numSections - goalNumSections;
		trimNumSections = (trimNumSections>=0) ? trimNumSections : 0;

		// Drop the sections requested
		for( int i=0; i<trimNumSections ; i++ ) {
			int dotAt = inURL.indexOf( '.' );
			if( dotAt < 0 || dotAt==(inURL.length()-1) ) {
				errorMsg( kFName,
					"Not able to remove prefix section # " + (i+1) + " in the domain name \"" + origDomain + "\" passed in."					+ " dotAt=" + dotAt
					+ ", length=" + inURL.length()
					+ " Returning null." );
				return null;
			}
			fromIndex = dotAt+1;
			inURL = inURL.substring( fromIndex );
			inURL = NIEUtil.trimmedStringOrNull( inURL );
			if( null==inURL ) {
				errorMsg( kFName, "Wound up with Null/empty domain when reducing \"" + origDomain + "\" passed in, returning null." );
				return null;
			}
		}

		// add the prefix and decorations
		inURL = "http://www." + inURL + '/';

		infoMsg( kFName, "From " + origDomain + " to " + inURL );

		return inURL;
	}

	// public static Document fetchUrlAsDom( String inURL )
	public static Element fetchUrlAsDom( String inURL )
		throws PageInfoException, IOException
	{
		final String kFName = "fetchUrlAsDom(1)";
		final String kExTag = kClassName + '.' + kFName + ": ";
	
		Document doc = null;
		Element root = null;
		// try {
	
			// Fetch and Convert the content into an XHTML tree, including
			// the handling of mangled HTML
			doc = JDOMHelper.readHTML( inURL );
			// ^^^ this can cause exceptions!!!
			root = doc.getRootElement();

			fixupHtml( root, inURL );

			/***
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
			***/

		// }
		// catch( IOException ioe ) {
		//	// e.printStackTrace( System.err );
		//	throw new IOException( kExTag + "IO exception: " + ioe );
		// }
		// catch( Exception e ) {
		// catch( JDOMException e ) {
		//	e.printStackTrace( System.err );
		//	throw new PageInfoException( kExTag + "Caught exception: " + e );
		// }
	
	
		// return doc;
		return root;
	}

	// public static Document fetchUrlAsDom( String inURL )
	public static Element bytesToDom( byte [] inPageBuffer )
		throws PageInfoException, IOException
	{
		final String kFName = "bytesToDom(2)";
		final String kExTag = kClassName + '.' + kFName + ": ";
	
		Document doc = null;
		Element root = null;
		// try {
	
			// Fetch and Convert the content into an XHTML tree, including
			// the handling of mangled HTML
			doc = JDOMHelper.readHTML( inPageBuffer );
			// ^^^ this can cause exceptions!!!
			root = doc.getRootElement();

			fixupHtml( root, null );

		// }
		// catch( IOException ioe ) {
		//	// e.printStackTrace( System.err );
		//	throw new IOException( kExTag + "IO exception: " + ioe );
		// }
		// catch( Exception e ) {
		// catch( JDOMException e ) {
		// 	e.printStackTrace( System.err );
		// 	throw new PageInfoException( kExTag + "Caught exception: " + e );
		// }
	
	
		// return doc;
		return root;
	}

	public static void fixupHtml( Element inRoot, String optURL )
		throws PageInfoException
	{
		final String kFName = "fixupHtml";
		final String kExTag = kClassName + '.' + kFName + ": ";

		if( null==inRoot )
			throw new PageInfoException( kExTag + "Null root element passed in." );

		// Rename from document to html
		inRoot.setName( "html" );
		// Rename "info" to head
		List children = inRoot.getChildren( "info" );
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

		try {
			XPath clean1 = XPath.newInstance( cleanPath1 );
			// List cleanList = clean1.selectNodes( doc );
			List cleanList = clean1.selectNodes( inRoot );
			for( Iterator cl = cleanList.iterator() ; cl.hasNext() ; ) {
				Element badElem = (Element) cl.next();
				debugMsg( kFName, "cleaned " + badElem.getName() );
				badElem.detach();
				badElem = null;
			}
		}
		catch( JDOMException e ) {
			e.printStackTrace( System.err );
			throw new PageInfoException( kExTag + "Caught exception: " + e );
		}

	
		// <head>
		//	<meta content="text/html;charset=iso-8859-1" http-equiv="content-type"
		// <base href="http://ideaeng.com/search/">
	
		// Fix the encoding
		Element meta = JDOMHelper.findOrCreateElementByPath(
			inRoot,
			"head/meta[+]/@http-equiv=content-type",
			true
			);
		if( null!=meta )
			meta.setAttribute( "content", "text/html;charset=utf8" );
		// fix the relative tag
		if( null!=optURL ) {
			Element base = JDOMHelper.findOrCreateElementByPath(
				inRoot,
				"head/base",
				true
				);
			if( null!=base )
				base.setAttribute( "href", optURL );
		}

	}


	public Hashtable getPageWords() {
		final String kFName = "getPageWords";
		String pageText = getPageTreeTextOrNull();
		// statusMsg( kFName, "pageText=" + pageText );
		Hashtable outHash = NIEUtil.singleStringToHashOfWordCounts(
			pageText,	// String inSourceString,
			"<>\"[] \t\n\r=+%#!,(){}|:;",	// String optDelimiters,
			true,	// boolean inDoTrimNull,
			true,	// boolean inDumpOuterQuotes,
			false,	// boolean inIsCasen,
			true	// boolean inDoWarnings
			);
		// statusMsg( kFName, "outHash=" + outHash );
		return outHash;
	}

	private static void ___Page_Properties___() {}
	public String getDeclaredBaseUrlOrNull( boolean inWarnOnNullFormOrPageURL ) {
	
		// cSpecificPageBaseURL
		if( ! mUseCache ) {
	
			final String kFName = "getDeclaredBaseUrlOrNull";
			try {
				Element mainElem = getPageElem();
				if( null==mainElem ) {
					if( inWarnOnNullFormOrPageURL )
						errorMsg( kFName,
							"Null base page Element; returning null."
							+ " (url: " + getPageUrlOrNull() + ")"
							);
					return null;
				}
	
				XPath myXpath = XPath.newInstance( "//base" );
			
				// List results = xpath.selectNodes( inDoc );
				List bases = myXpath.selectNodes( getPageElem() );
	
				if( null==bases || bases.isEmpty() ) {
					// no warning here
					return null;
				}
				if( bases.size() > 1 )
					warningMsg( kFName,
						"Multiple base url elements declared; returning first valid one."
						+ " (url: " + getPageUrlOrNull() + ")"
						);
	
				boolean foundOne = false;
				for( Iterator it = bases.iterator() ; it.hasNext() ; ) {
					Element baseElem = (Element) it.next();
					String url = baseElem.getAttributeValue( "href" );
					url = NIEUtil.trimmedStringOrNull( url );
					if( null!=url ) {
						cSpecificPageBaseURL = url;
						foundOne = true;
						break;
					}
				}	// End for each base element
				if( ! foundOne )
					errorMsg( kFName,
						"Looked at " + bases.size() + " candiate base tags but did not find one with valid non-null href."
						+ " (url: " + getPageUrlOrNull() + ")"
						);
	
			}
			catch( Exception e ) {
				errorMsg( kFName,
					"Cuaght exception looking for base tag(s) on page: " + e
					+ " (url: " + getPageUrlOrNull() + ")"
					);
			}
		}	// End if not using cache
	
		return cSpecificPageBaseURL;
	}

	public String getTitle( boolean inWarnOnNullFormOrPageURL ) {
	
		// cSpecificPageBaseURL
		if( ! mUseCache ) {
	
			final String kFName = "getTitle";
			try {
				Element mainElem = getPageElem();
				if( null==mainElem ) {
					if( inWarnOnNullFormOrPageURL )
						errorMsg( kFName,
							"Null base page Element; returning null."
							+ " (url: " + getPageUrlOrNull() + ")"
							);
					return null;
				}
	
				XPath myXpath = XPath.newInstance( "//head/title" );
			
				// List results = xpath.selectNodes( inDoc );
				List bases = myXpath.selectNodes( getPageElem() );
	
				if( null==bases || bases.isEmpty() ) {
					// no warning here
					return null;
				}
				if( bases.size() > 1 )
					warningMsg( kFName,
						"Multiple title elements declared; returning first valid one."
						+ " (url: " + getPageUrlOrNull() + ")"
						);
	
				boolean foundOne = false;
				for( Iterator it = bases.iterator() ; it.hasNext() ; ) {
					Element tmpElem = (Element) it.next();
					String title = JDOMHelper.getTreeText( tmpElem, true, true );
					title = NIEUtil.trimmedStringOrNull( title );
					if( null!=title ) {
						cTitle = title;
						foundOne = true;
						break;
					}
				}	// End for each base element
				if( ! foundOne )
					errorMsg( kFName,
						"Looked at " + bases.size() + " candiate title tags but did not find one with valid non-null href."
						+ " (url: " + getPageUrlOrNull() + ")"
						);
	
			}
			catch( Exception e ) {
				errorMsg( kFName,
					"Cuaght exception looking for tile tag(s) on page: " + e
					+ " (url: " + getPageUrlOrNull() + ")"
					);
			}
		}	// End if not using cache
	
		return cTitle;
	}

//	meta[lower-case(@name)='description']/@content

	public String getDescription( boolean inWarnOnNullFormOrPageURL ) {
	
		if( ! mUseCache ) {
	
			final String kFName = "getDescription";
			try {
				Element mainElem = getPageElem();
				if( null==mainElem ) {
					if( inWarnOnNullFormOrPageURL )
						errorMsg( kFName,
							"Null base page Element; returning null."
							+ " (url: " + getPageUrlOrNull() + ")"
							);
					return null;
				}

				XPath myXpath = XPath.newInstance( "//meta[lower-case(@name)='description']/@content" );
			
				// List results = xpath.selectNodes( inDoc );
				List attrs = myXpath.selectNodes( getPageElem() );
	
				if( null==attrs || attrs.isEmpty() ) {
					// no warning here
					return null;
				}
				if( attrs.size() > 1 )
					warningMsg( kFName,
						"Multiple description elements declared; returning first valid one."
						+ " (url: " + getPageUrlOrNull() + ")"
						);
	
				boolean foundOne = false;
				for( Iterator it = attrs.iterator() ; it.hasNext() ; ) {
					Attribute tmpAttr = (Attribute) it.next();
					String desc = tmpAttr.getValue();
					desc = NIEUtil.trimmedStringOrNull( desc );
					if( null!=desc ) {
						cDesc = desc;
						foundOne = true;
						break;
					}
				}	// End for each base element
				if( ! foundOne )
					errorMsg( kFName,
						"Looked at " + attrs.size() + " candiate meta description tags but did not find one with valid non-null href."
						+ " (url: " + getPageUrlOrNull() + ")"
						);
	
			}
			catch( Exception e ) {
				errorMsg( kFName,
					"Cuaght exception looking for description tag(s) on page: " + e
					+ " (url: " + getPageUrlOrNull() + ")"
					);
			}
		}	// End if not using cache
	
		return cDesc;
	}




	public String getPageUrlOrNull() {
		return mPageURL;
		// TODO: what about redirects?
	}

	public String getDeclaredBaseUrlOrNull() {
		return getDeclaredBaseUrlOrNull( true );
	}

	public String getNormalizedURL( String inURL ) {
		final String kFName = "getNormalizedURL";
	
		String outURL = NIEUtil.trimmedStringOrNull( inURL );
		// If no action, then never mind
		if( null==outURL ) {
			errorMsg( kFName, "Null/empty URL passed in, returning NULL." );
			return null;
		}
	
		if( NIEUtil.isStringAURL( outURL ) )
			return outURL;
	
		String tmpBase = getBaseUrlOrNull();
		if( null!=tmpBase ) {
			try {
				String tmpURL2 = NIEUtil.combineParentAndChildURLs( tmpBase, outURL );
				outURL = tmpURL2;
			}
			catch( IOException e ) {
				errorMsg( kFName,
					"Caught exception combining base and child URLs: " + e
					+ " base=" + tmpBase + "\""
					+ ", child=\"" + outURL + "\""
					+ " Returning null."
					);
				return null;
			}
		}
		else {
			errorMsg( kFName,
				"Unable to form abslute URL; no parent URL to base it on."
				+ " relative child URL =\"" + outURL + "\""
				+ " Returning null."
				);
			return null;
		}
	
		return outURL;
	}

	public String getBaseUrlOrNull() {
		final String kFName = "getBaseUrlOrNull";
		String startURL = getDeclaredBaseUrlOrNull();
	
		// If they gave us a full URL
		if( null!=startURL && NIEUtil.isStringAURL(startURL, true) )
			return startURL;
	
		// OK, where was the page?
		String pageURL = getPageUrlOrNull();
		String outURL = pageURL;
		// If we have both, combine them
		if( null!=pageURL && null!=startURL ) {
			try {
				outURL = NIEUtil.combineParentAndChildURLs( pageURL, startURL );
			}
			catch( IOException e ) {
				errorMsg( kFName,
					"Error combining URL's: " + e
					+ " parent=\"" + pageURL + "\""
					+ ", child=\"" + startURL + "\""
					+ " Returning null."
					);
				outURL = null;
			}
		}
	
		return outURL;
	}

	private static void ___simple_Get_and_Set___() {}
	public Element getPageElem() {
		return mPageTreeElem;
	}


	String getPageTreeTextOrNull() {
		final String kFName = "getPageTreeTextOrNull";
		String outText = null;
		Element tmpElem = getPageElem();
		if( null!=tmpElem ) {
			outText = JDOMHelper.getTreeText( tmpElem, true );
		}
		// statusMsg( kFName, "Page Text=" + NIEUtil.NL + outText );
		return outText;
	}





	private static void ___Display_and_Reporting___() {}
	public String topWordsReport( int inRankCount ) {
		final String kFName = "topWordsReport";
		Hashtable topWordsHash = getTopNPageWords( inRankCount );
		if( null==topWordsHash || topWordsHash.isEmpty() )
			return "No words found on page?";
		StringBuffer outBuff = new StringBuffer();
		outBuff.append( "# Times\tWord(s)" ); // .append( NIEUtil.NL );
		List keys = NIEUtil.sortHashKeysDesc( topWordsHash );
		for( Iterator it=keys.iterator() ; it.hasNext() ; ) {
			Integer keyCount = (Integer) it.next();
			List words = (List) topWordsHash.get( keyCount );
			outBuff.append( NIEUtil.NL );
			outBuff.append(' ').append( keyCount ).append( '\t' ).append( words );
		}
		return new String( outBuff );
	}

	private static void ___Runtime_Logging___() {}
	////////////////////////////////////////////////////////////



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
	private static boolean shouldDoDebugMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoDebugMsg( kClassName, inFromRoutine );
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

	private static void ___Member_Fields_and_Constants___() {}
	////////////////////////////////////////////////////////////

	String mPageURL;
	Element mPageElem;
	String mPageText;
	String cTitle;
	String cDesc;

	boolean mUseCache;

	String cSpecificPageBaseURL;
	Element mPageTreeElem;

	static String mCacheDirName;
	static CachingRetriever mCacheObj;

}
