package nie.sn;

import nie.core.*;
import org.jdom.Element;
import java.util.*;
import java.sql.*;
import nie.sr2.ReportConstants;



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


public class DbMapRecord extends BaseMapRecord
{

	public String kClassName() { return "DbMapRecord"; }
	private static final String kStaticClassName = "DbMapRecord";


//	public SnRedirectRecord( int inMethod, String inURL, String inTerm )
	// public SnMapRecord( String inTerm, Element inElement
	public DbMapRecord( SearchTuningConfig inMainConfig, Element inMainElement, int inID )
		throws MapRecordException
	{
		super( inMainConfig, inMainElement, inID );

		final String kFName = "(DbMapRecord)Constructor";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		// TODO: Moving create/modify logic to THIS CLASS
		// NOTE:
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		// Currently map records in the Database are created
		// and modified in the CreateMapForm class
		// nie.web_ui.xml_screens.CreateMapForm
		

		// cID = JDOMHelper.getIntFromAttribute( inMainElement, "id", -1 );
		if( getID() < 1 )
			throw new MapRecordException( kExTag +
				"Missing or invalid ID."
				);
		debugMsg( kFName, "Constructing map id " + getID() );

		List terms = getTerms();
		if( terms == null || terms.size() < 1 ) {
			String msg = "No target terms in this map."
				+ " Should have at least one term to match on."
				;
			// throw new MapRecordException( kExTag + msg );
			warningMsg( kFName, msg );
		}

		Hashtable modes = getMatchModes();

		/***

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
			String msg = " Empty or NULL URL and alternative term found."
				+ " Must have a URL or alterative term, or user data items."
				+ " getHasURLObjects()=" + getHasURLObjects()
				+ " getHasAlternateTerms()=" + getHasAlternateTerms()
				+ " getHasUserDataItems()=" + getHasUserDataItems()
				;
			// throw new MapRecordException( kExTag + msg );
			warningMsg( kFName, msg );
		}

		***/

//		debugMsg( kFName,
//			"URL=\"" + tmpURL + "\""
//			+ ", alt term=\"" + altTerm + "\""
//			+ " (it's OK for one of them to be null)"
//			);

		// cache other answers????
		// Todo: not yet
	}


	public static DbMapRecord static_getDbMapRecordFromDatabase(
		SearchTuningConfig inMainConfig, int inMapID
		)
			throws MapRecordException
	{
		Element mapElem = new Element( "map" );
		mapElem.setAttribute( "id", ""+inMapID );
	
		return new DbMapRecord( inMainConfig, mapElem, inMapID );
	}



	public List getTerms() {
		if( fTerms == null ) {
			fTerms = static_getTermsForMapID(
				getDBConfig(),
				getID(),
				true
				);
		}
		return fTerms;
	}
	// No exceptions because that's a pain each time
	// but return of null means there's a problem
	// zero item list means it just appears to be empty, and caller
	// can decide if that's acceptable
	public static List static_getTermsForMapID(
		DBConfig inDBConfig, int inMapID, boolean inForceLowerCase
	) {

		final String kFName = "static_getTermsForMapID";

		List outTerms = null;

		// boolean debug = shouldDoDebugMsg( kFName );

		if( inMapID < 1 ) {
			staticErrorMsg( kFName, "Invalid map id " + inMapID + ", returning null." );
			return null;
		}

		// Get the maps from the DB server
		/*** breaks with postgres
		String qry = "SELECT m.id map_id, t.id term_id, t.text_as_entered term" // t.text_normalized term"
			+ " FROM nie_map m,"
			+ " 	nie_term t,"
			+ " 	nie_map_term_assoc mta"
			+ " WHERE m.id = mta.map_id"
			+ " AND t.id = mta.term_id"
			+ " AND m.id = " + inMapID
			+ " ORDER BY text_normalized"
			;
		String qry = "SELECT m.id AS map_id, t.id AS term_id, t.text_as_entered AS term" // t.text_normalized term"
			+ " FROM nie_map AS m,"
			+ " 	nie_term AS t,"
			+ " 	nie_map_term_assoc AS mta"
			+ " WHERE m.id = mta.map_id"
			+ " AND t.id = mta.term_id"
			+ " AND m.id = " + inMapID
			+ " ORDER BY text_normalized"
			;
		***/
		String qry = "SELECT m.id " + inDBConfig.getVendorAliasString() + " map_id"
			+ ", t.id " + inDBConfig.getVendorAliasString() + " term_id"
			+ ", t.text_as_entered " + inDBConfig.getVendorAliasString() + " term" // t.text_normalized term"
			+ " FROM nie_map m,"
			+ " 	nie_term t,"
			+ " 	nie_map_term_assoc mta"
			+ " WHERE m.id = mta.map_id"
			+ " AND t.id = mta.term_id"
			+ " AND m.id = " + inMapID
			+ " ORDER BY text_normalized"
			;
		/***
		String qry = "SELECT nie_map.id, nie_term.id, nie_term.text_as_entered" // t.text_normalized term"
			+ " FROM nie_map,"
			+ " 	nie_term,"
			+ " 	nie_map_term_assoc mta"
			+ " WHERE nie_map.id = nie_map_term_assoc.map_id"
			+ " AND nie_term.id = nie_map_term_assoc.term_id"
			+ " AND nie_map.id = " + inMapID
			+ " ORDER BY nie_term.text_normalized"
			;
		***/
		// TODO: Join with site table and also check for active status from there

		// ResultSet records = inDBConfig.runQueryOrNull( qry );
		Object [] objs = inDBConfig.runQueryOrNull( qry, true );
		// ResultSet mapRecords = getDBConfig().runQuery( qry );
		//if( records == null ) {
		if( null == objs ) {
			staticErrorMsg( kFName,
				"Got back Null results set when querying database."
				);
			return null;
		}
		ResultSet records = (ResultSet) objs[0];
		Statement myStatement = (Statement) objs[1];
		Connection myConnection = (Connection) objs[2];

		int lRowCount = 0;
		int lMapCount = 0;

		try {

			outTerms = new Vector();

			// For each mapping record
			while( records.next() )
			{
				lRowCount++;

				int mapID = records.getInt("map_id");
				// int mapID = records.getInt("nie_map.id");
				// int mapID = records.getInt( "nie_map", "id");
				int termID = records.getInt("term_id");
				// int termID = records.getInt("nie_term.id");
				String term = records.getString("term");
				// String term = records.getString("nie_term.text_as_entered");

				// if(debug) debugMsg( kFName,
				//	"Processing mid/tid/term = " + mapID + " / " + termID + " / \"" + term + "\""
				//	);

				if( inForceLowerCase )
					term = NIEUtil.trimmedLowerStringOrNull(term);
				else
					term = NIEUtil.trimmedStringOrNull(term);
				if( null==term ) {
					staticErrorMsg( kFName,
						"Null/empty term found in map, map id=" + mapID
						+ ", term id=" + termID
						+ " Will continue loading other terms for this map."
						);
					continue;
				}

				outTerms.add(term);

			}

		}
		catch (SQLException e)
		{
			staticErrorMsg( kFName,
				"Error while reading through records: " + e
				+ " Returning null."
				);
			outTerms = null;
		}
		finally {
			records = DBConfig.closeResults( records, kStaticClassName, kFName, false );
			myStatement = DBConfig.closeStatement( myStatement, kStaticClassName, kFName, false );
			// myConnection = DBConfig.closeConnection( myConnection, kStaticClassName, kFName, false );
		}

		return outTerms;
	}

	// No exceptions because that's a pain each time
	// but return of null means there's a problem
	// zero item list means it just appears to be empty, and caller
	// can decide if that's acceptable
	public List getAlternateTerms() {
		if( fAltTerms == null ) {
			fAltTerms = static_getAltTermsForMapID(
				getDBConfig(),
				getID(),
				false	// true
				);
		}
		// May return an empty but non-null hash
		return fAltTerms;
	}


	public static List static_getAltTermsForMapID(
		DBConfig inDBConfig, int inMapID, boolean inForceLowerCase
	) {

		final String kFName = "static_getAltTermsForMapID";

		List outTerms = null;

		// boolean debug = shouldDoDebugMsg( kFName );

		if( inMapID < 1 ) {
			staticErrorMsg( kFName, "Invalid map id " + inMapID + ", returning null." );
			return null;
		}

		// Get the maps from the DB server
		/***
		String qry = "SELECT m.id map_id, r.id alt_term_id,"
			+ " r.text_normalized alt_term,"
			+ " r.text_as_entered display_term,"
			+ " mra.banner_text banner_text"
			+ " FROM nie_map m,"
			+ " 	nie_term r,"
			+ " 	nie_map_relterm_assoc mra"
			+ " WHERE m.id = mra.map_id"
			+ " AND r.id = mra.related_term_id"
			+ " AND m.id = " + inMapID
			+ " ORDER BY mra.sort_order"
			+ ", r.text_normalized"
			;
		String qry = "SELECT m.id AS map_id, r.id AS alt_term_id,"
			+ " r.text_normalized AS alt_term,"
			+ " r.text_as_entered AS display_term,"
			+ " mra.banner_text AS banner_text"
			+ " FROM nie_map AS m,"
			+ " 	nie_term AS r,"
			+ " 	nie_map_relterm_assoc AS mra"
			+ " WHERE m.id = mra.map_id"
			+ " AND r.id = mra.related_term_id"
			+ " AND m.id = " + inMapID
			+ " ORDER BY mra.sort_order"
			+ ", r.text_normalized"
			;
		***/
		String qry = "SELECT m.id " + inDBConfig.getVendorAliasString() + " map_id"
			+ ", r.id " + inDBConfig.getVendorAliasString() + " alt_term_id"
			+ ", r.text_normalized " + inDBConfig.getVendorAliasString() + " alt_term"
			+ ", r.text_as_entered " + inDBConfig.getVendorAliasString() + " display_term"
			+ ", mra.banner_text " + inDBConfig.getVendorAliasString() + " banner_text"
			+ " FROM nie_map m,"
			+ " 	nie_term r,"
			+ " 	nie_map_relterm_assoc mra"
			+ " WHERE m.id = mra.map_id"
			+ " AND r.id = mra.related_term_id"
			+ " AND m.id = " + inMapID
			+ " ORDER BY mra.sort_order"
			+ ", r.text_normalized"
			;


		// TODO: Join with site table and also check for active status from there

		// ResultSet records = inDBConfig.runQueryOrNull( qry );
		Object [] objs = inDBConfig.runQueryOrNull( qry, true );
		// ResultSet mapRecords = getDBConfig().runQuery( qry );
		// if( records == null ) {
		if( null == objs ) {
			staticErrorMsg( kFName,
				"Got back Null results set when querying database."
				);
			return null;
		}


		ResultSet records = (ResultSet) objs[0];
		Statement myStatement = (Statement) objs[1];
		Connection myConnection = (Connection) objs[2];

		int lRowCount = 0;
		int lMapCount = 0;

		try {

			outTerms = new Vector();

			// For each mapping record
			while( records.next() )
			{
				lRowCount++;

				int mapID = records.getInt("map_id");
				int termID = records.getInt("alt_term_id");
				String term = records.getString("alt_term");
				String displayTerm = records.getString("display_term");
				String bannerStr = records.getString("banner_text");


				// if(debug) debugMsg( kFName,
				//	"Processing mid/tid/term = " + mapID + " / " + termID + " / \"" + term + "\""
				//	);

				if( inForceLowerCase )
					term = NIEUtil.trimmedLowerStringOrNull(term);
				else
					term = NIEUtil.trimmedStringOrNull(displayTerm);
				if( null==term ) {
					staticErrorMsg( kFName,
						"Null/empty term found in map, map id=" + mapID
						+ ", alt term id=" + termID
						+ " Will continue loading other terms for this map."
						);
					continue;
				}

				outTerms.add(term);

			}

		}
		catch (SQLException e)
		{
			staticErrorMsg( kFName,
				"Error while reading through records: " + e
				+ " Returning -1."
				);
			outTerms = null;
		}
		finally {
			records = DBConfig.closeResults( records, kStaticClassName, kFName, false );
			myStatement = DBConfig.closeStatement( myStatement, kStaticClassName, kFName, false );
			// myConnection = DBConfig.closeConnection( myConnection, kStaticClassName, kFName, false );
		}

		return outTerms;
	}


	public static int static_getFirstMapIDForTerm( DBConfig inDBConfig, String inTargetTerm ) {
		final String kFName = "static_getFirstMapIDForTerm";
		List ids = static_getMapIDsForTerm( inDBConfig, inTargetTerm );
		if( ids == null ) {
			staticErrorMsg( kFName, "Got back Null ID list.  term=\"" + inTargetTerm + "\"" );
			return -1;
		}
		if( ids.size() > 0 ) {
			Integer idObj = (Integer)ids.get( 0 );
			return idObj.intValue();
		}
		else
		{
			staticErrorMsg( kFName, "Zero length ID list.  term=\"" + inTargetTerm + "\"" );
			return -1;
		}
	}


	public static List static_getMapIDsForTerm( DBConfig inDBConfig, String inTargetTerm ) {
		final String kFName = "getMapIDsForTerm";
		if( null == inDBConfig ) {
			staticErrorMsg( kFName, "Null database configuration passed in." );
			return null;
		}
		inTargetTerm = NIEUtil.trimmedLowerStringOrNull( inTargetTerm );
		if( null == inTargetTerm || inTargetTerm.equals(ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE) ) {
			staticErrorMsg( kFName, "Null/empty term passed in." );
			return null;
		}
		String normalizedTerm = NIEUtil.sqlEscapeString( inTargetTerm, true );
		if( null == normalizedTerm ) {
			staticErrorMsg( kFName, "Null/empty sql-escaped term.  Orig term=\"" + inTargetTerm + "\"");
			return null;
		}


		List outIds = null;

		// Get the maps from the DB server
		/***
		String qry = "SELECT mta.map_id map_id, mta.term_id term_id, t.text_normalized term"
			+ " FROM nie_term t, nie_map_term_assoc mta"
			+ " WHERE mta.term_id = t.id"			+ " AND t.text_normalized = '" + normalizedTerm + "'"
			+ " ORDER BY map_id"
			;
		String qry = "SELECT mta.map_id AS map_id, mta.term_id AS term_id"
			+ ", t.text_normalized AS term"
			+ " FROM nie_term AS t, nie_map_term_assoc AS mta"
			+ " WHERE mta.term_id = t.id"
			+ " AND t.text_normalized = '" + normalizedTerm + "'"
			+ " ORDER BY map_id"
			;
		***/
		String qry = "SELECT mta.map_id " + inDBConfig.getVendorAliasString() + " map_id"
			+ ", mta.term_id " + inDBConfig.getVendorAliasString() + " term_id"
			+ ", t.text_normalized " + inDBConfig.getVendorAliasString() + " term"
			+ " FROM nie_term t, nie_map_term_assoc mta"
			+ " WHERE mta.term_id = t.id"
			+ " AND t.text_normalized = '" + normalizedTerm + "'"
			+ " ORDER BY map_id"
			;
		staticDebugMsg( kFName, "Qry =\"" + qry + "\"" );
		// TODO: Join with site table and also check for active status from there

		// ResultSet records = inDBConfig.runQueryOrNull( qry );
		Object [] objs = inDBConfig.runQueryOrNull( qry, true );

		//if( records == null ) {
		if( null == objs ) {
			staticErrorMsg( kFName,
				"Got back Null results set when querying database."
				);
			return null;
		}
		ResultSet records = (ResultSet) objs[0];
		Statement myStatement = (Statement) objs[1];
		Connection myConnection = (Connection) objs[2];

		int lRowCount = 0;
		try {

			outIds = new Vector();

			// For each mapping record
			while( records.next() )
			{
				lRowCount++;

				int mapID = records.getInt("map_id");
				int termID = records.getInt("term_id");
				String term = records.getString("term");

				// if(debug) debugMsg( kFName,
				//	"Processing mid/tid/term = " + mapID + " / " + termID + " / \"" + term + "\""
				//	);

				if( mapID < 1 ) {
					staticErrorMsg( kFName,
						"Invalid map id=" + mapID
						+ ", term=\"" + inTargetTerm + "\""
						+ ", term id=" + termID
						+ " Will continue loading other terms for this map."
						);
					continue;
				}

				outIds.add( new Integer( mapID ) );
			}
		}
		catch (SQLException e)
		{
			staticErrorMsg( kFName,
				"Error while reading through records: " + e
				+ " Returning null."
				);
			outIds = null;
		}
		finally {
			records = DBConfig.closeResults( records, kStaticClassName, kFName, false );
			myStatement = DBConfig.closeStatement( myStatement, kStaticClassName, kFName, false );
			// myConnection = DBConfig.closeConnection( myConnection, kStaticClassName, kFName, false );
		}

		staticDebugMsg( kFName,
			"Done, outIds"
			+ ( null!=outIds ? " has " + outIds.size() + " items" : " is NULL" )
			);

		return outIds;
	}

	public Hashtable getMatchModes() {
		if( fMatchModes == null ) {
			fMatchModes = static_getMatchModesForMapID(
				getDBConfig(),
				getSearchEngineConfig(),
				getID()
				);
		}
		return fMatchModes;
	}

	// No exceptions because that's a pain each time
	// but return of null means there's a problem
	// zero item list means it just appears to be empty, and caller
	// can decide if that's acceptable
	// Hashkeys are currently always normalized to lower case
	public static Hashtable static_getMatchModesForMapID(
		DBConfig inDBConfig, SearchEngineConfig inSearchConfig, int inMapID
	) {

		final String kFName = "static_getMatchModesForMapID";
		final boolean kCasenKeys = false;

		if( null==inDBConfig ) {
			staticErrorMsg( kFName, "Null DBConfig passed in, returning null." );
			return null;
		}
		if( null==inSearchConfig ) {
			staticErrorMsg( kFName, "Null Search Engine passed in, returning null." );
			return null;
		}

		Hashtable outModes = null;

		// boolean debug = shouldDoDebugMsg( kFName );

		if( inMapID < 1 ) {
			staticErrorMsg( kFName, "Invalid map id " + inMapID + ", returning null." );
			return null;
		}

		String qry = "SELECT owner_id, owner"
			+ ", name, value, sort_order"
			+ " FROM " + DBConfig.META_DATA_TABLE
			+ " WHERE owner = '" + META_DATA_FIELD_MODE_OWNER_NAME + "'"
			+ " AND owner_id = " + inMapID
			+ " ORDER BY sort_order"
			;
		// TODO: Join with site table

		// ResultSet records = inDBConfig.runQueryOrNull( qry );
		Object [] objs = inDBConfig.runQueryOrNull( qry, true );
		// ResultSet mapRecords = getDBConfig().runQuery( qry );
		//if( records == null ) {
		if( null == objs ) {
			staticErrorMsg( kFName,
				"Got back Null results set when querying database."
				);
			return null;
		}
		ResultSet records = (ResultSet) objs[0];
		Statement myStatement = (Statement) objs[1];
		Connection myConnection = (Connection) objs[2];

		// Get the master list of allowed criteria fields
		Collection critFieldNames = inSearchConfig.getSearchFormOptionFieldNames();
		if( null!=critFieldNames ) {
			HashSet tmpNames = new HashSet();
			// It is not case senstive, so everything should be
			// normalized to lower case
			if( ! kCasenKeys ) {
				for( Iterator it=critFieldNames.iterator() ; it.hasNext() ; ) {
					String tmpName = (String) it.next();
					tmpName = tmpName.toLowerCase();
					tmpNames.add( tmpName );
				}
			}
			// They are case sensitive, just add them as we've seen
			else {
				tmpNames.addAll( critFieldNames );
			}
			critFieldNames = tmpNames;
		}

		int lRowCount = 0;
		int lMetaFieldCount = 0;

		try {

			outModes = new Hashtable();

			// For each mapping record
			while( records.next() )
			{
				lRowCount++;

				// int mapID = records.getInt("owner_id");
				// String owner = records.getString("owner");
				String fieldName = records.getString("name");
				String fieldValue = records.getString("value");
				// int sortOrder = records.getInt("sort_order");

				// Obsess about the key
				if( kCasenKeys )
					fieldName = NIEUtil.trimmedStringOrNull( fieldName );
				else
					fieldName = NIEUtil.trimmedLowerStringOrNull( fieldName );
				if( null==fieldName ) {
					staticErrorMsg( kFName,
						"Empty key name for record # " + lRowCount
						+ ", mapID=" + inMapID
						+ "; ignoring this record."
						);
					continue;
				}
				// We should not allow in fields that are no longer defined
				// as criteria / moded fields
				if( null==critFieldNames || ! critFieldNames.contains(fieldName) ) {
					staticErrorMsg( kFName,
						"Error reconciling database match-mode field with declared match-mode fields."
						+ " Database Map ID=" + inMapID
						+ ", orphan field=\"" + fieldName + "\""
						+ ", value=\"" + fieldValue + "\""
						+ ( null==critFieldNames
							? " There currently NO declared match-mode fields."
							: " Currently declared/valid match-mode field(s): " + critFieldNames
							)
						+ " - Perhaps the declarations of match-mode fields was recently changed and this legacy field is no longer defined?"
						+ " This field's match criteria will be ingored."
						);
					continue;
				}

				// Obsess about the value, which in this case
				// CAN BE an empty string
				// And for these we do not trim
				if( null==fieldValue )
					fieldValue = ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE;

				List values = null;
				if( outModes.containsKey(fieldName) ) {
					// Object tmpObj = outModes.get(fieldName);
					// staticStatusMsg( kFName, "tmpObj is a " + tmpObj.getClass().getName() );
					values = (List) outModes.get(fieldName);
				}
				else {
					values = new Vector();
					lMetaFieldCount++;
				}
				values.add( fieldValue );
				outModes.put( fieldName, values );
			}
		}
		catch (SQLException e)
		{
			staticErrorMsg( kFName,
				"Error while reading through records: " + e
				+ " Returning null."
				);
			outModes = null;
		}
		finally {
			records = DBConfig.closeResults( records, kStaticClassName, kFName, false );
			myStatement = DBConfig.closeStatement( myStatement, kStaticClassName, kFName, false );
			// myConnection = DBConfig.closeConnection( myConnection, kStaticClassName, kFName, false );
		}

		return outModes;
	}


	public boolean checkModeMatch( AuxIOInfo inRequestObject ) {
		final String kFName = "checkModeMatch";
		if( null==inRequestObject ) {
			errorMsg( kFName, "Null input request object, returning false." );
			return false;
		}
		Hashtable modes = getMatchModes();
		// No criteria means match all
		if( null==modes || modes.isEmpty() )
			return true;

		// We start by assuming it is a match, until we see otherwise
		boolean isStillAMatch = true;

		// Check all the keys in the criteria hash
		// For each criteria field
		for( Iterator it = modes.keySet().iterator(); it.hasNext() && isStillAMatch ; ) {

			// These field names should already have been normalized
			String criteriaFieldName = (String) it.next();
			List criteriaFieldValues = (List) modes.get( criteriaFieldName );
			// Sanity check, this should be able to happen
			if( null==criteriaFieldValues || criteriaFieldValues.isEmpty() ) {
				errorMsg( kFName, "Null/empty values for criteria field \"" + criteriaFieldName + "\"; there can be no match, so returning false." );
				return false;
			}

			// Get the values from the request
			List queryFieldValues = inRequestObject.getMultivalueCGIField( criteriaFieldName );
			// If the query didn't have values for this
			if( null==queryFieldValues || queryFieldValues.isEmpty() ) {
				// There's a special case where they can allow null
				boolean allowNull =
					criteriaFieldValues.contains( ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE )
					|| criteriaFieldValues.contains( "" )
					|| criteriaFieldValues.contains( " " )
					;
				// return allowNull;
				if( allowNull )
					continue;
				else
					isStillAMatch = false;				
			}
			// So we do have a list
			// Do the two lists overlap with any values?
			// compare each query item to the criteria
			boolean foundValueMatch = false;
			// For each query field
			for( Iterator it2 = queryFieldValues.iterator() ; it2.hasNext() ; ) {
				String queryValue = (String) it2.next();
				// Any match is fine
				if( criteriaFieldValues.contains( queryValue ) ) {
					foundValueMatch = true;
					break;
				}
				// All done, if no matches by now then the test failed
				if( ! foundValueMatch )
					isStillAMatch = false;
			}
		}

		// We're done
		return isStillAMatch;
	}

	public List getURLObjects() {
		if( fUrlObjects == null ) {
			fUrlObjects = static_getURLObjects(
				getMainConfig(),
				getID()
				);
		}
		return fUrlObjects;
	}

	// No exceptions because that's a pain each time
	// but return of null means there's a problem
	// zero item list means it just appears to be empty, and caller
	// can decide if that's acceptable
	public static List static_getURLObjects(
		// DBConfig inDBConfig, int inMapID, boolean inForceLowerCase
		SearchTuningConfig inMainConfig, int inMapID
	) {
		final String kFName = "static_getURLObjects";

		boolean debug = staticShouldDoDebugMsg( kFName );

		if( inMapID < 1 ) {
			staticErrorMsg( kFName, "Invalid map id " + inMapID + ", returning null." );
			return null;
		}
		if( null==inMainConfig ) {
			staticErrorMsg( kFName, "Null app config passed in, returning null." );
			return null;
		}


		DBConfig dbConfig = inMainConfig.getDBConfig();
		if( null==dbConfig ) {
			staticErrorMsg( kFName, "Got null DB config, returning null." );
			return null;
		}

		// Get the URLs from the DB server
		/***
		String qry = "SELECT m.id map_id, u.id url_id,"
			+ " u.type type, u.subtype subtype,"
			+ " u.href_url href, display_url,"
			+ " title, hover_title, u.description description"
			// TODO: Add other advertisement related items
			+ " FROM nie_map m,"
			+ " 	nie_url u,"
			+ " 	nie_map_url_assoc mua"
			+ " WHERE m.id = mua.map_id"
			+ " AND u.id = mua.url_id"
			+ " AND m.id = " + inMapID
			+ " ORDER BY mua.sort_order, url_id"
			;
		String qry = "SELECT m.id AS map_id, u.id AS url_id,"
			+ " u.type AS type, u.subtype AS subtype,"
			+ " u.href_url AS href, display_url,"
			+ " title, hover_title, u.description AS description"
			// TODO: Add other advertisement related items
			+ " FROM nie_map AS m,"
			+ " 	nie_url AS u,"
			+ " 	nie_map_url_assoc AS mua"
			+ " WHERE m.id = mua.map_id"
			+ " AND u.id = mua.url_id"
			+ " AND m.id = " + inMapID
			+ " ORDER BY mua.sort_order, url_id"
			;
		***/
		String qry = "SELECT m.id " + dbConfig.getVendorAliasString() + " map_id"
			+ ", u.id " + dbConfig.getVendorAliasString() + " url_id"
			+ ", u.type " + dbConfig.getVendorAliasString() + " type"
			// + " u.subtype AS subtype,"
			+ ", u.href_url " + dbConfig.getVendorAliasString() + " href_url"
			+ ", display_url"
			+ ", title, hover_title"
			+ ", u.description " + dbConfig.getVendorAliasString() + " description"
			// TODO: Add other advertisement related items
			+ " FROM nie_map m,"
			+ " 	nie_url u,"
			+ " 	nie_map_url_assoc mua"
			+ " WHERE m.id = mua.map_id"
			+ " AND u.id = mua.url_id"
			+ " AND m.id = " + inMapID
			+ " AND ( u.type = " + SnURLRecord.TYPE_WMS
			+ "   OR u.type = " + SnURLRecord.TYPE_REDIRECT + ")"
			+ " ORDER BY mua.sort_order, url_id"
			;
		// TODO: Join with site table and also check for active status from there

		// ResultSet records = dbConfig.runQueryOrNull( qry );
		Object [] objs = dbConfig.runQueryOrNull( qry, true );

		// if( records == null ) {
		if( null==objs ) {
			staticErrorMsg( kFName,
				"Got back Null results set when querying database."
				);
			return null;
		}
		ResultSet records = (ResultSet) objs[0];
		Statement myStatement = (Statement) objs[1];
		Connection myConnection = (Connection) objs[2];

		List outURLs = null;

		int lRowCount = 0;
		int lUrlCount = 0;

		try {

			outURLs = new Vector();

			// For each mapping record
			while( records.next() )
			{
				lRowCount++;

				int mapID = records.getInt("map_id");
				int urlID = records.getInt("url_id");
				int typeCode = records.getInt("type");
				// String subtypeStr = records.getString("subtype");
				String hrefUrl = records.getString("href_url");
				String displayUrl = records.getString("display_url");
				String title = records.getString("title");
				String hoverTitle = records.getString("hover_title");
				String description = records.getString("description");


if(debug) staticDebugMsg( kFName,
	"Processing mid/uid/href = " + mapID + " / " + urlID + " / \"" + hrefUrl + "\""
	+ NIEUtil.NL + "Title: " + title
	);

				// hrefUrl = NIEUtil.trimmedLowerStringOrNull( hrefUrl );
				hrefUrl = NIEUtil.trimmedStringOrNull( hrefUrl );
				if( null==hrefUrl ) {
					staticErrorMsg( kFName,
						"Null/empty term found in map, map id=" + mapID
						+ ", url id=" + urlID
						+ " Will continue loading other urls for this map."
						);
					continue;
				}

				// Create the URL object
				SnURLRecord url = null;
				try {
					// Convert the URL element tree into a full URL object
					url = new SnURLRecord(
						inMainConfig,
						urlID,
						typeCode,
						// subtypeStr,
						hrefUrl,
						displayUrl,
						title,
						hoverTitle,
						description
						);
				}
				catch (SnURLRecordException e) {
					staticErrorMsg( kFName,
						"Unable to load URL # " + lRowCount
						+ " in this map."
						+ " Will continue trying to load remaining items."
						);
					continue;
				}
				// Save it in th elist
				outURLs.add( url );
				lUrlCount++;

			}	// End For each url record
		}
		catch (SQLException e)
		{
			staticErrorMsg( kFName,
				"Error while reading through records: " + e
				+ " Returning -1."
				);
			outURLs = null;
		}
		finally {
			records = DBConfig.closeResults( records, kStaticClassName, kFName, false );
			myStatement = DBConfig.closeStatement( myStatement, kStaticClassName, kFName, false );
			// myConnection = DBConfig.closeConnection( myConnection, kStaticClassName, kFName, false );
		}

//		if( lUrlCount < 1 )
//			staticWarningMsg( kFName,
//				"No URLs associated with map " + inMapID
//				);
		// ^^^ this was bogus as it did not count user data items or alt terms
		staticInfoMsg( kFName,
			"" + lUrlCount + " traditional URLs associated with map " + inMapID
			);


		// Return the answer
		return outURLs;
	}


	// No exceptions because that's a pain each time
	// but return of null means there's a problem
	// zero item list means it just appears to be empty, and caller
	// can decide if that's acceptable
	public static SnURLRecord static_getASpecificURLObject(
		SearchTuningConfig inMainConfig, Connection inDBConn, int inUrlID
		)
			throws SnURLRecordException
	{
		final String kFName = "static_getASpecificURLObject";
		final String kExTag = kStaticClassName + '.' + kFName + ": ";

		if( null==inMainConfig )
			throw new SnURLRecordException( kExTag + "Null application configuration passed in." );
		if( null==inDBConn )
			throw new SnURLRecordException( kExTag + "Null database connection passed in." );
		if( inUrlID < 1 )
			throw new SnURLRecordException( kExTag + "Invalid URL id " + inUrlID + " passed in." );

		DBConfig lDB = inMainConfig.getDBConfig();
		if( null==lDB )
			throw new SnURLRecordException( kExTag + "Got back Null DB config from application config." );


		// Get the URLs from the DB server
		String qry = "SELECT id,"
			+ " type,"
			// + " subtype,"
			+ " href_url, display_url,"
			+ " title, hover_title, description"
			// TODO: Add other advertisement related items
			+ " FROM nie_url"
			+ " WHERE id = " + inUrlID
			;
		// TODO: Join with site table and also check for active status from there


		SnURLRecord outURL = null;
		ResultSet records = null;
		Statement myStatement = null;
		Connection myConnection = null;
		try {

			// records = lDB.runQueryWithConnection( qry, inDBConn );
			Object [] objs = lDB.runQueryWithConnection( qry, inDBConn, true );

			// if( records == null ) {
			if( null==objs )
				throw new SnURLRecordException( kExTag + 
					"Got back Null results set when querying database."
					);

			records = (ResultSet) objs[0];
			myStatement = (Statement) objs[1];
			myConnection = (Connection) objs[2];

			// For each mapping record
			if( ! records.next() ) {
				records = DBConfig.closeResults( records, kStaticClassName, kFName, false );
				throw new SnURLRecordException( kExTag + 
					"Did not find URL record ID=" + inUrlID
					);
			}

			int urlID = records.getInt("id");
			int typeCode = records.getInt("type");
			// String subtypeStr = records.getString("subtype");
			String href = records.getString("href_url");
			String displayUrl = records.getString("display_url");
			String title = records.getString("title");
			String hoverTitle = records.getString("hover_title");
			String description = records.getString("description");

			// href = NIEUtil.trimmedLowerStringOrNull(href);
			href = NIEUtil.trimmedStringOrNull(href);
			if( null==href )
				throw new SnURLRecordException( kExTag +
					"Null/empty href found in url, url id=" + inUrlID
					);

			// Create the URL object
			try {
				// Convert the URL element tree into a full URL object
				outURL = new SnURLRecord(
					inMainConfig,
					urlID,
					typeCode,
					// subtypeStr,
					href,
					displayUrl,
					title,
					hoverTitle,
					description
					);
			}
			catch (SnURLRecordException e2) {
				throw new SnURLRecordException( kExTag +
					"Error instantiating URL ID " + inUrlID + ", Error: " + e2
					);
			}

		}
		catch (Exception e1)
		{
			records = DBConfig.closeResults( records, kStaticClassName, kFName, false );
			throw new SnURLRecordException( kExTag +
				"Unable to load URL ID " + inUrlID + ", Error: " + e1
				);
		}
		finally {
			records = DBConfig.closeResults( records, kStaticClassName, kFName, false );
			myStatement = DBConfig.closeStatement( myStatement, kStaticClassName, kFName, false );
			// myConnection = DBConfig.closeConnection( myConnection, kStaticClassName, kFName, false );
		}

		// Return the answer
		return outURL;
	}
	

	// No exceptions because that's a pain each time
	// but return of null means there's a problem
	// zero item list means it just appears to be empty, and caller
	// can decide if that's acceptable
	public static UserDataItem static_getASpecificAdObject(
		SearchTuningConfig inMainConfig, Connection inDBConn, int inUrlID
		)
			throws SnURLRecordException
	{
		final String kFName = "static_getASpecificAdObject";
		final String kExTag = kStaticClassName + '.' + kFName + ": ";

		if( null==inMainConfig )
			throw new SnURLRecordException( kExTag + "Null application configuration passed in." );
		if( null==inDBConn )
			throw new SnURLRecordException( kExTag + "Null database connection passed in." );
		if( inUrlID < 1 )
			throw new SnURLRecordException( kExTag + "Invalid URL id " + inUrlID + " passed in." );

		DBConfig lDB = inMainConfig.getDBConfig();
		if( null==lDB )
			throw new SnURLRecordException( kExTag + "Got back Null DB config from application config." );


		// Get the URLs from the DB server
		String qry = "SELECT id " + lDB.getVendorAliasString() + " url_id,"
			+ " type,"
			+ " user_class,"
			+ " advertisement_code,"
			// + " subtype,"
			+ " href_url, display_url,"
			+ " title, hover_title, description"
			// TODO: Add other advertisement related items
			+ " FROM nie_url"
			+ " WHERE id = " + inUrlID
			;
		// TODO: Join with site table and also check for active status from there


		// SnURLRecord outURL = null;
		UserDataItem outURL = null;
		ResultSet records = null;
		Statement myStatement = null;
		Connection myConnection = null;
		try {

			// records = lDB.runQueryWithConnection( qry, inDBConn );
			Object [] objs = lDB.runQueryWithConnection( qry, inDBConn, true );

			// if( records == null ) {
			if( null==objs )
				throw new SnURLRecordException( kExTag + 
					"Got back Null results set when querying database."
					);

			records = (ResultSet) objs[0];
			myStatement = (Statement) objs[1];
			myConnection = (Connection) objs[2];

			// For each mapping record
			if( ! records.next() ) {
				records = DBConfig.closeResults( records, kStaticClassName, kFName, false );
				throw new SnURLRecordException( kExTag + 
					"Did not find User data / URL / Ad record ID=" + inUrlID
					);
			}

			String userClassName = records.getString( "user_class" );
			outURL = new UserDataItem( records, userClassName );

			/***
			int urlID = records.getInt("id");
			String adCode = records.getInt("advertisement_code");
			int typeCode = records.getInt("type");
			// String subtypeStr = records.getString("subtype");
			String href = records.getString("href_url");
			String displayUrl = records.getString("display_url");
			String title = records.getString("title");
			String hoverTitle = records.getString("hover_title");
			String description = records.getString("description");

			href = NIEUtil.trimmedLowerStringOrNull(href);
			if( null==href )
				throw new SnURLRecordException( kExTag +
					"Null/empty href found in url, url id=" + inUrlID
					);

			// Create the URL object
			try {
				// Convert the URL element tree into a full URL object
				outURL = new SnURLRecord(
					inMainConfig,
					urlID,
					typeCode,
					// subtypeStr,
					href,
					displayUrl,
					title,
					hoverTitle,
					description
					);
			}
			catch (SnURLRecordException e2) {
				throw new SnURLRecordException( kExTag +
					"Error instantiating URL ID " + inUrlID + ", Error: " + e2
					);
			}
			***/

		}
		catch (Exception e1)
		{
			records = DBConfig.closeResults( records, kStaticClassName, kFName, false );
			throw new SnURLRecordException( kExTag +
				"Unable to load User Data / Ad / URL ID " + inUrlID + ", Error: " + e1
				);
		}
		finally {
			records = DBConfig.closeResults( records, kStaticClassName, kFName, false );
			myStatement = DBConfig.closeStatement( myStatement, kStaticClassName, kFName, false );
			// myConnection = DBConfig.closeConnection( myConnection, kStaticClassName, kFName, false );
		}

		// Return the answer
		return outURL;
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

		if( ioMasterList == null || ioMasterHash == null )
		{
			errorMsg( kFName,
				"One or more null inputs:"
				+ "ioMasterList=" + ioMasterList
				+ ", ioMasterHash=" + ioMasterHash
				);
			return -1;
		}

		List newUrls = getWmsURLObjects();

		int addedCount = -1;
		if( null!=newUrls ) {
			addedCount = 0;
			int urlCount = 0;
			for( Iterator it=newUrls.iterator(); it.hasNext(); ) {
				SnURLRecord newURLObj = (SnURLRecord)it.next();
				urlCount++;
				String newURL = newURLObj.getURL();
				String checkURL = NIEUtil.trimmedLowerStringOrNull( newURL );
				if( null!=checkURL )
					if( ! ioMasterHash.containsKey(checkURL) ) {
						ioMasterList.add( newURLObj );
						ioMasterHash.put( checkURL, newURLObj );
						addedCount++;
					}
				else
					errorMsg( kFName, "Null url # " + urlCount );
			}
		}
		else
			errorMsg( kFName, "Null urls" );

		return addedCount;
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




	// Get the list and ADD it to an existing list, do not add dupes
	// YOU supply the master list and master lookup hash
	// Returns the number added, -1 = hard failure
	public int getAlternateTerms( List ioMasterList, Hashtable ioMasterHash )
	{

		final String kFName = "getAlternateTerms(2)";

		if( ioMasterList == null || ioMasterHash == null )
		{
			errorMsg( kFName,
				"One or more null inputs:"
				+ "ioMasterList=" + ioMasterList
				+ ", ioMasterHash=" + ioMasterHash
				);
			return -1;
		}

		List newTerms = getAlternateTerms();

		int addedCount = -1;
		if( null!=newTerms ) {
			addedCount = 0;
			int termCount = 0;
			for( Iterator it=newTerms.iterator(); it.hasNext(); ) {
				String newTerm = (String)it.next();
				termCount++;
				String checkTerm = NIEUtil.trimmedLowerStringOrNull( newTerm );
				if( null!=checkTerm )
					if( ! ioMasterHash.containsKey(checkTerm) ) {
						ioMasterList.add( newTerm );
						ioMasterHash.put( checkTerm, newTerm );
						addedCount++;
					}
				else
					errorMsg( kFName, "Null term # " + termCount );
			}
		}
		else
			errorMsg( kFName, "Null terms" );

		return addedCount;
	}




	// Return a list of fully populated User Data Records
	public List getUserDataItems()
	{
		final String kFName = "getUserDataItems";

		// If we haven't done this before
		if( fUserDataItems == null ) {
			if( getMainConfig().hasUserClasses() ) {
				// Save the answer
				fUserDataItems = static_getUserDataItems(
					getMainConfig(),
					getID()
					);
			}
		}	// End if list was null

		// Return the answer
		return fUserDataItems;
	}

	public static List static_getUserDataItems(
		SearchTuningConfig inMainConfig, int inMapID
		)
	{
		final String kFName = "static_getUserDataItems";

		if( inMapID < 1 ) {
			staticErrorMsg( kFName, "Invalid map id " + inMapID + ", returning null." );
			return null;
		}
		if( null==inMainConfig ) {
			staticErrorMsg( kFName, "Null app config passed in, returning null." );
			return null;
		}


		DBConfig dbConfig = inMainConfig.getDBConfig();
		if( null==dbConfig ) {
			staticErrorMsg( kFName, "Got null DB config, returning null." );
			return null;
		}

		String qry = "SELECT m.id " + dbConfig.getVendorAliasString() + " map_id"
			+ ", u.id " + dbConfig.getVendorAliasString() + " url_id"
			+ ", u.type " + dbConfig.getVendorAliasString() + " type"
			+ ", u.user_class " + dbConfig.getVendorAliasString() + " user_class"
			+ ", u.href_url " + dbConfig.getVendorAliasString() + " href_url"
			+ ", display_url"
			+ ", title, hover_title"
			+ ", u.description " + dbConfig.getVendorAliasString() + " description"
			+ ", advertisement_code"
			// TODO: Add other advertisement related items
			+ " FROM nie_map m,"
			+ " 	nie_url u,"
			+ " 	nie_map_url_assoc mua"
			+ " WHERE m.id = mua.map_id"
			+ " AND u.id = mua.url_id"
			+ " AND m.id = " + inMapID
			+ " AND u.type = " + SnURLRecord.TYPE_AD
			// + " AND u.user_class = " + getUserAliasClass()
			+ " ORDER BY mua.sort_order, url_id"
			;
		// TODO: Join with site table and also check for active status from there


		// ResultSet records = dbConfig.runQueryOrNull( qry );
		Object [] objs = dbConfig.runQueryOrNull( qry, true );
		// if( records == null ) {
		if( null==objs ) {
			staticErrorMsg( kFName,
				"Got back Null results set when querying database."
				);
			return null;
		}
		ResultSet records = (ResultSet) objs[0];
		Statement myStatement = (Statement) objs[1];
		Connection myConnection = (Connection) objs[2];


		List outItems = null;

		int lRowCount = 0;
		int lItemCount = 0;

		try {

			outItems = new Vector();

			// For each mapping record
			while( records.next() )
			{
				lRowCount++;

				UserDataItem item = null;
				try {
					String userClassName = records.getString( "user_class" );
					// Convert the URL element tree into a full URL object
					item = new UserDataItem( records, userClassName );
				}
				catch (Exception e) {
					staticErrorMsg( kFName,
						"Unable to load User Data Item # " + lRowCount
						+ " in this map."
						+ " Will continue trying to load remaining items."
						+ " Error: " + e
						);
					continue;
				}

				// Save it in th elist
				outItems.add( item );
				lItemCount++;

			}	// End For each url record
		}
		catch (SQLException e)
		{
			staticErrorMsg( kFName,
				"Error while reading through records: " + e
				+ " Returning -1."
				);
			outItems = null;
		}
		finally {
			records = DBConfig.closeResults( records, kStaticClassName, kFName, false );
			myStatement = DBConfig.closeStatement( myStatement, kStaticClassName, kFName, false );
			// myConnection = DBConfig.closeConnection( myConnection, kStaticClassName, kFName, false );
		}

		if( lItemCount < 1 )
			staticDebugMsg( kFName,
				"No user data items associated with map " + inMapID
				);

		// Return the answer
		return outItems;
	}


	public static String genericGetMetaPropertyOrNull(
		String inOnwer, int inObjID, String inPropertyName,
		DBConfig inDBConfig, Connection optConnection
	) {
		final String kFName = "genericGetMetaPropertyOrNull";
		if( null==inOnwer ) {
			staticErrorMsg( kFName, "Null owner name passed in; returning null." );
			return null;
		}
		if( null==inPropertyName ) {
			staticErrorMsg( kFName, "Null property name passed in; returning null." );
			return null;
		}
		if( inObjID < 1 ) {
			staticErrorMsg( kFName, "Invalid object ID " + inObjID + " passed in; returning null." );
			return null;
		}
		if( null==inDBConfig ) {
			staticErrorMsg( kFName, "Null database config passed in; returning null." );
			return null;
		}

		String qry = "SELECT value FROM " + DBConfig.META_DATA_TABLE
			+ " WHERE owner='" + inOnwer + "'"
			+ " AND owner_id=" + inObjID
			+ " AND name='" + inPropertyName + "'"
			+ " ORDER BY sort_order"
			;

		String outStr = null;
		ResultSet results = null;
		Statement myStatement = null;
		Connection myConnection = null;
		try {
			//if( null==optConnection )
			//	results = inDBConfig.runQuery( qry );
			//else
			//	results = inDBConfig.runQueryWithConnection( qry, optConnection );
			Object [] objs = null;
			if( null==optConnection )
				objs = inDBConfig.runQuery( qry, true );
			else
				objs = inDBConfig.runQueryWithConnection( qry, optConnection, true );

			results = (ResultSet) objs[0];
			myStatement = (Statement) objs[1];
			myConnection = (Connection) objs[2];

		
			if( results.next() ) {
				outStr = results.getString( "value" );
				if( results.next() ) {
					staticWarningMsg( kFName,
						"Multiple meta data items found; returning first."
						+ " owner/id/property = " + inOnwer + '/' + inObjID + '/' + inPropertyName
						+ " Keeping first value \"" + outStr + "\""
						+ ", qry=\"" + qry + "\""
						);
					List otherValues = new Vector();
					while(true) {
						String tmpStr = results.getString( "value" );
						if( null!=tmpStr )
							otherValues.add( tmpStr );
						else
							otherValues.add( ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE );
						if( ! results.next() )
							break;
					}
					staticWarningMsg( kFName,
						"Other values = " + otherValues
						);
				}
			}
		}
		catch( Exception e ) {
			staticErrorMsg( kFName, "Error reading meta data; returning null. Error: " + e );
		}
		finally {
			results = DBConfig.closeResults( results, kStaticClassName, kFName, false );
			myStatement = DBConfig.closeStatement( myStatement, kStaticClassName, kFName, false );
			// myConnection = DBConfig.closeConnection( myConnection, kStaticClassName, kFName, false );
		}

		if( null!=outStr )
			staticInfoMsg( kFName,
				"Found owner/id/property(" + inOnwer + ',' + inObjID + ',' + inPropertyName + ")"
				+ " = \"" + outStr + "\""
				);

		return outStr;
	}

	// returns an emtpy list if nothing
	// will always return a list
	// if inKeepNulls is set, null values will be
	// added to the list as INTERNAL_NULL_MARKER
	public static List genericGetMultiMetaProperty(
		String inOnwer, int inObjID, String inPropertyName,
		DBConfig inDBConfig, Connection optConnection,
		boolean inKeepNulls
	) {
		final String kFName = "genericGetMultiMetaPropertyOrNull";

		List outList = new Vector();

		if( null==inOnwer ) {
			staticErrorMsg( kFName, "Null owner name passed in; returning empty list." );
			return outList;
		}
		if( null==inPropertyName ) {
			staticErrorMsg( kFName, "Null property name passed in; returning empty list." );
			return outList;
		}
		if( inObjID < 1 ) {
			staticErrorMsg( kFName, "Invalid object ID " + inObjID + " passed in; empty list." );
			return outList;
		}
		if( null==inDBConfig ) {
			staticErrorMsg( kFName, "Null database config passed in; returning empty list." );
			return outList;
		}

		String qry = "SELECT value FROM " + DBConfig.META_DATA_TABLE
			+ " WHERE owner='" + inOnwer + "'"
			+ " AND owner_id=" + inObjID
			+ " AND name='" + inPropertyName + "'"
			+ " ORDER BY sort_order"
			;

		// String outStr = null;	// see outList above instead
		ResultSet results = null;
		Statement myStatement = null;
		Connection myConnection = null;
		try {
			//if( null==optConnection )
			//	results = inDBConfig.runQuery( qry );
			//else
			//	results = inDBConfig.runQueryWithConnection( qry, optConnection );
			Object [] objs = null;
			if( null==optConnection )
				objs = inDBConfig.runQuery( qry, true );
			else
				objs = inDBConfig.runQueryWithConnection( qry, optConnection, true );

			results = (ResultSet) objs[0];
			myStatement = (Statement) objs[1];
			myConnection = (Connection) objs[2];
		
			while( results.next() ) {
				String tmpStr = results.getString( "value" );
				if( null!=tmpStr )
					outList.add( tmpStr );
				else
					if( inKeepNulls )
						outList.add( ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE );
			}
		}
		catch( Exception e ) {
			staticErrorMsg( kFName, "Error reading meta data; returning null. Error: " + e );
		}
		finally {
			results = DBConfig.closeResults( results, kStaticClassName, kFName, false );
			myStatement = DBConfig.closeStatement( myStatement, kStaticClassName, kFName, false );
			// myConnection = DBConfig.closeConnection( myConnection, kStaticClassName, kFName, false );
		}

		return outList;
	}





	public static void genericSetOrClearSingularMetaProperty(
		String inOnwer, int inObjID,
		String inPropertyName,
		String optPropertyValue,
		DBConfig inDBConfig, Connection optConnection
	) {
		final String kFName = "genericSetOrClearSingularMetaProperty";
		if( null==inOnwer ) {
			staticErrorMsg( kFName, "Null owner name passed in; returning null." );
			return;
		}
		if( null==inPropertyName ) {
			staticErrorMsg( kFName, "Null property name passed in; returning null." );
			return;
		}
		if( inObjID < 1 ) {
			staticErrorMsg( kFName, "Invalid object ID " + inObjID + " passed in; returning null." );
			return;
		}
		if( null==inDBConfig ) {
			staticErrorMsg( kFName, "Null database config passed in; returning null." );
			return;
		}

		// we start by checking the old value
		String previousValueIfAny = genericGetMetaPropertyOrNull(
			inOnwer, inObjID, inPropertyName, inDBConfig, optConnection
			);

		// Normalize the new property, including seeing if it's set to our internal Null
		optPropertyValue = NIEUtil.trimmedStringOrNull( optPropertyValue );
		if( null!=optPropertyValue && optPropertyValue.equals(ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE) )
			optPropertyValue = null;

		// If both null, don't do anything
		if( null==optPropertyValue && null==previousValueIfAny ) {
			staticDebugMsg( kFName,
				"Old and new values were both null, nothing to do."
				);
			return;
		}

		// Unchanged values, still nothing to do
		if( null!=optPropertyValue && null!=previousValueIfAny
			&& previousValueIfAny.equals( optPropertyValue )	
		) {
			staticDebugMsg( kFName,
				"Old and new values were both =\"" + optPropertyValue + "\""
				+ ", nothing to do."
				);
			return;
		}

		String sql = null;
		String debugModeStr = null;

		// Is it a delete?
		if( null==optPropertyValue ) {
			sql = "DELETE FROM "  + DBConfig.META_DATA_TABLE
				+ " WHERE owner='" + inOnwer + "'"
				+ " AND owner_id=" + inObjID
				+ " AND name='" + inPropertyName + "'"
				;
			debugModeStr = "delete";
		}
		// Is it an insert?
		else if( null==previousValueIfAny ) {
			optPropertyValue = NIEUtil.sqlEscapeString( optPropertyValue, true );
			sql = "INSERT INTO " + DBConfig.META_DATA_TABLE
				+ " (owner,owner_id,name,value,sort_order)"
				+ " VALUES ("
				+ "'" + inOnwer + "'"
				+ "," + inObjID
				+ ",'" + inPropertyName + "'"
				+ ",'" + optPropertyValue + "'"
				+ ",1)"
				;
			debugModeStr = "insert";
		}
		// Else it's an update
		else {
			optPropertyValue = NIEUtil.sqlEscapeString( optPropertyValue, true );
			sql = "UPDATE " + DBConfig.META_DATA_TABLE + " SET"
				+ " value='" + optPropertyValue + "'"
				+ ", sort_order=1"
				+ " WHERE owner='" + inOnwer + "'"
				+ " AND owner_id=" + inObjID
				+ " AND name='" + inPropertyName + "'"
				;
			debugModeStr = "update";
		}

		staticDebugMsg( kFName, "sql=\"" + sql + "\"" );

		boolean didit=false;
		try {
			inDBConfig.executeStatementWithConnection(
				sql, optConnection, true
				);
			didit = true;
		}
		catch( Exception e ) {
			staticErrorMsg( kFName, "Error reading meta data; returning null. Error: " + e );
		}

		if( didit )
			staticInfoMsg( kFName,
				"" + debugModeStr + ": owner/id/property(" + inOnwer + ',' + inObjID + ',' + inPropertyName + ")"
				+ " = \"" + optPropertyValue + "\""
				);
	}


	public static void updateMultiMetaProperty(
		String inOnwer, int inObjID,
		String inPropertyName,
		String inPropertyValue,
		DBConfig inDBConfig, Connection optConnection,
		boolean inIsDelete
	) {
		final String kFName = "updateMultiMetaProperty";
		if( null==inOnwer ) {
			staticErrorMsg( kFName, "Null owner name passed in; returning null." );
			return;
		}
		inPropertyName = NIEUtil.sqlEscapeString( inPropertyName, false );
		if( null==inPropertyName ) {
			staticErrorMsg( kFName, "Null property name passed in; returning null." );
			return;
		}
		if( inObjID < 1 ) {
			staticErrorMsg( kFName, "Invalid object ID " + inObjID + " passed in; returning null." );
			return;
		}
		if( null==inDBConfig ) {
			staticErrorMsg( kFName, "Null database config passed in; returning null." );
			return;
		}

		// we start by checking the old value
		// staticStatusMsg( kFName, "A" );
		// String previousValueIfAny = genericGetMetaPropertyOrNull(
		// List previousValuesIfAny = genericGetMultiMetaProperty(
		//	inOnwer, inObjID, inPropertyName, inDBConfig, optConnection, false
		//	);
		// staticStatusMsg( kFName, "B" );
		// ^^^ why do we even care at this point?
		// presumably the caller alreedy figured this out

		// Normalize the new property, including seeing if it's set to our internal Null
		inPropertyValue = NIEUtil.sqlEscapeString( inPropertyValue, false );
		inPropertyValue = (null!=inPropertyValue) ? inPropertyValue : ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE;

		// For each value
		// for( Iterator it = previousValuesIfAny.iterator(); it.hasNext(); ) {

			String sql = null;
			String debugModeStr = null;
	
			// Is it a delete?
			if( inIsDelete ) {
				sql = "DELETE FROM " + DBConfig.META_DATA_TABLE
					+ " WHERE owner='" + inOnwer + "'"
					+ " AND owner_id=" + inObjID
					+ " AND name='" + inPropertyName + "'"
					+ " AND value='" + inPropertyValue + "'"
					;
				debugModeStr = "delete";
			}
			// Is it an insert?
			else {
				sql = "INSERT INTO " + DBConfig.META_DATA_TABLE
					+ " (owner,owner_id,name,value)" // ,sort_order)"
					+ " VALUES ("
					+ "'" + inOnwer + "'"
					+ "," + inObjID
					+ ",'" + inPropertyName + "'"
					+ ",'" + inPropertyValue + "'"
					+ ")"
					//+ ",1)"
					;
				debugModeStr = "insert";
			}
			/***
			// Else it's an update
			else {
				optPropertyValue = NIEUtil.sqlEscapeString( optPropertyValue, true );
				sql = "UPDATE nie_meta_data SET"    + DBConfig.META_DATA_TABLE
					+ " value='" + optPropertyValue + "'"
					+ ", sort_order=1"
					+ " WHERE owner='" + inOnwer + "'"
					+ " AND owner_id=" + inObjID
					+ " AND name='" + inPropertyName + "'"
					;
				debugModeStr = "update";
			}
			***/
	
			staticDebugMsg( kFName, "sql=\"" + sql + "\"" );
	
			boolean didit=false;
			try {
				inDBConfig.executeStatementWithConnection(
					sql, optConnection, true
					);
				didit = true;
			}
			catch( Exception e ) {
				staticErrorMsg( kFName, "Error reading meta data; returning null. Error: " + e );
			}
	
			if( didit )
				staticInfoMsg( kFName,
					"" + debugModeStr + ": owner/id/property(" + inOnwer + ',' + inObjID + ',' + inPropertyName + ")"
					+ " = \"" + inPropertyValue + "\""
					);

		// }	// End for each value


	}






	public String getWmsBoxUserPropertyOrNull( String inPropertyName )
	{
		return genericGetMetaPropertyOrNull(
			BaseMapRecord.WMS_BOX_PATH,
			getID(),
			inPropertyName,
			getDBConfig(), null
			);
	}

	public String getWmsIconUserPropertyOrNull( String inPropertyName )
	{
		return genericGetMetaPropertyOrNull(
			BaseMapRecord.WMS_ICON_PATH,
			getID(),
			inPropertyName,
			getDBConfig(), null
			);
	}
	public String getWmsDocTitleUserPropertyOrNull( String inPropertyName )
	{
		return genericGetMetaPropertyOrNull(
			BaseMapRecord.WMS_DOC_TITLE_PATH,
			getID(),
			inPropertyName,
			getDBConfig(), null
			);
	}
	public String getWmsDocUrlUserPropertyOrNull( String inPropertyName )
	{
		return genericGetMetaPropertyOrNull(
			BaseMapRecord.WMS_DOC_URL_PATH,
			getID(),
			inPropertyName,
			getDBConfig(), null
			);
	}
	public String getWmsDocSummaryUserPropertyOrNull( String inPropertyName )
	{
		return genericGetMetaPropertyOrNull(
			BaseMapRecord.WMS_DOC_SUMMARY_PATH,
			getID(),
			inPropertyName,
			getDBConfig(), null
			);
	}

	public String getWmsSloganUserPropertyOrNull( String inPropertyName )
	{
		return genericGetMetaPropertyOrNull(
			BaseMapRecord.WMS_SLOGAN_PATH,
			getID(),
			inPropertyName,
			getDBConfig(), null
			);
	}

	public String getAltSloganUserPropertyOrNull( String inPropertyName )
	{
		return genericGetMetaPropertyOrNull(
			BaseMapRecord.ALT_SLOGAN_PATH,
			getID(),
			inPropertyName,
			getDBConfig(), null
			);
	}


	/***
	public String getWmsUserSloganFontFace() { return null; }
	public String getWmsUserSloganFontColor() { return null; }
	public String getWmsUserSloganFontSize() { return null; }
	public String getWmsUserSloganStartText() { return null; }
	public String getWmsUserSloganEndText() { return null; }
	public String getWmsUserSloganCssClass() { return null; }

	public String getAltUserSlogan() { return null; }
	public String getAltUserSloganFontFace() { return null; }
	public String getAltUserSloganFontColor() { return null; }
	public String getAltUserSloganFontSize() { return null; }
	public String getAltUserSloganStartText() { return null; }
	public String getAltUserSloganEndText() { return null; }
	public String getAltUserSloganCssClass() { return null; }
	***/

	private static final void __sep__Static_Lookup_Routines__() {}
	////////////////////////////////////////////////////////////////////////

	// Is the term defined and associated with ANY map?
	public static boolean static_isADefinedTerm(
			SearchTuningConfig inMainConfig,
			String inTerm
	) {
		final String kFName = "static_getIsADefinedTerm";
		inTerm = NIEUtil.trimmedLowerStringOrNull( inTerm );
		inTerm = NIEUtil.sqlEscapeString( inTerm, true );
		if( null==inMainConfig ) {
			staticErrorMsg( kFName, "Null config passed in." );
			return false;
		}
		if( null==inTerm ) {
			staticErrorMsg( kFName, "Null/empty input term passed in." );
			return false;
		}
		String qry = "SELECT count(*)"
			+ " FROM nie_term t, nie_map m, nie_map_term_assoc mta"
			+ " WHERE t.id = mta.term_id"
			+ " AND mta.map_id = m.id"
			// + " AND t.text_normalized ='" + inTerm + "'"
			+ " AND t.text_normalized ='" + NIEUtil.sqlEscapeString(inTerm, true) + "'"
			;
		int howMany = inMainConfig.getDBConfig().simpleCountQuery( qry, true, true );
		return howMany > 0;
	}

	public static boolean static_hasWmsOrAltTermsForTerm(
			SearchTuningConfig inMainConfig,
			String inTerm
	) {
		int wms = static_getDefinedWmsCountForTerm( inMainConfig, inTerm );
		if( wms > 0 )
			return true;
		return static_getDefinedAltTermCountForTerm( inMainConfig, inTerm ) > 0;	
	}

	public static boolean static_hasWmsOrAltTermsForMap(
			SearchTuningConfig inMainConfig,
			int inMapID
	) {
		int wms = static_getDefinedWmsCountForMap( inMainConfig, inMapID );
		if( wms > 0 )
			return true;
		return static_getDefinedAltTermCountForMap( inMainConfig, inMapID ) > 0;	
	}



	public static int static_getDefinedWmsCountForTerm(
			SearchTuningConfig inMainConfig,
			String inTerm
	) {
		final String kFName = "static_getDefinedWmsCountForTerm";
		inTerm = NIEUtil.trimmedLowerStringOrNull( inTerm );
		inTerm = NIEUtil.sqlEscapeString( inTerm, true );
		if( null==inMainConfig ) {
			staticErrorMsg( kFName, "Null config passed in." );
			return -1;
		}
		if( null==inTerm ) {
			staticErrorMsg( kFName, "Null/empty input term passed in." );
			return -1;
		}
		String qry = "SELECT count(*)"
			+ " FROM nie_term t, nie_map m, nie_map_term_assoc mta, nie_url u, nie_map_url_assoc mua"
			// Join maps to terms
			+ " WHERE m.id = mta.map_id"
			+ " AND t.id = mta.term_id"
			// Join maps to URLs
			+ " AND m.id = mua.map_id"
			+ " AND u.id = mua.url_id"
			// Apply term and URL filters
			+ " AND t.text_normalized ='" + NIEUtil.sqlEscapeString(inTerm, true) + "'"
			+ " AND (u.type = " + SnURLRecord.TYPE_WMS + " OR u.type = " + SnURLRecord.TYPE_REDIRECT + " )"
			;
		int howMany = inMainConfig.getDBConfig().simpleCountQuery( qry, true, true );
		return howMany;
	}

	public static int static_getDefinedWmsCountForMap(
			SearchTuningConfig inMainConfig,
			int inMapID
	) {
		final String kFName = "static_getDefinedWmsCountForMap";
		if( null==inMainConfig ) {
			staticErrorMsg( kFName, "Null config passed in." );
			return -1;
		}
		if( inMapID < 1 ) {
			staticErrorMsg( kFName, "Invalid map ID passed in:" + inMapID );
			return -1;
		}
		String qry = "SELECT count(*)"
			// + " FROM nie_term t, nie_map m, nie_map_term_assoc mta, nie_url u, nie_map_url_assoc mua"
			+ " FROM nie_map m, nie_url u, nie_map_url_assoc mua"
			// Join maps to terms
			// + " WHERE m.id = mta.map_id"
			// + " AND t.id = mta.term_id"
			// Join maps to URLs
			// + " AND m.id = mua.map_id"
			+ " WHERE m.id = mua.map_id"
			+ " AND u.id = mua.url_id"
			// Apply term and URL filters
			// + " AND t.text_normalized ='" + NIEUtil.sqlEscapeString(inTerm, true) + "'"
			+ " AND m.id =" + inMapID
			+ " AND (u.type = " + SnURLRecord.TYPE_WMS + " OR u.type = " + SnURLRecord.TYPE_REDIRECT + " )"
			;
		int howMany = inMainConfig.getDBConfig().simpleCountQuery( qry, true, true );
		return howMany;
	}



	public static int static_getDefinedAltTermCountForTerm(
			SearchTuningConfig inMainConfig,
			String inTerm
	) {
		final String kFName = "static_getDefinedAltTermCountForTerm";
		inTerm = NIEUtil.trimmedLowerStringOrNull( inTerm );
		inTerm = NIEUtil.sqlEscapeString( inTerm, true );
		if( null==inMainConfig ) {
			staticErrorMsg( kFName, "Null config passed in." );
			return -1;
		}
		if( null==inTerm ) {
			staticErrorMsg( kFName, "Null/empty input term passed in." );
			return -1;
		}
		String qry = "SELECT count(*)"
			+ " FROM nie_term t, nie_map m, nie_map_term_assoc mta, nie_term rt, nie_map_relterm_assoc mra"
			// Join maps to terms
			+ " WHERE m.id = mta.map_id"
			+ " AND t.id = mta.term_id"
			// Join maps to alt terms
			+ " AND m.id = mra.map_id"
			+ " AND rt.id = mra.related_term_id"
			// Apply the term filter
			+ " AND t.text_normalized ='" + NIEUtil.sqlEscapeString(inTerm, true) + "'"
			;
		int howMany = inMainConfig.getDBConfig().simpleCountQuery( qry, true, true );
		return howMany;
	}

	public static int static_getDefinedAltTermCountForMap(
			SearchTuningConfig inMainConfig,
			int inMapID
	) {
		final String kFName = "static_getDefinedAltTermCountForMap";
		if( null==inMainConfig ) {
			staticErrorMsg( kFName, "Null config passed in." );
			return -1;
		}
		if( inMapID < 1 ) {
			staticErrorMsg( kFName, "Invalid map ID passed in:" + inMapID );
			return -1;
		}
		String qry = "SELECT count(*)"
			// + " FROM nie_term t, nie_map m, nie_map_term_assoc mta, nie_term rt, nie_map_relterm_assoc mra"
			+ " FROM nie_map m, nie_term rt, nie_map_relterm_assoc mra"
			// Join maps to terms
			// + " WHERE m.id = mta.map_id"
			// + " AND t.id = mta.term_id"
			// Join maps to alt terms
			// + " AND m.id = mra.map_id"
			+ " WHERE m.id = mra.map_id"
			+ " AND rt.id = mra.related_term_id"
			// Apply the term filter
			// + " AND t.text_normalized ='" + NIEUtil.sqlEscapeString(inTerm, true) + "'"
			+ " AND m.id =" + inMapID
			;
		int howMany = inMainConfig.getDBConfig().simpleCountQuery( qry, true, true );
		return howMany;
	}

	public static Set static_getUserDataClassNamesForTerm(
			SearchTuningConfig inMainConfig,
			String inTerm
	) {
		final String kFName = "static_getUserDataClassesForTerm";
		inTerm = NIEUtil.trimmedLowerStringOrNull( inTerm );
		inTerm = NIEUtil.sqlEscapeString( inTerm, true );
		if( null==inMainConfig ) {
			staticErrorMsg( kFName, "Null config passed in." );
			return null;
		}
		if( null==inTerm ) {
			staticErrorMsg( kFName, "Null/empty input term passed in." );
			return null;
		}
		String qry = "SELECT DISTINCT user_class"
			+ " FROM nie_term t, nie_map m, nie_map_term_assoc mta, nie_url u, nie_map_url_assoc mua"
			// Join maps to terms
			+ " WHERE m.id = mta.map_id"
			+ " AND t.id = mta.term_id"
			// Join maps to URLs
			+ " AND m.id = mua.map_id"
			+ " AND u.id = mua.url_id"
			// Apply term and URL filters
			+ " AND t.text_normalized ='" + NIEUtil.sqlEscapeString(inTerm, true) + "'"
			+ " AND u.type = " + SnURLRecord.TYPE_AD
			+ " AND user_class IS NOT NULL"
			;

		Object [] objs = inMainConfig.getDBConfig().runQueryOrNull( qry, true );
		if( null==objs ) {
			staticErrorMsg( kFName,
				"Got back Null results set when querying database."
				);
			return null;
		}
		ResultSet records = (ResultSet) objs[0];
		Statement myStatement = (Statement) objs[1];
		Connection myConnection = (Connection) objs[2];

		Set outItems = new HashSet();

		try {
			// For each mapping record
			while( records.next() ) {
				String className = records.getString( "user_class" );
				outItems.add( className );
			}	// End For each url record
		}
		catch (SQLException e)
		{
			staticErrorMsg( kFName,
				"Error while reading through records: " + e
				+ " Returning -1."
				);
			outItems = null;
		}
		finally {
			records = DBConfig.closeResults( records, kStaticClassName, kFName, false );
			myStatement = DBConfig.closeStatement( myStatement, kStaticClassName, kFName, false );
			// myConnection = DBConfig.closeConnection( myConnection, kStaticClassName, kFName, false );
		}

		// It's fine if there are none

		// Return the answer
		return outItems;
	}

	public static Set static_getUserDataClassNamesForMap(
			SearchTuningConfig inMainConfig,
			int inMapID
	) {
		final String kFName = "static_getUserDataClassesForTerm";
		if( null==inMainConfig ) {
			staticErrorMsg( kFName, "Null config passed in." );
			return null;
		}
		if( inMapID < 1 ) {
			staticErrorMsg( kFName, "Invalid map ID passed in:" + inMapID );
			return null;
		}
		String qry = "SELECT DISTINCT user_class"
			// + " FROM nie_term t, nie_map m, nie_map_term_assoc mta, nie_url u, nie_map_url_assoc mua"
			+ " FROM nie_map m, nie_url u, nie_map_url_assoc mua"
			// Join maps to terms
			// + " WHERE m.id = mta.map_id"
			// + " AND t.id = mta.term_id"
			// Join maps to URLs
			// + " AND m.id = mua.map_id"
			+ " WHERE m.id = mua.map_id"
			+ " AND u.id = mua.url_id"
			// Apply term and URL filters
			// + " AND t.text_normalized ='" + NIEUtil.sqlEscapeString(inTerm, true) + "'"
			+ " AND m.id = " + inMapID
			+ " AND u.type = " + SnURLRecord.TYPE_AD
			+ " AND user_class IS NOT NULL"
			;

		Object [] objs = inMainConfig.getDBConfig().runQueryOrNull( qry, true );
		if( null==objs ) {
			staticErrorMsg( kFName,
				"Got back Null results set when querying database."
				);
			return null;
		}
		ResultSet records = (ResultSet) objs[0];
		Statement myStatement = (Statement) objs[1];
		Connection myConnection = (Connection) objs[2];

		Set outItems = new HashSet();

		try {
			// For each mapping record
			while( records.next() ) {
				String className = records.getString( "user_class" );
				outItems.add( className );
			}	// End For each url record
		}
		catch (SQLException e)
		{
			staticErrorMsg( kFName,
				"Error while reading through records: " + e
				+ " Returning -1."
				);
			outItems = null;
		}
		finally {
			records = DBConfig.closeResults( records, kStaticClassName, kFName, false );
			myStatement = DBConfig.closeStatement( myStatement, kStaticClassName, kFName, false );
			// myConnection = DBConfig.closeConnection( myConnection, kStaticClassName, kFName, false );
		}

		// It's fine if there are none

		// Return the answer
		return outItems;
	}



	private static final void __sep__Simple_Set_Get__() {}
	////////////////////////////////////////////////////////////////////////


	DBConfig getDBConfig() {
		return getMainConfig().getDBConfig();
	}
	SearchEngineConfig getSearchEngineConfig() {
		return getMainConfig().getSearchEngine();
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

	static final boolean staticErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}
	static final boolean staticWarningMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().warningMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}
	static final boolean staticInfoMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().infoMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}
	static final boolean staticDebugMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().debugMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}
	static final boolean staticShouldDoDebugMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoDebugMsg( kStaticClassName, inFromRoutine );
	}
	static final boolean staticStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}


	// Some caching of list values
	// These are populated as needed, vs specific cache fills

	// The ID of this map object
	int _cID;

	// The master hash of matching terms
	private List fTerms;
	// additioanl match conditions, by site specific field name and value
	private Hashtable fMatchModes;
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

	public static final String META_DATA_FIELD_MODE_OWNER_NAME = "moded_field_match";

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

