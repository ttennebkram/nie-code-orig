package nie.sn;

import nie.core.*;
import java.util.*;
// import java.sql.*;

import org.jdom.Element;
import org.jdom.Document;
import org.jdom.transform.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;



/****************************************************************************************
	This class declares the association between
	the little fragments of
	XML data that "users" (developers, administrators, etc)
	can attach to mapped terms
	and java classes

	There is one instance per DATA TYPE, not per URL (see UserDataItem for the per URL)
****************************************************************************************/


public class BaseMarkup
{

	private static final String kClassName = "BaseMarkup";

	// The configuration for a user defined markup item or Ad
	// This instance is stored and used every time this class of
	// ad is encountered and rendered
	// This is only called once, no matter how many ads they have
	public BaseMarkup( Element inElement,
		SearchTuningConfig inConfig
		)
			throws MarkupException
	{

		final String kFName = "constructor";
		final String kExTag = kClassName + '.' + kFName + ": ";

		debugMsg( kFName, "Start." );

		// Start with some Sanity checking

		// For performance, we want to cache some answers
		// But we will probably allow updates in the future, so it's
		// important to respect caching rules
		// fAccessLockObject = new Object();
		// fDirtyCache = true;

		// Sanity checks
		if( inElement == null )
		{
			String msg = "Constructor was passed in a NULL element.";
			errorMsg( kFName, msg );
			throw new MarkupException( kExTag + msg );
		}
		if( null == inConfig )
		{
			String msg = "Constructor was passed in a NULL app config.";
			errorMsg( kFName, msg );
			throw new MarkupException( kExTag + msg );
		}
		/***
		if( null == inApp )
		{
			String msg = "Constructor was passed in a NULL application.";
			errorMsg( kFName, msg );
			throw new MarkupException( kExTag + msg );
		}
		***/
		
		// We will need to refer back to the SN configuration
		fInitConfig = inConfig;

		// Instantiate and store the main JDOMHelper
		fConfigTree = null;
		try
		{
			fConfigTree = new JDOMHelper( inElement );
		}
		catch (JDOMHelperException e)
		{
			String msg = "Error processing XML settings: " + e;
			errorMsg( kFName, msg );
			throw new MarkupException( kExTag + msg );
		}
		if( fConfigTree == null )
		{
			String msg = "Got back a NULL xml tree"
				+ " when trying to create a redirect record"
				;
			errorMsg( kFName, msg );
			throw new MarkupException( kExTag + msg );
		}

		debugMsg( kFName,
			"Finished constructing JDOM, start caching values ..."
			);

		// Prepopulate cache
		mIsFreshCache = false;
		// In no cache mode, this will do the actual lookup and cache the values
		getUserAliasClass();
		getUserAliasSubClass();
		getRequestedJavaClass();
		getMarkerLiteralText();
		getIsMarkerNewTextInsertAfter();
		getIsMarkerReplaced();
		getIsMarkerCaseSensitive();

		getShortDesc();
		if( null==getUIScreenName() )
			if( getSearchTuningConfig().getHasDbBasedMaps() )
				errorMsg( kFName, "No UI Screen associated with this class - will not be editable from UI." );


		// Get and compile the XSLT formatter
		getXSLTLiteralText();
		getXSLTFileName();
		debugMsg( kFName, "Compiling XSLT ..." );
		getCompiledXSLTDoc();

		// Turn on cache
		mIsFreshCache = true;

		// Some sanity checking
		additionalSanityChecking();

		debugMsg( kFName, "Done." );

	}



	void additionalSanityChecking()
		throws MarkupException
	{
		final String kFName = "additionalSanityChecking";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// Check that we have a text pattern to look for
		List tmpPats = getMarkerLiteralText();
		if( tmpPats == null || tmpPats.size() < 1 )
		{
			String msg = "Empty or NULL marker text specified."
				+ " Would have no way to place items in your results."
				+ " Reminders:"
				+ " The attribute is \"" + MARKER_TEXT_PATH + "\""
				+ " And if your marker text contains HTML TAGS you need"
				+ " to either escape them or surround the text with a"
				+ " <![CDATA[ ... ]]> tag set."
				;
			errorMsg( kFName, msg );
			throw new MarkupException( kExTag + msg );
		}
	}






	// This is the actual routine that does the work
	// Given an input document and an XML snippet:
	// - Get the XSLT formatting rules
	// - Format the XML snippet to HTML
	// - Markup the document with the result
	// We also pass in the request and response objects, which
	// might be needed, but should be treated as READ ONLY please!
	public String doMarkup(
		String inDoc, Element itemsRoot,
		AuxIOInfo inRequest, AuxIOInfo inResponse
		)
			throws MarkupException
	{
		final String kFName = "doMarkup";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// Sanity checks
		if( inDoc == null )
		{
			String msg = "Null input document.";
			throw new MarkupException( kExTag + msg );
			// return inDoc;
		}
		if( itemsRoot == null )
		{
			String msg = "Null input XML Data to markup.";
			throw new MarkupException( kExTag + msg );
			// return inDoc;
		}

		// Initiate our answer to the orginal doc, just in case
		String outDoc = inDoc;

		infoMsg( kFName,
			"Processing user XML data snippet."
			+ " Original document is " + inDoc.length() + " chars."
			);

		// Do all the work
		try
		{
			debugMsg( kFName, "Getting formatter ..." );	


			// Get compiled and cached XSLT
			Transformer formatter = getCompiledXSLTDoc();
			if( formatter == null )
			{
				throw new MarkupException( kExTag
					+ "Could not obtain XSLT formatting rules"
					// + ", returning original document."
					);
			}

			debugMsg( kFName, "Formatting XML ..." );	
	
			// Now Transform it!
			// We have no parameters hash, and since we're already
			// working with CLONED data, it's OK to not have them reclone
			Document newJDoc = JDOMHelper.xsltElementToDoc(
				itemsRoot,
				formatter,
				null, false
				);
			// throws JDOMHelperException?
			
			if( newJDoc == null )
				throw new Exception( kExTag
					+ "Got back null document from XSLT formatter."
					);	

			debugMsg( kFName,
				"Converting formatted XML into text"
				+ " with newJDoc = " + newJDoc
				);
	
			// Convert to a string, use pretty formatting
			// This does not add the xml doc header junk, which we
			// don't want anyway
			String snippet = JDOMHelper.JDOMToString( newJDoc, true );

			if( snippet == null )
				throw new Exception( kExTag
					+ "Got back null text from xml-to-string conversion."
					);	

			debugMsg( kFName,
				"Adding new text to main document."
				+ " New text is " + snippet.length() + " chars."
				);
			
			// markup the document
			String tmpSDoc = NIEUtil.markupStringWtihString(
				inDoc,
				snippet,
				getMarkerLiteralText(),
				getIsMarkerNewTextInsertAfter(),
				getIsMarkerReplaced(),
				getIsMarkerCaseSensitive()
				);
			if( tmpSDoc != null )
				outDoc = tmpSDoc;
			else
				throw new MarkupException( kExTag
					+ "Error while adding formatted XML data"
					+ " to the original document."
					+ " Returning unmodified source document."
					+ " Error: null returned from NIEUtil.markupStringWtihString" 
					);
		}
		catch( Exception e )
		{
			String msg = "Error adding user data to document. Error: " + e;
			// errorMsg( kFName, kExTag );
			throw new MarkupException( kExTag + msg );
			// Set it back, just to be sure
			// outDoc = inDoc;
		}

		debugMsg( kFName,
			"Returning new doc with " + outDoc.length() + " chars."
			);

		// Return our answer
		return outDoc;
	}



	private String getUserAliasClass()
	{
		if( ! mIsFreshCache )
		{
			cUserAlias = getUserAliasClass(
				fConfigTree.getJdomElement()
				);
		}
		return cUserAlias;
	}
	// The static version that we can access before instantiation
	public static String getUserAliasClass( Element inElem )
	{
		String tmpStr = JDOMHelper.getStringFromAttributeTrimOrNull(
			inElem, USER_ALIAS_CLASS_ATTR
			);
		if( tmpStr != null )
			tmpStr = tmpStr.toLowerCase();
		return tmpStr;
	}

	private String getUserAliasSubClass()
	{
		if( ! mIsFreshCache )
		{
			cUserAliasSubClass = getUserAliasSubClass(
				fConfigTree.getJdomElement()
				);
		}
		return cUserAliasSubClass;
	}
	public static String getUserAliasSubClass( Element inElem )
	{
		return JDOMHelper.getStringFromAttributeTrimOrNull(
			inElem, USER_ALIAS_SUB_CLASS_ATTR
			);
	}

	private String getRequestedJavaClass()
	{
		if( ! mIsFreshCache )
		{
			cRequestedJavaClass = getRequestedJavaClass(
				fConfigTree.getJdomElement()
				);
		}
		return cRequestedJavaClass;
	}
	public static String getRequestedJavaClass( Element inElem )
	{
		// Look it up in the element
		String tmpStr = JDOMHelper.getStringFromAttributeTrimOrNull(
			inElem, USER_JAVA_CLASS_ATTR
			);
		// If it's null, use the default
		if( tmpStr == null )
			tmpStr = DEFAULT_MARKUP_CLASS;
		// We always need a fully qualified class for "instance of"
		if( tmpStr.indexOf('.') < 0 )
			tmpStr = MARKUP_CLASS_PREFIX + tmpStr;
		// We're done
		return tmpStr;
	}

	public String getShortDesc()
	{
		if( ! mIsFreshCache ) {
			// Look it up in the element
			cShortDesc = fConfigTree.getStringFromAttributeTrimOrNull(
				SHORT_DESC_ATTR
				);
			// If it's null, use the default
			if( null == cShortDesc )
				cShortDesc = getUserAliasClass();
		}
		// We're done
		return cShortDesc;
	}
	public String getUIScreenName()
	{
		if( ! mIsFreshCache ) {
			// Look it up in the element
			cUIScreen = fConfigTree.getStringFromAttributeTrimOrNull(
				UI_SCREEN_ATTR
				);
		}
		// We're done
		return cUIScreen;
	}


	public String getXSLTLiteralText()
	{
		if( ! mIsFreshCache )
		{
			final String kFName = "getXSLTLiteralText";
			cXsltLiteralText = fConfigTree.getTextByPathTrimOrNull(
				XSLT_PATH
				);
			if( cXsltLiteralText != null )
				infoMsg( kFName,
					"Read inline XSLT from config file."
					);

		}
		return cXsltLiteralText;
	}
	
	


	public String getXSLTFileName()
	{
		if( ! mIsFreshCache )
		{
			cXsltFileName = fConfigTree.getTextFromSinglePathAttrTrimOrNull(
				XSLT_PATH,
				XSLT_LOCATION_ATTR
				);
		}
		return cXsltFileName;
	}



	// Gives a list, but we only use the first element right now
	public List getMarkerLiteralText()
	{
		// The string to look for when we want to insert a webmaster suggests
		// Todo: would be nice to support more than one of these
		// Todo: would be nice to support literal or regex
		// Todo: would be nice to say before or after
		// Todo: would be nice to say "keep this text" or dump it after subst
		if( ! mIsFreshCache )
		{
			cMarkerList = fConfigTree.getTextListByPathNotNullTrim(
				MARKER_TEXT_PATH
				);
		}
		return cMarkerList;
	}


	public boolean getIsMarkerNewTextInsertAfter()
	{
		if( ! mIsFreshCache )
		{
			cIsInsertAfter = fConfigTree.getBooleanFromSinglePathAttr(
				MARKER_TEXT_PATH,
				MARKER_TEXT_AFTER_ATTR,
				DEFAULT_SUGGESTION_MARKER_NEW_TEXT_AFTER
				);
		}
		return cIsInsertAfter;
	}

	public boolean getIsMarkerReplaced()
	{
		if( ! mIsFreshCache )
		{
			cIsMarkerReplaced = fConfigTree.getBooleanFromSinglePathAttr(
				MARKER_TEXT_PATH,
				MARKER_TEXT_REPLACE_ATTR,
				DEFAULT_SUGGESTION_MARKER_IS_REPLACED
				);
		}
		return cIsMarkerReplaced;
	}

	public boolean getIsMarkerCaseSensitive()
	{
		if( ! mIsFreshCache )
		{
			cIsMarkerCasen = fConfigTree.getBooleanFromSinglePathAttr(
				MARKER_TEXT_PATH,
				MARKER_TEXT_CASEN_ATTR,
				DEFAULT_SUGGESTION_MARKER_IS_CASEN
				);
		}
		return cIsMarkerCasen;
	}


	// Can NOT use getApp's version because at first, during const,
	// app hasn't got it assigned back yet, race condition
	SearchTuningConfig getSearchTuningConfig()
	{
		return fInitConfig;

//		if( null != getMainApplication() )
//			return getMainApplication().getSearchTuningConfig();
//		else
//		{
//			errorMsg( "getSearchTuningConfig", "Null application." );
//			return null;
//		}

		// statusMsg( "getSearchTuningConfig", "fSNConfig=" + fSNConfig );

		// return fSNConfig;

		/***
		SearchTuningConfig answer = fApp.getSearchTuningConfig();
		if( null != answer ) {
			fInitConfig = null;
			return answer;
		}
		else
			return fInitConfig;
		***/


	}

	public SearchTuningApp _getMainApplication()
	{
		return _fApp;
	}

	String getBaseURI()
	{
		final String kFName = "getBaseURI";
		// return getConfig().getConfigFileURI();
		// We want the DIRECTORY of where the main configuration was found

		if( null != getSearchTuningConfig() )
		{
			String tmpURI = getSearchTuningConfig().getConfigFileURI();
			debugMsg( kFName, "will return PARENT of \"" + tmpURI + "\"" );
			return NIEUtil.calculateDirOfURI( tmpURI );
		}
		else
		{
			errorMsg( kFName, "Null config, returning null." );
			return null;
		}
	}

	// Get the XSLT text from either the literal text
	// or from a file
	// results cached by calling routine
	byte [] getXSLTTextAsBytes()
		throws MarkupException
	{
		final String kFName = "getXSLTTextAsBytes";
		String tmp1 = getXSLTLiteralText();
		byte [] tmp2 = getXSLTTextFromFileAsBytes();

		if( tmp1 == null && tmp2.length < 1 )
			throw new MarkupException(
				"No XSLT formatting specified."
				+ " Must set literal text or location URI attribute."
				);
		if( tmp1 != null && tmp2.length > 0 )
			throw new MarkupException(
				"Conflicting XSLT formatting specified."
				+ " Must set either literal text or location URI attribute"
				+ " but not BOTH."
				);
		// If we have a byte array, just return it
		if( tmp2.length > 0 )
			return tmp2;
		// Else convert the inline string to bytes
		// Todo: not sure if this is right?  maybe tie to encoding
		// of parant config file???
		else
			return tmp1.getBytes();
	}
	
	// This is only an error if there is a file name
	// present and we can't get at  it
	// If the file name is null, that's an expected event
	// results cached by calling routine
	byte [] getXSLTTextFromFileAsBytes()
		throws MarkupException
	{
		final String kFName = "getXSLTTextFromFileAsBytes";
		String fileName = null;
		String baseURI = null;
		try
		{
			fileName = getXSLTFileName();
			if( fileName == null )
				return new byte [0];
			baseURI = getBaseURI();
			if( baseURI == null )
				throw new MarkupException(
					"Unable to get URI for base configuration file."
					);
			infoMsg( kFName,
				"Reading XSLT from file \"" + fileName + "\""
				);
			byte [] contents = NIEUtil.fetchURIContentsBin(
				fileName, baseURI
				);
			if( contents.length < 1 )
				throw new MarkupException(
					"Unable to load XSLT file"
					+ ", filename=\"" + fileName + "\""
					+ ", base URI=\"" + baseURI + "\""
					);
			// we're done!
			return contents;
		}
		catch(Exception e)
		{
			throw new MarkupException(
				"Error while loading XSLT file"
				+ ", filename=\"" + fileName + "\""
				+ ", base URI=\"" + baseURI + "\""
				+ ". Error: " + e
				);
		}
	}
	
	Transformer getCompiledXSLTDoc()
		throws MarkupException
	{
		if( ! mIsFreshCache )
		{
			final String kFName = "getCompiledXSLTDoc";
			try
			{
				byte [] contents = getXSLTTextAsBytes();
				cTransformer = JDOMHelper.compileXSLTString( contents );
			}
			catch(Exception e)
			{
				String msg = "Error getting / compiling XSLT: " + e;
				errorMsg( kFName, msg );
				throw new MarkupException( msg );
			}
		}
		return cTransformer;
	}



	// Simple gets and sets for a given transaction (items/item/item/item, etc)
	static String getTransQuery( Element items )
	{
		return JDOMHelper.getTextFromSinglePathAttrTrimOrNull(
			items, AUX_INFO_TAG_NAME, AUX_INFO_QUERY_ATTR
			);
	}
	static String getTransQueryField( Element items )
	{
		return JDOMHelper.getTextFromSinglePathAttrTrimOrNull(
			items, AUX_INFO_TAG_NAME, AUX_INFO_QUERY_FIELD_NAME_ATTR
			);
	}
	static long getTransID( Element items )
	{
		return JDOMHelper.getLongFromSinglePathAttr(
			items, AUX_INFO_TAG_NAME, AUX_INFO_TRANS_ID_ATTR, -1L
			);
	}

	static String getTransQueryNormalized( Element items )
	{
		return JDOMHelper.getTextFromSinglePathAttrTrimOrNull(
			items, AUX_INFO_TAG_NAME, AUX_INFO_QUERY_NORM_ATTR
			);
	}
	static int getTransItemCount( Element items )
	{
		return JDOMHelper.getIntFromSinglePathAttr(
			items, AUX_INFO_TAG_NAME, AUX_INFO_TOTAL_AD_COUNT_ATTR, -1
			);
	}
	
	
	
	
	

//	String formatItems( Element inItemsRoot, Hashtable inParameterHash )
//	{
//		JDOMHelper.xsltDocToDoc( Document inDoc,
//		Transformer inTransformer,
//		Hashtable inParamsHash
//		)
//		throws JDOMHelperException
//		return null;
//	}






	// This gets us to the logging object
	static RunLogInterface getRunLogObject()
	{
		return RunLogBasicImpl.getRunLogObject();
	}
	static boolean statusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	static boolean transactionStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().transactionStatusMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	static boolean shouldDoTransactionStatusMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTransactionStatusMsg( kClassName, inFromRoutine );
	}
	static boolean infoMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().infoMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	static boolean debugMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().debugMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	static boolean traceMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().traceMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	static boolean shouldDoTraceMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTraceMsg( kClassName, inFromRoutine );
	}
	static boolean warningMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().warningMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	static boolean errorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	static boolean fatalErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().fatalErrorMsg( kClassName, inFromRoutine,
			inMessage
			);
	}

	// Our main element tree
	protected JDOMHelper fConfigTree;
	// The main Search Names configuration object
	private SearchTuningConfig fInitConfig;
	// NO, just use configc
	private SearchTuningApp _fApp;
	private Object fAccessLockObject;


	// Cached values
	private boolean mIsFreshCache;
	private String cUserAlias;
	private String cUserAliasSubClass;
	private String cRequestedJavaClass;
	private boolean cIsInsertAfter;
	private boolean cIsMarkerReplaced;
	private boolean cIsMarkerCasen;
	private String cShortDesc;
	private String cUIScreen;
	private List cMarkerList;
	private String cXsltFileName;
	private String cXsltLiteralText;
	private Transformer cTransformer;	

	// The optional/system id for each map
	// private static final String MAP_ID_ATTR_NAME = "_id";

	// How we refer to specific fields
	// Relative to parent map statement
	private static final String MARKER_TEXT_PATH = "marker_text";
	// we reference these from SnRedirectRecord
	public static final String XSLT_PATH = "xslt_formatting";
	public static final String XSLT_LOCATION_ATTR = "location";

	// Atributes to change the modified behavior
	private static final String MARKER_TEXT_AFTER_ATTR = "after";
	private static final String MARKER_TEXT_REPLACE_ATTR = "replace";
	private static final String MARKER_TEXT_CASEN_ATTR = "casen";

	// By default, where do we put the new text
	private static final boolean DEFAULT_SUGGESTION_MARKER_NEW_TEXT_AFTER
		= true;
	// By default, should we replace the marker text when we find it
	private static final boolean DEFAULT_SUGGESTION_MARKER_IS_REPLACED
		= false;
	// By default, are the patterns case sensitive
	private static final boolean DEFAULT_SUGGESTION_MARKER_IS_CASEN
		= false;



	// Some default values for behavior
	// public static final String DEFAULT_MARKUP_CLASS = "MapKeywordPlacement";
	public static final String DEFAULT_MARKUP_CLASS = kClassName;
	public static final String MARKUP_CLASS_PREFIX = "nie.sn.";

	// Information about our main mission
	public static final String USER_ALIAS_CLASS_ATTR = "class";
	public static final String USER_ALIAS_SUB_CLASS_ATTR = "sub_class";
	public static final String USER_JAVA_CLASS_ATTR = "java_class";
	public static final String SHORT_DESC_ATTR = "short_description";
	public static final String UI_SCREEN_ATTR = "ui_edit_screen";

	// This is info we will ADD to the top level data items we pass
	// The main tag name we will ad
	public static final String AUX_INFO_TAG_NAME = "transaction_info";
	// The attribute we will add the query to
	public static final String AUX_INFO_QUERY_ATTR = "user_query";
	// The attribute we will add the query to
	public static final String AUX_INFO_QUERY_NORM_ATTR = "user_query_normalized";
	// The attribute we will add the query to
	public static final String AUX_INFO_QUERY_FIELD_NAME_ATTR =
		"cgi_query_field_name";
	// The actual transaction ID, based on processed requests
	public static final String AUX_INFO_TRANS_ID_ATTR = "transaction_id";
	// The number of items that were presented in this group
	public static final String AUX_INFO_TOTAL_AD_COUNT_ATTR =
		"item_group_count";



};

