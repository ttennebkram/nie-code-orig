package nie.sn;

import nie.core.*;
import org.jdom.Element;
import java.util.*;

//
// WARNING:
// For performance reasons we must use caching
// It is generally NOT SAFE to add or edit this record and then use the GET
// methods - these caches will likely provide invalid data
//
// The idea is that, for now, to change something, you create a whole new map
// This should be OK since updating the map is rather infrequent
//
// Todo: allow editing with fancy cache control...
//


class XmlMapRecord extends BaseMapRecord
{

	public String kClassName() { return "XmlMapRecord"; }


//	public SnRedirectRecord( int inMethod, String inURL, String inTerm )
	// public SnMapRecord( String inTerm, Element inElement
	public XmlMapRecord( SearchTuningConfig inMainConfig, Element inMainElement, int inID )
		throws MapRecordException
	{
		super( inMainConfig, inMainElement, inID );

		final String kFName = "(XmlMapRecord)Constructor";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		List terms = getTerms();
		if( terms == null || terms.size() < 1 )
			throw new MapRecordException( kExTag
				+ " No target terms in this map."
				+ " Must have at least one term to match on."
				);

		// Check that we have a URL or alternative spelling
		// or at least user defined data
//		List tmpURLs = getURLObjects();
//		List altTerms = getAlternateTerms();
//		if( (tmpURLs == null || tmpURLs.size() < 1  )
//			&& (altTerms == null || altTerms.size() < 1 )
//			)
		if( ! getHasURLObjects() && ! getHasAlternateTerms()
			&& ! getHasUserDataItems()
			)
		{
			throw new MapRecordException( kExTag
				+ " Empty or NULL URL and alternative term found."
				+ " Must have a URL or alterative term, or user data items."
				+ " getHasURLObjects()=" + getHasURLObjects()
				+ " getHasAlternateTerms()=" + getHasAlternateTerms()
				+ " getHasUserDataItems()=" + getHasUserDataItems()
				);
		}

//		debugMsg( kFName,
//			"URL=\"" + tmpURL + "\""
//			+ ", alt term=\"" + altTerm + "\""
//			+ " (it's OK for one of them to be null)"
//			);

		// cache other answers????
		// Todo: not yet
	}

	public int _getID()
	{
		// Get the map's ID, if any
		return getMainElement().getIntFromAttribute(
			MAP_ID_ATTR_NAME, -1
			);
	}
	public boolean setID( int inNewID )
	{
		// Store / overwrite as an attribute
		return getMainElement().setAttributeInt( MAP_ID_ATTR_NAME, inNewID );
	}

	public List getTerms() {
		if( fTerms == null )
			fTerms = getMainElement().getTextListByPathNotNullTrim( SEARCH_TERM_PATH );
		return fTerms;
	}

	public List getURLObjects()
	{
		final String kFName = "getURLObjects";

		// If we haven't done this before
		if( fUrlObjects == null )
		{
			// The results
			fUrlObjects = new Vector();
	
			// Get the urls for this map
			List urlElems = getMainElement().findElementsByPath( URL_PATH );
			if( urlElems == null || urlElems.size() < 1 )
			{
				return fUrlObjects;
			}


			// Convert each URL branch into a full object
			int urlCounter = 0;
			for( Iterator it = urlElems.iterator(); it.hasNext(); )
			{
				Element elem = (Element)it.next();
				urlCounter++;
				SnURLRecord url = null;
				try
				{
					// Convert the URL element tree into a full URL object
					url = new SnURLRecord( getMainConfig(), elem );
				}
				catch (SnURLRecordException e)
				{
					errorMsg( kFName,
						"Unable to load URL # " + urlCounter
						+ " in this map."
						+ " Will continue trying to load remaining items."
						);
					continue;
				}
				// Save it in th elist
				fUrlObjects.add( url );
			}

		}
		// Return the answer
		return fUrlObjects;
	}
	
	// Get a list of ONLY the Webmaster Suggests URL Objects
	public List getWmsURLObjects()
	{
		if( fWmsUrlObjects == null )
		{
			fWmsUrlObjects = new Vector();
			List tmpList = getURLObjects();
			for( Iterator it = tmpList.iterator(); it.hasNext() ; )
			{
				SnURLRecord url = (SnURLRecord) it.next();
				if( url.getIsASuggestion() )
					fWmsUrlObjects.add( url );
			}
		}
		return fWmsUrlObjects;
	}
	// Get the list and ADD it to an existing list, do not add dupes
	// YOU supply the master list and master lookup hash
	// Returns the number added, -1 = hard failure
	public int getWmsURLObjects( List ioMasterList, Hashtable ioMasterHash )
	{
		final String kFName = "getWmsURLObjects(2)";
		final boolean debug = shouldDoDebugMsg( kFName );
		final boolean trace = shouldDoTraceMsg( kFName );

		if( ioMasterList == null || ioMasterHash == null )
		{
			errorMsg( kFName,
				"One or more null inputs:"
				+ "ioMasterList=" + ioMasterList
				+ ", ioMasterHash=" + ioMasterHash
				);
			return -1;
		}

		if(debug) debugMsg( kFName,
			"Starting with pre-existing list of " + ioMasterList.size() + " URL objects."
			);

		List candidates = getWmsURLObjects();
		if(debug) debugMsg( kFName,
			"Will examine " + candidates.size() + " candidate URL objects."
			);
		int checked = 0;
		int kept = 0;
		for( Iterator it = candidates.iterator(); it.hasNext() ; )
		{
			SnURLRecord candidate = (SnURLRecord) it.next();
			checked++;
			String tmpURL = candidate.getURL();
			String tmpURL2 = NIEUtil.trimmedLowerStringOrNull( tmpURL );
			if( tmpURL2 != null && ! ioMasterHash.containsKey( tmpURL2 ) )
			{
				// add to the list
				ioMasterList.add( candidate );
				// Remember that we added it
				ioMasterHash.put( tmpURL2, candidate );
				kept++;
				if(trace) traceMsg( kFName,
					"Aadded # " + checked + " URL \"" + tmpURL + "\""
					);
			}
			else if( tmpURL2 == null )
			{
				warningMsg( kFName,
					"Null/empty # " + checked + " URL from URL object."
					+ " Ignoring this bad object and continuing."
					);
			}
			else
			{
				if(trace) traceMsg( kFName,
					"Did not add # " + checked + " URL \"" + tmpURL + "\""
					);
			}
		}
		if(debug) debugMsg( kFName,
			"Ending with updated list of " + ioMasterList.size() + " URL objects."
			+ " Checked " + checked + ", kept " + kept
			);
		return kept;
	}


	// Get a list of ONLY the REDIRECT URL Objects
	public List getRedirectURLObjects()
	{
		final String kFName = "getRedirectURLObjects";
		if( fRedirURLObjects == null )
		{
			fRedirURLObjects = new Vector();
			List tmpList = getURLObjects();
			for( Iterator it = tmpList.iterator(); it.hasNext() ; )
			{
				SnURLRecord url = (SnURLRecord) it.next();
				if( url.getIsARedirect() )
				{
					String tmpURL = url.getURL();
					if( tmpURL != null )
						fRedirURLObjects.add( url );
					else
					{
						warningMsg( kFName,
							"URL object with null/empty URL field."
							+ " Will ignore and keep searching for valid ones."
							);
					}
				}
			}
		}
		return fRedirURLObjects;
	}


	// Return strings, not objects
	public List getAlternateTerms()
	{
//		return fMainElement.findElementsByPath( ALTTERM_PATH );
		if( fAltTerms == null )
			fAltTerms = getMainElement().getTextListByPathNotNullTrim( ALTTERM_PATH );
		return fAltTerms;
	}

	// Get the list and ADD it to an existing list, do not add dupes
	// YOU supply the master list and master lookup hash
	// Returns the number added, -1 = hard failure
	public int getAlternateTerms( List ioMasterList, Hashtable ioMasterHash )
	{
		final String kFName = "getAlternateTerms(2)";
		final boolean debug = shouldDoDebugMsg( kFName );
		final boolean trace = shouldDoTraceMsg( kFName );

		if( ioMasterList == null || ioMasterHash == null )
		{
			errorMsg( kFName,
				"One or more null inputs:"
				+ "ioMasterList=" + ioMasterList
				+ ", ioMasterHash=" + ioMasterHash
				);
			return -1;
		}

		if(debug) debugMsg( kFName,
			"Starting with pre-existing list of " + ioMasterList.size() + " URL objects."
			);

		List candidates = getAlternateTerms();
		if(debug) debugMsg( kFName,
			"Will examine " + candidates.size() + " candidate terms."
			);
		int checked = 0;
		int kept = 0;
		for( Iterator it = candidates.iterator(); it.hasNext() ; )
		{
			String term = (String) it.next();
			checked++;
			String term2 = NIEUtil.trimmedLowerStringOrNull( term );
			if( term2 != null && ! ioMasterHash.containsKey( term2 ) )
			{
				// add to the list
				ioMasterList.add( term );
				// Remember that we added it
				ioMasterHash.put( term2, term );
				kept++;
				if(trace) traceMsg( kFName,
					"Aadded # " + checked + " term \"" + term + "\""
					);
			}
			else if( term2 == null )
			{
				warningMsg( kFName,
					"Null/empty # " + checked + " term."
					+ " Ignoring this bad term and continuing."
					);
			}
			else
			{
				if(trace) traceMsg( kFName,
					"Did not add # " + checked + " term \"" + term + "\""
					);
			}
		}
		if(debug) debugMsg( kFName,
			"Ending with updated list of " + ioMasterList.size() + " alternate terms."
			+ " Checked " + checked + ", kept " + kept
			);
		return kept;
	}




	// Return a list of fully populated User Data Records
	public List getUserDataItems()
	{
		final String kFName = "getUserDataItems";

		// If we haven't done this before
		if( fUserDataItems == null )
		{
			// Get the urls for this map
			List itemElems = getMainElement().findElementsByPath(
				// USER_DATA_ITEMS_PATH
				UserDataItem.MAP_USER_DATA_ITEMS_PATH
				);
			if( itemElems == null || itemElems.size() < 1 )
			{
				fUserDataItems = new Vector();
				return fUserDataItems;
			}

			// The results
			List results = new Vector();

			// Convert each <item> branch into a full object
			int itemCounter = 0;
			for( Iterator it = itemElems.iterator(); it.hasNext(); )
			{
				Element elem = (Element)it.next();
				itemCounter++;
				UserDataItem item = null;
				try
				{
					// Convert the URL element tree into a full URL object
					item = new UserDataItem( elem );
				}
				catch (UserDataItemException e)
				{
					errorMsg( kFName,
						"Unable to load User Data Item # " + itemCounter
						+ " in this map."
						+ " Will continue trying to load remaining items."
						);
					continue;
				}
				// Save it in th elist
				results.add( item );
			}

			// Save the answer
			fUserDataItems = results;
		}	// End if list was null

		// Return the answer
		return fUserDataItems;

	}


	String getUserLookFeelOrNull(
		String inMainPath, String inSubPath
		)
	{
		final String kFName = "getUserLookFeelOrNull";

		String outStr = null;

		if( null==inMainPath || null==inSubPath ) {
			errorMsg( kFName, "Null input(s), returning null. Values: inMainPath=" + inMainPath + ", inSubPath=" + inSubPath );
			return outStr;
		}

		JDOMHelper mainElem = getMainElement();
		if( null!=mainElem )
			outStr = mainElem.getTextFromSinglePathAttrTrimOrNull( inMainPath, inSubPath );
		else
			errorMsg( kFName, "No map config JDOM tree; returning null." );

		return outStr;
	}

	public String getWmsBoxUserPropertyOrNull( String inPropertyName ) {
		return getUserLookFeelOrNull(
			BaseMapRecord.WMS_BOX_PATH,
			// BaseMapRecord.SLOGAN_TEXT_ATTR
			inPropertyName
			);
	}

	public String getWmsIconUserPropertyOrNull( String inPropertyName ) {
		return getUserLookFeelOrNull(
			BaseMapRecord.WMS_ICON_PATH,
			// BaseMapRecord.SLOGAN_TEXT_ATTR
			inPropertyName
			);
	}
	public String getWmsDocTitleUserPropertyOrNull( String inPropertyName ) {
		return getUserLookFeelOrNull(
			BaseMapRecord.WMS_DOC_TITLE_PATH,
			inPropertyName
			);
	}
	public String getWmsDocUrlUserPropertyOrNull( String inPropertyName ) {
		return getUserLookFeelOrNull(
			BaseMapRecord.WMS_DOC_URL_PATH,
			// BaseMapRecord.SLOGAN_TEXT_ATTR
			inPropertyName
			);
	}
	public String getWmsDocSummaryUserPropertyOrNull( String inPropertyName ) {
		return getUserLookFeelOrNull(
			BaseMapRecord.WMS_DOC_SUMMARY_PATH,
			inPropertyName
			);
	}

	public String getWmsSloganUserPropertyOrNull( String inPropertyName ) {
		return getUserLookFeelOrNull(
			BaseMapRecord.WMS_SLOGAN_PATH,
			// BaseMapRecord.SLOGAN_TEXT_ATTR
			inPropertyName
			);
	}
	public String getAltSloganUserPropertyOrNull( String inPropertyName ) {
		return getUserLookFeelOrNull(
			BaseMapRecord.ALT_SLOGAN_PATH,
			// BaseMapRecord.SLOGAN_TEXT_ATTR
			inPropertyName
			);
	}

	// Get the list and ADD it to an existing list, do not add dupes
	// YOU supply the master list and master lookup hash
	// Returns the number added, -1 = hard failure
	// ??? not sure we need this for data items
	// or if it's appropriate, how do you efficiently compare xml trees?
//	public int getUserDataItems( List ioMasterList, Hashtable ioMasterHash )
//	{
//		final String kFName = "getAlternateTerms(2)";
//		final boolean debug = shouldDoDebugMsg( kFName );
//		final boolean trace = shouldDoTraceMsg( kFName );
//
//		if( ioMasterList == null || ioMasterHash == null )
//		{
//			errorMsg( kFName,
//				"One or more null inputs:"
//				+ "ioMasterList=" + ioMasterList
//				+ ", ioMasterHash=" + ioMasterHash
//				);
//			return -1;
//		}
//
//		if(debug) debugMsg( kFName,
//			"Starting with pre-existing list of " + ioMasterList.size() + " URL objects."
//			);
//
//		List candidates = getAlternateTerms();
//		if(debug) debugMsg( kFName,
//			"Will examine " + candidates.size() + " candidate terms."
//			);
//		int checked = 0;
//		int kept = 0;
//		for( Iterator it = candidates.iterator(); it.hasNext() ; )
//		{
//			String term = (String) it.next();
//			checked++;
//			String term2 = NIEUtil.trimmedLowerStringOrNull( term );
//			if( term2 != null && ! ioMasterHash.containsKey( term2 ) )
//			{
//				// add to the list
//				ioMasterList.add( term );
//				// Remember that we added it
//				ioMasterHash.put( term2, term );
//				kept++;
//				if(trace) traceMsg( kFName,
//					"Aadded # " + checked + " term \"" + term + "\""
//					);
//			}
//			else if( term2 == null )
//			{
//				warningMsg( kFName,
//					"Null/empty # " + checked + " term."
//					+ " Ignoring this bad term and continuing."
//					);
//			}
//			else
//			{
//				if(trace) traceMsg( kFName,
//					"Did not add # " + checked + " term \"" + term + "\""
//					);
//			}
//		}
//		if(debug) debugMsg( kFName,
//			"Ending with updated list of " + ioMasterList.size() + " alternate terms."
//			+ " Checked " + checked + ", kept " + kept
//			);
//		return kept;
//	}


	// Some caching of list values
	// These are populated as needed, vs specific cache fills

	// The master hash of matching terms
	private List fTerms;
	// The list of URL objects, for redirs and Web Master Suggests
	private List fUrlObjects;
	// The list of alternative terms
	private List fAltTerms;
	// The list of user defined XML snippets, the markup_items/item nodes
	private List fUserDataItems;
	// The list of strictly Web Master Suggests URLS
	private List fWmsUrlObjects;
	// The list of strictly URL Redirects
	private List fRedirURLObjects;

	// The optional/system id for each map
	private static final String MAP_ID_ATTR_NAME = "_id";

	// How we refer to specific fields
	// Relative to parent map statement
	private static final String SEARCH_TERM_PATH = "term";
	// we reference these from SnRedirectRecord
	public static final String URL_PATH = "url";
	public static final String ALTTERM_PATH = "alternate_term";
	// Fields under the URL field, see SnRedirectRecord
	// public static final String USER_DATA_ITEMS_PATH =
	//	"markup_items/item"
	//	;


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

};

