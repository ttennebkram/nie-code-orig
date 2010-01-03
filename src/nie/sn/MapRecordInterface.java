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


interface MapRecordInterface
{


	public int getID();
	public List getTerms();

	public List getURLObjects();
	public int getURLObjectsCount();
	public boolean getHasURLObjects();

	// Get a list of ONLY the Webmaster Suggests URL Objects
	public List getWmsURLObjects();
	// We use this version when gathing URLs from multiple maps, to efficiently
	// come up with a unique list
	public int getWmsURLObjects( List ioURLs, Hashtable ioDupeCheckHash );
	public int getWmsURLObjectsCount();
	public boolean getHasWmsURLs();


	// Get a list of ONLY the REDIRECT URL Objects
	public List getRedirectURLObjects();
	public int getRedirectURLObjectsCount();
	public boolean getHasRedirURLs();

	// Return strings, not objects
	public List getAlternateTerms();
	// We use this version when gathing alt terms from multiple maps, to efficiently
	// come up with a unique list
	public int getAlternateTerms( List ioURLs, Hashtable ioDupeCheckHash );

	// Get the list and ADD it to an existing list, do not add dupes
	// YOU supply the master list and master lookup hash
	// Returns the number added, -1 = hard failure
	// public int getAlternateTerms( List ioMasterList, Hashtable ioMasterHash );

	public int getAlternateTermsCount();
	public boolean getHasAlternateTerms();


	// Return a list of fully populated User Data Records
	public List getUserDataItems();
	// Get the list and ADD it to an existing list, do not add dupes
	// YOU supply the master list and master lookup hash
	// Returns the number added, -1 = hard failure
	// ??? not sure we need this for data items
	// or if it's appropriate, how do you efficiently compare xml trees?
//	public int getUserDataItems( List ioMasterList, Hashtable ioMasterHash )
	public int getUserDataItemsCount();
	public boolean getHasUserDataItems();

	// Will need to generate some XML items
	public Element generateWmsSloganElement();
	public Element generateAltSloganElement();
	public Element [] generateWmsBoxElements();
	public Element generateWmsIconElementOrNull();
	public Element generateWmsDocTitleElementOrNull( SnURLRecord inURL );
	public Element generateWmsDocUrlElementOrNull( SnURLRecord inURL );
	public Element generateWmsDocSummaryElementOrNull( SnURLRecord inURL );


	// Some default values for behavior
	public static final boolean DEFAULT_IS_A_REDIRECT = true;
	public static final boolean DEFAULT_IS_A_SUGGESTION = true;

};

