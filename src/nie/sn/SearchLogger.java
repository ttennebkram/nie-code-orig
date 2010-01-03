package nie.sn;

import java.util.*;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import org.jdom.Element;
import nie.core.*;

public class SearchLogger
{
	private final static String kClassName = "SearchLogger";

	public SearchLogger( Element inElement,
		SearchTuningConfig inSNConfig
		)
		throws SearchLoggerException
	{
		final String kFName = "constructor";
		final String kExTag = kClassName + ':' + kFName + ": ";

		// create jdom element and store info
		// Sanity checks
		if( inElement == null )
			throw new SearchLoggerException( kExTag
				+ "Constructor was passed in a NULL element."
				);

		fInitConfig = inSNConfig;

		// Instantiate and store the main JDOMHelper a
		fConfigTree = null;
		try
		{
			fConfigTree = new JDOMHelper( inElement );
		}
		catch (JDOMHelperException e)
		{
			throw new SearchLoggerException( kExTag
				+ "Got JDOMHelper Exception: " + e
				);
		}
		if( fConfigTree == null )
			throw new SearchLoggerException( kExTag
				+ "Got back a NULL xml tree when trying to create"
				+ " a Search Logger object."
				);

		// We need to convert the static array of field names into
		// usable structures
		// Throws SearchLoggerException if it is unhappy
		initSqlFieldInfo();

		// We only bother to instantiate the database settings if we're
		// actually going to use them; in testing, we may not use it,
		// usually we WOULD use it.
		if( shouldUseDatabase() )
		{
			if( null == getDBConfig() ) {
				String tmpMsg = "Unable to instantiate database configuration."
					+ " This may be caused by a simple database configuration error."
					+ " Or perhaps a database has not been configured."
					;

				if( shouldFailOnDBConfigError() )
				{
					throw new SearchLoggerException( kExTag, tmpMsg );
				}
				else
				{
					errorMsg( kFName, tmpMsg );
					errorMsg( kFName,
						"*** Even though the database is not reachable"
						+ ", we will still allow the Search Names Server"
						+ " to process searches, so the site will REMAIN UP."
						+ " However searches will likely NOT be logged. ***"
						);
				}
			}

			/***
			// Now check for, instantiate, and store the dbconfig section
			// This uses the new JDOMHelper object-by-path factory
			try
			{
				fDBConf = (DBConfig)fConfigTree.makeObjectFromConfigPath(
					"nie.core.DBConfig", DB_CONFIG_PATH
					);
			}
			catch(Exception dbe)
			{
				String tmpMsg = "Unable to instantiate database configuration."
					+ " This may be caused by a simple database configuration error."
					+ " Reason/Exception = \"" + dbe + "\""
					;

				if( shouldFailOnDBConfigError() )
				{
					throw new SearchLoggerException( kExTag, tmpMsg );
				}
				else
				{
					errorMsg( kFName, tmpMsg );
					errorMsg( kFName,
						"*** Even though the database is not reachable"
						+ ", we will still allow the Search Names Server"
						+ " to process searches, so the site will REMAIN UP."
						+ " However searches will likely NOT be logged. ***"
						);
				}
			}
			***/
		}   // End if using a database connection

		// Force us to read all get methods once, and store results
		reinitFieldCache();

		// Check that a couple critical items exist
		String tmpStr;
		int tmpInt;

		// Must have patterns if proxy
		tmpStr = getDocsFoundPattern();
		if( tmpStr == null
			// && doesSearchLoggingRequireProxy() && shouldDoSearchLogging()
			&& doesSearchLoggingRequireScraping() && shouldDoSearchLogging()
			)
			throw new SearchLoggerException( kExTag,
				"Must specifiy pattern to locate document count in proxied docs."
				+ " This is the " + DOCS_FOUND_PATTERN_PATH + " element."
				);

	}


	// Force us to read all get methods once, and store results
	private void reinitFieldCache()
	{
		// Get everything in to the cache
		// First, specifically turn caching off (should be false anyway via Java init)
		mUseCache = false;

		// WARNING: All your routines need to handle the case where
		// the DB config is gone

		// Todo: this is caching data, which means it won't survive a reinit???
		// or will they reference a new object anyway???

		// Now call each getter once, populates cached values
		shouldUseDatabase();
		shouldFailOnDBConfigError();
		// doesSearchLoggingRequireProxy();
		doesSearchLoggingRequireScraping();
		alwaysProxy();
		getLogTableName();
		getDocsFoundPattern();
		getNoDocsFoundPattern();
		getMaxDocsFoundLookAhead();
		doDocsFoundMatchBoth();
		getMaxDocsFoundSearchedLookAhead();
		getConfiguredSiteID();
		// Things NOT to log
		shouldIgnoreNullSearches();
		getIgnoreClientAddresses();
		// Now turn caching on
		mUseCache = true;
	}


	private void initSqlFieldInfo()
		throws SearchLoggerException
	{
		final String kFName = "initSqlFieldInfo";
		final String kExTag = kClassName + ':' + kFName + ": ";

		if( fSqlFieldNameToStatementIntegerMap != null ||
			fSqlFieldNameList != null
			)
		{
			// warningMsg( kFName,
			infoMsg( kFName,
				"Tables have already been initialized, returning."
				+ " This is normal for a server refresh."
				);
			return;
		}

		// Init our hash and list
		fSqlFieldNameToStatementIntegerMap = new Hashtable();
		// fSqlFieldAssignedMap = new Hashtable();
		fSqlFieldNameList = new Vector();

		// Main loop, for each field defined in the static array
		int i;  // We'd like i to remain in scope after the loop
		for( i=0; i < kSqlFields.length ; i++ )
		{
			// The field name
			String fieldName = kSqlFields[i];
			// Normalize
			fieldName = NIEUtil.trimmedStringOrNull( fieldName );
			// Sanity check
			if( fieldName == null )
				throw new SearchLoggerException( kExTag
					+ "Field # " + (i+1) + " is null/empty."
					);
			// Now get the lower case version for the hash
			String key = fieldName.toLowerCase();
			// If it's already in the hash, we are very unhappy
			if( fSqlFieldNameToStatementIntegerMap.containsKey( key ) )
				throw new SearchLoggerException( kExTag
					+ "Field # " + (i+1) + " \"" + fieldName + "\""
					+ " is a duplicate, which is not allowed."
					);

			// Create an object to store, one-based
			Integer value = new Integer( i + 1 );
			// Boolean boolValue = new Boolean( false );

			// Store the data
			// Original field name goes into the list
			fSqlFieldNameList.add( fieldName );
			// Normalized key and the integer object goes into the hash
			fSqlFieldNameToStatementIntegerMap.put( key, value );
			// And whether or not we have assigned it a value
			// fSqlFieldAssignedMap.put( key, boolValue );
			// ^^^ We now do this elsewhere

		}   // End for each field in static array

		// i will be == length now
		debugMsg( kFName, "Added " + i + " fields." );

	}

	private Hashtable createNewHasBeenAssignedHash()
	{
		final String kFName = "createNewHasBeenAssignedHash";

		Hashtable outAssignedMap = new Hashtable();

		// Main loop, for each field defined in the static array
		int i;  // We'd like i to remain in scope after the loop
		for( i=0; i < kSqlFields.length ; i++ )
		{
			// The field name
			String fieldName = kSqlFields[i];
			// Normalize
			String key = NIEUtil.trimmedLowerStringOrNull( fieldName );
			// Sanity check
			if( key == null )
			{
//				throw new SearchLoggerException( kExTag
//					+ "Field # " + (i+1) + " is null/empty."
//					);
				errorMsg( kFName,
					"Field # " + (i+1) + " is null/empty, skipping."
					);
				continue;
			}
			// Create an object to store, one-based
			Boolean boolValue = new Boolean( false );

			// Store the data
			outAssignedMap.put( key, boolValue );

		}   // End for each field in static array

		// i will be == length now
		debugMsg( kFName, "Set " + i + " fields." );

		return outAssignedMap;
	}


	private List findUnassignedFields( Hashtable inIsAssignedMap )
	{
		final String kFName = "findUnassignedFields";
		List fields = new Vector();
		if( inIsAssignedMap == null )
		{
			errorMsg( kFName,
				"Field list has not been initialized, returning zero length list."
				);
			return fields;
		}
		List keys = new Vector( inIsAssignedMap.keySet() );
		for( Iterator it = keys.iterator(); it.hasNext(); )
		{
			String key = (String) it.next();
			Boolean obj = (Boolean) inIsAssignedMap.get( key );
			if( ! obj.booleanValue() )
				fields.add( key );
		}
		return fields;
	}

	private static void ___Sep__Higher_Level_Logic__(){}
	//////////////////////////////////////////////////////////////

	////////////////////////////////////////////////////////////
	//
	//      Higher Level Methods
	//
	/////////////////////////////////////////////////////////////


	// Log a transaction
	public boolean logTransaction(
		AuxIOInfo inRequestObj,         // Request
		AuxIOInfo inResponseObj,        // Response
		int inTransactionType,			// or -1 to let us figure it out
		String inQuery,                 // Query
		int inSNActionCode,             // int inSNActionCode,
		int inSNActionCount,            // # of high level actions taken
		int inSNActionItemCount,        // # of units of into sent
		int inSNStatusCode,             // int inSNStatusCode,
		String inSNStatusMsg,           // String inSNStatusMsg,
		boolean inFromDirectLogAction,   // inFromDirectLogAction
		boolean inWasFromRedirect,       // Did the engine just do directed URL redirect
		boolean inFromSnippetServeAction
		)
			throws SearchLoggerException,
				SQLException,
				DBConfigException
				//, DBConfigInServerReconnectWait
	{

		// Log a transaction
		return logTransaction(
			inRequestObj,         // Request
			inResponseObj,        // Response
			inTransactionType,			// or -1 to let us figure it out
			inQuery,                 // Query
			inSNActionCode,             // int inSNActionCode,
			inSNActionCount,            // # of high level actions taken
			inSNActionItemCount,        // # of units of into sent
			inSNStatusCode,             // int inSNStatusCode,
			inSNStatusMsg,           // String inSNStatusMsg,
			inFromDirectLogAction,   // inFromDirectLogAction
			inWasFromRedirect,       // Did the engine just do directed URL redirect
			inFromSnippetServeAction,
			null, null, null
			);
	}


	// Log a transaction
	public boolean logTransaction(
		AuxIOInfo inRequestObj,         // Request
		AuxIOInfo inResponseObj,        // Response
		int inTransactionType,			// or -1 to let us figure it out
		String inQuery,                 // Query
		int inSNActionCode,             // int inSNActionCode,
		int inSNActionCount,            // # of high level actions taken
		int inSNActionItemCount,        // # of units of into sent
		int inSNStatusCode,             // int inSNStatusCode,
		String inSNStatusMsg,           // String inSNStatusMsg,
		boolean inFromDirectLogAction,   // inFromDirectLogAction
		boolean inWasFromRedirect,       // Did the engine just do directed URL redirect
		boolean inFromSnippetServeAction,
		String inAdKey,
		String inAdURL,
		String inAdGraphicURL
		)
			throws SearchLoggerException,
				SQLException,
				DBConfigException
				//, DBConfigInServerReconnectWait
	{
		final String kFName = "logTransaction";
		final String kExTag = kClassName + ':' + kFName + ": ";

		boolean debug = shouldDoDebugMsg( kFName );

		// We ALWAYS require a Request object
		// And if we're proxying, we also require a response object
		if( inRequestObj == null )
		{
			throw new SearchLoggerException( kExTag,
				"Null request input, can not log event."
				);
		}

		if( ! shouldLogThisSearch( inRequestObj, inQuery ) ) {
			if(debug) debugMsg( kFName, "Told to not log this event." );
			return true;
		}
		
		if( getDBConfig() == null )
		{
			if( ! fHaveIssuesBrokenWarning )
			{
				errorMsg( kFName,
					"The database is NOT configured, so we can not log events."
					+ " To avoid filling up your log file, this is the ONLY time"
					+ " we will issue this error message."
					+ " ******* So Logging is NOT WORKING!!!! *******"
					+ " Please check earlier log errors and your configuration."
					);
				fHaveIssuesBrokenWarning = true;
			}
			return false;
		}

		if(debug) debugMsg( kFName,
			"Logging Transaction, inFromDirectLogAction=" + inFromDirectLogAction
			);

		// ***
		// Interesting stuff
		// ResultSetMetaData getMetaData()
		// Gets the number, types and properties of a ResultSet object's columns.

		// Get a new, empty statement to fill in, for this table
		// This also gets the table definition from the Table Def class factory
		// ACTIVITY_LOG_TABLE_NAME must LINE UP with a schema definition
		// in the system dir
		// DBUpdateStmt statement = new DBUpdateStmt(
		//	ACTIVITY_LOG_TABLE_NAME, getDBConfig()
		//	);
		DBUpdateStmt statement = new DBUpdateStmt(
			DBConfig.LOG_TABLE, getDBConfig()
			);


		if(debug) debugMsg( kFName, "Setting up variables." );

		// Here we test the waters, to see if we can get a connection
		// If we can't, no sense going further
		// this is particularly true if we had a previous error and are
		// in the midst of a retry connection wait interval
		// other exceptions are allow to bubble up
		try
		{
			statement.cacheConnection();
		}
		catch( DBConfigInServerReconnectWait wait1 )
		{
			errorMsg( kFName,
				"There was a problem executing the update,"
				+ " or perhaps the database connection is down"
				+ "; this may be temporary (1)."
				+ " This particular search will not be logged."
				+ " Error: " + wait1
				);
			return false;
		}


		// The basic transaction info, site ID, transaction ID, referring URL, etc.
		long [] tmpAry = populateBasicTransactionInfo(
			statement,
			inRequestObj,
			inTransactionType,
			inFromDirectLogAction
			);
		long actualTransactionID = tmpAry[0];
		int actualTransactionType = (int) (tmpAry[1]);


		// Information about the Search
		populateSearchInfo(
			statement,
			inRequestObj,
			inResponseObj,
			inQuery,
			inFromDirectLogAction,
			inWasFromRedirect,
			inFromSnippetServeAction
			);

		// A lot of stuff about Search Names
		// Actions taken, error flags, etc
		populateSearchNamesStatusInfoIncomplete(
			statement,
			inRequestObj,
			inFromDirectLogAction,
			inSNStatusCode,
			inSNActionCount,
			inSNActionItemCount,
			inSNStatusMsg
			);

		// Fields that We don't fully Implement yet
		// We DO implement some, so do keep calling these routines
		///////////////////////////////////////////////////////////

		// User Info
		populateUserInfoIncomplete(
			statement,
			inRequestObj
			);

		// Advertising Info
		populateAdvertisementInfo(
			statement,
			inRequestObj,
			inFromDirectLogAction,
			inAdKey,
			inAdURL,
			inAdGraphicURL
			);

		// Information about click throughs
		populateClickSpecificInfoIncomplete(
			statement,
			inRequestObj
			);

		// The date and time items
		populateDateTimeInfoIncomplete(
			statement,
			inRequestObj
			);

		// Done populating the statement
		///////////////////////////////////////////////////////
		if(debug) debugMsg( kFName,
			"Have populate statement."
			);

		// Send the updates

		// USUALLY, if there's a problem, we get a _SQL_ Exception
		// which we let flow upwards, like all other errors.
		// However, a DBConfigException indicates that the connection
		// is temporarily down, so we should not freak out as loudly
		int numRows = -1;
		boolean badError = false;
		Exception badException = null;
		try
		{
			// send the update, WITH a specific commit
			// TODO: could change this to an option, but I'm not sure I buy
			// the auto commit propoganda in the dc, in particular if the DB crashes
			numRows = statement.sendUpdate( true );
		}
		// This exception means we are just not trying right now
		// typically this would be caught above
		catch( DBConfigInServerReconnectWait wait2 )
		{
			errorMsg( kFName,
				"There was a problem executing the update,"
				+ " or perhaps the database connection is down"
				+ "; this may be temporary (2)."
				+ " This particular search will not be logged."
				+ " Error: " + wait2
				);
			return false;
		}
		// These two exceptions are bad
		// A SQL error may just be that the DB is down and worth
		// trying again later
		catch(SQLException sqle1)
		{
			// Since some retry-worthy errors come out as SQLExceptions
			// we also have to seed that logic for this exception
			errorMsg( kFName,
				"Got an exeception executiong the prepared statement"
				+ ", reporting to central database object."
				+ " Error: " + sqle1
				);
			getDBConfig().reportError();
			return false;
			// badError = true;
			// badException = sqle1;
		}
		// This is really bad
		// let's go ahead and let it be thrown
		// catch(DBConfigException dbe)
		// {
		//	// This is just plain bad!
		//	badError = true;
		//	badException = dbe;
		// }

		// the above creates and closes the actual database statement

		if(debug) debugMsg( kFName, "Finishing up." );

		// Also log things like moded search to nie_log_meta  LOG_META_TABLE
		logMetaData( inRequestObj, actualTransactionID, actualTransactionType );

		// The above throws SQLException, but we also double check the results
		if( numRows != 1 )
		{
			warningMsg( kFName,
				"Insert statement did not return expected row count of 1;"
				+ " actual count = " + numRows
				);
			return false;
		}
		else
		{
			// debugMsg( kFName, "Ran SQL \"" + sqlStr + "\"" );
			return true;
		}
	}






	// Log a transaction
	public boolean logMetaData(
		AuxIOInfo inRequestObj,         // Request
		long inTransactionID,
		int inTransactionType
		)
			throws SearchLoggerException,
				SQLException,
				DBConfigException
				//, DBConfigInServerReconnectWait
	{
		final String kFName = "logMetaData";
		final String kExTag = kClassName + ':' + kFName + ": ";

		// Quick escape, we currently only log meta data for searches
		if( TRANS_TYPE_SEARCH != inTransactionType )
			return false;

		if( getDBConfig() == null )
		{
			if( ! fHaveIssuesBrokenWarning )
			{
				errorMsg( kFName,
					"The database is NOT configured, so we can not log events."
					+ " To avoid filling up your log file, this is the ONLY time"
					+ " we will issue this error message."
					+ " ******* So Logging is NOT WORKING!!!! *******"
					+ " Please check earlier log errors and your configuration."
					);
				fHaveIssuesBrokenWarning = true;
			}
			return false;
		}

		boolean debug = shouldDoDebugMsg( kFName );

		// We ALWAYS require a Request object
		// And if we're proxying, we also require a response object
		if( null == inRequestObj )
		{
			throw new SearchLoggerException( kExTag,
				"Null request input, can not log event."
				);
		}

		if( inTransactionID < 1 )
		{
			throw new SearchLoggerException( kExTag,
				"Invalid transaction ID " + inTransactionID + ", can not log event."
				);
		}

		boolean everythingOK = true;

		// If we have configured modal fields
		Collection critFieldNames = getMainConfig().getSearchEngine().getSearchFormOptionFieldNames();
		if( null!=critFieldNames && ! critFieldNames.isEmpty() ) {
			Hashtable fieldModes = new Hashtable();

			// For each criteria field
			for( Iterator fit=critFieldNames.iterator(); fit.hasNext() ; ) {
				String critFieldName = (String) fit.next();
				// List cgiValues = inRequestObject.getMultivalueCGIField( critFieldName );
				// We really need to know even about blank/empty values
				List cgiValues = inRequestObj.getMultivalueCGIField_UnnormalizedValues( critFieldName );

				// If we have some values
				if( null!=cgiValues && ! cgiValues.isEmpty() ) {
					int valCounter = 0;

					// For each value
					for( Iterator vit=cgiValues.iterator(); vit.hasNext() ; ) {
						String value = (String) vit.next();
						valCounter++;
						// if( value.equals("") )
						//	value = nie.sr2.ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE;

						// <<== from here
	// <<< =========== set back indent
	DBUpdateStmt statement = new DBUpdateStmt(
			DBConfig.LOG_META_TABLE, getDBConfig()
			);

	if(debug) debugMsg( kFName, "Setting up variables." );

	// Basic info

	// SEARCH_TRANSACTION_NUMBER
	statement.setValue( "search_transaction_number", inTransactionID );
	// OWNER = moded_field_match
	statement.setValue( "owner", DbMapRecord.META_DATA_FIELD_MODE_OWNER_NAME );
	// LOGGED_ORDER
	statement.setValue( "logged_order", valCounter );

	// The actual data
	statement.setValue( "field_name", critFieldName );
	statement.setValue( "field_value", value );

	// Some description of the data
	String fieldDesc = getMainConfig().getSearchEngine().getFormOptionFieldDescOrNull( critFieldName );
	if( null!=fieldDesc )
		statement.setValue( "field_description", fieldDesc );
	String valueDesc = getMainConfig().getSearchEngine().getFormOptionFieldValueDescOrNull( critFieldName, value );
	if( null!=valueDesc )
		statement.setValue( "value_description", valueDesc );

	// Done populating the statement
	if(debug) debugMsg( kFName,
		"Have populate statement."
		);

	// Send the updates

	// USUALLY, if there's a problem, we get a _SQL_ Exception
	// which we let flow upwards, like all other errors.
	// However, a DBConfigException indicates that the connection
	// is temporarily down, so we should not freak out as loudly
	int numRows = -1;
	boolean badError = false;
	Exception badException = null;
	try
	{
		// send the update, WITH a specific commit
		// Todo: could change this to an option, but I'm not sure I buy
		// the auto commit propoganda in the dc, in particular if the DB crashes
		numRows = statement.sendUpdate( true );
	}
	// This exception means we are just not trying right now
	// typically this would be caught above
	catch( DBConfigInServerReconnectWait wait2 )
	{
		errorMsg( kFName,
			"There was a problem executing the update,"
			+ " or perhaps the database connection is down"
			+ "; this may be temporary (2)."
			+ " This particular search will not be logged."
			+ " Error: " + wait2
			);
		// return false;
		everythingOK = false;
		return false;	// We DO want to bail immediately

	}
	// These two exceptions are bad
	// A SQL error may just be that the DB is down and worth
	// trying again later
	catch(SQLException sqle1)
	{
		// Since some retry-worthy errors come out as SQLExceptions
		// we also have to seed that logic for this exception
		errorMsg( kFName,
			"Got an exeception executiong the prepared statement"
			+ ", reporting to central database object."
			+ " Error: " + sqle1
			);
		getDBConfig().reportError();

		everythingOK = false;
		return false;	// We DO want to bail immediately

		// return false;
		// badError = true;
		// badException = sqle1;
	}
	// This is really bad
	// let's go ahead and let it be thrown
	// catch(DBConfigException dbe)
	// {
	//	// This is just plain bad!
	//	badError = true;
	//	badException = dbe;
	// }

	// the above creates and closes the actual database statement

	// if(debug) debugMsg( kFName, "Finishing up." );

	// The above throws SQLException, but we also double check the results
	if( numRows != 1 )
	{
		warningMsg( kFName,
			"Insert statement did not return expected row count of 1;"
			+ " actual count = " + numRows
			);
		// return false;
		everythingOK = false;
	}
	else
	{
		// debugMsg( kFName, "Ran SQL \"" + sqlStr + "\"" );
		// return true;
		// leave everythingOK alone
	}


	// >>> =========== set indent back out
						// ==>> back to here

					}	// End for each CGI value
				}	// End if there were CGI values for this criteria
			}	// End for each criteria field
		}	// End if we have configured modal fields

		// We're done
		return everythingOK;

	}







	// The basic transaction info, site ID, transaction ID, referring URL, etc.
	// Returns array with the transaction number and type
	/////////////////////////////////////////////////////////////////////
	private long [] populateBasicTransactionInfo(
		DBUpdateStmt ioStatement,
		AuxIOInfo inRequestObj,
		int inTransactionType,
		boolean inFromDirectLogAction
		)
			throws DBConfigException
	{
		final String kFName = "populateBasicTransactionInfo";

		// WARNING:
		// You've gotta keep these field names in sync with a couple
		// other places:
		// 1: The database
		// 2: Our XML schema
		// 3: the code that creates Click-Through links
		
		// Site ID
		int lSiteID = getConfiguredSiteID();
		Integer lSiteIDObj = lSiteID > 0 ? new Integer( lSiteID ) : null ;
		if( lSiteID <= 0 )
			warningMsg( kFName,
				"No default site ID configured."
				);
		// Set the value
		ioStatement.setValueFromCGIOrDefault(
			SITE_ID_DB_FIELD,
			inRequestObj,
			lSiteIDObj,
			false,	// inOverwriteOK,
			false,	// inAutoGenerateDefaultCGIFieldName,
			null,	// optAutoGeneratePrefix,
			null,	// optAutoGenerateSuffix,
			null,	// optOverrideWithSpecificCGIFieldName,
			true,	// inErrorOnNullCGIObject,
			false,	// inErrorOnMissingCGIField
			false	// inErrorOnAlsoMissingDefaultValue
			);
			
		// SEARCH_TRANSACTION_NUMBER
		long lSearchTransNum = inRequestObj.getTransactionID();
		if( lSearchTransNum >= 0 )
			ioStatement.setValue( "search_transaction_number", lSearchTransNum );

		// REFERRING_TRANSACTION_NUMBER
		ioStatement.setValueFromCGI_MissingIsOK(
			REF_ID_DB_FIELD,
			inRequestObj
			// , REF_ID_CGI_FIELD
			);

		// TRANSACTION_TYPE
		// A search event, a log event, a docview event, a user feedback event
		// Todo: support other types, figure out dynamically
		int lTransType = -1;
		if( ! inFromDirectLogAction )
		{
			if( inTransactionType > 0 )
				lTransType = inTransactionType;
			else
				lTransType = TRANS_TYPE_SEARCH;
		}
		else
			lTransType = TRANS_TYPE_UNKNOWN;

		/***
		ioStatement.setValueFromCGIOrDefault(
			TRANS_TYPE_DB_FIELD,
			inRequestObj,
			new Integer( lTransType ),
			false
			);
        ***/
		ioStatement.setValueFromCGIOrDefault(
		        TRANS_TYPE_DB_FIELD, // String inDBFieldName,
		        inRequestObj, // AuxIOInfo inCGIData,
		        new Integer( lTransType ), // Object optDefaultValue,
				false	// boolean inOverwriteOK,
				);
//				false, // boolean inAutoGenerateDefaultCGIFieldName,
//				null, // String optAutoGeneratePrefix,
//				null, // String optAutoGenerateSuffix,
//				TRANS_TYPE_CGI_FIELD, // String optOverrideWithSpecificCGIFieldName,
//				true, // boolean inErrorOnNullCGIObject,
//				false, // boolean inErrorOnMissingCGIField,
//				false // boolean inErrorOnAlsoMissingDefaultValue
//				);

		// REFERER
		String lRefererURI = inRequestObj.getReferer();
		debugMsg( kFName, "lRefererURI=" + lRefererURI );
		if( lRefererURI != null )
			ioStatement.setValue( "referer", lRefererURI );

		// Return the transaction ID and type we wound up logging
		return new long [] {
			lSearchTransNum,
			(long)lTransType
		};
	}


	// Information about the Search
	// including query, num matched, num searched, etc
	// We need the response object in case we have to parse the
	// results list for the fields
	private void populateSearchInfo(
		DBUpdateStmt ioStatement,
		AuxIOInfo inRequestObj,
		AuxIOInfo inResponseObj,
		String inQuery,
		boolean inFromDirectLogAction,
		boolean inWasFromRedirect,
		boolean inFromSnippetServeAction
		)
			throws DBConfigException
	{
		final String kFName = "populateSearchInfo";

		// ORIGINAL_QUERY
		String lOriginalQuery = inQuery;

		// We now support sentinels / sentinals that represent a NULL search
		// So look for those sentinels, and force to null
		if( getMainConfig().isNullSearch(lOriginalQuery) )
			lOriginalQuery = null;
		
		// NORMALIZED_QUERY
		// Don't whine about nulls
		String lNormalizedQuery = normalizeString(
			lOriginalQuery, false
			);
		// Log them, if present
		if( lOriginalQuery != null )
			ioStatement.setValue( ORIGINAL_SEARCH_DB_FIELD_NAME, lOriginalQuery );
		if( lNormalizedQuery != null )
			ioStatement.setValue( NORMALIZED_SEARCH_DB_FIELD_NAME, lNormalizedQuery );


		// FORM_NAME, cgi is "sn_form", no it's now "nie_form_name"
		ioStatement.setValueFromCGI_MissingIsOK(
		    SUBMITTED_FORM_DB_FIELD_NAME,
		    inRequestObj
		    // , SUBMITTED_FORM_CGI_FIELD_NAME
			);


		// NUM_RESULTS
		// NUM_SEARCHED
		// These are more complicated
		// If we are actually doing a search, we need to parse
		// the results page for this info
		// String _lNumFoundDBFieldName = "num_results";
		// String _lNumSearchedFieldDBName = "num_searched";

		// Start getting our answers together
		int numFound = -1;
		int numSearched = -1;
		if( inRequestObj.getIsGoogleOneBoxSnippetRequest() )
		{
			// setup a temp request object
			AuxIOInfo newRequest = new AuxIOInfo();
			Hashtable origSearchFields = getMainConfig().getSearchEngine().getSearchEngineTestDriveURLFields();
			// Make a copy for us to mess with
			Hashtable miscSearchFields = new Hashtable();
			miscSearchFields.putAll( origSearchFields );
			// String requestedStyle = miscSearchFields.containsKey( SnRequestHandler.GOOGLE_STYLE_SHEET_CGI_FIELD )
			//	? (String) miscSearchFields.get( SnRequestHandler.GOOGLE_STYLE_SHEET_CGI_FIELD )
			//	: null ;
			String requestedGSAFrontEnd = miscSearchFields.containsKey( SnRequestHandler.GOOGLE_FRONT_END_CGI_FIELD )
				? (String) miscSearchFields.get( SnRequestHandler.GOOGLE_FRONT_END_CGI_FIELD )
				: null ;
			// We need to be careful to not start an infinite request loop
			// TODO: Use the NIE no action CGI variables instead, when verified to be working
			// if( null==requestedStyle || ! requestedStyle.equalsIgnoreCase( SnRequestHandler.GOOGLE_STYLE_SHEET_DEFAULT_VALUE ) )
			if( null==requestedGSAFrontEnd || ! requestedGSAFrontEnd.equalsIgnoreCase( SnRequestHandler.GOOGLE_FRONT_END_DEFAULT_VALUE ) )
			{
				// We want the DEFAULT sheet which presumably would NOT use OneBox modules
				// miscSearchFields.put( SnRequestHandler.GOOGLE_STYLE_SHEET_CGI_FIELD,
				//		SnRequestHandler.GOOGLE_STYLE_SHEET_DEFAULT_VALUE
				//		);
				miscSearchFields.put( SnRequestHandler.GOOGLE_FRONT_END_CGI_FIELD,
						SnRequestHandler.GOOGLE_FRONT_END_DEFAULT_VALUE
						);
				newRequest.addOnlyMissingValuesToHashes( miscSearchFields );
				String queryField = getMainConfig().getSearchEngine().getQueryField();
				newRequest.addCGIField( queryField, inQuery );

				// We want to retain information about the search results
				AuxIOInfo intermediateIoInfo = new AuxIOInfo();

				try {
					// Try to fetch
					// Clarification on AuxIOInfo objects
					// * newRequest is a simulated end-user query object, from end-client to searchtrack
					// * intermediateIoInfo is the scratchpad for response settings from host search engine
					//   searchtrack to search engine
					String doc = nie.sn.SnRequestHandler.staticDoActualSearch(
							inQuery, getMainConfig().getSearchEngine(), newRequest, intermediateIoInfo, null
							);

					// Double check content type / mime type
					// TODO: last 2 could be turned down from warning to info/status if it
					// comes up a lot, as it might parse OK, but for now we'd like to know about it and people
					// will not generally turn on higher levels
					String contentType = intermediateIoInfo.getContentType();
					if( null==contentType )
						warningMsg( kFName, "Null content-type / mime-type for term '" + inQuery + "'" );
					else if( ! contentType.startsWith("text/") )
						warningMsg( kFName, "Binary content-type / mime-type '" + contentType + "' for term '" + inQuery + "'" );
					else if( contentType.startsWith("text/xml") )
						warningMsg( kFName, "XML content-type / mime-type - but still using text parsing, which is probably still OK - for term '" + inQuery + "'" );
					else if( ! contentType.startsWith("text/html") )
						warningMsg( kFName, "Non HTML/XML content-type / mime-type '" + contentType + "' - which is unusual but may still parse OK - for term '" + inQuery + "'" );

					debugMsg( kFName, "Parse Try 1" );
					List tmpResults = nie.sr2.util.BackfillMatchCounts.parseResults_static( doc, getMainConfig().getSearchLogger(), false, false ); // boolean inUseGoogleInstead, do warnings )
					if( null!=tmpResults && ! tmpResults.isEmpty() )
					{
						if( tmpResults.size() >= 1 )
							numFound = ( (Integer) tmpResults.get(0) ).intValue();
						if( tmpResults.size() >= 2 )
							numSearched = ( (Integer) tmpResults.get(1) ).intValue();
					}
					// Try again with hard coded Google rules
					if( numFound < 0 )
					{
						debugMsg( kFName, "Parse Try 2" );

						tmpResults = nie.sr2.util.BackfillMatchCounts.parseResults_static( doc, getMainConfig().getSearchLogger(), true, true ); // boolean inUseGoogleInstead, do warnings )
						if( null!=tmpResults && ! tmpResults.isEmpty() )
						{
							if( tmpResults.size() >= 1 )
								numFound = ( (Integer) tmpResults.get(0) ).intValue();
							if( tmpResults.size() >= 2 )
								numSearched = ( (Integer) tmpResults.get(1) ).intValue();
						}					
					}
					if( numFound < 0 )
						warningMsg( kFName, "Unable to get doc counts for OneBox term '" + inQuery + "'" );
				}
				catch( Exception e ) {
					errorMsg( kFName, "Error getting OneBox doc counts: " + e );
				}
			}
			// Else skipped due to potential for an infinite loop
			else {
				warningMsg( kFName, "Skipping backfill of Google OneBox result counts because Google default is being used."
					+ " GSA='" + requestedGSAFrontEnd + "'"
					+ " Search config vendor='" + getMainConfig().getSearchEngine().getVendor() + "'"
					+ " " + SnRequestHandler.GOOGLE_FRONT_END_CGI_FIELD + "='" + SnRequestHandler.GOOGLE_FRONT_END_DEFAULT_VALUE + "'"
					);
			}
		}
		// Else not OneBox, is it something else?
		else if( inFromDirectLogAction || inFromSnippetServeAction )
		{
		    debugMsg( kFName, "direct log or snippet: looking at CGI fields." );
			numFound = ioStatement.getIntFromCGIOrDefault(
			        NUM_FOUND_DB_FIELD_NAME,
			        inRequestObj, -1
				);
			numSearched = ioStatement.getIntFromCGIOrDefault(
			        NUM_SEARCHED_DB_FIELD_NAME,
			        inRequestObj, -1
				);
		}
		else    // Else we need to parse it
		{
			// We can only parse doc counts if we did NOT do a
			// search term to specific URL redirect.
			// In the case of a redirect, we never talked to the
			// search engine, so would have nothing to parse!
			if( ! inWasFromRedirect && null != inResponseObj )
			{
			    debugMsg( kFName, "will parse" );

				String doc = null;
				if( inResponseObj != null )
					doc = inResponseObj.getContent();
				// We will get a 1 or 2 element vector of Integer objects
				// We don't obsess about warnings here, the parsing routine
				// generates plenty of them
				// they will also whine about a null doc
				List counts = parseValuesFromDocument( doc );
				// If we have values, record them
				if( counts != null && counts.size() >= 1 )
				{
					// Record the first value
					Integer obj1 = (Integer) counts.get(0);
					numFound = obj1.intValue();
					// If there's a second value, record it also
					if( counts.size() >= 2 )
					{
						Integer obj2 = (Integer) counts.get(1);
						numSearched = obj2.intValue();
					}
				}
			}
			else    // Else it IS from a redirect
			{
				// We count this as one of each
				// Per Miles
				numFound = 1;
				numSearched = 1;
			}
		}
		infoMsg( kFName,
			"numFound=" + numFound + ", numSearched=" + numSearched
			);

		// Conditionally log them
		if( numFound >= 0 )
			ioStatement.setValue( NUM_FOUND_DB_FIELD_NAME, numFound );
		if( numSearched >= 0 )
			ioStatement.setValue( NUM_SEARCHED_DB_FIELD_NAME, numSearched );

	}



	// A lot of stuff about Search Names
	// Actions taken, error flags, etc
	private void populateSearchNamesStatusInfoIncomplete(
		DBUpdateStmt ioStatement,
		AuxIOInfo inRequestObj,
		boolean inFromDirectLogAction,
		int inSNStatusCode,
		int inSNActionCount,
		int inSNActionItemCount,
		String inSNStatusMsg
		)
			throws DBConfigException
	{
		final String kFName = "populateSearchNamesStatusInfoIncomplete";

		// First get the base code
		// The undefined values are always negative (< 0)
		int lActionCode = SearchTuningConfig.SN_ACTION_CODE_UNDEFINED;
		int lSNStatusCode = SearchTuningConfig.SN_STATUS_UNDEFINED;
		int lSNActionCount = -1;
		int lSNActionItemCount = -1;
		String lSNStatusMsg = null;

		// IMPORTANT NOTE:
		// These field names are also
		// used in SnRequestHandler.addSearchTrackDirectLoggingCGIFields

		// / SN_ACTION_CODE_DB_FIELD =
		//	"search_names_action_code";
		// / SN_ACTION_COUNT_DB_FIELD =
		//	"search_names_action_count";

		//SN_ACTION_DETAILS_DB_FIELD =
		//	"search_names_action_details";

		// / SN_PROBLEM_FLAG_DB_FIELD =
		//	"search_names_problem_flag";
		// / SN_PROBLEM_CODE_DB_FIELD =
		//	"search_names_problem_code";
		// / SN_PROBLEM_MSG_DB_FIELD


		// If we're processing a direct log event, we get these
		// from the CGI
		if( inFromDirectLogAction )
		{
			lActionCode = ioStatement.getIntFromCGIOrDefault(
			        SN_ACTION_CODE_DB_FIELD,
			        inRequestObj, -1
				);
			lSNStatusCode = ioStatement.getIntFromCGIOrDefault(
			        SN_PROBLEM_CODE_DB_FIELD,
			        inRequestObj, -1
				);
			lSNActionCount = ioStatement.getIntFromCGIOrDefault(
			        SN_ACTION_COUNT_DB_FIELD,
			        inRequestObj, -1
				);
			// lSNActionItemCount = ioStatement.getIntFromCGIOrDefault(
			//	"sn_item_count", inRequestObj, -1
			//	);

			/***
			lSNStatusMsg = ioStatement.getStringFromCGIOrNull(
				SN_PROBLEM_MSG_DB_FIELD, inRequestObj
				);
			***/
		}
		// Else we're doing proxy, should have been passed in to us
		else
		{
			// Just copy them from the input
			// lActionCode = inActionCode;
			lSNStatusCode = inSNStatusCode;
			lSNActionCount = inSNActionCount;
			lSNActionItemCount = inSNActionItemCount;
			lSNStatusMsg = inSNStatusMsg;
		}

		// Some values we derive

		// -1 means we don't know, and we should put a null
		// this is important - in theory we won't have -1's in the DB,
		// and this value distinguishes them from a real 0

		// If any action was taken, then this is a YES
		int lWasSNActionTakenInt = -1;
		if( lActionCode >= 0 )
			if( lActionCode > SearchTuningConfig.SN_ACTION_CODE_NONE )
				lActionCode = 1;
			else
				lActionCode = 0;


		// Almost any tremor in the force indicates the presence of
		// a search names term; whether it was PRESENTED to the user
		// is another matter, for the status codes and action counts
		int lWasAnSNTerm = -1;
		if( lWasSNActionTakenInt >= 0 || lSNStatusCode >= 0)
		{
			if(
					(lWasSNActionTakenInt > 0)
					|| (lSNStatusCode > SearchTuningConfig.SN_STATUS_OK)
					|| (lSNActionCount >= 0)
					|| (lSNActionItemCount >= 0)
				)
			{
				lWasAnSNTerm = 1;
			}
			else
				lWasAnSNTerm = 0;
		}
		// We treat the question of whether or not there was a problem
		// as a separate field from the actual problem code
		int lWasThereAnSNProblem = -1;
		if( lSNStatusCode >= 0)
		{
			if( lSNStatusCode == SearchTuningConfig.SN_STATUS_OK )
			{
				lWasThereAnSNProblem = 0;
				// We DON'T log a status code if there was no problem
				lSNStatusCode = -1;
			}
			else
			{
				lWasThereAnSNProblem = 1;
			}
		}

		// Record the values

		// WAS_SEARCH_NAMES_TERM
		infoMsg( kFName, "lWasAnSNTerm=" + lWasAnSNTerm );
		if( lWasAnSNTerm >= 0 )
			ioStatement.setValue( "was_search_names_term", lWasAnSNTerm );


//		// WAS_SEARCH_NAMES_ACTION_TAKE
//		if( lWasSNActionTakenInt >= 0 )
//			ioStatement.setValue( "was_search_names_action_taken", lWasSNActionTakenInt );

		// SEARCH_NAMES_ACTION_CODE
		if( lActionCode >= 0 )
			ioStatement.setValue( SN_ACTION_CODE_DB_FIELD, lActionCode );

		// SEARCH_NAMES_ACTION_COUNT
		if( lSNActionCount >= 0 )
			ioStatement.setValue( SN_ACTION_COUNT_DB_FIELD, lSNActionCount );

		// SEARCH_NAMES_PROBLEM_FLAG
		if( lWasThereAnSNProblem >= 0 )
			ioStatement.setValue( SN_PROBLEM_FLAG_DB_FIELD, lWasThereAnSNProblem );

		// SEARCH_NAMES_PROBLEM_CODE
		if( lSNStatusCode >= 0 )
			ioStatement.setValue( SN_PROBLEM_CODE_DB_FIELD, lSNStatusCode );

		// SEARCH_NAMES_PROBLEM_MESSAGE
		if( null != lSNStatusMsg ) {
			// ioStatement.setValue( SN_PROBLEM_MSG_DB_FIELD, lSNStatusMsg );
			infoMsg( kFName, "Skipping the recodring of unimplemented field prob msg: " + lSNStatusMsg );
		}
			
	}

	// User Info
	private void populateUserInfoIncomplete(
		DBUpdateStmt ioStatement,
		AuxIOInfo inRequestObj
		)
			throws DBConfigException
	{
		final String kFName = "populateUserInfoIncomplete";

		// CLIENT_HOST
		String lClientIP = inRequestObj.getIPAddress();
		if( inRequestObj.getIsGoogleOneBoxSnippetRequest() )
		{
			// The IP address is passed to us by Google, vs the IP
			// address of the actual Google appliance
			lClientIP = inRequestObj.getScalarCGIFieldTrimOrNull(
				SnRequestHandler.GOOGLE_ONEBOX_CGI_CLIENT_IP_FIELD );
			if( null==lClientIP )
				warningMsg( kFName,
					"Google appliance did not pass in the IP address of the user"
					+ ", please update the OneBox configuration to enable this."
					);
		}

		// lClientIP = NIEUtil.sqlEscapeString( lClientIP, false );
		// It's OK to call with a Java null!
		if( lClientIP != null )
			// ioStatement.setValue( "client_host", lClientIP );
			ioStatement.setValue( CLIENT_IP_DB_FIELD_NAME, lClientIP );

		// Unimplemented stuff
		////////////////////////////////////
		// "extra_search_parameters"
		// "user_name"
		// "user_domain"

	}

	// Advertising Info
	private void populateAdvertisementInfo(
		DBUpdateStmt ioStatement,
		AuxIOInfo inRequestObj,
		boolean inFromDirectLogAction,
		String inAdKey,
		String inAdURL,
		String inAdGraphicURL
		)
			throws DBConfigException
	{
		final String kFName = "populateAdvertisementInfoIncomplete";

		// TODO: all unimplemented at this time


		if( inFromDirectLogAction )
		{
			// Advertising Info
			// "advertisement_code_sent"
			ioStatement.setValueFromCGI_MissingIsOK(
				ADVERTISEMENT_ID_DB_FIELD,
				inRequestObj
				// , ADVERTISEMENT_ID_CGI_FIELD
				);
	
			// "advertisement_href"
			ioStatement.setValueFromCGI_MissingIsOK(
				ADVERTISEMENT_URL_DB_FIELD,
				inRequestObj
				// , ADVERTISEMENT_URL_CGI_FIELD
				);	
	
			// "advertisement_graphic_url"
			ioStatement.setValueFromCGI_MissingIsOK(
				ADVERTISEMENT_IMAGE_URL_DB_FIELD,
				inRequestObj
				// , ADVERTISEMENT_IMAGE_URL_CGI_FIELD
				);	
		}
		else
		{
			if( null != inAdKey )
				ioStatement.setValue( ADVERTISEMENT_ID_DB_FIELD, inAdKey );

			if( null != inAdURL )	
				ioStatement.setValue( ADVERTISEMENT_URL_DB_FIELD, inAdURL );	
	
			if( null != inAdGraphicURL )
				ioStatement.setValue(
					ADVERTISEMENT_IMAGE_URL_DB_FIELD, inAdGraphicURL
					);	
		}


		// Todo:
		// Also some tie-in when we finish
		// populateClickSpecificInfoIncomplete()

	}


	// Information about click throughs
	private void populateClickSpecificInfoIncomplete(
		DBUpdateStmt ioStatement,
		AuxIOInfo inRequestObj
		)
			throws DBConfigException
	{
		final String kFName = "populateClickSpecificInfoIncomplete";

		// Todo: unimplemented

		// More info about the click
		// "type_of_document_click"
		// "user_feedback"

		// About the document's ranking
		// "rank_in_results_list" or in list of advertisements
		// "rank_in_results_on_this_page"

		// Not in AOPA table
		// "results_list_page_number"

	}


	private void populateDateTimeInfoIncomplete(
		DBUpdateStmt ioStatement,
		AuxIOInfo inRequestObj
		)
			throws DBConfigException
	{
		final String kFName = "populateDateTimeInfoIncomplete";

		// Needs formatting with Java JDBC
		// START_TIME
		// END_TIME
		// Todo: for now, just grab the current date and time
		// Later we should look in cgi
		java.sql.Timestamp currentTime = new Timestamp(
			System.currentTimeMillis()
			);
		// start_time START_TIME_DB_FIELD_NAME
		// ioStatement.setValue( "start_time", currentTime );
		ioStatement.setValue( START_TIME_DB_FIELD_NAME, currentTime );
		// end_time END_TIME_DB_FIELD_NAME
		ioStatement.setValue( END_TIME_DB_FIELD_NAME, currentTime );
		// ioStatement.setValue( "end_time", currentTime );
		// Different format
		// Date currentTime = new Date();
//		statement.setDate( getIntForFieldName("start_time",lIsAssignedMap),
//			currentTime
//			);
//		statement.setDate( getIntForFieldName("end_time",lIsAssignedMap),
//			currentTime
//			);

		// This gives a compile error?
//		statement.setDate( 5, new Date() );


		// Todo: unimplemented
		// Not yet implemented stuff
		////////////////////////////////////

		// Break out about the date in which the search was performed

		// "year",lIsAssignedMap),
		// "month_of_year",lIsAssignedMap),
		// "week_of_year"
		// "day_of_year"
		// "day_of_month"
		// "day_of_week"
		// "hour_of_day"

	}




	/*private*/ public static String normalizeString( String inString, boolean inDoWarnings )
	{
		final String kFName = "normalizeString";
		if( inString == null )
		{
			if( inDoWarnings )
				errorMsg( kFName, "Null input string, returning null." );
			return inString;
		}

		// We want everything in lower case
		String lString = inString.toLowerCase();

		// What we are normalizing out
		final String lDelims = " \t\r\n\"<>(){}[],=!?";
		// Not -, +, *, /, ., ', %, #

		// Chop up the string, do NOT give us back the delimiter cruft
		StringTokenizer st = new StringTokenizer(
			lString, lDelims, false
			);

		// Our pending answer
		StringBuffer outBuff = new StringBuffer();

		boolean lIsFirst = true;
		boolean lHaveSeenAny = false;
		// Main loop
		while( st.hasMoreTokens() )
		{
			String word = st.nextToken();
			if( word.length() < 1 )
				continue;
			if( ! lIsFirst )
				outBuff.append( ' ' );
			outBuff.append( word );
			lIsFirst = false;
			lHaveSeenAny = true;
		}

		// Sanity check
		if( ! lHaveSeenAny || outBuff.length() < 1 )
		{
			if( inDoWarnings )
				warningMsg( kFName,
					"Buffer reduced to no non-token words, returning null."
				);
			return null;
		}

		String answer = new String( outBuff );

		debugMsg( kFName,
			"Normalized \"" + inString + "\" to \"" + answer + "\""
			);

		// We have our answer
		return answer;
//		return null;
	}



	// For use with prepareStatement and the list of SQL fields
	// In this case, throwing an exception saves us a LOT of coding
	// in the calling routine, as we can have dozens of calls to this
	private int getIntForFieldName( String inFieldName, Hashtable inIsAssignedMap )
		throws SearchLoggerException
	{
		final String kFName = "initSqlFieldInfo";
		final String kExTag = kClassName + ':' + kFName + ": ";

		// Sanity check, this shouldn't happen, but beats a cryptic
		// null pointer exception
		if( fSqlFieldNameToStatementIntegerMap == null )
			throw new SearchLoggerException( kExTag,
				"Field list not initialized."
				);

		// Normalize and check input field
		inFieldName = NIEUtil.trimmedStringOrNull( inFieldName );
		if( inFieldName == null || inIsAssignedMap == null )
			throw new SearchLoggerException( kExTag,
				"Null/empty inputs passed in."
				);
		String key = inFieldName.toLowerCase();

		if( ! fSqlFieldNameToStatementIntegerMap.containsKey(key) )
			throw new SearchLoggerException( kExTag,
				"Unknown field name \"" + inFieldName + "\" passed in (1)."
				);

		// Make a note that we have seen this field, so presumably it
		// is being assigned
		// And some sanity checks
		if( inIsAssignedMap.containsKey( key ) )
		{
			Boolean oldBoolObj = (Boolean) inIsAssignedMap.get( key );
			if( oldBoolObj.booleanValue() )
				throw new SearchLoggerException( kExTag,
					"Field name \"" + inFieldName + "\" has already been assigned."
					);
		}
		else    // Else it doesn't have the key?
		{
			throw new SearchLoggerException( kExTag,
				"Unknown field name \"" + inFieldName + "\" passed in (2)."
				);
		}
		Boolean newBoolObj = new Boolean( true );
		inIsAssignedMap.put( key, newBoolObj );

		// Get the object and return it's value
		Integer intObj = (Integer) fSqlFieldNameToStatementIntegerMap.get(key);
		return intObj.intValue();
	}

	private String createSqlStatementWithPlaceholders()
		throws SearchLoggerException
	{
		final String kFName = "createSqlStatementWithPlaceholders";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// The table we will insert into
		String tableName = getLogTableName();
		if( tableName == null )
			throw new SearchLoggerException( kExTag,
				"No table name defined."
				);

		// Start working on our answer
		StringBuffer buff = new StringBuffer();

		// Preamble
		buff.append( "INSERT INTO " + tableName + " (" );
		// The list of fields
		buff.append( concatenateEntireFieldList() );
		// The value place holders
		buff.append( ") VALUES (" );
		buff.append( concatenatePlaceHolders() );
		// wrap up
		buff.append( ')' );

		String answer = new String( buff );
		debugMsg( kFName,
			"Built SQL Query \"" + answer + "\""
			);
		return answer;
	}

	private String concatenateEntireFieldList()
		throws SearchLoggerException
	{
		final String kFName = "concatenateEntireFieldList";
		final String kExTag = kClassName + ':' + kFName + ": ";

		// Sanity checks
		if( fSqlFieldNameList == null )
			throw new SearchLoggerException( kExTag,
				"Field list has not been initialized."
				);
		if( fSqlFieldNameList.size() < 1 )
			throw new SearchLoggerException( kExTag,
				"No fields to use; zero-length list."
				);

		// Iterate through all the fields
		StringBuffer buff = new StringBuffer();
		boolean isFirst = true;
		for( Iterator it = fSqlFieldNameList.iterator(); it.hasNext() ; )
		{
			String fieldName = (String) it.next();
			if( ! isFirst )
				buff.append( ", " );
			buff.append( fieldName );
			isFirst = false;
		}

		return new String( buff );
	}

	// Believe it or not, just create "?, ?, ?...etc..."
	private String concatenatePlaceHolders()
		throws SearchLoggerException
	{
		final String kFName = "concatenatePlaceHolders";
		final String kExTag = kClassName + ':' + kFName + ": ";

		// Sanity checks
		if( fSqlFieldNameList == null )
			throw new SearchLoggerException( kExTag,
				"Field list has not been initialized."
				);
		if( fSqlFieldNameList.size() < 1 )
			throw new SearchLoggerException( kExTag,
				"No fields to use; zero-length list."
				);

		// Iterate through for each field
		StringBuffer buff = new StringBuffer();
		for( int i = 0 ; i < fSqlFieldNameList.size() ; i++ )
		{
			if( i > 0 )
				buff.append( ", " );
			buff.append( '?' );
		}

		return new String( buff );
	}


	public long _getNextSearchTransactionNumber()
	{
		// NO, see AuxIOInfo getTransactionID()
		final String kFName = "getNextSearchTransactionNumber";
		synchronized( fCounterLock )
		{
		}
		errorMsg( kFName, "Not yet impelemented." );
		return 0;
	}

	// Normally yes, we should log transactions
	// Currently implemented exceptions
	// 1: We're told to not log null searches AND this is null
	// 2: We're told to not log from certain IP addresses, and this from one of them
	public boolean shouldLogThisSearch( AuxIOInfo inRequestObject, String inQuery )
	{
		final String kFName = "shouldLogThisSearch";
		boolean debug = shouldDoDebugMsg( kFName );
		if(debug) debugMsg( kFName, "Start, qry='" + inQuery + "'" );
		if( shouldIgnoreNullSearches() && getMainConfig().isNullSearch(inQuery) ) {
			if(debug) debugMsg( kFName, "Told to not log this null query." );
			return false;
		}
		Set addrs = getIgnoreClientAddresses();
		if( null!=addrs && addrs.size()>0 ) {
			if(debug) debugMsg( kFName, "Checking " + addrs.size() + " address(es)" );
			// get the client address
			String clientAddr = inRequestObject.getIPAddress();
			clientAddr = NIEUtil.trimmedStringOrNull( clientAddr );
			if( null!=clientAddr && addrs.contains(clientAddr) ) {
				if(debug) debugMsg( kFName, "Not logging search from address " + clientAddr );
				return false;
			}
		}
		return true;
	}


	// Returns a 0, 1 or two element list of values, as full Integer objects
	// first element: number of documents found
	// second element: number of documents searched
	private List _parseValuesFromDocument_v1( String inDoc )
	{
		final String kFName = "_parseValuesFromDocument_v1";
	
		Vector outVect = new Vector();
	
		final boolean debug = shouldDoDebugMsg( kFName );
		final boolean trace = shouldDoTraceMsg( kFName );
	
		if( null == inDoc )
		{
			errorMsg( kFName,
				"Was passed in a null document to parse"
				+ ", unable to parse counts in results list."
				);
			return outVect;
		}
		String lOrigDoc = inDoc;
		inDoc = inDoc.toLowerCase();
	
		// The pattern for docs found
		String pattern1 = getDocsFoundPattern();
		if( pattern1 == null )
		{
			errorMsg( kFName,
				"Null pattern to use for parsing"
				+ ", unable to parse counts in results list."
				+ " Please check " + DOCS_FOUND_PATTERN_PATH + " in your config."
				);
			return outVect;
		}
		pattern1 = pattern1.toLowerCase();
	
		// Todo: let them turn on and off case sensitivity
	
		// Look for it
		int patternAt1 = inDoc.indexOf( pattern1 );
	
		// If it's not found
		if( patternAt1 < 0 )
		{
	
	
			// Check pattern for NO docs found
			String noDocsPattern = getNoDocsFoundPattern();
			// Was there a zero docs pattern?
			if( null != noDocsPattern )
			{
				noDocsPattern = noDocsPattern.toLowerCase();
				// Todo: let them turn on and off case sensitivity
	
				// Look for it
				int noDocsPatAt = inDoc.indexOf( noDocsPattern );
	
				// If we found the zero docs pattern
				if( noDocsPatAt >= 0 )
				{
					debugMsg( kFName,
						"Foud defined 'no docs found' pattern at " + noDocsPatAt
						);
					// Convert to an int object and store
					Integer obj0 = new Integer( 0 );
					outVect.add( obj0 );
				}
				// Else not even zero docs pattern
				else
				{
					warningMsg( kFName,
						"Neither main nor zero-docs Pattern was found in document"
						+ ", unable to parse counts in results list."
						+ " Please check " + DOCS_FOUND_PATTERN_PATH
						+ " and " + NO_DOCS_FOUND_PATTERN_PATH + " in your config."
						);
				}
			}
			// Else no zero docs pattern
			else
			{
				errorMsg( kFName,
					"Pattern not found in document"
					+ ", unable to parse counts in results list."
					+ " Please check " + DOCS_FOUND_PATTERN_PATH + " in your config."
					);
			}
	
			// Nothing else to do, return whatever we found
			return outVect;
		}
	
		// Where to start
		int startAt1 = patternAt1 + pattern1.length();
	
		// Get ready for loop
		StringBuffer lDoc = new StringBuffer( inDoc );
		StringBuffer buff1 = new StringBuffer();
		boolean inNum1 = false;
		boolean doneNum1 = false;
		int limitPosition1 = startAt1 + getMaxDocsFoundLookAhead();
		boolean doNum2 = doDocsFoundMatchBoth();
		boolean inNum2 = false;
		boolean doneNum2 = false;
		StringBuffer buff2 = new StringBuffer();
		int limitPosition2 = limitPosition1 + getMaxDocsFoundSearchedLookAhead();
	
		if(debug)
			debugMsg( kFName,
				"Start: doc is " + lDoc.length() + " chars long."
				+ "patternAt1=" + patternAt1
				+ ", pattern length = " + pattern1.length()
				+ ", startAt1=" + startAt1
				+ ", limitPosition1=" + limitPosition1
				+ ", doNum2=" + doNum2
				+ ", limitPosition2=" + limitPosition2
				);
		else if( !doNum2 )
			infoMsg( kFName,
				"Will NOT search for the total number of docs searched."
				);
	
		// While loop, additional escape logic in the loop
		for( int i = startAt1; i < lDoc.length() ; i++ )
		{
			// Get and characterize the next character
			char c = lDoc.charAt( i );
			boolean isDigit = c >= '0' && c <= '9';
	
			if(trace)
				traceMsg( kFName,
					"Character offset " + i + " is '" + c + "'"
					+ ", isDigit=" + isDigit
					);
	
			// If it's a digit
			if( isDigit )
			{
				// Are we in num1 or maybe should start it
				if( inNum1 || ! doneNum1 )
				{
					buff1.append( c );
					inNum1 = true;
					if( trace ) traceMsg( kFName, "Added to buff1." );
				}
				// If we're looking for a second number, are we in it
				// or at least at a point to start it?
				else if( doNum2 && doneNum1 && ( inNum2 || ! doneNum2 )  )
				{
					buff2.append( c );
					inNum2 = true;
					if( trace ) traceMsg( kFName, "Added to buff2." );
				}
				else
				{
					warningMsg( kFName,
						"Stray digit '" + c + "' at offset " + i
						+ " after matching pattern, ignoring."
						);
					// exit logic below will exit if we need to
				}
			}
			else    // Else it's not a digit
			{
				// If we were in the middle of creating num1, we're done!
				if( inNum1 )
				{
					inNum1 = false;
					doneNum1 = true;
				}
				// If we were in the middle of creating num2, we're done!
				else if( doNum2 && inNum2 )
				{
					inNum2 = false;
					doneNum2 = true;
				}
	
			}   // End else not a digit
	
			// Some exit logic to leave the loop before the end of the doc
			// If we're done
			if( doneNum1 && ( !doNum2 || (doNum2 && doneNum2) ) )
			{
				if(debug)
					debugMsg( kFName,
						"Break 1: done."
						+ " doneNum1=" + doneNum1
						+ ", doNum2=" + doNum2
						+ ", doneNum2=" + doneNum2
						);
				break;
			}
			// If we're past num1 and not doing num2
			if( ! inNum1 && i >= limitPosition1 && ! doNum2 )
			{
				if(debug)
					debugMsg( kFName,
						"Break 2: past 1 and not doing 2."
						+ " inNum1=" + inNum1
						+ ", doNum2=" + doNum2
						+ ", i=" + i
						+ ", limitPosition1=" + limitPosition1
						);
				break;
			}
			// If we're past num2
			if( ! inNum2 && ! inNum1 && i >= limitPosition2 && doNum2 )
			{
				if(debug)
					debugMsg( kFName,
						"Break 3: past 2."
						+ " inNum2=" + inNum2
						+ ", inNum1=" + inNum1
						+ ", doNum2=" + doNum2
						+ ", i=" + i
						+ ", limitPosition2=" + limitPosition2
						);
				break;
			}
			// The main for loop keeps us from going past the end and
			// serves as an overall break
		}
	
		if(debug)
		{
			debugMsg( kFName,
				"After loop."
				+ " buff1=\"" + new String(buff1) + "\""
				+ ", buff2=\"" + new String(buff2) + "\""
				+ ", inNum1=" + inNum1
				+ ", doneNum1=" + doneNum1
				+ ", doNum2=" + doNum2
				+ ", inNum2=" + inNum2
				+ ", doneNum2=" + doneNum2
				);
		}
		else
		{
			infoMsg( kFName,
				"Parsed buff1=\"" + new String(buff1) + "\""
				+ " and buff2=\"" + new String(buff2) + "\"."
				);
		}
	
		// Start recording the results
		if( doneNum1 || inNum1 )
		{
			// This normally can't happen
			if( ! doneNum1 )
				warningMsg( kFName,
					"Possible truncation of docs found count digits"
					+ "\"" + new String(buff1) + "\", will use what we have."
					+ " You may want to adjust " + DOCS_FOUND_MAX_LOOK_AHEAD_ATTR
					);
			// Convert to an integer, and turn on full warnings, this
			// value should not be empty, if there's a problem the static
			// method in NIEUtil will give appropriate warnings
			int num1 = NIEUtil.stringToIntOrDefaultValue(
				new String(buff1), -1, true, true
				);
			// Convert to an int object and store
			Integer obj1 = new Integer( num1 );
			outVect.add( obj1 );
	
			// Possibly add in the second number
			if( doNum2 )
			{
				// If we were tabulating a second number
				if( doneNum2 || inNum2 )
				{
					if( ! doneNum2 )
						warningMsg( kFName,
							"Possible truncation of docs searched count digits"
							+ "\"" + new String(buff2) + "\", will use what we have."
							+ " You may want to adjust " + DOCS_FOUND_MAX_SEARCHED_LOOK_AHEAD_ATTR
							);
					// Convert to an integer, and turn on full warnings, this
					// value should not be empty, if there's a problem the static
					// method in NIEUtil will give appropriate warnings
					int num2 = NIEUtil.stringToIntOrDefaultValue(
						new String(buff2), -1, true, true
						);
					// Convert to an int object and store
					Integer obj2 = new Integer( num2 );
					outVect.add( obj2 );
				}
				else    // No second number
				{
					errorMsg( kFName,
						"Did not find the docs searched count in the document. (1)"
						);
				}
			}   // End if do num2
	
		}
		else    // Else no first number found
		{
			errorMsg( kFName,
				"Did not find the docs found count in the document."
				+ " We DID find the pattern, but no doc count within the "
				+ getMaxDocsFoundLookAhead()
				+ " characters after that pattern."
				+ " Reminder: you can adjust how far we look ahead"
				+ " by setting the " + DOCS_FOUND_MAX_LOOK_AHEAD_ATTR + " attribute."
				);
		}
	
		// Return whatever we were left with
		return outVect;
	
	}


	// Returns a 0, 1 or two element list of values, as full Integer objects
	// first element: number of documents found
	// second element: number of documents searched
	// This version is refactored so that the static part can be
	// called from other classes
	private List parseValuesFromDocument( String inDoc )
	{
		final String kFName = "parseValuesFromDocument";
		return staticParseValuesFromDocument( inDoc, this, true );
	}

	// We want to be able to call this logic from other classes as well
	// In some cases we'll have a config object
	// In others we'll just pass in fixed values

	public static List staticParseValuesFromDocument( String inDoc, SearchLogger config, boolean inDoParseWarnings )
	{
		final String kFName = "staticParseValuesFromDocument(1)";

		// The pattern for docs found
		String pattern1 = config.getDocsFoundPattern();
		// Check pattern for NO docs found
		String noDocsPattern = config.getNoDocsFoundPattern();
		// Limits on how much to look for
		int maxDocLookAhead = config.getMaxDocsFoundLookAhead();
		boolean doNum2 = config.doDocsFoundMatchBoth();
		int maxDocSearchedLookAhead = config.getMaxDocsFoundSearchedLookAhead();
	
		// Return whatever we were left with
		return staticParseValuesFromDocument(
			inDoc, pattern1, noDocsPattern,
			maxDocLookAhead, doNum2, maxDocSearchedLookAhead, inDoParseWarnings
			);	
	}

	// Returns a 0, 1 or two element list of values, as full Integer objects
	// first element: number of documents found
	// second element: number of documents searched
	public static List staticParseValuesFromDocument(
			String inDoc, String pattern1, String noDocsPattern,
			int maxDocLookAhead, boolean doNum2, int maxDocSearchedLookAhead, boolean inDoParseWarnings
			)
	{
		final String kFName = "staticParseValuesFromDocument(2)";

		Vector outVect = new Vector();

		final boolean debug = shouldDoDebugMsg( kFName );
		final boolean trace = shouldDoTraceMsg( kFName );

		if( null == inDoc )
		{
			errorMsg( kFName,
				"Was passed in a null document to parse"
				+ ", unable to parse counts in results list."
				);
			return outVect;
		}
		String lOrigDoc = inDoc;
		inDoc = inDoc.toLowerCase();

		// The pattern for docs found
		// String pattern1 = getDocsFoundPattern();
		if( pattern1 == null )
		{
			errorMsg( kFName,
				"Null pattern to use for parsing"
				+ ", unable to parse counts in results list."
				+ " Please check " + DOCS_FOUND_PATTERN_PATH + " in your config."
				);
			return outVect;
		}
		pattern1 = pattern1.toLowerCase();

		// Todo: let them turn on and off case sensitivity

		// Look for it
		int patternAt1 = inDoc.indexOf( pattern1 );

		// If it's not found
		if( patternAt1 < 0 )
		{


			// Check pattern for NO docs found
			// String noDocsPattern = getNoDocsFoundPattern();
			// Was there a zero docs pattern?
			if( null != noDocsPattern )
			{
				noDocsPattern = noDocsPattern.toLowerCase();
				// Todo: let them turn on and off case sensitivity

				// Look for it
				int noDocsPatAt = inDoc.indexOf( noDocsPattern );

				// If we found the zero docs pattern
				if( noDocsPatAt >= 0 )
				{
					debugMsg( kFName,
						"Foud defined 'no docs found' pattern at " + noDocsPatAt
						);
					// Convert to an int object and store
					Integer obj0 = new Integer( 0 );
					outVect.add( obj0 );
				}
				// Else not even zero docs pattern
				else
				{
					String msg =
						"Neither main nor zero-docs Pattern was found in document"
						+ ", unable to parse counts in results list."
						+ " Please check " + DOCS_FOUND_PATTERN_PATH
						+ " and " + NO_DOCS_FOUND_PATTERN_PATH + " in your config."
						;
					if( inDoParseWarnings )
						warningMsg( kFName, msg );
					else
						debugMsg( kFName, msg );
				}
			}
			// Else no zero docs pattern
			else
			{
				errorMsg( kFName,
					"Pattern not found in document"
					+ ", unable to parse counts in results list."
					+ " Please check " + DOCS_FOUND_PATTERN_PATH + " in your config."
					);
			}

			// Nothing else to do, return whatever we found
			return outVect;
		}

		// Where to start
		int startAt1 = patternAt1 + pattern1.length();

		// Get ready for loop
		StringBuffer lDoc = new StringBuffer( inDoc );
		StringBuffer buff1 = new StringBuffer();
		boolean inNum1 = false;
		boolean doneNum1 = false;
		// int limitPosition1 = startAt1 + getMaxDocsFoundLookAhead();
		int limitPosition1 = startAt1 + maxDocLookAhead;
		// boolean doNum2 = doDocsFoundMatchBoth();
		boolean inNum2 = false;
		boolean doneNum2 = false;
		StringBuffer buff2 = new StringBuffer();
		// int limitPosition2 = limitPosition1 + getMaxDocsFoundSearchedLookAhead();
		int limitPosition2 = limitPosition1 + maxDocSearchedLookAhead;

		if(debug)
			debugMsg( kFName,
				"Start: doc is " + lDoc.length() + " chars long."
				+ "patternAt1=" + patternAt1
				+ ", pattern length = " + pattern1.length()
				+ ", startAt1=" + startAt1
				+ ", limitPosition1=" + limitPosition1
				+ ", doNum2=" + doNum2
				+ ", limitPosition2=" + limitPosition2
				);
		else if( !doNum2 )
			infoMsg( kFName,
				"Will NOT search for the total number of docs searched."
				);

		// While loop, additional escape logic in the loop
		for( int i = startAt1; i < lDoc.length() ; i++ )
		{
			// Get and characterize the next character
			char c = lDoc.charAt( i );
			boolean isDigit = c >= '0' && c <= '9';

			if(trace)
				traceMsg( kFName,
					"Character offset " + i + " is '" + c + "'"
					+ ", isDigit=" + isDigit
					);

			// If it's a digit
			if( isDigit )
			{
				// Are we in num1 or maybe should start it
				if( inNum1 || ! doneNum1 )
				{
					buff1.append( c );
					inNum1 = true;
					if( trace ) traceMsg( kFName, "Added to buff1." );
				}
				// If we're looking for a second number, are we in it
				// or at least at a point to start it?
				else if( doNum2 && doneNum1 && ( inNum2 || ! doneNum2 )  )
				{
					buff2.append( c );
					inNum2 = true;
					if( trace ) traceMsg( kFName, "Added to buff2." );
				}
				else
				{
					warningMsg( kFName,
						"Stray digit '" + c + "' at offset " + i
						+ " after matching pattern, ignoring."
						);
					// exit logic below will exit if we need to
				}
			}
			else    // Else it's not a digit
			{
				// If we were in the middle of creating num1, we're done!
				if( inNum1 )
				{
					inNum1 = false;
					doneNum1 = true;
				}
				// If we were in the middle of creating num2, we're done!
				else if( doNum2 && inNum2 )
				{
					inNum2 = false;
					doneNum2 = true;
				}

			}   // End else not a digit

			// Some exit logic to leave the loop before the end of the doc
			// If we're done
			if( doneNum1 && ( !doNum2 || (doNum2 && doneNum2) ) )
			{
				if(debug)
					debugMsg( kFName,
						"Break 1: done."
						+ " doneNum1=" + doneNum1
						+ ", doNum2=" + doNum2
						+ ", doneNum2=" + doneNum2
						);
				break;
			}
			// If we're past num1 and not doing num2
			if( ! inNum1 && i >= limitPosition1 && ! doNum2 )
			{
				if(debug)
					debugMsg( kFName,
						"Break 2: past 1 and not doing 2."
						+ " inNum1=" + inNum1
						+ ", doNum2=" + doNum2
						+ ", i=" + i
						+ ", limitPosition1=" + limitPosition1
						);
				break;
			}
			// If we're past num2
			if( ! inNum2 && ! inNum1 && i >= limitPosition2 && doNum2 )
			{
				if(debug)
					debugMsg( kFName,
						"Break 3: past 2."
						+ " inNum2=" + inNum2
						+ ", inNum1=" + inNum1
						+ ", doNum2=" + doNum2
						+ ", i=" + i
						+ ", limitPosition2=" + limitPosition2
						);
				break;
			}
			// The main for loop keeps us from going past the end and
			// serves as an overall break
		}

		if(debug)
		{
			debugMsg( kFName,
				"After loop."
				+ " buff1=\"" + new String(buff1) + "\""
				+ ", buff2=\"" + new String(buff2) + "\""
				+ ", inNum1=" + inNum1
				+ ", doneNum1=" + doneNum1
				+ ", doNum2=" + doNum2
				+ ", inNum2=" + inNum2
				+ ", doneNum2=" + doneNum2
				);
		}
		else
		{
			infoMsg( kFName,
				"Parsed buff1=\"" + new String(buff1) + "\""
				+ " and buff2=\"" + new String(buff2) + "\"."
				);
		}

		// Start recording the results
		if( doneNum1 || inNum1 )
		{
			// This normally can't happen
			if( ! doneNum1 )
				warningMsg( kFName,
					"Possible truncation of docs found count digits"
					+ "\"" + new String(buff1) + "\", will use what we have."
					+ " You may want to adjust " + DOCS_FOUND_MAX_LOOK_AHEAD_ATTR
					);
			// Convert to an integer, and turn on full warnings, this
			// value should not be empty, if there's a problem the static
			// method in NIEUtil will give appropriate warnings
			int num1 = NIEUtil.stringToIntOrDefaultValue(
				new String(buff1), -1, true, true
				);
			// Convert to an int object and store
			Integer obj1 = new Integer( num1 );
			outVect.add( obj1 );

			// Possibly add in the second number
			if( doNum2 )
			{
				// If we were tabulating a second number
				if( doneNum2 || inNum2 )
				{
					if( ! doneNum2 )
						warningMsg( kFName,
							"Possible truncation of docs searched count digits"
							+ "\"" + new String(buff2) + "\", will use what we have."
							+ " You may want to adjust " + DOCS_FOUND_MAX_SEARCHED_LOOK_AHEAD_ATTR
							);
					// Convert to an integer, and turn on full warnings, this
					// value should not be empty, if there's a problem the static
					// method in NIEUtil will give appropriate warnings
					int num2 = NIEUtil.stringToIntOrDefaultValue(
						new String(buff2), -1, true, true
						);
					// Convert to an int object and store
					Integer obj2 = new Integer( num2 );
					outVect.add( obj2 );
				}
				else    // No second number
				{
					errorMsg( kFName,
						"Did not find the docs searched count in the document. (1)"
						);
				}
			}   // End if do num2

		}
		else    // Else no first number found
		{
			errorMsg( kFName,
				"Did not find the docs found count in the document."
				+ " We DID find the pattern, but no doc count within the "
				// + getMaxDocsFoundLookAhead()
				+ maxDocLookAhead
				+ " characters after that pattern."
				+ " Reminder: you can adjust how far we look ahead"
				+ " by setting the " + DOCS_FOUND_MAX_LOOK_AHEAD_ATTR + " attribute."
				);
		}

		// Return whatever we were left with
		return outVect;

	}


	private static void ___Sep__Get_and_Set__(){}
	//////////////////////////////////////////////////////////////
	//
	//      Simple Get/Set stuff
	//
	///////////////////////////////////////////////////////////////


	// Whether or not to actually try the dabase
	// In testing, you may not always want to worry about it
	public boolean shouldUseDatabase()
	{
		if( ! mUseCache )
			cIsDBActive = fConfigTree.getBooleanFromAttribute(
				ACTIVATE_DATABASE_ATTR, DEFAULT_DB_ACTIVE
				);
		return cIsDBActive;
	}

	// How seriously we should take the fact that we can't contact the DB
	// In a production environment, you might want to stay up regardless
	public boolean shouldFailOnDBConfigError()
	{
		if( ! mUseCache )
			cDoExitOnDBConfig = fConfigTree.getBooleanFromAttribute(
				EXIT_ON_DB_CONFIG_ERROR_ATTR, DEFAULT_EXIT_ON_DB_CONFIG
				);
		return cDoExitOnDBConfig;
	}

	// What pattern do we look for "Your query matched 17 of 572 documents"
	public String getDocsFoundPattern()
	{
		if( ! mUseCache )
			cMatchPattern = fConfigTree.getTextByPathTrimOrNull(
				DOCS_FOUND_PATTERN_PATH
				);
		return cMatchPattern;
	}
	// What pattern do we look for "No Documents Found"
	public String getNoDocsFoundPattern()
	{
		if( ! mUseCache )
			cNoMatchPattern = fConfigTree.getTextByPathTrimOrNull(
				NO_DOCS_FOUND_PATTERN_PATH
				);
		return cNoMatchPattern;
	}

	// How far ahead are we allowed to look for digits
	public int getMaxDocsFoundLookAhead()
	{
		if( ! mUseCache )
			cFoundLookAhead = fConfigTree.getIntFromSinglePathAttr(
				DOCS_FOUND_PATTERN_PATH,
				DOCS_FOUND_MAX_LOOK_AHEAD_ATTR,
				DEFAULT_DOCS_FOUND_LOOK_AHEAD
				);
		return cFoundLookAhead;
	}


	// Should we try to also match the "of nnn" total docs searched
	public boolean doDocsFoundMatchBoth()
	{
		if( ! mUseCache )
		{
			final String kFName = "doDocsFoundMatchBoth";
			int tmpNumber = getMaxDocsFoundSearchedLookAhead();
			cDoMatchBoth = fConfigTree.getBooleanFromSinglePathAttr(
				DOCS_FOUND_PATTERN_PATH,
				DOCS_FOUND_MATCH_BOTH_ATTR,
				DEFAULT_DOCS_FOUND_MATCH_BOTH
				);

			if( cDoMatchBoth && tmpNumber <= 0 )
			{
				errorMsg( kFName,
					"Conflicing settings. "
					+ DOCS_FOUND_MATCH_BOTH_ATTR + " was set to true"
					+ " but " + DOCS_FOUND_MAX_SEARCHED_LOOK_AHEAD_ATTR
					+ " was set to " + tmpNumber
					+ " which is <= 0."
					+ " Can't match the 2nd number if not allowed to look"
					+ " at text, so disabling " + DOCS_FOUND_MATCH_BOTH_ATTR + "."
					);
				cDoMatchBoth = false;
			}
		}
		return cDoMatchBoth;
	}


	// How far ahead are we allowed to look for digits for second value
	public int getMaxDocsFoundSearchedLookAhead()
	{
		if( ! mUseCache )
			cFoundSearchedLookAhead = fConfigTree.getIntFromSinglePathAttr(
				DOCS_FOUND_PATTERN_PATH,
				DOCS_FOUND_MAX_SEARCHED_LOOK_AHEAD_ATTR,
				DEFAULT_DOCS_FOUND_MAX_SEARCHED_LOOK_AHEAD
				);
		return cFoundSearchedLookAhead;
	}



	public int getConfiguredSiteID()
	{
		if( ! mUseCache )
			cSiteID = fConfigTree.getIntFromAttribute(
				SITE_ID_ATTR,
				DEFAULT_SITE_ID
				);
		return cSiteID;
	}

	// Whether or not we need to proxy a page to get the data to log
	// In some cases, such as Verity Search 97 Search Script, we DON'T
	// need to scrape the text for data, Verity can call out to us instead,
	// so that would be more efficient
	// public boolean doesSearchLoggingRequireProxy()
	public boolean doesSearchLoggingRequireScraping()
	{
		if( ! mUseCache )
			cIsProxyActive = fConfigTree.getBooleanFromSinglePathAttr(
				PROXY_INFO_PATH,
				PROXY_ACTIVE_ATTR,
				DEFAULT_PROXY_ACTIVE
				);
		return cIsProxyActive;
	}
	public boolean alwaysProxy()
	{
		final String kFName = "alwaysProxy";
		if( ! mUseCache )
		{
			cIsProxyAlways = fConfigTree.getBooleanFromSinglePathAttr(
				PROXY_INFO_PATH,
				PROXY_ALWAYS_ATTR,
				DEFAULT_PROXY_ALWAYS
				);

			// if( cIsProxyAlways && ! doesSearchLoggingRequireProxy() )
			if( cIsProxyAlways && ! doesSearchLoggingRequireScraping() )
				warningMsg( kFName,
					"A setting overrides another conflicting setting."
					+ " Configured to always proxy, but also told not proxy"
					+ " for Search Names logging; 'always' takes precedence over"
					+ " search names queries, so will always proxy."
					);
		}
		return cIsProxyAlways;
	}

	// For now, if we have a connection, then we'll do logging
	// Todo: maybe make fancier logic with error conditions, etc.
	public boolean shouldDoSearchLogging()
	{
		// if( shouldUseDatabase() && getDBConfig() != null
		//	&& getDBConfig().getConnectionOrNull() != null
		//	)
		// ^^^ Hits getConnection too many times, hammering reconnect logic
		if( shouldUseDatabase() && getDBConfig() != null
			&& getDBConfig() != null
			)
		{
			return true;
		}
		else {
			return false;
		}
	}

	public boolean shouldDoClickThroughLogging()
	{
	    if( ! shouldDoSearchLogging() )
	        return false;

	    return DEFAULT_SHOULD_DO_CLICKTHROUGH_LOGGING;
	}
//	public boolean shouldDoClickThroughLogging()
//	{
//	private static final String ACTIVATE_CLICKTHROUGH_ATTR = "do_click_through_logging";
//	}
//	public boolean shouldModifySearchForms()
//	{
//	private static final String _ACTIVATE_FORM_MODS_ATTR = "do_form_mods";
//	}

	public Set getIgnoreClientAddresses()
	{
		if( ! mUseCache )
		{
			cIgnoreClientAddressesRaw = fConfigTree.getTextListByPathNotNullTrim(
				IGNORE_ADDRESS_PATH
				);
			if( null!=cIgnoreClientAddressesRaw && cIgnoreClientAddressesRaw.size()>0 )
				cIgnoreClientAddressesFinal = NIEUtil.lookupIpAddressesAsSet( cIgnoreClientAddressesRaw );
		}
		return cIgnoreClientAddressesFinal;
	}

	public boolean shouldIgnoreNullSearches()
	{
		if( ! mUseCache )
			cIgnoreNullSearches = fConfigTree.getBooleanFromAttribute(
					IGNORE_NULL_SEARCHES_ATTR, DEFAULT_IGNORE_NULL_SEARCHES
				);
		return cIgnoreNullSearches;
	}


	public String getLogTableName()
	{
		// if( ! mUseCache )
		//	if( getDBConfig() != null )
		//		cTableName = getDBConfig().getMainTable();
		// return cTableName;

		// *** SEE ALSO nie.sr.SRRerport.getLogTAbleName(), they should match

		return nie.core.DBConfig.LOG_TABLE;
	}

	public DBConfig getDBConfig()
	{
		// return fDBConf;
		return getMainConfig().getDBConfig();
	}


	private SearchTuningConfig getMainConfig() {
		return fInitConfig;

		/***
		SearchTuningConfig answer = fMainApp.getSearchTuningConfig();
		if( null != answer ) {
			fInitConfig = null;
			return answer;
		}
		else
			return fInitConfig;
		***/
	}


	private static void ___Sep__Run_Logging__(){}
	//////////////////////////////////////////////////////////////


	///////////////////////////////////////////////////////////
	//
	//  Logging
	//
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


	private static void ___Sep__Member_Fields_and_Constants__(){}
	//////////////////////////////////////////////////////////////

	////////////////////////////////////////////////////////////////////
	//
	//      Variables and constants
	//
	/////////////////////////////////////////////////////////////////////

	// Cached variable flag
	// Off by default
	private boolean mUseCache;

	// The main JDOM configuration tree
	private JDOMHelper fConfigTree;


	SearchTuningApp _fMainApp;
	SearchTuningConfig fInitConfig;

	// A semephor to use for the counting flag
	private Object fCounterLock = new Object();

	// Whether we actually use the database
	// Normally yes, but may want to turn off for testing purposes
	// The attribute name
	private static final String ACTIVATE_DATABASE_ATTR = "use_database";
	// Our default assumption
	private static final boolean DEFAULT_DB_ACTIVE = true;
	// Cache this often-checked item
	private boolean cIsDBActive;

	// Cache of the table name we will insert into
	// from dbconfig's main table items
	private String cTableName;

	// When things are horribly broken, we don't want to fill up the log
	// file with zillions of the same error
	private boolean fHaveIssuesBrokenWarning;

	// Whether to exit if we can't reach the DB or not
	private static final String  EXIT_ON_DB_CONFIG_ERROR_ATTR =
		"exit_on_db_config_error";
	private static final boolean DEFAULT_EXIT_ON_DB_CONFIG = false;
	private boolean cDoExitOnDBConfig;


	
	// Information about a configured site ID
	private static final String  SITE_ID_ATTR = "site_id";
	// The database name for this field
	public static final String SITE_ID_DB_FIELD = "site_id";
	private static final int DEFAULT_SITE_ID = -1;
	private int cSiteID;

	public static final boolean DEFAULT_SHOULD_DO_CLICKTHROUGH_LOGGING = true;
	public static final boolean _DEFAULT_SHOULD_DO_FORM_MARKUP = true;
	// TODO: stop and start patterns

	// private static final String LOG_NULL_SEARCHES_ATTR = "log_null_searches";
	// public static final boolean DEFAULT_LOG_NULL_SEARCHES = true;
	private static final String IGNORE_NULL_SEARCHES_ATTR = "ignore_null_searches";
	public static final boolean DEFAULT_IGNORE_NULL_SEARCHES = true;
	private boolean cIgnoreNullSearches;
	private static final String IGNORE_ADDRESS_ELEM = "ignore_address";
	private static final String IGNORE_ADDRESS_PATH = IGNORE_ADDRESS_ELEM;
	private List cIgnoreClientAddressesRaw;
	private Set cIgnoreClientAddressesFinal;
	// TODO: will need to allow them to put in names, we will need to translate
	// TODO: also ideas for ignoring certain fields, or field/value pairs
	
	// Proxy Settings
	// These settings are clustered under one main node
	private static final String PROXY_INFO_PATH = "proxy_settings";
	// Whether to specifically proxy every search, looking for data
	private static final String PROXY_ACTIVE_ATTR =
		"for_search_names_terms";  // TODO: ????
	private static final boolean DEFAULT_PROXY_ACTIVE = true;
	private boolean cIsProxyActive;
	// Whether we should FORCE proxy for all searches
	private static final String PROXY_ALWAYS_ATTR =
		"always";
	private static final boolean DEFAULT_PROXY_ALWAYS = false;
	private boolean cIsProxyAlways;

	// Info for parsing various items from the resulting page
	// for search track reporting
	private static final String PATTERN_INFO_PATH = PROXY_INFO_PATH;

	// What pattern do we look for "Your query matched 17 of 572 documents"
	private static final String DOCS_FOUND_PATTERN_PATH =
		PATTERN_INFO_PATH + "/docs_found_pattern";
	private String cMatchPattern;
	// What pattern, if any, do we look for "No matching documents found"
	private static final String NO_DOCS_FOUND_PATTERN_PATH =
		PATTERN_INFO_PATH + "/no_docs_found_pattern";
	private String cNoMatchPattern;



	// How far ahead are we allowed to look for digits
	private static final String DOCS_FOUND_MAX_LOOK_AHEAD_ATTR =
		"max_found_look_ahead";
	private int cFoundLookAhead;
	// private static final int DEFAULT_DOCS_FOUND_LOOK_AHEAD = 10;
	/*private*/ public static final int DEFAULT_DOCS_FOUND_LOOK_AHEAD = 15;

	// Should we try to also match the "of nnn" total docs searched
	private static final String DOCS_FOUND_MATCH_BOTH_ATTR = "match_both";
	private boolean cDoMatchBoth;
	private static final boolean DEFAULT_DOCS_FOUND_MATCH_BOTH = true;

	// How far ahead are we allowed to look for digits for second value
	private static final String DOCS_FOUND_MAX_SEARCHED_LOOK_AHEAD_ATTR =
		"max_searched_look_ahead";
	private int cFoundSearchedLookAhead;
	// private static final int DEFAULT_DOCS_FOUND_MAX_SEARCHED_LOOK_AHEAD = 20;
	/*private*/ public static final int DEFAULT_DOCS_FOUND_MAX_SEARCHED_LOOK_AHEAD = 25;

	// Unimplemented
	// no_docs_found_pattern

	// All about the Configured Database
	// Where to look for it
	// Deprecated, moved to global database
	public static final String OLD_DB_CONFIG_PATH =
		"database";
	// Store the instance we get back
	private DBConfig _fDBConf;


	// The name of the table we will log transactions to
	// MUST LINE UP with a schema definition in the system dir
	// private static final String ACTIVITY_LOG_TABLE_NAME = "nie_log";
	// ^^^ PLEASE USE DBConfig.LOG_TABLE from now on

	// Yes, a long name, but very clear
	// used to sn_, but now nie_
	public static final String _NIE_CGI_FIELD_NAME_PREFIX_FOR_LOGGING_FIELDS = "nie_";
	// ^^^ No!  CGI field name for each DB field is set in the DB schema XML files
	// in core.system.db.schema.nie_log_schema.xml
	
	
	// The name of the field in the table that holds the transaction type info
	public static final String TRANS_TYPE_DB_FIELD = "transaction_type";
//	public static final String TRANS_TYPE_CGI_FIELD =
//	    NIE_CGI_FIELD_NAME_PREFIX_FOR_LOGGING_FIELDS
//	    + TRANS_TYPE_DB_FIELD;

	// The name of the field in the table that holds the referring ID
	public static final String REF_ID_DB_FIELD = "referring_transaction_number";
	// public static final String REF_ID_DB_FIELD = "ref_trans_id";
	// public static final String REF_ID_CGI_FIELD =
	//    TRANS_TYPE_DB_FIELD
	//    + REF_ID_DB_FIELD;

	// The CGI field that says where to send a user after a transaction has been logged
	// public static final String _DESTINATION_URL_CGI_FIELD = "sn_dest_url";
	public static final String DESTINATION_URL_CGI_FIELD = "nie_dest_url";
	//     NIE_CGI_FIELD_NAME_PREFIX_FOR_LOGGING_FIELDS + "dest_url";

	// the field where advertiser info is stored
	public static final String ADVERTISEMENT_ID_DB_FIELD =
		"advertisement_code_sent";
//	public static final String ADVERTISEMENT_ID_CGI_FIELD =
//	    NIE_CGI_FIELD_NAME_PREFIX_FOR_LOGGING_FIELDS
//	    + ADVERTISEMENT_ID_DB_FIELD;

	// the field where advertiser info is stored
	public static final String ADVERTISEMENT_URL_DB_FIELD =
		"advertisement_href";
//	public static final String ADVERTISEMENT_URL_CGI_FIELD =
//	    NIE_CGI_FIELD_NAME_PREFIX_FOR_LOGGING_FIELDS
//	    + ADVERTISEMENT_URL_DB_FIELD;

	// The image, if any, that was displayed
	public static final String ADVERTISEMENT_IMAGE_URL_DB_FIELD =
		"advertisement_graphic_url";
//	public static final String ADVERTISEMENT_IMAGE_URL_CGI_FIELD =
//	    NIE_CGI_FIELD_NAME_PREFIX_FOR_LOGGING_FIELDS
//	    + ADVERTISEMENT_IMAGE_URL_DB_FIELD;

	// Search Names status information
	public static final String SN_ACTION_CODE_DB_FIELD =
		"search_names_action_code";
	public static final String SN_ACTION_COUNT_DB_FIELD =
		"search_names_action_count";
	public static final String SN_ACTION_DETAILS_DB_FIELD =
		"search_names_action_details";
	public static final String SN_PROBLEM_FLAG_DB_FIELD =
		"search_names_problem_flag";
	public static final String SN_PROBLEM_CODE_DB_FIELD =
		"search_names_problem_code";
	public static final String SN_PROBLEM_MSG_DB_FIELD =
		"search_names_problem_message";

	// parameters explicity passed in from us
	public static final String NUM_FOUND_DB_FIELD_NAME = "num_results";
	// public static final String NUM_FOUND_CGI_FIELD_NAME =
	//    NIE_CGI_FIELD_NAME_PREFIX_FOR_LOGGING_FIELDS
	//    + NUM_FOUND_DB_FIELD_NAME;

	public static final String NUM_SEARCHED_DB_FIELD_NAME = "num_searched";
	// public static final String NUM_SEARCHED_CGI_FIELD_NAME =
	//    NIE_CGI_FIELD_NAME_PREFIX_FOR_LOGGING_FIELDS
	//    + NUM_SEARCHED_DB_FIELD_NAME;

	public static final String ORIGINAL_SEARCH_DB_FIELD_NAME = "original_query";
//	public static final String ORIGINAL_SEARCH_CGI_FIELD_NAME =
//	    NIE_CGI_FIELD_NAME_PREFIX_FOR_LOGGING_FIELDS
//	    + ORIGINAL_SEARCH_DB_FIELD_NAME;

	public static final String NORMALIZED_SEARCH_DB_FIELD_NAME = "normalized_query";
//	public static final String NORMALIZED_SEARCH_CGI_FIELD_NAME =
//	    NIE_CGI_FIELD_NAME_PREFIX_FOR_LOGGING_FIELDS
//	    + NORMALIZED_SEARCH_DB_FIELD_NAME;

	public static final String SUBMITTED_FORM_DB_FIELD_NAME = "form_name";
//	public static final String SUBMITTED_FORM_CGI_FIELD_NAME =
//	    NIE_CGI_FIELD_NAME_PREFIX_FOR_LOGGING_FIELDS
//	    + SUBMITTED_FORM_DB_FIELD_NAME;

	public static final String START_TIME_DB_FIELD_NAME = "start_time";
	public static final String END_TIME_DB_FIELD_NAME = "end_time";

	public static final String CLIENT_IP_DB_FIELD_NAME = "client_host";

	// List our types of transactions
	// TODO: expand list
	// Note:
	// The Context in SnRequestionHandler is the general high level command
	// If doing Direct Logging, an ADDITIONAL field is passed in
	// giving the specific transation type we're logging in SearchLogger
	
	public static final int TRANS_TYPE_UNKNOWN = -1;
	public static final int TRANS_TYPE_SEARCH = 1;
	public static final int TRANS_TYPE_LOG_DOC_CLICK = 2;
	public static final int TRANS_TYPE_LOG_USER_FEEDBACK = 3;
	public static final int TRANS_TYPE_ADVERTISEMENT_EXPOSURE = 4;
	public static final int TRANS_TYPE_ADVERTISEMENT_CLICK_THROUGH = 5;

	// There are other types of links a document results list
	// and we might want to know the difference
	public static final int TRANS_TYPE_SEARCH_NAV_CLICK = 6;
	public static final int TRANS_TYPE_GENERIC_CLICK = 7;


	private static void ___Sep__SQL_Fields__(){}
	//////////////////////////////////////////////////////////////

	// We need an easy way to build sql statements, so we will use
	// the ODBC prepareStatement construct.
	// We need some reference data for it.

	private static Hashtable fSqlFieldNameToStatementIntegerMap;
	// private Hashtable fSqlFieldAssignedMap;
	private static List fSqlFieldNameList;

	// We need these to build the sql statement more easily
	// You'll note that there's no indication of type here
	private static final String kSqlFields [] =
	{

		// THIS ARRAY IS OBSOLETE
		// see the system schema definition instead
		// leaving this here just for old version of logger

		"site_id",
		"search_transaction_number",
		"referring_transaction_number",
		"transaction_type",
		"form_name",
		"client_host",
		"user_name",
		"user_domain",
		"referer",
		// "user_agent",
		"num_results",
		"num_searched",
		"start_time",
		"end_time",
		"original_query",
		"normalized_query",
		"extra_search_parameters",
		"was_search_names_term",
		"was_search_names_action_taken",
		"search_names_action_code",
		"search_names_action_count",
		// "search_names_action_details",
		"search_names_problem_flag",
		"search_names_problem_code",
		// "search_names_problem_message",
		"type_of_document_click",
		"rank_in_results_list",
		"rank_in_results_on_this_page",
		// "results_list_page_number",  // Not in new AOPA table?  See also logTransaction
		"user_feedback",
		"advertisement_graphic_url",
		"advertisement_href",
		"advertisement_code_sent"
		// "count_of_occurrences"
		/***
		"hour_of_day",
		"hour_of_week",
		"hour_of_year",
		"day_of_week",
		"day_of_month",
		"day_of_year",
		"week_of_year",
		"month_of_year",
		"quarter_of_year",
		"year"
		***/
	};


}