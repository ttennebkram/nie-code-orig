package nie.sn;

import nie.core.*;
import java.util.*;

import org.jdom.Element;
import org.jdom.Document;
import org.jdom.transform.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;

/**
 * @author mbennett
 *
 * Similar to BaseMarkup, but also has the capability to
 * log exposures and click throughs
 */
public class AdLoggingMarkup
	extends BaseMarkup
	implements Runnable, Cloneable
{


	private static final String kClassName = "AdLoggingMarkup";

	public AdLoggingMarkup(
		Element inElement,
		SearchTuningConfig inSNConfig
		)
			throws MarkupException
	{
		super( inElement, inSNConfig );
		final String kFName = "constructor";
		final String kExTag = kClassName + '.' + kFName + ": ";

		debugMsg( kFName, "Start." );

		// Note: Read the warning below or die!
		fUseCache = false;
		getShouldLogExposures();
		getShouldLogClickThroughs();
		fUseCache = true;

//		if( null == inApp )
//		{
//			String msg = "No application configuration data passed in.";
//			errorMsg( kFName, msg );
//			throw new MarkupException( kExTag + msg );
//		}
//		// fSNConfig = inSNConfig;
//		fApp = inApp;
//		statusMsg( kFName, "fApp==null = " + (fApp==null) );


		if( ! getShouldLogExposures()
			&& ! getShouldLogClickThroughs()
			)
		{
			warningMsg( kFName,
				"Not configured to log any exposure or click through"
				+ " activity.  The ads will still display, but will not"
				+ " appear in reports."
				);
		}


		// ATTENTION!!!!!!
		// In order to do logging we will be cloning this Ad Logging object
		// To save time, we really don't need to clone the JDOM helper stuff,
		// which IS a DEEP clone operation, because we have ALREADY CACHED THE VALUES
		// so we will take the rather extraordinary step of NULL'ing out the
		// JDOMHlper tree
		// Make DARN SURE you have cached any values from the XML that you care about
		
		// Nuke the XML that was used to construct this node
		fConfigTree = null;

	}

	public String doMarkup(
		String inDoc, Element itemsRoot,
		AuxIOInfo inRequest, AuxIOInfo inResponse
		)
			throws MarkupException
	{

		// Sanity check, bypass all the logic if no markup is being done
		// if( ! getShouldLogExposures() && ! getShouldLogClickThroughs() )
		//	return super.doMarkup( inDoc, itemsRoot, inRequest, inResponse );

		// OK, we're actually gonna do something

		final String kFName = "doMarkup";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// This adds in additional XML data
		// that MUST BE USED BY THE STYLE SHEET to create
		// click back logging links
		// if( getShouldLogClickThroughs() )
		// And we always augment, even if we aren't adding to the URL, so
		// that the style sheet sees a consistent element name for href urls
		try
		{
			augmentXMLDataItem( itemsRoot, inRequest );
		}
		catch( SearchLoggerException e )
		{
			throw new MarkupException( kExTag
				+ "Problem adding advertising data to XML data tree."
				);
		}

		// Now we get the results
		String outDoc = super.doMarkup( inDoc, itemsRoot, inRequest, inResponse );

		// IF we're to log exposures, we have some work to do
		if( getShouldLogExposures() )
		{
			// Start the logging thread
	
			// We need to do some cloning in order for this to happen
			// safely
	
			// Since we're done with the XML snippet in terms of display
			// there is no need to clone that
	
			// Since run can only look at member fields we'll need to store
			// this XML so it is accessible when we .run()
			// but since there may be many instances of us, we need one instance
			// PER thread
	
			// The new instance
			AdLoggingMarkup myClone = null;
			try
			{
				// Do the clone
				myClone = (AdLoggingMarkup)this.clone();
				// Save the XML data to that instance
				myClone.fClonedXMLData = itemsRoot;
				// Save the request and reponse info, for read-only
				myClone.fCachedRequest = inRequest;
				// Now start the other copy of ourselves
				Thread lThread = new Thread( myClone );
				// lThread.setName(name);
				// lThread.setPriority(newPriority);
				lThread.start();
			}
			catch( Exception e )
			{
				// Though this is very serious, we do not want to impact
				// what the user sees, so it's a bad error, but we don't
				// want to throw an exception, which will prevent the user
				// from seeing the markup
				errorMsg( kFName,
					"Error creating exposure-logging thread"
					+ " - this Ad/item exposure will not be logged."
					+ " The user will still see the normal results list with Ads"
					+ " but the exposure will not appear in any reports."
					+ " Error: " + e
					);
				myClone = null;
			}

		}


		// return the answer
		return outDoc;

	}
	
	// Add the click links
	// This will create nie href nodes to use in the XSLT that creates the ads
	Element augmentXMLDataItem(
		Element inItemsRoot,
		AuxIOInfo inRequestObj
		)
			throws SearchLoggerException
	{
		final String kFName = "augmentXMLDataItem";
		final boolean debug = shouldDoDebugMsg( kFName );

		if( null==inItemsRoot )
		{
			errorMsg( kFName,
				"Was passed in NULL data tree to augment."
				+ " Returning NULL."
				);
			return null;
		}

		// We need to add the nie_href field to each item
		List items = JDOMHelper.findElementsByPath(
			inItemsRoot, UserDataItem.XSLT_USER_DATA_ITEMS_PATH
			);
		if( null==items || items.size() < 1 )
		{
			errorMsg( kFName,
				"No individual item elements."
				+ " Returning NULL."
				);
			errorMsg( kFName,
				"XML tree: "
				+ JDOMHelper.JDOMToString( inItemsRoot, true )
				);
			return null;
		}


		// Cache some values that are the same for every item
		String lQuery = getTransQuery( inItemsRoot );
		// int lGroupCount = items.size();


		// Loop through the items
		for( Iterator it = items.iterator(); it.hasNext() ; )
		{
			Element item = (Element)it.next();
			String origURL = JDOMHelper.getTextByPathTrimOrNull(
				item, URL_PATH
				);

			if( null == origURL )
			{
				errorMsg( kFName,
					"No URL in user data, this item may not be clickable by users."
					);
				continue;
			}

			// We start by assuming it's the same
			String newURL = origURL;

			// Do we need to markup the URL?
			if( getShouldLogClickThroughs() )
			{
				LoggingLink linkObj = new LoggingLink(
					getSearchTuningConfig(), inRequestObj
					);

				// Set some key fields

				// Transaction type is an Advertisement Click-Through
				linkObj.setTransactionType(
					SearchLogger.TRANS_TYPE_ADVERTISEMENT_CLICK_THROUGH
					);

				// We need some info about the Ad
				String adID = getItemID( item );
				String destinationURL = getItemURL( item );


				// And now we store that info
				// These will throw execeptions on nulls
				linkObj.setAdvertisementID( adID );
				// These are both the same URL in this case
				linkObj.setAdvertisementURL( destinationURL );
				linkObj.setDestinationURL( destinationURL );


				// THIS IS ALREADY HANDLED by LoggingLink's constructor
				// linkObj.setReferingID( lTransID );

				linkObj.setQueryText( lQuery );

				// Todo: should we try to get the search term here?
				// Or have them get it from the referring parent?
				// lGroupCount
				// Rank in set of ads presented



				// Now generate the link
				String tmpURL = linkObj.generateURL();
				if( null != tmpURL )
					newURL = tmpURL;
				else
					errorMsg( kFName,
						"Got back a null logging-link."
						+ " Will retain original URL and continue processing other records."
						+ " Orig URL = \"" + origURL + "\""
						);
			}


			// We now have the final href / url to use,
			// in the newURL variable,
			// which may just be a copy of the original URL
			// let's go ahead and add it
			Element newURLElem = new Element( NEW_URL_PATH );
			// add in the URL
			newURLElem.addContent( newURL );
			// And then attach it to the main item
			newURLElem.detach();
			item.addContent( newURLElem );	


		}	// End for each item

		if(debug)
			debugMsg( kFName,
				"Final XML tree: "
				+ JDOMHelper.JDOMToString( inItemsRoot, true )
				);

		// We're all set!
		return inItemsRoot;
	}

	public void run()
	{
		postMarkupExposureLogging();
	}
	void postMarkupExposureLogging()
	{
		final String kFName = "postMarkupExposureLogging";
		final boolean debug = shouldDoDebugMsg( kFName );

		// We read from Element fCloneXMLData
		if( null == fClonedXMLData )
		{
			errorMsg( kFName,
				"XML Data was NULL, nothing to log."
				);
			return;
		}

		// Lookup each item
		List items = JDOMHelper.findElementsByPath(
			fClonedXMLData, UserDataItem.XSLT_USER_DATA_ITEMS_PATH
			);
		if( null==items || items.size() < 1 )
		{
			errorMsg( kFName,
				"No individual item found to log."
				);
			errorMsg( kFName,
				"XML tree: "
				+ JDOMHelper.JDOMToString( fClonedXMLData, true )
				);
			return;
		}


		// Cache some values that are the same for every item
		// trans ID comes from request ID
		String lQuery = getTransQuery( fClonedXMLData );
		// currently not fully implemented
		int lGroupCount = items.size();

		// Loop through the items
		for( Iterator it = items.iterator(); it.hasNext() ; )
		{
			Element item = (Element)it.next();

			// Transaction type is an Advertisement Exposure
			// SearchLogger.TRANS_TYPE_ADVERTISEMENT_EXPOSURE

			// We need some info about the Ad
			String adID = getItemID( item );
			if( null == adID )
			{
				errorMsg( kFName,
					"No advertisement code to log."
					);
				continue;
			}
			String adURL = getItemURL( item );
			String adGraphic = null;

			// stuff in the advertisers code, graphic, url, etc, yuck

			// Log the transaction
			try
			{
				getSearchLogger().logTransaction(
					fCachedRequest,         // Request
					null,        // Response
					SearchLogger.TRANS_TYPE_ADVERTISEMENT_EXPOSURE,
					lQuery,                 // Query
					0,				// Search Names action code
					1,            // # of high level actions taken
					lGroupCount,        // # of units of into sent
					0,             // int inSNStatusCode,
					null,           // String inSNStatusMsg,
					false,   // inFromDirectLogAction
					false,       // Did the engine just do directed URL redirect
					// TODO: Support snippet serving for ads
					false,		// Was this from a snippet serve (not currently supported
								// for ads)
					adID,		// Advertisement info
					adURL,
					adGraphic
					);
			}
			catch(Exception e)
			{
				// SearchLoggerException,
				// SQLException,
				// DBConfigException
				//, DBConfigInServerReconnectWait

				errorMsg( kFName, "Error logging exposure, error: " + e );
			}

		}	// End for each item
	}



	// Given a PARTICULARE item, what is its ID?
	// Obviously this is NOT cached
	String getItemID( Element item )
	{
		return JDOMHelper.getTextByPathTrimOrNull(
			item, ITEM_ID_PATH
			);
	}
	String getItemURL( Element item )
	{
		return JDOMHelper.getTextByPathTrimOrNull(
			item, URL_PATH
			);
	}
	boolean getShouldLogExposures()
	{
		if( ! fUseCache )
			cShouldLogExposure = fConfigTree.getBooleanFromAttribute(
				DO_EXPOSURE_LOGGING_ATTR, DEFAULT_DO_EXPOSURE_LOGGING
				);
		return cShouldLogExposure;
	}
	boolean getShouldLogClickThroughs()
	{
		if( ! fUseCache )
			cShouldLogClickthroughs = fConfigTree.getBooleanFromAttribute(
				DO_CLICK_LOGGING_ATTR, DEFAULT_DO_CLICK_LOGGING
				);
		return cShouldLogClickthroughs;
	}




	private SearchLogger getSearchLogger()
	{
		// return getMainApplication().getSearchLogger();
		return getSearchTuningConfig().getSearchLogger();
	}


	// private SearchTuningConfig fSNConfig;
	// private SearchTuningApp fApp;

	private boolean fUseCache;
	private boolean cShouldLogExposure;
	private boolean cShouldLogClickthroughs;

	// This will point to the XML data we are to do our logging from
	// This field is ONLY FILLED OUT AFTER WE HAVE BEEN CLONED
	private Element fClonedXMLData;
	// we do logging in a separate thread, this is a copy/reference
	// to the request that we are logging from
	private AuxIOInfo fCachedRequest;


	// When we log to the database, where will we find the key to log
	// against?
	// private static final String ITEM_ID_PATH = "items/item/product_id";

	// The URL field that they will supply (assume <item> is current base)
	public static final String URL_PATH = "url";

	// the href field we will supply in return, this should only be ONE level
	public static final String NEW_URL_PATH = "sn_href";
	// the path to a string that gives us the product or vendor ID to log against
	public static final String ITEM_ID_PATH = "advertisement_code";


	private static final String DO_EXPOSURE_LOGGING_ATTR =
		"log_exposures";
	private static final String DO_CLICK_LOGGING_ATTR =
		"log_clicks";


	private static final boolean DEFAULT_DO_EXPOSURE_LOGGING = true;
	private static final boolean DEFAULT_DO_CLICK_LOGGING = true;


	///////////////////////////////////////////////////////////
	//
	//  Logging
	//
	////////////////////////////////////////////////////////////

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
	private static boolean shouldDoDebugMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoDebugMsg( kClassName, inFromRoutine );
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


}
