package nie.sn;

import java.util.*;
import java.sql.*;
import nie.core.*;
import org.jdom.Element;

/**********************************************************************
	This class encapsulates the little fragments of
	XML data that "users" (developers, administrators, etc)
	can attach to mapped terms
	Each instance is a specific URL, etc
***********************************************************************/

public class UserDataItem
{

	private static final String kClassName = "UserDataItem";

	public UserDataItem( Element inElement )
		throws UserDataItemException
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
		if( inElement == null )
			throw new UserDataItemException( kExTag +
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
			throw new UserDataItemException( kExTag +
				"Exception 1: " + e
				);
		}
		if( fMainElement == null )
			throw new UserDataItemException( kExTag +
				"Got back a NULL xml tree when trying to create a redirect record"
				);


		// Prepopulate cache
		mIsFreshCache = false;
		// In no cache mode, this will do the actual lookup and cache the values
		getUserClassName();

		// Turn on cache
		mIsFreshCache = true;

		// Some sanity checking

		// Check that we have a URL or alternative spelling
		String tmpStr = getUserClassName();
		if( tmpStr == null )
			throw new UserDataItemException( "constructor:"
				+ " No class found or inherrited."
				);

	}

	public UserDataItem( ResultSet inRecord, String inUserClassName )
		throws UserDataItemException
	{
		// this( recordToXML( inRecord ) );
		fMainElement = recordToXML( inRecord );

		final String kFName = "constructor(2)";
		final String kExTag = kClassName + '.' + kFName + ": ";
		inUserClassName = NIEUtil.trimmedStringOrNull( inUserClassName );
		if( null==inUserClassName )
			throw new UserDataItemException( kExTag + "Null/empty user class name." );
		cClassName = inUserClassName;

		getUrlId();
		getURL();
		getTitle();
		getAdCode();
		getDescriptionLines();

		// Turn on cache
		mIsFreshCache = true;

	}

	static JDOMHelper recordToXML( ResultSet inRecord )
		throws UserDataItemException
	{
		final String kFName = "recordToXML";
		final String kExTag = kClassName + '.' + kFName + ": ";
		boolean debug = shouldDoDebugMsg( kFName );

		if( null==inRecord )
			throw new UserDataItemException( kExTag + "Null record/result-set passed in." );

		JDOMHelper outElem = null;
		try {
			int itemID = inRecord.getInt( "url_id" );
			if( itemID < 1 )
				throw new UserDataItemException( kExTag + "Invalid URL ID " + itemID + " in record." );
			// cID = itemID;

			// cClassName = inRecord.getString( "user_class" );

			// #id, type, user_class, href_url, display_url, title, description,advertisement_code,created_by_person
			Element myElem = new Element( "item" );
			myElem.setAttribute( ITEM_ID_ATTR, ""+itemID );

			// A few requried fields
			String url = inRecord.getString( "href_url" );
			url = NIEUtil.trimmedStringOrNull( url );
			if( null==url )
				throw new UserDataItemException( kExTag + "Null/empty URL in record, id=" + itemID );
			// cURL = url;

			String itemCode = inRecord.getString( "advertisement_code" );
			// String itemCode = inRecord.getString( AD_CODE_ELEMENT_NAME );
			itemCode = NIEUtil.trimmedStringOrNull( itemCode );
			if( null==itemCode )
				throw new UserDataItemException( kExTag + "Null/empty item-code/ad-code in record, id=" + itemID );

			// Add the ad code
			// Element tmpElem = new Element( "advertisement_code" );
			Element tmpElem = new Element( AD_CODE_ELEMENT_PATH );
			tmpElem.addContent( itemCode );
			myElem.addContent( tmpElem );

			// Now add the URL
			Element urlElem = new Element( URL_ELEMENT_NAME );
			urlElem.addContent( url );
			myElem.addContent( urlElem );

			// Title
			String title = inRecord.getString( "title" );
			title = NIEUtil.trimmedStringOrNull( title );
			if( null!=title ) {
				tmpElem = new Element( TITLE_ELEMENT_NAME );
				tmpElem.addContent( title );
				urlElem.addContent( tmpElem );
				// cTitle = title;
			}

			// Description
			String desc = inRecord.getString( "description" );
			desc = NIEUtil.trimmedStringOrNull( desc );

			if(debug) debugMsg( kFName, "* description=" + desc );

			if( null!=desc ) {

				// Break the description down into specific lines
				List descLines = NIEUtil.singleStringToListOfStringsBigDelim(
					desc,	// String inSourceString,
					LINE_BREAK_MARKER,	// String inDelimitStr,
					true,	// boolean inDoTrimNull,
					true,	// per Jessica, was false,	// boolean inKeepEmpties,
					false,	// boolean inDumpOuterQuotes,
					false,	// boolean inNormalizeToLowerCase,
					true,	// boolean inAllowDupes,
					false,	// boolean inIsDupeCheckCasen,
					true	// boolean inDoWarnings
					);

				// Sanity
				if( null==descLines || descLines.size() < 1 ) {
					errorMsg( kFName,
						"Unable to break description into separate lines."
						);
				}
				// Else we do have some lines
				else {
					// For each broken out line
					for( Iterator it = descLines.iterator() ; it.hasNext() ; ) {
						// Grab it
						String myLine = (String) it.next();
						// Create an element and add the line's text to it
						// tmpElem = new Element( "description_line" );
						tmpElem = new Element( DESC_LINE_ELEM_NAME );
						tmpElem.addContent( myLine );
						// and it winds up under the URL element
						urlElem.addContent( tmpElem );
					}
				}
			}	// End if there was a description

			// Now convert to a JDOMHelper element
			outElem = new JDOMHelper( myElem );

		}
		catch( Throwable t ) {
			throw new UserDataItemException( kExTag +
				"Error converting DB record to XML object: " + t
				);
		}

		if(debug) debugMsg( kFName, "final xml = "
			+ outElem.JDOMToString( true )
			);

		return outElem;
	}


	public Element getXMLDataAsCopy()
	{
		Element clone = (Element)( fMainElement.getJdomElement().clone() );
		clone.detach();
		return clone;
	}



	public String getUserClassName()
	{
		if( null==cClassName && ! mIsFreshCache )
		{
			cClassName = fMainElement.getTextFromPathOrInheritTrimOrNull(
				null, CLASS_NAME_ATTR,
				null, 1
				);
		}
		return cClassName;
	}


	public String getAdCode()
	{
		if( null==cAdCode && ! mIsFreshCache )
			cAdCode = fMainElement.getTextByPathSuperTrimOrNull( AD_CODE_ELEMENT_PATH );
		return cAdCode;
	}

	public String getURL()
	{
		if( null==cURL && ! mIsFreshCache )
			cURL = fMainElement.getTextByPathSuperTrimOrNull( URL_ELEMENT_PATH );
		return cURL;
	}

	public int getUrlId()
	{
		if( cID<1 && ! mIsFreshCache )
			cID = fMainElement.getIntFromAttribute( ITEM_ID_ATTR, -1 );
		return cID;
	}

	public String getTitle()
	{
		if( null==cTitle && ! mIsFreshCache )
			cTitle = fMainElement.getTextByPathSuperTrimOrNull( TITLE_ELEMENT_PATH );
		return cTitle;
	}

	public List getDescriptionLines()
	{
		if( null==cDescLines && ! mIsFreshCache ) {
			final String kFName = "getDescriptionLines";
			boolean debug = shouldDoDebugMsg( kFName );
			// cDescLines = fMainElement.getTextByPathSuperTrimOrNull( TITLE_ELEMENT_PATH );
			cDescLines = fMainElement.getTextListByPath( DESC_LINE_ELEM_PATH );

			if(debug) {
				debugMsg( kFName, "path=" + DESC_LINE_ELEM_PATH );
				debugMsg( kFName, "fMainElement=" + fMainElement.JDOMToString( true ) );
				debugMsg( kFName, "cDescLines=" + cDescLines );
			}

		}
		return cDescLines;
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


	// private String fTerm;
	private JDOMHelper fMainElement;
	// private boolean fDirtyCache;
	private Object fAccessLockObject;

	// Whether or not to use the cache
	// Java will set this to false (safest) by default
	private boolean mIsFreshCache;

	private String cClassName;
	private int cID = -1;
	private String cURL;
	private String cTitle;
	private String cAdCode;
	private List cDescLines;

	private final static String CLASS_NAME_ATTR = "class";

	// We need a bunch of XML path names so that everybody who uses
	// user data items will agree on where to look

	// The main name of a user data item node
	public final static String ITEM_NODE_NAME = "item";
	public final static String ITEM_ID_ATTR = "id";
	public final static String URL_ELEMENT_NAME = "url";
	public final static String TITLE_ELEMENT_NAME = "title";
	public final static String AD_CODE_ELEMENT_NAME = "advertisement_code";
	public final static String DESC_LINE_ELEM_NAME = "description_line";

	// Wel also need paths
	public final static String URL_ELEMENT_PATH = URL_ELEMENT_NAME;
	public final static String TITLE_ELEMENT_PATH = URL_ELEMENT_PATH + '/' + TITLE_ELEMENT_NAME;
	public final static String AD_CODE_ELEMENT_PATH = AD_CODE_ELEMENT_NAME;
	public final static String DESC_LINE_ELEM_PATH = URL_ELEMENT_PATH + '/' + DESC_LINE_ELEM_NAME;

	// This is the path, relative to a MAP node, to find the items
	// this is their PARENT
	// public final static String MAP_NODE_CONTAINER_PATH = "markup_items";
	// renaming for more clarity (even though it's longer)
	public final static String MAP_NODE_CONTAINER_PATH = "user_data_markup_items";
	// This is the path to find all of the ITEM nodes
	public final static String MAP_USER_DATA_ITEMS_PATH =
		MAP_NODE_CONTAINER_PATH + '/' + ITEM_NODE_NAME;

	// This is the top level node in the XML tree passed to XSLT
	public final static String TOP_LEVEL_XSLT_CONTAINER_NAME = "items";
	// This is the path to find all of the item nodes
	// public final static String XSLT_USER_DATA_ITEMS_PATH =
	//	TOP_LEVEL_XSLT_CONTAINER_NAME + '/' + ITEM_NODE_NAME;
	// but we're ALREADY on the <items> node
	public final static String XSLT_USER_DATA_ITEMS_PATH =
		ITEM_NODE_NAME;

	public final static String LINE_BREAK_MARKER = "[BR]";
	
};

