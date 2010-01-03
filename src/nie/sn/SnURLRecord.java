package nie.sn;

import nie.core.*;
import org.jdom.Element;
import java.util.*;

public class SnURLRecord
{

	private static final String kClassName = "SnURLRecord";

	public SnURLRecord( SearchTuningConfig inMainConfig, Element inElement )
		throws SnURLRecordException
	{

		final String kFName = "constructor(1)";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// Start with some Sanity checking

		// For performance, we want to cache some answers
		// But we will probably allow updates in the future, so it's
		// important to respect caching rules
		// fAccessLockObject = new Object();
		// fDirtyCache = true;

		// Sanity checks
		if( null == inMainConfig )
			throw new SnURLRecordException( kExTag +
				"NULL application config passed in."
				);

		fMainConfig = inMainConfig;

		// Sanity checks
		if( inElement == null )
			throw new SnURLRecordException( kExTag +
				"Constructor was passed in a NULL element."
				);

		// Instantiate and store the main JDOMHelper a
		fMainElement = null;
		try
		{
			fMainElement = new JDOMHelper( inElement );
		}
		catch (JDOMHelperException e)
		{
			throw new SnURLRecordException( kExTag +
				"JDOM Helper Exception: " + e
				);
		}
		if( fMainElement == null )
			throw new SnURLRecordException( kExTag +
				"Got back a NULL xml tree when trying to create a redirect record"
				);


		// Prepopulate cache
		mIsFreshCache = false;
		// In no cache mode, this will do the actual lookup and cache the values
		getIsARedirect();
		getIsASuggestion();
		getURL();
		getDisplayURL();
		getTitle();
		getDescription();

		// debugMsg( kFName, "calling shuld display()" );
		// boolean tmp=shouldDisplay();
		// debugMsg( kFName, "got back " + tmp );

		// Turn on cache
		mIsFreshCache = true;


		// Some sanity checking

		// Check that we have a URL or alternative spelling
		String tmpURL = getURL();
		if( tmpURL == null )
			throw new SnURLRecordException( "constructor:"
				+ " Empty or NULL URL and alternative term found."
				+ " Must have a URL or alterative term."
				);

		debugMsg( kFName,
			"URL=\"" + tmpURL + "\""
			);

		additionalSanityChecks();

	}


	public SnURLRecord( SearchTuningConfig inMainConfig,
		int inUrlID,
		int inTypeCode,
		// String optSubtypeStr,
		String inHref,
		String optDisplayUrl,
		String inTitle,
		String optHoverTitle,
		String inDescription
		)
			throws SnURLRecordException
	{

		final String kFName = "constructor(2)";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// Start with some Sanity checking

		// For performance, we want to cache some answers
		// But we will probably allow updates in the future, so it's
		// important to respect caching rules
		// fAccessLockObject = new Object();
		// fDirtyCache = true;

		// Sanity checks
		if( null == inMainConfig )
			throw new SnURLRecordException(
				"NULL application config passed in."
				);
		fMainConfig = inMainConfig;

		// Sanity checks
		cURL = NIEUtil.trimmedStringOrNull(inHref);
		if( null==cURL )
			throw new SnURLRecordException( kExTag +
				"Empty or NULL URL and alternative term found."
				+ " Must have a URL or alterative term."
				);

		debugMsg( kFName,
			"URL=\"" + cURL + "\""
			+ ", title=\"" + inTitle + "\""
			);

		cDisplayURL = NIEUtil.trimmedStringOrNull(optDisplayUrl);
		if( null==cDisplayURL )
			cDisplayURL = cURL;

		cID = inUrlID;

		cShouldDisplay = DEFAULT_SHOULD_DISPLAY;

		// Translate the type code into boolean flags
		if( TYPE_WMS == inTypeCode )
			cIsASuggestion = true;
		else
			cIsASuggestion = DEFAULT_IS_A_SUGGESTION;
		if( TYPE_REDIRECT == inTypeCode )
			cIsARedirect = true;
		else
			cIsARedirect = DEFAULT_IS_A_REDIRECT;

		// cSubtypeStr = optSubtypeStr;

		cTitle = inTitle;
		cHoverTitle = optHoverTitle;
		cDescription = inDescription;

		// Turn on cache
		mIsFreshCache = true;

		// Some additional sanity checking
		additionalSanityChecks();

	}

	void additionalSanityChecks()
	{
		final String kFName = "additionalSanityChecks";

		// Some additional sanity checking
		if( getIsASuggestion() && getTitle() == null )
		{
			// If it's not a redirect, then they really messed up
			if( ! getIsARedirect() )
				warningMsg( kFName,
					"This URL is intended for the Suggestion box, but it has no title."
					+ " URL=" + getURL()
					+ " Will keep this listing, but display will show URL as title."
					);
			// Otherwise we'll just make a note of it
			else
				debugMsg( kFName,
					"This URL is intended for the Suggestion box, but it has no title."
					+ " However, it is also a redirect, which will normally take precdence"
					+ " and redirects don't use titles - so perhaps that's the intent."
					+ " URL=" + getURL()
					+ " Will keep this listing, but Webmaster Suggests display will show URL as title."
					);

		}

	}





	// NOTE: Make sure to cache the answers to these from the constructor!!!!

	public boolean getIsARedirect()
	{
		if( ! mIsFreshCache )
			cIsARedirect = fMainElement.getBooleanFromAttribute(
				URL_DO_REDIR_ATTR, DEFAULT_IS_A_REDIRECT
				);
		return cIsARedirect;
	}
	public boolean getIsASuggestion()
	{
		if( ! mIsFreshCache )
			cIsASuggestion = fMainElement.getBooleanFromAttribute(
				URL_DO_SUGGEST_ATTR, DEFAULT_IS_A_SUGGESTION
				);
		return cIsASuggestion;
	}


	public boolean _shouldDisplayOBS()
	{
		if( ! mIsFreshCache )
		{
			// The name and default name for the slogan/header
			// final String kMainAttr = SHOULD_DISPLAY_ATTR_NAME;
			// final String kMainAttr = BaseMapRecord.WMS_DOC_URL_PATH;
			// final String kBackupTag = "default_url_settings";
			final String kBackupTag = "default_" + BaseMapRecord.DEFAULT_WMS_DOC_URL_SHOULD_DISPLAY;

			/***
			// Grab the values
			cShouldDisplay = fMainElement.getBooleanFromPathOrInherit(
				null, kMainAttr,
				kBackupTag,
				DEFAULT_SHOULD_DISPLAY,
				3
				);
			***/
		}
		return cShouldDisplay;
	}





//	int getID()
//	{
//		// Get the map's ID, if any
//		return fMainElement.getIntFromAttribute(
//			MAP_ID_ATTR_NAME, -1
//			);
//	}
//	boolean setID( int inNewID )
//	{
//		// Store / overwrite as an attribute
//		return fMainElement.setAttributeInt( MAP_ID_ATTR_NAME, inNewID );
//	}

	//	int getID()
	//	{
	//		// Get the map's ID, if any
	//		return fMainElement.getIntFromAttribute(
	//			MAP_ID_ATTR_NAME, -1
	//			);
	//	}
	//	boolean setID( int inNewID )
	//	{
	//		// Store / overwrite as an attribute
	//		return fMainElement.setAttributeInt( MAP_ID_ATTR_NAME, inNewID );
	//	}
	
		// Generate a <div> tag with title, desc and URL
		// public Element generateWebmasterSuggestsItem( BaseMapRecord inPrimaryMapRecord ) {
		public Element generateWebmasterSuggestsItem( MapRecordInterface inPrimaryMapRecord ) {
			final String kFName = "generateWebmasterSuggestsItem";
	
			// Grab the fields we'll be using
			String url = getURL();
			String displayURL = getDisplayURL();
			String title = getTitle();
			String desc = getDescription();
	
			// Sanity check
			if( null==url || null==displayURL || null==inPrimaryMapRecord )
			{
				errorMsg( kFName,
					"Null URL(s) or inputs, returning null."
					+ " link = " + url
					+ ", display = " + displayURL
					+ ", inPrimaryMapRecord = " + inPrimaryMapRecord
					);
				return null;
			}
	
			if( null == title )
			{
				warningMsg( kFName,
					"Null title, will use dispaly URL for title."
					);
				title = displayURL;
			}
	
			// The overall output is a <div> tag, to contain multiple things
			Element outElem = new Element( "div" );
	
			Element tElem = inPrimaryMapRecord.generateWmsDocTitleElementOrNull( this );
			/***
			// Work on the anchor
			// Add the hyperlinked title
			Element linkElem = new Element( "a" );
			linkElem.setAttribute( "href", url );
			outElem.addContent( linkElem );
			// Now the bold tag with the text
			Element boldElem = new Element( "b" );
			boldElem.addContent( title );
			linkElem.addContent( boldElem );
			// Add a BR for this row
			outElem.addContent( new Element("br") );
			***/
			if( null!=tElem ) {
				outElem.addContent( tElem );
				// Add a BR for this row
				outElem.addContent( new Element("br") );
			}
	
	
			// Add the description, if any
			if( null!=desc ) {
				Element sElem = inPrimaryMapRecord.generateWmsDocSummaryElementOrNull( this );
				/***
				Element descElem = new Element( "small" );
				descElem.addContent( desc );
				outElem.addContent( descElem );
				// Add a BR for this row
				outElem.addContent( new Element("br") );
				***/
				if( null!=sElem ) {
					outElem.addContent( sElem );
					// Add a BR for this row
					outElem.addContent( new Element("br") );
				}
			}
	
			// Add the final link, if requested
			// if( shouldDisplay() )
			// {
				// Element aElem = inPrimaryMapRecord.generateWebmasterSuggestsLink( this );
				Element aElem = inPrimaryMapRecord.generateWmsDocUrlElementOrNull( this );
			
				// Create the anchor
				/***
				Element aElem = new Element( "a" );
				aElem.setAttribute( "href", url );
				outElem.addContent( aElem );
				// create the nested text element
				Element displayURLElem = JDOMHelper.findOrCreateElementByPath(
					aElem,			// Starting at
					"small/i",	// path
					true			// Yes, tell us about errors
					);
				// And add that text
				displayURLElem.addContent( displayURL );
				***/
	
				if( null!=aElem ) {
					outElem.addContent( aElem );
					// Add a BR for this row
					outElem.addContent( new Element("br") );
				}
			// }
	
			// A final newline
			// buff.append( "<br>" + NL );
			// No, let the caller do that
	
			// Return results
			// return new String( buff );
			return outElem;
		}

	// See also:
	// nie.sn.static_files.onebox.template.xslt
	public Element generateWebmasterSuggestsGoogleOneBoxItem(
			MapRecordInterface inPrimaryFormatMapRecord
	) {
		final String kFName = "generateWebmasterSuggestsGoogleOneBoxItem";

		// Grab the fields we'll be using
		String url = getURL();
		String displayURL = getDisplayURL();
		String title = getTitle();
		String desc = getDescription();

		// Sanity check
		if( null==url || null==displayURL /*|| null==inPrimaryMapRecord*/ )
		{
			errorMsg( kFName,
				"Null URL(s) or inputs, returning null."
				+ " link = " + url
				+ ", display = " + displayURL
				// + ", inPrimaryMapRecord = " + inPrimaryMapRecord
				);
			return null;
		}

		if( null == title )
		{
			warningMsg( kFName,
				"Null title, will use dispaly URL for title."
				);
			title = displayURL;
		}

		// The names of these values are set by Google
		// and must align with the Skeleton
		// nie/sn/static_files/onebox/results_skeleton.xml

		// The overall output is a <MODULE_RESULT> tag, to contain multiple things
		Element outElem = new Element( SnRequestHandler.GOOGLE_ONEBOX_RESULTS_HIT_ELEMENT_NAME );

		// Add the hyperlink
		JDOMHelper.addSimpleTextToNewPath( outElem,
				SnRequestHandler.GOOGLE_ONEBOX_RESULTS_HIT_HREF_ELEMENT_NAME, url );
		
		// From here down these fixed strings have to align with the values
		// used in
		// nie/sn/static_files/onebox/template.xslt

		// The title
		Element titleElem = new Element( SnRequestHandler.GOOGLE_ONEBOX_RESULTS_HIT_FIELD_ELEMENT_NAME );
		titleElem.setAttribute( SnRequestHandler.GOOGLE_ONEBOX_RESULTS_HIT_FIELD_NAME_ATTR, "title" );
		titleElem.addContent( title );
		outElem.addContent( titleElem );

		// Add the description, if any
		if( null!=desc )
		{
			// Element sElem = inPrimaryMapRecord.generateWmsDocSummaryElementOrNull( this );
			Element sElem = new Element( SnRequestHandler.GOOGLE_ONEBOX_RESULTS_HIT_FIELD_ELEMENT_NAME );
			sElem.setAttribute( SnRequestHandler.GOOGLE_ONEBOX_RESULTS_HIT_FIELD_NAME_ATTR, "description" );
			sElem.addContent( desc );
			outElem.addContent( sElem );
		}

		return outElem;
	}

	public int getID() {
		return cID;
	}

	public String getURL()
	{
		if( ! mIsFreshCache )
			cURL = fMainElement.getTextTrimOrNull();
		return cURL;
	}

	public String getDisplayURL()
	{
		if( ! mIsFreshCache ) {
			cDisplayURL = fMainElement.getTextByPath( URL_TITLE_PATH );
			if( null==cDisplayURL )
				cDisplayURL = cURL;
		}
		return cDisplayURL;
	}

//	// Get the main map element that this node is associated with
//	public Element getParentMapElementOBS()
//	{
//		final String kFName = "getParentMapElement";
//
//		if( fMainElement != null )
//		{
//			Element elem = fMainElement.getJdomElement();
//			if( elem != null )
//				if( ! elem.isRootElement() )
//					return elem.getParent();
//		}
//		errorMsg( kFName,
//			"Unable to find parent / map node for this record."
//			+ " Will return NULL."
//			);
//		return null;
//	}

	// Get a title
	// Prefer user title over any auto title
	// return a true NULL if either not found or empty string
	public String getTitle()
	{
		if( ! mIsFreshCache )
		{
			String tmpStr = getUserTitle();
			if( tmpStr == null || tmpStr.trim().equals("") )
			{
				tmpStr = getAutoTitle();
				if( tmpStr == null || tmpStr.trim().equals("") )
					tmpStr = null;
			}
			if( tmpStr != null )
				tmpStr = tmpStr.trim();
			cTitle = tmpStr;
		}
		return cTitle;
	}
	private String getUserTitle()
	{
		String tmpStr = fMainElement.getTextByPath( URL_TITLE_PATH );
		if( tmpStr != null && ! tmpStr.trim().equals("") )
			return tmpStr.trim();
		else
			return null;
	}
//	public void setUserTitle()
//	{
//	}
	private String getAutoTitle()
	{
		String tmpStr = fMainElement.getTextByPath( URL_AUTO_TITLE_PATH );
		if( tmpStr != null && ! tmpStr.trim().equals("") )
			return tmpStr.trim();
		else
			return null;
	}
//	public void setAutoTitle()
//	{
//	}

	public String getDescription()
	{
		if( ! mIsFreshCache )
		{
			String tmpStr = getUserDescription();
			if( tmpStr == null || tmpStr.trim().equals("") )
			{
				tmpStr = getAutoDescription();
				if( tmpStr == null || tmpStr.trim().equals("") )
					tmpStr = null;
			}
			if( tmpStr != null )
				tmpStr = tmpStr.trim();
			cDescription = tmpStr;
		}
		return cDescription;
	}
	private String getUserDescription()
	{
		String tmpStr = fMainElement.getTextByPath( URL_DESCRIPTION_PATH );
		if( tmpStr != null && ! tmpStr.trim().equals("") )
			return tmpStr.trim();
		else
			return null;
	}
//	public void setUserDescription()
//	{
//	}
	private String getAutoDescription()
	{
		String tmpStr = fMainElement.getTextByPath( URL_AUTO_DESCRIPTION_PATH );
		if( tmpStr != null && ! tmpStr.trim().equals("") )
			return tmpStr.trim();
		else
			return null;
	}
//	public void setAutoDescription()
//	{
//	}



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


	// private String fTerm;
	private JDOMHelper fMainElement;

	private SearchTuningConfig fMainConfig;
	// private boolean fDirtyCache;
	private Object fAccessLockObject;


	// Whether or not to use the cache
	// Java will set this to false (safest) by default
	private boolean mIsFreshCache;

	private int cID = -1;
	// Some fields to hold cached values
	private boolean cIsARedirect;
	private boolean cIsASuggestion;
	// private String cSubtypeStr;
	private String cURL;
	private String cDisplayURL;
	private String cTitle;
	private String cHoverTitle;
	private String cDescription;

	// Whether or not to display the URL
	private boolean cShouldDisplay;

	// The optional/system id for each map
	private static final String MAP_ID_ATTR_NAME = "_id";

	// How we refer to specific fields
	// Relative to parent map statement
	private static final String SEARCH_TERM_PATH = "term";
	// we reference these from SnRedirectRecord
	public static final String _URL_PATH = "url";
	public static final String DISPLAY_URL_PATH = "display_url";
	public static final String ALTTERM_PATH = "alternate_term";
	// Fields under the URL field, see SnRedirectRecord


	// Fields under the URL field, see SnRedirectRecord
	private static final String URL_TITLE_PATH = "title";
	private static final String URL_DESCRIPTION_PATH = "description";
	// Items we may automatically fill in
	// Not yet implemented
	private static final String URL_AUTO_TITLE_PATH = "_auto_title";
	private static final String URL_AUTO_DESCRIPTION_PATH = "_auto_description";
	// Attributes of a URL entry
	private static final String URL_DO_REDIR_ATTR = "redirect";
	private static final String URL_DO_SUGGEST_ATTR = "suggest";
	// Whether or not we should display it
	// private static final String SHOULD_DISPLAY_ATTR_NAME = "display";
	private static final String _SHOULD_DISPLAY_ATTR_NAME = "should_show_url";
	// ^^^ moved to BaseMapRecord as WMS_DOC_URL_SHOULD_DISPLAY_ATTR

	// Some default values for behavior
	private static final boolean DEFAULT_IS_A_REDIRECT = false;
	private static final boolean DEFAULT_IS_A_SUGGESTION = true;
	private static final boolean DEFAULT_SHOULD_DISPLAY = true;

	public static final int TYPE_WMS = 1;
	public static final int TYPE_REDIRECT = 2;
	public static final int TYPE_AD = 3;



};

