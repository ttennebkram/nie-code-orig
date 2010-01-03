package nie.spider;

import java.io.*;
import java.util.*;
import java.net.*;
import org.jdom.*;
import org.jdom.xpath.*;
import nie.core.*;


// Track information about an HTML form
public class FormInfo
{

	private static final String kClassName = "FormInfo";

//	// private static boolean debug = true;
//	// Don't specifically set it if you want FALSE, it'll default to that
//	private static boolean debug;
//	public static void setDebug( boolean flag )
//	{
//		debug = flag;
//	}


	public static void _main(String[] args)
	{
		final String kFName = "main";

		statusMsg( kFName, "Main6" );
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
		// String pattern = null;
		// String config = null;
		String targetVendor = null;
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
				if( ! doShowConfig && ! doShowSource && ! doShowText && ! doShowWords )
					doShowSummary = true;

			// For each thing to try
			for( Iterator it = stuffToTry.iterator(); it.hasNext() ; ) {
				String theString = (String) it.next();

				// String theURL = (null!=theString && theString.indexOf(":/") >= 0)
				//	? theString : finder.expandTermToTestDriveURL( theString )
				//	;

				System.out.println();
				System.out.println( "Checking URL " + theString );

				// Get the forms from that URL
				try {
					List forms = findForms( theString );
					System.out.println( "Found " + forms.size() + " forms." );
					int formCounter = 0;
					for( Iterator fit = forms.iterator() ; fit.hasNext() ; ) {
						FormInfo form = (FormInfo) fit.next();
						formCounter++;
						if( doShowSummary )
							form.printFormSummary();
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
						/***	
						if( doShowWords && 1==formCounter )
							System.out.println(
								"Top Words:" + NIEUtil.NL
								+ form.topWordsReport( 5 )
								+ NIEUtil.NL
								);
						***/
					}
				}
				catch( Exception e ) {
					e.printStackTrace( System.out );
					errorMsg( kFName, "Error analyzing URL \"" + theString + "\", error: " + e );
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

	public FormInfo()
	{
		initVars();
	}

	public FormInfo(
		Element inFormElem,
		String optURL,
		int optFormOffsetBase1,
		Element optPageRootElem,
		String optPathToForm
		)
			throws FormInfoException
	{
		final String kFName = "constructor(2)";
		final String kExTag = kClassName + '.' + kFName + ": ";
		if( null==inFormElem )
			throw new FormInfoException( kExTag + "Null form element passed in." );
		mFormElem = inFormElem;
		mPageURL = optURL;
		mThisFormCountOnPage = optFormOffsetBase1;
		mPageTreeElem = optPageRootElem;
		mPathToForm = optPathToForm;

		initVars();

		analyzeForm();

		mUseCache = false;
		// getDeclaredBaseUrlOrNull( false );
		// vvv forces ^^^^ and others to fire and cache
		guessSearchVendor();
		mUseCache = true;
	}


	void initVars() {
		mCGIFieldHash = new Hashtable();
		mFieldDescHash = new Hashtable();
		mFieldValueDescHash = new Hashtable();

		mTextFieldsList = new Vector();
		mTextFieldsNormSet = new HashSet();
		mOptionFieldsList = new Vector();
		mOptionFieldsNormSet = new HashSet();
		mButtonFieldsList = new Vector();
		mButtonFieldsNormSet = new HashSet();
		mHiddenFieldsList = new Vector();
		mHiddenFieldsNormSet = new HashSet();
	}

	private static void ___High_Level_Analysis___() {}
	////////////////////////////////////////////////////////////

	// public static void findAndShowPath( Document inDoc, String inPath )
	public static List findForms( String inURL )
			throws FormInfoException
	{
		final String kFName = "findForms";
		final String kExTag = kClassName + '.' + kFName + ": ";
		boolean trace = shouldDoTraceMsg( kFName );
		boolean debug = shouldDoDebugMsg( kFName );

		// Will complain if there's a problem
		Element pageRoot = fetchUrlAsDom( inURL );

		List outList = new Vector();

		final String kPath = "//form";

		try {
			XPath xpath = XPath.newInstance( kPath );
	
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

					// Will throw exception if it's not happy
					FormInfo newForm = new FormInfo(
						currElem, inURL, formCounter, pageRoot, currPath
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
			throw new FormInfoException( kExTag + "Got exception: " + e );
		}

		return outList;

	}


	void analyzeForm()
		throws FormInfoException
	{
		final String kFName = "analyzeForm";
		final String kExTag = kClassName + '.' + kFName + ": ";
		boolean trace = shouldDoTraceMsg( kFName );
		boolean debug = shouldDoDebugMsg( kFName );

		if( null==mFormElem )
			throw new FormInfoException( kExTag + "Null form root element." );

		String method = JDOMHelper.getStringFromAttributeTrimOrNull( getFormElem(), "method" );
		if( null!=method )
			setMethod( method );
		String action = JDOMHelper.getStringFromAttributeTrimOrNull( getFormElem(), "action" );
		if( null!=action )
			setDeclaredActionURL( action, null );


		try {
	
			XPath xpath2 = XPath.newInstance( ".//input | .//select" );
		
			// List results = xpath.selectNodes( inDoc );
			List widgets = xpath2.selectNodes( getFormElem() );
		
			debugMsg( kFName,
				"Looking at form widgets: " + widgets.size()
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
				String wigValue = widget.getAttributeValue( "value" );
				if( wigType.equals("textarea") )
					wigValue = widget.getTextNormalize();
		

				debugMsg( kFName,
					NIEUtil.NL + "Widget=" + wigType + " / " + wigName + " / \"" + wigValue + "\""
					);
		


				if( wigType.startsWith("text") ) {
					registerTextField( wigName, wigValue );
				}
				if( wigType.equals("hidden") ) {
					registerHiddenField( wigName, wigValue );
				}
				if( wigType.equals("submit") ) {
					registerButton( wigName, wigValue );
				}
				else if( wigType.startsWith("radio") || wigType.startsWith("check") ) {
					registerOptionValue( wigName, wigValue, null );
				}
				else if( wigType.equals("select") ) {
					XPath xpath3 = XPath.newInstance( ".//option" );
					// List results = xpath.selectNodes( inDoc );
					List opts = xpath3.selectNodes( widget );
					for( Iterator it3 = opts.iterator() ; it3.hasNext() ; ) {
						Element option = (Element) it3.next();
						String optValue = option.getAttributeValue( "value" );
						String optDesc = option.getTextNormalize();
						registerOptionValue( wigName, optValue, optDesc );
					}
				}
	
			}	// end for each form Widget

		}
		catch( Exception e ) {
			throw new FormInfoException( kFName + "Caught exception: " + e );
		}

	}

	public Element generateSearchEngineConfigTree() {
		final String kFName = "generateSearchEngineConfigTree";

		Element outElem = new Element( nie.sn.SearchEngineConfig.MAIN_ELEM_NAME );

		// The search URL and method
		// "search_url"
		Element searchURLElem = JDOMHelper.findOrCreateElementByPath(
			outElem, nie.sn.SearchEngineConfig.SEARCH_ENGINE_URL_PATH, true
			);
		if( null==searchURLElem ) {
			errorMsg( kFName, "Error creating element (1), returning null." );
			return null;
		}
		String searchURL = getAbsoluteActionURL();
		if( null!=searchURL ) {
			JDOMHelper.updateSimpleTextToExistingOrNewPath(
				searchURLElem, null, searchURL
				);
		}
		else {
			searchURLElem.addContent( new Comment(
				"WARNING: No search URL found for this form, check manually."
				));
		}
		String method = getMethod();
		if( null!=method )
			searchURLElem.setAttribute(
				nie.sn.SearchEngineConfig.SEARCH_ENGINE_METHOD_ATTR, method
				);


		String vendor = guessSearchVendor();
		if( null!=vendor ) {
			Element vendorElem = JDOMHelper.findOrCreateElementByPath(
				outElem,
				nie.sn.SearchEngineConfig.VENDOR_PATH,
				true
				);
			if( null==vendorElem ) {
				errorMsg( kFName, "Error creating element (1pre), returning null." );
				return null;
			}
			vendorElem.addContent( vendor );
		}

		// The Search Field
		Element searchFieldElem = JDOMHelper.findOrCreateElementByPath(
			outElem, nie.sn.SearchEngineConfig.QUERY_FIELD_PATH, true
			);
		if( null==searchFieldElem ) {
			errorMsg( kFName, "Error creating element (1a), returning null." );
			return null;
		}
		// Get and add the text field
		String textFieldName = getPrimaryTextFieldName();
		if( null==textFieldName )
			textFieldName = "";
		searchFieldElem.addContent( textFieldName );
		// Also warn about problems
		String searchFieldCommentText = null;
		List textFields = getAllTextFieldNames();
		if( textFieldName.trim().length() < 1 )
			searchFieldCommentText = "WARNING: could not determin QUERY FIELD";
		else if( null!=textFields && textFields.size() > 1 )
			searchFieldCommentText = "WARNING: Ambiguous QUERY FIELD"
				+ ", choosing first found, probably one of "
				+ textFields
				;
		if( null!=searchFieldCommentText ) {
			Comment searchFieldComment = new Comment( searchFieldCommentText );
			searchFieldElem.addContent( searchFieldComment );
		}

		// The hidden fields
		// The search form otions
		List hiddenNames = getAllHiddenFieldNames();
		if( null!=hiddenNames && ! hiddenNames.isEmpty() ) {
			Element mainHiddenFieldsElem = JDOMHelper.findOrCreateElementByPath(
				outElem,
				//nie.sn.SearchEngineConfig.SEARCH_ENGINE_URL_TEST_DRIVE_FIELDS_PATH,
				nie.sn.SearchEngineConfig.SEARCH_ENGINE_URL_TEST_DRIVE_FIELDS_ROOT_PATH,
				true
				);
			if( null==mainHiddenFieldsElem ) {
				errorMsg( kFName, "Error creating element (1b), returning null." );
				return null;
			}

			// For each hidden field
			for( Iterator hit=hiddenNames.iterator(); hit.hasNext() ; ) {
				String hiddenFieldName = (String) hit.next();

				// Get the values (usually only one)


				List hiddenValues = getMultivalueCGIField( hiddenFieldName );
				if( null==hiddenValues || hiddenValues.isEmpty() ) {
					mainHiddenFieldsElem.addContent( new Comment(
						"WARNING: No hidden field values found for this field \"" + hiddenFieldName + "\", check manually."
						));
				}
				// Else we do have some options
				else {

					// For each hidden value
					for( Iterator hvit = hiddenValues.iterator(); hvit.hasNext() ; ) {
						String value = (String) hvit.next();
						value = (null!=value) ? value : "";

						// create the <field> node
						Element currHiddenElem = JDOMHelper.findOrCreateElementByPath(
							mainHiddenFieldsElem,
							nie.sn.SearchEngineConfig.FIELD_ELEM + "[+]",
							true
							);
						if( null==currHiddenElem ) {
							errorMsg( kFName, "Error creating element (1c), returning null." );
							return null;
						}
						// Set the name and value
						currHiddenElem.setAttribute(
							nie.sn.SearchEngineConfig.NAME_ATTR, hiddenFieldName
							);
						currHiddenElem.setAttribute(
							nie.sn.SearchEngineConfig.VALUE_ATTR, value
							);



					}	// End for each hidden value
				}	// Else we do have some values

			}	// For each hidden field
		}


		// The search form otions
		List optionNames = getAllOptionFieldNames();
		if( null!=optionNames && ! optionNames.isEmpty() ) {
			Element mainOptionsElem = JDOMHelper.findOrCreateElementByPath(
				outElem, nie.sn.SearchEngineConfig.SEARCH_FORM_OPTIONS_ROOT_PATH, true
				);
			if( null==mainOptionsElem ) {
				errorMsg( kFName, "Error creating element (2), returning null." );
				return null;
			}

			// For each main option
			for( Iterator onit=optionNames.iterator(); onit.hasNext() ; ) {
				String optionName = (String)onit.next();

				// create the <field> node
				Element currOptionElem = JDOMHelper.findOrCreateElementByPath(
					mainOptionsElem,
					nie.sn.SearchEngineConfig.FIELD_ELEM + "[+]",
					true
					);
				if( null==currOptionElem ) {
					errorMsg( kFName, "Error creating element (3), returning null." );
					return null;
				}
				currOptionElem.setAttribute(
					nie.sn.SearchEngineConfig.NAME_ATTR, optionName
					);
				String fieldDesc = getScalarFieldDescription( optionName );
				if( null!=fieldDesc )
					currOptionElem.setAttribute(
						nie.sn.SearchEngineConfig.DESC_ATTR, fieldDesc
						);


				List values = getMultivalueCGIField( optionName );
				if( null==values || values.isEmpty() ) {
					currOptionElem.addContent( new Comment(
						"WARNING: No options found for this field, check manually."
						));
				}
				// Else we do have some options
				else {

					for( Iterator vit = values.iterator(); vit.hasNext() ; ) {
						String value = (String) vit.next();
						value = (null!=value) ? value : "";

						// create the <option> node
						Element currValueElem = JDOMHelper.findOrCreateElementByPath(
							currOptionElem,
							nie.sn.SearchEngineConfig.OPTION_ELEM + "[+]",
							true
							);
						if( null==currValueElem ) {
							errorMsg( kFName, "Error creating element (4), returning null." );
							return null;
						}
						// Element currValueElem = new Element( nie.sn.SearchEngineConfig.OPTION_ELEM );
						// currOptionElem.addContent( currValueElem );

						currValueElem.setAttribute(
							nie.sn.SearchEngineConfig.VALUE_ATTR, value
							);

						// And get the description
						String optionDesc = getScalarOptionDescription( optionName, value );
						optionDesc = NIEUtil.trimmedStringOrNull( optionDesc );
						if( null!=optionDesc )
							currValueElem.setAttribute(
								nie.sn.SearchEngineConfig.DESC_ATTR, optionDesc
								);

					}
				}
			}
		}

		return outElem;
	}





	private static void ___Lower_Level_Logic___() {}
	// public static Document fetchUrlAsDom( String inURL )
	public static Element fetchUrlAsDom( String inURL )
		throws FormInfoException
	{
		final String kFName = "fetchUrlAsDom";
		final String kExTag = kClassName + '.' + kFName + ": ";
	
		Document doc = null;
		Element root = null;
		try {
	
			// Fetch and Convert the content into an XHTML tree, including
			// the handling of mangled HTML
			doc = JDOMHelper.readHTML( inURL );
			root = doc.getRootElement();
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
		}
		catch( Exception e ) {
			throw new FormInfoException( kExTag + "Caught exception: " + e );
		}
	
	
		// return doc;
		return root;
	}
	public String guessSearchVendor()
	{
		// URL Rules, per Miles
		//	/search.pl	Perl script
		//	/search.cgi	Perl/Python/other script
		// /	/s97cgi	Verity
		// /	/s97vts	Verity
		// /	/search.atomz.com	Atomz
		// /	/freefind.com	FreeFind
		// /	/pl-web	PLS
		// /	/query.html	Ultraseek
		// /	/AT-search	Excite for Web Servers
		// /	/htsearch	//ht-dig
		// /	/mondosearch	MondoSoft
		// /	/psearch.cgi	Glimpse
		// /	/compass	Netscape Server (Verity subset)
		// /	/texis	Thunderstone/Webinator
		//
		// Search Field rules, per Mark
		// url with Google
		// queryText = Verity
		// q = Google
		// qt = Ultraseek?

		// Mystery vendor
		// q1
		// help text discusses NEAR operator and "equivalent to Sports Events"



		if( ! mUseCache ) {

			final String kFName = "guessSearchVendor";
			String searchURL = getAbsoluteActionURL();
			if( null!=searchURL )
				searchURL = searchURL.toLowerCase();
			String searchField = getPrimaryTextFieldName();
			if( null!=searchField )
				searchField = searchField.toLowerCase();
			// If no action nor query field, then never mind
			if( null==searchURL && null==searchField ) {
				infoMsg( kFName, "No action URL or query field to test, returning null." );
				return null;
			}

			// URLs
			if( null!=searchURL &&
					(searchURL.indexOf( "s97cgi" ) >= 0 || searchURL.indexOf( "s97vts" ) >= 0 )
				)
				cSearchVendor = "Verity Search97";	// test 1
			else if( null!=searchURL && searchURL.indexOf( "atomz" ) >= 0 )
				cSearchVendor = "Atomz";
			else if( null!=searchURL && searchURL.indexOf( "freefind" ) >= 0 )
				cSearchVendor = "FreeFind";
			else if( null!=searchURL && searchURL.indexOf( "mondosearch" ) >= 0 )
				cSearchVendor = "MondoSoft";
			else if( null!=searchURL && searchURL.indexOf( "/htsearch" ) >= 0 )
				cSearchVendor = "HTDig";
			else if( null!=searchURL && searchURL.indexOf( "pl-web" ) >= 0 )
				cSearchVendor = "PLS";
			else if( null!=searchURL && searchURL.indexOf( "/vsearch" ) >= 0 )
				cSearchVendor = "Verity K2";
			else if( null!=searchURL && searchURL.indexOf( "/compass" ) >= 0 )
				cSearchVendor = "Netscape (Verity)";
			else if( null!=searchURL && searchURL.indexOf( "/psearch.cgi" ) >= 0 )
				cSearchVendor = "Glimpse";
			else if( null!=searchURL && searchURL.indexOf( "texis" ) >= 0 )
				cSearchVendor = "Thunderstone/Webinator";
			else if( null!=searchURL && searchURL.indexOf( "/at-search" ) >= 0 )
				cSearchVendor = "Excite for Web Servers";
			else if( null!=searchURL && searchURL.indexOf( "google" ) >= 0 )
				cSearchVendor = "Google";
			else if( null!=searchURL && searchURL.indexOf( "/query.html" ) >= 0 )
				cSearchVendor = "Ultraseek";

			// Query field
			else if( null!=searchField && searchField.equals( "querytext" ) )
				cSearchVendor = "Verity Search97";	// test 2
			else if( null!=searchField && searchField.equals( "q" ) )
				cSearchVendor = "Google?";
			else if( null!=searchField && searchField.equals( "qt" ) )
				cSearchVendor = "Ultraseek";
			// A couple generic ones
			else if( null!=searchURL && searchURL.indexOf( ".pl" ) >= 0 )
				cSearchVendor = "generic Perl";
			else if( null!=searchURL && searchURL.indexOf( ".py" ) >= 0 )
				cSearchVendor = "generic Python";
			else if( null!=searchURL && searchURL.indexOf( ".asp" ) >= 0 )
				cSearchVendor = "generic .ASP";

		}
		return cSearchVendor;

	}


	private static void ___Tabulating_what_was_Found___() {}

	public void registerHiddenField( String inFieldName, String optFieldValue ) {
		final String kFName = "registerHiddentField";
		inFieldName = NIEUtil.trimmedStringOrNull( inFieldName );
		if( null==inFieldName ) {
			errorMsg( kFName, "Null/empty field name passed in." );
			return;
		}
		String normFieldName = isCGIFieldsCaseSensitive() ? inFieldName : inFieldName.toLowerCase();
		if( ! mHiddenFieldsNormSet.contains(normFieldName) ) {
			mHiddenFieldsNormSet.add( normFieldName );
			mHiddenFieldsList.add( inFieldName );
		}
		addCGIField( inFieldName, optFieldValue );
	}
	public List getAllHiddenFieldNames() {
		return mHiddenFieldsList;
	}

	////////////////////////////////////////////////////////////

	public void registerButton( String optFieldName, String optFieldValue ) {
		final String kFName = "registerButton";
		mButtonCounter++;
		optFieldName = NIEUtil.trimmedStringOrNull( optFieldName );
		if( null==optFieldName ) {
			optFieldName = "button_" + mButtonCounter;
		}
		String normFieldName = isCGIFieldsCaseSensitive() ? optFieldName : optFieldName.toLowerCase();
		if( ! mButtonFieldsNormSet.contains(normFieldName) ) {
			mButtonFieldsNormSet.add( normFieldName );
			mButtonFieldsList.add( optFieldName );
		}
		addCGIField( optFieldName, optFieldValue );
	}

	////////////////////////////////////////////////////////////
	
	public void registerTextField( String inFieldName, String optFieldValue ) {
		final String kFName = "registerTextField";
		inFieldName = NIEUtil.trimmedStringOrNull( inFieldName );
		if( null==inFieldName ) {
			errorMsg( kFName, "Null/empty field name passed in." );
			return;
		}
		if( null==mQueryField )
			mQueryField = inFieldName;
	
		String normFieldName = isCGIFieldsCaseSensitive() ? inFieldName : inFieldName.toLowerCase();
		if( ! mTextFieldsNormSet.contains(normFieldName) ) {
			mTextFieldsNormSet.add( normFieldName );
			mTextFieldsList.add( inFieldName );
		}
		addCGIField( inFieldName, optFieldValue );
	}

	public List getAllTextFieldNames() {
		return mTextFieldsList;
	}



	public void registerOptionValue(
			String inFieldName, String inFieldValue, String optDescription
	) {
		final String kFName = "registerOptionValue";
		inFieldName = NIEUtil.trimmedStringOrNull( inFieldName );
		if( null==inFieldName ) {
			errorMsg( kFName, "Null/empty option field name passed in." );
			return;
		}

		if( null==mMainOptionField )
			mMainOptionField = inFieldName;

		// We don't trim or mess with field values
		if( null==inFieldValue )
			inFieldValue = "";

		String normFieldName = isCGIFieldsCaseSensitive() ? inFieldName : inFieldName.toLowerCase();
		if( ! mOptionFieldsNormSet.contains(normFieldName) ) {
			mOptionFieldsNormSet.add( normFieldName );
			mOptionFieldsList.add( inFieldName );
			debugMsg( kFName, "optfield " + inFieldName );
		}

		addCGIField( inFieldName, inFieldValue );

		optDescription = NIEUtil.trimmedStringOrNull( optDescription );
		if( null!=optDescription ) {
			// addFieldDesc( normFieldName + '=' + inFieldValue, optDescription );
			addOptionDescription( normFieldName, inFieldValue, optDescription );
		}
	}

	public List getAllOptionFieldNames() {
		return mOptionFieldsList;
	}


	public String getPrimaryTextFieldName() {
		return mQueryField;
	}
	public String getPrimaryOptionFieldName() {
		return mMainOptionField;
	}


	public String getFullCGIEncodedURL()
	{
		final String kFName = "getFullCGIEncodedURL";

		// String outURLString = getDeclaredActionURL();
		String outURLString = getAbsoluteActionURL();
		if( null == outURLString )
		{
			errorMsg( kFName,
				"No basic URL to add CGI varaibles to. Returning null."
				);
			return null;
		}


		// Now get all the variables as an encoded string
		String lCGIBuffer = getCGIFieldsAsEncodedBuffer();
		// traceMsg( kFName, "CGI Buffer = \"" + lCGIBuffer + "\"" );

		// If we got anything back, add it on
		if( lCGIBuffer != null && lCGIBuffer.length() > 0 )
		{
			// Join the two strings together, usually with a "?"
			outURLString = NIEUtil.concatURLAndEncodedCGIBuffer(
				outURLString, lCGIBuffer
				);
		}

		// Return whatever we're left with
		return outURLString;

	}

	public void setDeclaredActionURL( String inNewURL, String optBaseURL )
	{
		final String kFName = "setDeclaredActionURL";
		inNewURL = NIEUtil.trimmedStringOrNull( inNewURL );
		if( null == inNewURL )
			errorMsg( kFName,
				"Null/empty base URL sent in, will set field to null."
				);
		// TODO: do something useful with base URL
		mActionURL = inNewURL;
	}

	public void setMethod( String inMethod )
	{
		final String kFName = "setMethod";
		inMethod = NIEUtil.trimmedUpperStringOrNull(inMethod);
		if( null == inMethod )
			errorMsg( kFName,
				"Null/empty base URL sent in, will set field to null."
				);
		mMethod = inMethod;
	}

	public String getDeclaredBaseUrlOrNull( boolean inWarnOnNullFormOrPageURL ) {
	
		// cSpecificPageBaseURL
		if( ! mUseCache ) {
	
			final String kFName = "getDeclaredBaseUrlOrNull";
			try {
				Element mainElem = getFormElem();
				if( null==mainElem ) {
					if( inWarnOnNullFormOrPageURL )
						errorMsg( kFName, "Null base page Element; returning null." );
					return null;
				}
	
				XPath myXpath = XPath.newInstance( "//base" );
			
				// List results = xpath.selectNodes( inDoc );
				List bases = myXpath.selectNodes( getFormElem() );
	
				if( null==bases || bases.isEmpty() ) {
					// no warning here
					return null;
				}
				if( bases.size() > 1 )
					warningMsg( kFName, "Multiple base url elements declared; returning first valid one." );
	
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
						);
	
			}
			catch( Exception e ) {
				errorMsg( kFName,
					"Cuaght exception looking for base tag(s) on page: " + e
					);
			}
		}	// End if not using cache
	
		return cSpecificPageBaseURL;
	}

	public String getPageUrlOrNull() {
		return mPageURL;
		// TODO: what about redirects?
	}

	public Element getPageElem() {
		return mPageTreeElem;
	}

	public String getDeclaredBaseUrlOrNull() {
		return getDeclaredBaseUrlOrNull( true );
	}

	public String getDeclaredActionURL()
	{
		return mActionURL;
	}

	public String getMethod()
	{
		return mMethod;
	}

	public String getAbsoluteActionURL() {
		final String kFName = "getAbsoluteActionURL";
	
		String outURL = getDeclaredActionURL();
		// If no action, then never mind
		if( null==outURL )
			return null;
			// TODO: should we return the main page URL?
	
		if( NIEUtil.isStringAURL( outURL ) )
			return outURL;

		// We know these are bogus
		if( outURL.toLowerCase().startsWith("javascript:") ) {
			errorMsg( kFName,
				"Invalid URL, containts javascript: prefix."
				+ " URL =\"" + outURL + "\""
				+ " Returning null."
				);
			return null;
		}
		if( outURL.toLowerCase().startsWith("mailto:") ) {
			errorMsg( kFName,
				"Invalid URL, containts mailto: prefix."
				+ " URL =\"" + outURL + "\""
				+ " Returning null."
				);
			return null;
		}
	
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

	private static void ___CGI_Field_Hashes___() {}
	////////////////////////////////////////////////////////////

	// Get the named field, force to scalar
	// Note that we do NOT normalize the output since it is
	// valid to have field= with no value, which in some applications
	// may be different then not having the value at all
	public String getScalarCGIField( String inFieldName )
	{
		final String kFName = "getScalarCGIField";

		inFieldName = NIEUtil.trimmedStringOrNull( inFieldName );
		if( inFieldName == null )
		{
			errorMsg( kFName,
				"Was passed in null/empty field name."
				+ " Returning null."
				);
			return null;
		}

		// Go ahead and return the results
		// return getScalarFromHash(
		//	fIoCGIFieldHash, inFieldName, ! isCGIFieldsCaseSensitive()
		//	);

		String outStr = getScalarFromHash(
			mCGIFieldHash, inFieldName, ! isCGIFieldsCaseSensitive(), true
			);

		return outStr;
	}

	public List getCGIFieldKeys()
	{
		List keys = new Vector();
		if( mCGIFieldHash != null )
		{
			Set tmpSet = mCGIFieldHash.keySet();
			keys.addAll( tmpSet );
		}
		return keys;
	}

	public String getScalarCGIFieldTrimOrNull( String inFieldName )
	{
		String tmpStr = getScalarCGIField( inFieldName );
		tmpStr = NIEUtil.trimmedStringOrNull( tmpStr );
		return tmpStr;
	}

	public int getIntFromCGIField( String inFieldName,
		int inDefaultValue
		)
	{
		String tmpStr = getScalarCGIFieldTrimOrNull( inFieldName );
		// Warnings will be generated, if needed, from underlying routines
		return NIEUtil.stringToIntOrDefaultValue(
			tmpStr, inDefaultValue
			);
	}


	public List getMultivalueCGIField( String inFieldName )
	{
		final String kFName = "getMultivalueCGIField";

		inFieldName = NIEUtil.trimmedStringOrNull( inFieldName );
		if( inFieldName == null )
		{
			errorMsg( kFName,
				"Was passed in null/empty field name."
				+ " Returning null."
				);
			return new Vector();
		}

		// Go ahead and return the results
		return getMultivalueFromHash(
			mCGIFieldHash, inFieldName, ! isCGIFieldsCaseSensitive(), true
			);
	}



	// Get the fields as color=red&size=large&name=john+doe
	public String getCGIFieldsAsEncodedBuffer()
	{

		final String kFName = "getCGIFieldsAsEncodedBuffer";

		if( mCGIFieldHash == null )
		{
			errorMsg( kFName,
				"No cgi fields to encode; null field hash."
				+ " Returning empty string."
				);
			return "";
		}

		// The output buffer
		StringBuffer outBuff = new StringBuffer();

		// Loop through the hash keys
		Set keys = mCGIFieldHash.keySet();
		debugMsg( kFName, "Will encode " + keys.size() + " keys." );
		for( Iterator it = keys.iterator(); it.hasNext() ; )
		{
			// Get the key
			String newKey = (String) it.next();

			// We need to encode the key into x-www-form-urlencoded format
			String encodedKey = URLEncoder.encode( newKey );

			// Now get the object
			Object obj = mCGIFieldHash.get( newKey );

			// If it's a simple string, just add it in
			if( obj instanceof String )
			{
				// Convert to String
				String strValue = (String)obj;
				// We need to encode the key into x-www-form-urlencoded format
				String encodedValue = URLEncoder.encode( strValue );

				// Add delimiter, if not first
				if( outBuff.length() > 0 )
					outBuff.append( '&' );

				// And add it
				// The header field name
				outBuff.append( encodedKey );
				// The separator
				outBuff.append( '=' );
				// The actual value
				outBuff.append( encodedValue );

			}
			// If it's a list, add the list's elements
			else if( obj instanceof List || obj instanceof Vector )
			{
				// Convert to String
				List listOfValues = (List)obj;

				// And loop through the list
				for( Iterator it2 = listOfValues.iterator(); it2.hasNext() ;)
				{
					// Grab the value
					String tmpValue = (String) it2.next();
					// We need to encode the value into x-www-form-urlencoded format
					String encodedValue = URLEncoder.encode( tmpValue );

					// Add delimiter, if not first
					if( outBuff.length() > 0 )
						outBuff.append( '&' );

					// And add it
					// The header field name
					outBuff.append( encodedKey );
					// The separator
					outBuff.append( '=' );
					// The actual value
					outBuff.append( encodedValue );

				}
			}
			else
			{
				// Else we don't know what to do
				errorMsg( kFName,
					"Unable to convert requested Hash object into buffer."
					+ " Requested hash key = \"" + newKey + "\""
					+ " Unhandled object class = \"" + obj.getClass().getName() + "\""
					+ " Ingoring this item."
					);
			}

		}   // End of for each key in hash

		// Done
		return new String( outBuff );

	}


	// Get the fields as hidden fields underneath a presumed form tag
	public boolean addCGIFieldsToFormElemAsHiddenFields( Element inFormElem )
	{

		final String kFName = "addCGIFieldsToFormElemAsHiddenFields";

		if( mCGIFieldHash == null ) {
			errorMsg( kFName,
				"No cgi fields to encode; null field hash."
				+ " Returning failure."
				);
			return false;
		}

		boolean success = true;

		// Loop through the hash keys
		Set keys = mCGIFieldHash.keySet();
		debugMsg( kFName, "Will add " + keys.size() + " keys." );
		for( Iterator it = keys.iterator(); it.hasNext() ; )
		{
			// Get the key
			String newKey = (String) it.next();
			// Now get the object
			Object obj = mCGIFieldHash.get( newKey );

			// Normalize and check after we use it as a hash key
			newKey = NIEUtil.trimmedStringOrNull( newKey );
			if( null==newKey ) {
				errorMsg( kFName, "Empty field name, skipping." );
				success = false;
				continue;
			}

			// If it's a simple string, just add it in
			if( obj instanceof String )
			{
				// Convert to String
				String strValue = (String)obj;
				// Note that we will add empty strings
				strValue = strValue.trim();

				Element field = new Element( "input" );
				field.setAttribute( "type", "hidden" );
				field.setAttribute( "name", newKey );
				field.setAttribute( "value", strValue );

				inFormElem.addContent( field );

			}
			// If it's a list, add the list's elements
			else if( obj instanceof List || obj instanceof Vector )
			{
				// Convert to String
				List listOfValues = (List)obj;

				// And loop through the list
				for( Iterator it2 = listOfValues.iterator(); it2.hasNext() ;)
				{
					// Grab the value
					String tmpValue = (String) it2.next();
					// Note that we will add empty strings
					tmpValue = tmpValue.trim();

					Element field = new Element( "input" );
					field.setAttribute( "type", "hidden" );
					field.setAttribute( "name", newKey );
					field.setAttribute( "value", tmpValue );

					inFormElem.addContent( field );

				}
			}
			else {
				// Else we don't know what to do
				errorMsg( kFName,
					"Unable to convert requested Hash object into hidden form fields."
					+ " Requested hash key = \"" + newKey + "\""
					+ " Unhandled object class = \"" + obj.getClass().getName() + "\""
					+ " Ingoring this item."
					);
				success = false;
			}

		}   // End of for each key in hash

		// Done
		return success;
	}


	public void addCGIField( String inKey, long inValue )
	{
		addCGIField( inKey, ""+inValue );
	}
	public void addCGIField( String inKey, int inValue )
	{
		addCGIField( inKey, ""+inValue );
	}
	public void addCGIField( String inKey, String inValue )
	{
		addFieldToMultivalueHash( mCGIFieldHash,
			inKey, inValue, ! isCGIFieldsCaseSensitive()
			);
	}
	public void addFieldDescription( String inFieldName, String inDescription )
	{
		final String kFName = "addFieldDescription";
		if( inFieldName == null ) {
			errorMsg( kFName,
				"Was passed in null/empty field name."
				+ " Will not store anything."
				);
			return;
		}
		addFieldToMultivalueHash( mFieldDescHash,
			inFieldName, inDescription, ! isCGIFieldsCaseSensitive()
			);
	}
	public void addOptionDescription( String inFieldName,  String inFieldValue, String inDescription )
	{
		final String kFName = "addOptionDescription";
		if( inFieldName == null ) {
			errorMsg( kFName,
				"Was passed in null/empty field name."
				+ " Will not store anything."
				);
			return;
		}
		inFieldValue = (null!=inFieldValue) ? inFieldValue : "";
		String key = inFieldName + '=' + inFieldValue;
		addFieldToMultivalueHash( mFieldValueDescHash,
			key, inDescription, ! isCGIFieldsCaseSensitive()
			);
	}

	public String getScalarFieldDescription( String inFieldName )
	{
		final String kFName = "getScalarFieldDescription";

		inFieldName = NIEUtil.trimmedStringOrNull( inFieldName );
		if( inFieldName == null ) {
			errorMsg( kFName,
				"Was passed in null/empty field name."
				+ " Returning null."
				);
			return null;
		}

		String outStr = getScalarFromHash(
			mFieldDescHash, inFieldName, ! isCGIFieldsCaseSensitive(), true
			);

		return outStr;
	}

	public String getScalarOptionDescription( String inFieldName, String inFieldValue )
	{
		final String kFName = "getScalarOptionDescription";

		inFieldName = NIEUtil.trimmedStringOrNull( inFieldName );
		if( inFieldName == null ) {
			errorMsg( kFName,
				"Was passed in null/empty field name."
				+ " Returning null."
				);
			return null;
		}
		inFieldValue = (null!=inFieldValue) ? inFieldValue : "";

		String key = inFieldName + '=' + inFieldValue;

		// Go ahead and return the results
		// return getScalarFromHash(
		//	fIoCGIFieldHash, inFieldName, ! isCGIFieldsCaseSensitive()
		//	);

		String outStr = getScalarFromHash(
			mFieldValueDescHash, key, ! isCGIFieldsCaseSensitive(), true
			);

		return outStr;
	}



	public void setOrOverwriteCGIField( String inKey, String inValue )
	{
		setOrOverwriteHashValue( mCGIFieldHash,
			inKey, inValue, ! isCGIFieldsCaseSensitive()
			);
	}
	public void clearCGIField( String inKey )
	{
		clearHashValue( mCGIFieldHash,
			inKey, ! isCGIFieldsCaseSensitive()
			);
	}
	public void clearCGIField(
		String inKey,
		boolean inWarnIfNotPresent
		)
	{
		clearHashValue( mCGIFieldHash,
			inKey, ! isCGIFieldsCaseSensitive(), inWarnIfNotPresent
			);
	}




	// Copy in the CGI fields from a second (donor) Aux Info object
	// By default, copy all fields
	// public void copyInCGIFields( AuxIOInfo inDonorInfo )
	public void copyInCGIFields( FormInfo inDonorInfo )
	{
		copyInCGIFields( inDonorInfo, null );
	}
	public void copyInCGIFields(
		// AuxIOInfo inDonorInfo, Collection inExcludeFields
		FormInfo inDonorInfo, Collection inExcludeFields
		)
	{
		final String kFName = "copyInCGIFields";
		if( inDonorInfo == null )
		{
			errorMsg( kFName,
				"Was passed in a null donor object."
				+ " Nothing to copy in."
				);
			return;
		}

		// Go ahead and call the static hash copy method, with
		// the appropriate member fields
		combineMultivalueHashes(
			mCGIFieldHash,
			inDonorInfo.mCGIFieldHash,
			inExcludeFields,
			! isCGIFieldsCaseSensitive()
			);

	}

	// Clear out all CGI fields
	public void deleteAllCGIFields()
	{
		mCGIFieldHash = new Hashtable();
	}

	public boolean isCGIFieldsCaseSensitive()
	{
		return fIoIsCGIFieldsCasen;
	}

	public void setCGIFieldsCaseSensitive( boolean inFlag )
	{
		fIoIsCGIFieldsCasen = inFlag;
	}


	private static void ___simple_Get_and_Set___() {}
	////////////////////////////////////////////////////////////

	public Element getFormElem()
		throws FormInfoException
	{
		final String kFName = "getFormElem";
		final String kExTag = kClassName + '.' + kFName + ": ";
		if( null==mFormElem )
			throw new FormInfoException( kExTag + "Null form element." );
		return mFormElem;
	}


	public String getFormTreeTextOrNull() {
		final String kFName = "getFormTreeTextOrNull";
		String outText = null;
		try {
			Element tmpElem = getFormElem();
			if( null!=tmpElem )
				outText = JDOMHelper.getTreeText( tmpElem, true );
		}
		catch( FormInfoException e ) {
			errorMsg( kFName, "Got exception, returning null.  Exception: " + e );
			outText = null;
		}
		return outText;
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
	////////////////////////////////////////////////////////////


	public String displayCGIFieldsIntoBuffer()
	{
		return displayHashIntoBuffer(
			mCGIFieldHash, "Parsed CGI Fields"
			);
	}

	void printFormSummary() {
		System.out.println( getFormSummary() );
	}
	void printVendor() {
		printVendor( null );
	}
	void printVendor( String optVendorFilter ) {
		String vendor = guessSearchVendor();
		if( null!=optVendorFilter ) {
			if( null==vendor )
				return;
			String tmpTarget = optVendorFilter.toLowerCase();
			String tmpVendor = vendor.toLowerCase();
			if( tmpVendor.indexOf(tmpTarget) < 0 )
				return;
		}
		System.out.println(
			getPageUrlOrNull() + '\t' + guessSearchVendor()
			);
	}
	String getFormSummary() {
		StringBuffer sum = new StringBuffer();
		sum.append( getMethod() );
		String dURL = getDeclaredActionURL();
		sum.append( ' ' ).append( dURL );
		String aURL = getAbsoluteActionURL();
		if( null!=dURL && null!=aURL && ! aURL.equalsIgnoreCase(dURL) )
			sum.append( " (" ).append( aURL ).append( ')' );
		sum.append( NIEUtil.NL ).append( '\t' );
		sum.append( " Vendor: ").append( guessSearchVendor() );
		sum.append( " Query: ").append( getPrimaryTextFieldName() );
		List textFields = getAllTextFieldNames();
		if( null==textFields || textFields.isEmpty() ) {
			sum.append( NIEUtil.NL );
			sum.append( "    WARNING: No text/query field found, check manually." );
		}
		else if( textFields.size() > 1 ) {
			sum.append( NIEUtil.NL );
			sum.append( "    WARNING: Multiple/ambiguous text/query field found, check manually." );
			sum.append( NIEUtil.NL );
			sum.append( "    Found " ).append( textFields );
		}

		// String optName = getPrimaryOptionFieldName();
		List optNames = getAllOptionFieldNames();
		if( null!=optNames && ! optNames.isEmpty() ) {
			sum.append( NIEUtil.NL );
			sum.append( "    Found ").append( optNames.size() ).append( " option field" );
			if( optNames.size() != 1 )
				sum.append( 's' );

			for( Iterator onit=optNames.iterator(); onit.hasNext() ; ) {
				String optName = (String)onit.next();
				String fieldDesc = getScalarFieldDescription( optName );

				List opts = getMultivalueCGIField( optName );
				sum.append( NIEUtil.NL );
				sum.append( "    Option field: \"").append( optName ).append( '"' );
				if( null!=fieldDesc )
					sum.append( " (\"").append( fieldDesc ).append( "\")" );
				// sum.append( "=").append( ""+opts );
				if( null==opts || opts.isEmpty() ) {
					sum.append( " is EMPTY?" );
					// sum.append( NIEUtil.NL );
				}
				else {
					sum.append( " is one of:");
					sum.append( NIEUtil.NL );
					for( Iterator it = opts.iterator(); it.hasNext() ; ) {
						String option = (String) it.next();
						sum.append( "        \"").append( option ).append( '"' );
						if( it.hasNext() )
							sum.append( NIEUtil.NL );
					}
				}
			}
		}

		return new String( sum );
	}


	private static void ___Low_level_Hash_routines___() {}
	////////////////////////////////////////////////////////////


	private static String getScalarFromHash(
		// Hashtable inHash, String inKey
		Hashtable inHash, String inKey, boolean inNormalizeCase, boolean inDoKillNbsp
		)
	{
		final String kFName = "getScalarFromHash";

		// lNormalizeCase = isCGIFieldsCaseSensitive();

		if( inHash == null )
		{
			errorMsg( kFName,
				"Was passed in a NULL hash table."
				+ " Returning null."
				);
			return null;
		}

		inKey = NIEUtil.trimmedStringOrNull( inKey );
		if( inKey == null )
		{
			errorMsg( kFName,
				"Was passed in null/empty field name."
				+ " Returning null."
				);
			return null;
		}

		// Normalize if asked to do so
		if( inNormalizeCase )
			inKey = inKey.toLowerCase();

		// Return null if we don't have it
		// No need for a warning, this is a normal event
		if( ! inHash.containsKey( inKey ) )
			return null;

		// Get the object and see what it is
		Object obj = inHash.get( inKey );

		// If it's a simple string, just return it
		if( obj instanceof String ) {
			String tmpStr = (String) obj;
			if( inDoKillNbsp )
				tmpStr = NIEUtil.replaceChars( tmpStr, NIEUtil.NBSP, K_REPL_CHAR );
			return tmpStr;
		}

		if( obj instanceof List || obj instanceof Vector )
		{
			List tmpList = (List)obj;
			StringBuffer strbuff = new StringBuffer();
			for( Iterator it = tmpList.iterator(); it.hasNext(); )
			{
				String nextStr = (String)it.next();
				if( inDoKillNbsp )
					nextStr = NIEUtil.replaceChars( nextStr, NIEUtil.NBSP, K_REPL_CHAR );

				// If it's the first to be added, just append it
				if( strbuff.length() < 1 )
				{
					strbuff.append( nextStr );
				}
				// It's not the first, so add delimiter and then string
				else
				{
					strbuff.append( DEFAULT_MULTI_VALUE_FIELD_DELIMITER );
					strbuff.append( nextStr );
				}
			}
			// Convert string to buffer and return
			return new String( strbuff );
		}

		// Else we don't know what to do
		errorMsg( kFName,
			"Unable to convert requested Hash object to scalar String."
			+ " Requested hash key = \"" + inKey + "\""
			+ " Unhandled object class = \"" + obj.getClass().getName() + "\""
			+ " Returning null."
			);
		return null;
	}

	// We will ALWAYS return a list, even if it's zero items long
	// Return values are NOT normalized
	private static List getMultivalueFromHash(
		Hashtable inHash, String inKey, boolean inNormalizeCase, boolean inDoKillNbsp
		)
	{
		final String kFName = "getMultivalueFromHash";

		if( inHash == null )
		{
			errorMsg( kFName,
				"Was passed in a NULL hash table."
				+ " Returning null."
				);
			return new Vector();
		}

		inKey = NIEUtil.trimmedStringOrNull( inKey );
		if( inKey == null )
		{
			errorMsg( kFName,
				"Was passed in null/empty field name."
				+ " Returning null."
				);
			return new Vector();
		}

		// Normalize if asked to do so
		if( inNormalizeCase )
			inKey = inKey.toLowerCase();

		// Return null if we don't have it
		// No need for a warning, this is a normal event
		if( ! inHash.containsKey( inKey ) )
			return new Vector();

		// Start working on the return value
		List outVect = new Vector();

		// Get the object and see what it is
		Object obj = inHash.get( inKey );

		// If it's a simple string, just add it in
		if( obj instanceof String )
		{
			String tmpStr = (String)obj;
			// Accented lower case letter a, hex=A0, dec=160, oct=240
			if( inDoKillNbsp )
				tmpStr = NIEUtil.replaceChars( tmpStr, NIEUtil.NBSP, K_REPL_CHAR );
				// ^^^ he does a quick escape if char is not there at all

			outVect.add( tmpStr );
		}
		// If it's a list, add the list's elements
		// Todo: could scan list and do error checking
		// Todo: could have option to normalize values, skip nulls, etc.
		else if( obj instanceof List || obj instanceof Vector )
		{
			List tmpList = (List)obj;
			if( ! inDoKillNbsp )
				outVect.addAll( tmpList );
			else {
				for( Iterator it = tmpList.iterator(); it.hasNext() ; ) {
					String tmpStr = (String) it.next();
					// Accented lower case letter a, hex=A0, dec=160, oct=240
					tmpStr = NIEUtil.replaceChars( tmpStr, NIEUtil.NBSP, K_REPL_CHAR );
					// ^^^ he does a quick escape if char is not there at all
					outVect.add( tmpStr );
				}
			}
		}
		else
		{
			// Else we don't know what to do
			errorMsg( kFName,
				"Unable to convert requested Hash object into multivalue list."
				+ " Requested hash key = \"" + inKey + "\""
				+ " Unhandled object class = \"" + obj.getClass().getName() + "\""
				+ " Returning null."
				);
		}
		// Return the answer, whatever it is
		return outVect;
	}




	// Add a field to a hash.  If there is already a value (or values),
	// make a list and add this to the end
	// This version adds in an additional scalar field value
	private static void setMultivalueFromHash(
		Hashtable inHash,
		String inKey, String inValue,
		boolean inNormalizeCase
		)
	{
		final String kFName = "getMultivalueFromHash";
		if( inHash == null )
		{
			errorMsg( kFName,
				"Was passed in a NULL hash table."
				+ " Nowhere to store values, will discard values."
				);
			return;
		}

		// Normalize inputs
		inKey = NIEUtil.trimmedStringOrNull( inKey );
		// Note that we don't normalize the value

		// Sanity check
		if( inKey == null || inValue == null )
		{
			errorMsg( kFName,
				"Was passed in null/empty field name or null value."
				+ ", key=\"" + inKey + "\""
				+ ", value=\"" + inValue + "\""
				+ " Nothing to store."
				);
			return;
		}

		// Normalize if asked to do so
		if( inNormalizeCase )
			inKey = inKey.toLowerCase();

		// If we don't have it, which is usually the case, just add it
		if( ! inHash.containsKey( inKey ) )
		{
			inHash.put( inKey, inValue );
			return;
		}

		// The list we will create / update and restore into the hash
		List lVect = null;

		// Get the object and see what it is
		Object obj = inHash.get( inKey );

		// If it's a simple string, just add it in
		if( obj instanceof String )
		{
			lVect = new Vector();
			// Add the old value
			lVect.add( (String)obj );
			// And then add the new value
			lVect.add( inValue );
			// Store the new vector
			inHash.put( inKey, lVect );
		}
		// If it's a list, add the list's elements
		// Todo: could scan list and do error checking
		// Todo: could have option to normalize values, skip nulls, etc.
		else if( obj instanceof List || obj instanceof Vector )
		{
			lVect = (List)obj;
			lVect.add( inValue );
			// No need to store in hash, it's already there, we were
			// just using a reference
		}
		else
		{
			// Else we don't know what to do
			errorMsg( kFName,
				"Unable to convert requested Hash object into multivalue list."
				+ " Requested hash key = \"" + inKey + "\""
				+ ", value = \"" + inValue + "\""
				+ " Unhandled object class = \"" + obj.getClass().getName() + "\""
				+ " Returning null."
				);
			// And we leave the obj that we found "as is"
		}

		// Done
	}


	private static void addFieldToMultivalueHash(
		Hashtable inHash,
		String inKey, String inValue,
		boolean inNormalizeCase
		)
	{
		final String kFName = "addFieldToMultivalueHash";

		if( inKey == null /*|| inValue == null*/ )
		{
			errorMsg( kFName, "Null input(s):"
				+ "inHash=" + inHash + ", inKey=" + inKey
				// + "inValue=" + inValue + "."
				+ " Can not add to list with null values."
				);
			return;
		}
		// We DO allow empty strings
		inValue = (null!=inValue) ? inValue : "";

		List tmpList = new Vector();
		tmpList.add( inValue );

		// the main method with a one element list
		addFieldsToMultivalueHash( inHash, inKey, tmpList, inNormalizeCase );

	}


	// Add a field to a hash.  If there is already a value (or values),
	// make a list and add this to the end
	// This version adds in a list of additional field values
	private static void addFieldsToMultivalueHash(
		Hashtable inHash,
		String inKey, List inValues,
		boolean inNormalizeCase
		)
	{
		final String kFName = "addFieldToMultivalueHash(2)";
		if( inHash == null || inValues == null )
		{
			errorMsg( kFName,
				"Was passed in a NULL hash table."
				+ " Nowhere to store values, will discard values."
				);
			return;
		}

		// Normalize inputs
		inKey = NIEUtil.trimmedStringOrNull( inKey );

		// Sanity check
		if( inKey == null )
		{
			errorMsg( kFName,
				"Was passed in null/empty field name."
				+ ", key=\"" + inKey + "\""
				+ " Nothing to store."
				);
			return;
		}

		// Normalize if asked to do so
		if( inNormalizeCase )
			inKey = inKey.toLowerCase();

		// If we don't have it, which is usually the case, just add it
		if( ! inHash.containsKey( inKey ) )
		{
			inHash.put( inKey, inValues );
			return;
		}

		// The list we will create / update and restore into the hash
		List lVect = null;

		// Get the object and see what it is
		Object obj = inHash.get( inKey );

		// If it's a simple string, just add it in
		if( obj instanceof String )
		{
			lVect = new Vector();
			// Add the old value
			lVect.add( (String)obj );
			// And then add the new value
			lVect.addAll( inValues );
			// Store the new vector
			inHash.put( inKey, lVect );
		}
		// If it's a list, add the list's elements
		// Todo: could scan list and do error checking
		// Todo: could have option to normalize values, skip nulls, etc.
		else if( obj instanceof List || obj instanceof Vector )
		{
			lVect = (List)obj;
			lVect.addAll( inValues );
			// No need to store in hash, it's already there, we were
			// just using a reference
		}
		else
		{
			// Else we don't know what to do
			errorMsg( kFName,
				"Unable to convert requested Hash object into multivalue list."
				+ " Requested hash key = \"" + inKey + "\""
				+ " Unhandled object class = \"" + obj.getClass().getName() + "\""
				+ " Returning null."
				);
			// And we leave the obj that we found "as is"
		}

		// Done
	}






	// Join the contents of a second multivalue hash to the original
	// Todo: could be slightly more efficient
	private static void combineMultivalueHashes(
		Hashtable inCombinedHash,
		Hashtable inAdditionalValuesHash,
		Collection inExcludeKeys,
		boolean inNormalizeCase
		)
	{
		final String kFName = "combineMultivalueHashes";

		if( inCombinedHash == null || inAdditionalValuesHash == null )
		{
			errorMsg( kFName,
				"Was passed in a NULL hash table."
				+ " Nothing to do."
				);
			return;
		}

		if( shouldDoDebugMsg(kFName) )
		{
			debugMsg( kFName,
				"Asked to combine two hashes."
				+ " Hash 1 (destination) already has "
					+ inCombinedHash.keySet().size() + " keys."
				+ " Hash 2 (donar) has "
					+ inAdditionalValuesHash.keySet().size() + " keys."
				);
			if( inExcludeKeys != null )
				debugMsg( kFName,
					"Exclude list has "
					+ inExcludeKeys.size() + " keys."
					);
			else
				debugMsg( kFName, "No exclude list." );
		}


		// The hash we will use for quick lookup of excluded values
		Hashtable lExcludeHash = new Hashtable();
		// Were there any keys to exclude?
		if( inExcludeKeys != null )
		{
			// For each key
			for( Iterator it = inExcludeKeys.iterator(); it.hasNext() ; )
			{
				String tmpKey = (String) it.next();
				tmpKey = NIEUtil.trimmedStringOrNull( tmpKey );
				if( tmpKey == null )
					continue;
					// Todo: could add warning
				// Normalize if needed
				if( inNormalizeCase )
					tmpKey = tmpKey.toLowerCase();
				// Only store if not in the hash
				if( ! lExcludeHash.containsKey( tmpKey ) )
				{
					lExcludeHash.put( tmpKey, tmpKey );
				}
				// else it already had this key
				//  Todo: I suppose we could have an option to warn them
			}
		}   // End if we have some keys to exclude


		// Loop through the second hash
		Set newKeys = inAdditionalValuesHash.keySet();
		for( Iterator it2 = newKeys.iterator(); it2.hasNext() ; )
		{
			String newKey = (String) it2.next();
			newKey = NIEUtil.trimmedStringOrNull( newKey );
			if( newKey == null )
				continue;
				// Todo: could issue a warning
			// Normalize if asked to do so
			String lCheckKey = newKey;
			if( inNormalizeCase )
				lCheckKey = lCheckKey.toLowerCase();
			// If it's in the exclude hash, ignore it
			if( lExcludeHash.containsKey( lCheckKey ) )
				continue;
				// No warning necessary, this is an expected condition

			// Now get the object
			Object obj = inAdditionalValuesHash.get( newKey );

			// If it's a simple string, just add it in
			if( obj instanceof String )
			{
				// Convert to String
				String strValue = (String)obj;
				// Call static method to add it in
				addFieldToMultivalueHash(
					inCombinedHash,
					newKey, strValue,
					inNormalizeCase
					);
			}
			// If it's a list, add the list's elements
			else if( obj instanceof List || obj instanceof Vector )
			{
				// Convert reference to List
				List listOfValues = (List)obj;
				// Call static method to add it in
				addFieldsToMultivalueHash(
					inCombinedHash,
					newKey, listOfValues,
					inNormalizeCase
					);
			}
			else
			{
				// Else we don't know what to do
				errorMsg( kFName,
					"Unable to convert requested Hash object into multivalue list."
					+ " Requested hash key = \"" + newKey + "\""
					+ " Unhandled object class = \"" + obj.getClass().getName() + "\""
					+ " Returning null."
					);
			}


		}

		// Done

		if( shouldDoDebugMsg( kFName ) )
		{
			debugMsg( kFName,
				"At end."
				+ " Hash 1 (destination) now has "
				+ inCombinedHash.keySet().size() + " keys."
				);
		}

	}





	private void setOrOverwriteHashValue( Hashtable inHash,
		String inKey, String inValue,
		boolean inNormalizeCase
		)
	{
		final String kFName = "setOrOverwriteHashValue";
		if( inHash == null )
		{
			errorMsg( kFName,
				"Was passed in a NULL hash table."
				+ " Nowhere to store values, will discard values."
				);
			return;
		}

		// Normalize inputs
		inKey = NIEUtil.trimmedStringOrNull( inKey );
		// Note that we don't normalize the value

		// Sanity check
		if( inKey == null || inValue == null )
		{
			errorMsg( kFName,
				"Was passed in null/empty field name or null value."
				+ ", key=\"" + inKey + "\""
				+ ", value=\"" + inValue + "\""
				+ " Nothing to store."
				);
			return;
		}

		// Normalize if asked to do so
		if( inNormalizeCase )
			inKey = inKey.toLowerCase();

		// Now the easy part, just store the value!
		// Hashes generally have the correct behavior we want
		// Still nice to have a method for it, for consistency
		inHash.put( inKey, inValue );
	}


	private void clearHashValue( Hashtable inHash,
		String inKey,
		boolean inNormalizeCase
		)
	{
		clearHashValue( inHash, inKey, inNormalizeCase, true );
	}

	private void clearHashValue( Hashtable inHash,
		String inKey,
		boolean inNormalizeCase,
		boolean inWarnIfNotPresent
		)
	{
		final String kFName = "clearHashValue";
		if( inHash == null )
		{
			errorMsg( kFName,
				"Was passed in a NULL hash table."
				+ " Nowhere to store values, will discard values."
				);
			return;
		}

		// Normalize inputs
		inKey = NIEUtil.trimmedStringOrNull( inKey );
		// Note that we don't normalize the value

		// Sanity check
		if( inKey == null )
		{
			errorMsg( kFName,
				"Was passed in null/empty field name."
				+ ", key=\"" + inKey + "\""
				+ " Nothing to delete."
				);
			return;
		}

		// Normalize if asked to do so
		if( inNormalizeCase )
			inKey = inKey.toLowerCase();

		// Hashes generally have the correct behavior we want
		// Still nice to have a method for it, for consistency
		if( inHash.containsKey( inKey ) )
			inHash.remove( inKey );
		else if( inWarnIfNotPresent )
			errorMsg( kFName,
				"Key not found in hash"
				+ ", key=\"" + inKey + "\""
				+ " Nothing to delete."
				);
	}




	// list the entire hash into a text buffer
	private String displayHashIntoBuffer( Hashtable inHash, String optHashName )
	{
		final String kFName = "displayHashIntoBuffer";
		final String nl = "\r\n";

		if( inHash == null )
		{
			String msg =
				"Error: AuxIOInfo: displayHashIntoBuffer:"
				+ " No hash to display."
				+ " Returning this error message."
				;
			errorMsg( kFName, msg );
			return msg + nl;
		}

		// The output buffer
		StringBuffer outBuff = new StringBuffer();

		if( optHashName != null )
			outBuff.append( "Displaying hash \"" + optHashName + "\"" + nl );

		// Loop through the hash keys
		Set keys = inHash.keySet();
		for( Iterator it = keys.iterator(); it.hasNext() ; )
		{
			// Get the key
			String newKey = (String) it.next();

			// Now get the object
			Object obj = inHash.get( newKey );

			// If it's a simple string, just add it in
			if( obj instanceof String )
			{
				// Convert to String
				String strValue = (String)obj;

				outBuff.append( "\tkey \"" + newKey + "\"");
				outBuff.append( " (scalar value) \"" + strValue + "\"" + nl );

			}
			// If it's a list, add the list's elements
			else if( obj instanceof List || obj instanceof Vector )
			{
				// Convert to String
				List listOfValues = (List)obj;

				outBuff.append( "\tkey \"" + newKey + "\"" );
				outBuff.append( " multivalue with " + listOfValues.size()
					+ " entries:" + nl
					);

				// And loop through the list
				for( Iterator it2 = listOfValues.iterator(); it2.hasNext() ;)
				{
					// Grab the value
					String tmpValue = (String) it2.next();
					outBuff.append( "\t\t\"" + tmpValue + "\"" + nl );
				}
			}
			else
			{
				// Else we don't know what to do
				outBuff.append(
					"Error: AuxIOInfo: getCGIFieldsAsEncodedBuffer:"
					+ " Unable to convert requested Hash object into buffer."
					+ " Requested hash key = \"" + newKey + "\""
					+ " Unhandled object class = \"" + obj.getClass().getName() + "\""
					+ " Ingoring this item."
					+ nl
					);
			}

		}   // End of for each key in hash

		// Done
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

	Element mFormElem;
	String mFormText;

	boolean mUseCache;

	int mButtonCounter = 0;
	Hashtable mCGIFieldHash;
	Hashtable mFieldValueDescHash;
	Hashtable mFieldDescHash;

	List mTextFieldsList;
	HashSet mTextFieldsNormSet;
	List mOptionFieldsList;
	HashSet mOptionFieldsNormSet;
	List mButtonFieldsList;
	HashSet mButtonFieldsNormSet;
	List mHiddenFieldsList;
	HashSet mHiddenFieldsNormSet;

	String mActionURL;
	String mMethod;
	Element _mFormTree;
	String mQueryField;
	String mMainOptionField;

	String mPageURL;
	String cSpecificPageBaseURL;
	int mThisFormCountOnPage;
	String cSearchVendor;
	Element mPageTreeElem;
	String mPathToForm;

	public static final char DEFAULT_MULTI_VALUE_FIELD_DELIMITER = '\t';
	public static final boolean DEFAULT_HASH_KEY_NORMALIZE_CASE = true;

	// Whether or not CGI fields are case sensitive
	private boolean fIoIsCGIFieldsCasen = DEFAULT_CGI_FIELDS_CASEN;
	private static final boolean DEFAULT_CGI_FIELDS_CASEN = false;

	public static final char K_REPL_CHAR = ' ';

}
