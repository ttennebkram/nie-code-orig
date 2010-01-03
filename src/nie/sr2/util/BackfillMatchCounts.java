package nie.sr2.util;

import java.sql.*;
import java.util.*;
import java.io.IOException;
import java.net.*;

import com.sun.org.apache.bcel.internal.generic.FMUL;

import nie.config_ui.ConfiguratorException;
import nie.core.*;
import nie.sn.SearchEngineConfig;
import nie.sn.SearchTuningConfig;
import nie.sn.SearchTuningConfigFatalException;
import nie.sn.SnRequestHandler;
import nie.sr2.ReportConstants;
import nie.sr2.SearchEngineLink;

// Google Backfill: http://www.google.com/search?hl=en&q=site%3Afidelity.com+cancer

public class BackfillMatchCounts implements nie.sn.CronLiteJob // Runnable
{

	private final static String kClassName = "BackfillMatchCounts";

	static final long MY_INTERVAL = nie.sn.CronLite.HOUR;

	// How often to run
	public long getRunIntervalInMS() {
		return MY_INTERVAL;
	}



    ////////////////////////////////////////////////
    //
    // In case we're run as a stand-alone program
    // instead of being incorporated in another program
    //
    /////////////////////////////////////////////////
	
    static public void main( String[] inArgs )
	{
		final String kFName = "main";
		BackfillMatchCounts lLookup = new BackfillMatchCounts();
		lLookup.parseCommandLine( inArgs );
		try
		{
			// lLookup.commonInit();
			// ^^^ moved to top of run()
			lLookup.setupConfigFromURI();
		}
		catch( Exception e )
		{
			fatalErrorMsg( kFName,
				"Error initializing, exiting."
				+ " Error: " + e
				);
			System.exit( 2 );
		}
		lLookup.run();
    }

	///////////////////////////////////////////////////////////
	private static final void __Constructors_and_Initialization__() {}
    
    ////////////////////////////////////////////////
    //
    // Constructors.
    //
    // All constructors should call commonInit()
    //
    ////////////////////////////////////////////////
    
    private /*public*/ BackfillMatchCounts()
    //	throws UtilException
    {
    	// If you use this one, you must also then call
    	// .setupConfigFromURI() before running
    }

    // NOTE: This constructor may not be called
    // Instead, they may call the null arg version
    // and then call .setupConfigFromURI()
	public BackfillMatchCounts( SearchTuningConfig inMainConfig )
		throws Exception
	{
		this();
		setMainConfig( inMainConfig );
		// Deferred setupSearchEngineConfig();
	}


	public void setupConfigFromURI()
		 throws Exception
	{
		final String kFName = "setupConfigFromURI";
		final String kExTag = kClassName + '.' + kFName + ": ";
		try
		{
			// fDBConf = new DBConfig( fConfigFileURI );
			mMainConfig = new SearchTuningConfig( fConfigFileURI, null );
		}
		catch( Exception e )
		{
			throw new UtilException( kExTag
				+ "Error intializing config/data from URI \"" + fConfigFileURI + "\""
				+ " Error was: " + e
				);
		}
		// Deferred setupSearchEngineConfig();
	}


	// We don't always use the search engine that was
	// part of the main config
	// For example, we may use Google instead
	void setupSearchEngineConfig()
		throws Exception
	{
		final String kFName = "setupSearchEngineConfig";
		/*SearchEngineConfig*/ // mTargetSearchEngineConfig = getUseGoogleInstead()
			// ? getGooglePubConfig()
			// : getMainConfig().getSearchEngine()
			// ;
		String which = null;
		if( getUseGoogleInstead() ) {
			mTargetSearchEngineConfig = getGooglePubConfig();
			which = "Google Public Engine";
		}
		else {
			mTargetSearchEngineConfig = getMainConfig().getSearchEngine();
			which = "System Configured Search Engine";
		}
		statusMsg( kFName, "Using " + which + " with url " + mTargetSearchEngineConfig.getSearchEngineURL() );
	}
	
	SearchEngineConfig getTargetSearchEngine()
	{
		return mTargetSearchEngineConfig;
	}
		
	SearchEngineConfig getGooglePubConfig()
		throws
			// nie.sr2.util.UtilException,
			nie.core.JDOMHelperException,
			nie.sn.SearchEngineConfigException,
			nie.sn.SearchTuningConfigFatalException
	{
		final String kFName = "getGooglePubConfig";
		final String kExTag = kClassName + '.' + kFName + ": ";

		JDOMHelper outXML = null;
		debugMsg( kFName, "Looking for xml " + GOOGLE_CONFIG_URI );

		// Force system: references relative to this class
		AuxIOInfo tmpAuxInfo = new AuxIOInfo();
		tmpAuxInfo.setSystemRelativeBaseClassName( SYSTEM_RESOURCE_BASE_CLASS );
		// Now load the predefined XML
		JDOMHelper configXml = new JDOMHelper(
			GOOGLE_CONFIG_URI,
			null, // optRelativeRoot
			0, tmpAuxInfo
			);

		// Then convert to configuration
		SearchEngineConfig outConfig =
			new SearchEngineConfig( configXml.getJdomElement() );
		
		return outConfig;
	}
	
	
	public boolean hadError() {
		return mHadError;
	}

    
    ////////////////////////////////////////////////
    //
    // Parse an incomming array of strings that
    // are considered command line switches.
    // The following command line switches are
    // handled:
    //
    //		-m
    //		--minimum (default)
    //			Process only IP numbers that have not
    //			been seen before on any run.
    //
    //		-i
    //		--intermediate
    //			process only IP numbers that have not been
    //			seen before OR that have not been successfully
    //			resolved before.
    //
    //		-r
    //		--refresh
    //			process all IP numbers, regardless of whether
    //			they've been seen before or not.  Note that this
    //			will NOT change a resolved host to an unresolved
    //			one.
    //
    //		-f
    //		--full
    //			process all IP Numbers, regardless of whether
    //			they've been seen before or not.  Note that this
    //			WILL change a resolved host to an unresolved one.
    //
    //		These flags are mutually exclusive - the one encountered
    //		LAST on the command  line will be the one used.
    //
    //		The following switches may be used in conjunction with any
    //		of the above switches.
    //
    //		-d
    //		--driver
    //			Specify the JDBC driver to use.  Please refer to the
    //			JDBC API descriptions for information on this URL.
    //			There's a whole chapter on it there that I don't want
    //			to include as a comment.
    //
    //		-a
    //		--account
    //			Specify the account name to be used in connecting with
    //			the database.
    //
    //		-p
    //		--password
    //			Specify the password to be used in connecting  with the
    //			database.
    //
    //		-g
    //		--debug
    //			Specify debug mode
    //
    ////////////////////////////////////////////////////

	void parseCommandLine( String inArgs[] )
	{

		final String kFName = "parseCommandLine";

		boolean haveSeenConfigURI = false;

		// For each argument on the command line
		for( int i = 0; i < inArgs.length; i++ )
		{
			// If the argument starts with a dash then it's a switch
			///////
			if( inArgs[i].startsWith( "-" ) )
			{
				String lFlag = inArgs[i].substring( 1 ).toLowerCase();

				// See if it's a verbosity flag
				// boolean result = getRunLogObject().setVerbosityByString( lFlag, false );
				boolean result = getRunLogImplObject().setVerbosityByString(
					inArgs[i], false
					);
//                  // ^^^ Must preserve original case when controlling logging
//                  //  and setVerbosityByString can handle optional leading hyphen
//					lFlag, false
//					);

				// If it's not verbosity, keep ckecking
				if( ! result )
				{

				    // if( lFlag.equals( "minimum" ) )
					if( lFlag.equals( "null" ) )
						fOperatingMode = kNewOnlyMode;
					else if( lFlag.equals( "overwrite" ) ) 
						fOperatingMode = kOverwriteAllMode;
					// Use Google
					else if( lFlag.equals("use-google") || lFlag.equals("usge_google") || lFlag.equals("google") ) {
						mUseGoogleInstead = true;
					}
					// Site prefix (for google)
					else if( lFlag.equals("site-prefix") || lFlag.equals("site_prefix") || lFlag.equals("prefix") ) {
						if( i == inArgs.length-1 )
							bailOnBadSyntax( inArgs[i], "requires an argument" );
						String arg = inArgs[++i];
						mSitePrefix = NIEUtil.trimmedStringOrNull( arg );
						if( null==mSitePrefix )
							bailOnBadSyntax( arg, "prefix requires an argument" );
					}
					else
						// We don't know what it is
						bailOnBadSyntax( inArgs[i] );
				}
			}
			else {
				// If it's not a switch then the only other thing it
				// can legally be is the name of an alternate config
				// file.
				///////
				if( ! haveSeenConfigURI )
				{
					fConfigFileURI = inArgs[i];
					haveSeenConfigURI = true;
					getRunLogObject().debugMsg( kClassName, kFName,
						"Command line option " + (i+1)
						+ " is config file name \"" + fConfigFileURI + "\"."
						);
				}
				// Else we've already seen the config!
				else
				{
					getRunLogObject().fatalErrorMsg( kClassName, kFName,
						"Can only specify one config file"
						+ " on the command line."
						);
					bailOnBadSyntax( inArgs[i] );
				}
			}
		}	// End for each command line option

		debugMsg( kFName, "haveSeenConfigURI=" + haveSeenConfigURI );

		if( ! haveSeenConfigURI )
			bailOnBadSyntax( "<path-to-config-file>", "No config file given on command line." );

		/***
		if( ! haveSeenConfigURI ) {
			fatalErrorMsg( kFName,
				"No configuration file given on command line."
				);
			System.exit(1);
		}
		***/

	}

   

	private void bailOnBadSyntax( String inOpt )
	{
		bailOnBadSyntax( inOpt, null );
	}

	private void bailOnBadSyntax( String inOpt, String optMsg )
	{
		final String kFName = "bailOnBadSyntax";


		String msg = "Bad Command Line Syntax: ";
		if( optMsg != null )
			msg += optMsg;
		else
			msg += "Unknown option \"" + inOpt + "\"";
		msg += NIEUtil.NL;

		msg += "Required Parameter:" + NIEUtil.NL
			+ "\tdatabase_config_file_name_or_url.xml" + NIEUtil.NL
			+ NIEUtil.NL
			;

		msg += "Primary Options (choose only one):" + NIEUtil.NL
		+ NIEUtil.NL
		+ "-null (default)" + NIEUtil.NL
		+ "	Process only records" + NIEUtil.NL
		+ "	where the count is missing." + NIEUtil.NL
		+ NIEUtil.NL
		+ "-overwrite" + NIEUtil.NL
		+ "	Reprocess all records" + NIEUtil.NL
		+ "	whether they have a count or not." + NIEUtil.NL
		+ "	WILL change existing values." + NIEUtil.NL
		+ NIEUtil.NL
		+ "These flags are mutually exclusive - the one encountered" + NIEUtil.NL
		+ "LAST on the command  line will be the one used." + NIEUtil.NL
		+ NIEUtil.NL
		+ "-use_google (instead, for adding match counts)" + NIEUtil.NL
		+ "-site_prefix something.com (site prefix for using with Google)" + NIEUtil.NL
		+ NIEUtil.NL
		;


		msg = msg + RunLogBasicImpl.getVerbosityLevelDescriptions(
			true, true, true, true
			);

		getRunLogObject().fatalErrorMsg( kClassName, kFName,
			msg
			);


		System.exit( 1 );
	}





	///////////////////////////////////////////////////////////
	private static final void __Main_Logic__() {}
    
    public void run()
    {
    	final String kFName = "run";

		// Hashtable lCandidateIPNumbersHash = null;
		// Hashtable lShouldNotResolveHash = null;
		// Hashtable lDoNotChangeToUnknownHash = null;

		ResultSet lCandidateIPNumbersResultSet = null;
		try
		{
			mHadError = false;

			commonInit();

			// debugMsg( kFName,
			//	"Executing SQL: " + kGetCandidateIPNumbers
			//	);
			setupQuery();
			debugMsg( kFName,
				"Executing SQL: " + fSQL
				);

		    lCandidateIPNumbersResultSet =
				cStatementRead.executeQuery( fSQL )
				;
			debugMsg( kFName,
				"Result Set returned."
				);

			// Now process them
			doProcessRecords( lCandidateIPNumbersResultSet );	    

			if( getDBConfig().getVendorNeedsCommitByDefault() )
				cConnectionUpdate.commit();

			// Report Cache Cleanup
			// Do we have any cleanup work to do?
			// CURRENTLY (July 2008) this is not needed.
			// The only reports that currently cache are the Trend
			// reports, and they only rely on the number of times
			// a search was run, and NOT on the match count.
			/***
			if( count>0 ) {
				statusMsg( kFName, "Clearing report cache ..." );
				// Would ALSO need getMainConfigURIOrNull() see RollDates
				nie.sr2.java_reports.ActivityTrend.clearReportCache( fConfigFileURI );
			}
			// Else no sense doing those if no data added
			else {
				statusMsg( kFName, "No Records added, so skipping clearing of report cache and any back filling results." );
			}
			***/
		}
		catch( UtilException de )
		{
			// errorMsg( kFName, "SQL Exception caught: " + se );
			stackTrace( kFName, de, "DNS Exception or Init Error" );
			mHadError = true;
			// se.printStackTrace();
		}
		catch( SQLException se )
		{
		    // errorMsg( kFName, "SQL Exception caught: " + se );
			stackTrace( kFName, se, "SQL Exception" );
			mHadError = true;
		    // se.printStackTrace();
		}
		catch( Exception e )
		{
			stackTrace( kFName, e, "General Exception Caught" );
			mHadError = true;

		    // fatalErrorMsg( kFName, "General exception caught: " + t );
		    // e.printStackTrace();
		    // System.exit(-1);
		}
		finally {
			// quickie cleanup!
			lCandidateIPNumbersResultSet = DBConfig.closeResults( lCandidateIPNumbersResultSet, kClassName, kFName, false );
			cStatementRead = DBConfig.closeStatement( cStatementRead, kClassName, kFName, false );
			cConnectionRead = DBConfig.closeConnection( cConnectionRead, kClassName, kFName, false );
			cStatementUpdate = DBConfig.closeStatement( cStatementUpdate, kClassName, kFName, false );
			cConnectionUpdate = DBConfig.closeConnection( cConnectionUpdate, kClassName, kFName, false );
		}



    }
    
	// These are things that are done ONCE PER RUN
	// and this is NOT part of the constructor chain
    public void commonInit()
    	throws Exception
    {
		final String kFName = "commonInit";
		final String kExTag = kClassName + '.' + kFName + ": ";

		if( null==getDBConfig() )
			throw new Exception( kExTag +
				"Null DB config."
				);

		// We wait until now so that other users
		// have a chance to use the set() methods
		// related to Google
		setupSearchEngineConfig();

		cStatementRead = null;
		cConnectionRead = null;
		cStatementUpdate = null;
		cConnectionUpdate = null;
		fSQL = null;

		// Configure the database and cache a statement
		try
		{
			// fDBConf = new DBConfig( fConfigFileURI );
			// ^^^ moved to setupDBConfigFromURI()

			// cStatement = getDBConfig().createStatement();
		    debugMsg( kFName, "getDBConfig()=" + getDBConfig() );
			Object [] objs = getDBConfig().createStatement();
			cStatementRead = (Statement) objs[0];
			cConnectionRead = (Connection) objs[1];

			objs = getDBConfig().createStatement();
			cStatementUpdate = (Statement) objs[0];
			cConnectionUpdate = (Connection) objs[1];
		}
		catch( Exception e )
		{
		    stackTrace( kFName, e,
		        "Exception while caching database statements"
		        );
		    throw new UtilException( kExTag +
				"Error caching statement: " + e
				);
		}

		// Init the answer cache
		mDocCountCache = new Hashtable();
    }

	void setupQuery()
	{
		final String kFName = "setupQuery";

		switch( fOperatingMode )
		{

		case kNewOnlyMode:
			debugMsg( kFName,
				"Running in minimum / new_only mode"
				+ " (processing only records in log that do not"
				+ " have results)"
				);
			fSQL = SQL_NEW_ONLY;
			break;

		    
		case kOverwriteAllMode:
			debugMsg( kFName,
				"Running in overwrite_all mode (process all records)"
				);
			fSQL = SQL_ALL_RECORDS;
			fOverwriteGoodWithNotResolved = true;
			break;

				
		default:
	
			fatalErrorMsg( kFName,
				"Invalid operating mode \"" + fOperatingMode + "\""
				);
			System.exit(-1);

			break;


		}	// End switch on fOperatingMode
	}


    ///////////////////////////////////////////////////////////
    //
    // Process the three lists, updating the domainnames table.
    //
    ///////////////////////////////////////////////////////////
   
//    void doProcessIPNumbers(
//			     Hashtable inCandidates,
//			     Hashtable inDoNotResolve,
//			     Hashtable inDoNotUpdateToUnknown
//		)
//    {
	void doProcessRecords( ResultSet inCandidateResults )
		throws SQLException
	{
    	final String kFName = "doProcessRecords";
		final int kReportInterval = 10; // 1000;

		if( null == inCandidateResults )
		{
			errorMsg( kFName,
				"Null results set, exiting method."
				);
			return;
		}


		// int lExpectedTotal = inCandidates.size();
		int scannedCount = 0;
		// int lSkippedCount = 0;
		int checkedCount = 0;
		int foundCount = 0;
		int notFoundCount = 0;
		int cacheHitCount = 0;
		// statusMsg( kFName, "Will check " + lExpectedTotal + " records" );

		debugMsg( kFName, "inCandidateResults=" + inCandidateResults );



		// For each candidate IP address
		while( inCandidateResults.next() )
		{
			String queryText = inCandidateResults.getString( 1 );
			traceMsg( kFName,
				"Main loop: Checking '" + queryText + "'"
				);
			queryText = NIEUtil.trimmedLowerStringOrNull( queryText );

			// statusMsg( kFName, "main config = " + getMainConfig() );

			String queryKey = getMainConfig().isNullSearch( queryText )
				? SearchTuningConfig.getNullSearchMarker()
				: queryText
				;



			scannedCount++;
			if( scannedCount % kReportInterval == 0 )
				statusMsg( kFName,
					"Checking record " + scannedCount
					+ " with query='" + queryText + "'."
					+ " Previously:"
					// + " skipped=" + lSkippedCount
					+ " cache hits=" + cacheHitCount
					+ " checked=" + checkedCount
					+ ", found=" + foundCount
					+ ", not found=" + notFoundCount
					+ ". (stats do NOT include the current record)"
					);

			if( mDocCountCache.containsKey(queryText) )
			{
				cacheHitCount++;
				continue;
			}

			// Or count it as checked
			// Errors will show up in unresolved total
			// and error/warning messages
			checkedCount++;

			// Do the search
			String rawResults = null;
			int count = -1;
			int searched = -1;
		    try
		    {
				rawResults = doSearch( queryText );
				// do parsing....
				List tmpList = parseResults( rawResults );
				if( null!=tmpList && tmpList.size() > 0 )
					count = ( (Integer) tmpList.get(0) ).intValue();
				if( null!=tmpList && tmpList.size() > 1 )
					searched = ( (Integer) tmpList.get(1) ).intValue();
				
				// Remember in our cache
				// Create placeholder non null object, if needed
				if( null==tmpList ) {
					tmpList = new Vector();
					tmpList.add( new Integer(-1) );
					tmpList.add( new Integer(-1) );
				}
				mDocCountCache.put( queryKey, tmpList );
			}
			catch( IOException uhe )
			{
				errorMsg( kFName,
					"Unable to get count for search '" + queryText + "'"
					+ " Will continue looking at any subsequent records."
					+ " Error was: " + uhe
					);
			    notFoundCount++;
				continue;
			}

			if( count < 0 )
			{
				errorMsg( kFName,
					"Could not parse count from results for query '"
					+ queryText + "'"
					+ " doc=" + NIEUtil.NL + rawResults + NIEUtil.NL
					+ " Will continue looking at any subsequent records."
					);
			    notFoundCount++;
				continue;						
			}

	    	boolean result = updateRecord( queryText, count, searched );
	    	foundCount++;

			traceMsg( kFName,
				"Bottom of main loop"
				);

			// warningMsg( kFName, "hard coded premature break for debugging" );
			// break;

	    }	// End for each candidate IP number

		statusMsg( kFName,
			"Done."
			+ " Final statistics:"
    		+ " # candidates=" + scannedCount
			// + ", skipped=" + lSkippedCount
			+ ", cache hits=" + cacheHitCount
			+ ", checked=" + checkedCount
			+ ", found=" + foundCount
			+ ", not found=" + notFoundCount
			);


		if( foundCount + notFoundCount != checkedCount )
			errorMsg( kFName,
				"# records resolved + not resolved != # DNS-checked records:"
				+ " checks=" + checkedCount
				+ ", resolved=" + foundCount
				+ "+ not found=" + notFoundCount
				);

    }
 
	public String doSearch( String inQuery )
		throws IOException
	{
		// Use host search engine
		// Or use Google
		// nie.sn.SearchEngineConfig
		// This is whether to use PUBLIC Google, vs. local appliance / OneBox
		if( getUseGoogleInstead() ) {
			inQuery = "site:" + getSitePrefix() + ' ' + inQuery;
		}

		// Prepare the request
		AuxIOInfo requestObject = new AuxIOInfo();
		// TODO: Add in other test drive fields, if any, from search config
		// requestObject.addCGIField( getTargetSearchEngine().getQueryField(), inQuery );
		// requestObject.addCGIField( "hl", "en" );
		// requestObject.addCGIField( "btnG", "Google Search" );

		// We want to retain information about the search results
		mIntermediateIoInfo = new AuxIOInfo();

		return nie.sn.SnRequestHandler.staticDoActualSearch(
				inQuery, getTargetSearchEngine(), requestObject, mIntermediateIoInfo, null
				);
	}

	// Refactored as wrapper around static version so that we can
	// borrow this from other classes
	// TODO: Mime-type should be pushed way down into the lower static methods
	// someday, so they have a choice of how to parse
	List parseResults( String inDoc )
	{
		final String kFName = "parseResults";

		// Double check content type / mime type
		// TODO: last 2 could be turned down from warning to info/status if it
		// comes up a lot, as it might parse OK, but for now we'd like to know about it and people
		// will not generally turn on higher levels
		String contentType = null;
		if( null!=mIntermediateIoInfo )
			contentType = mIntermediateIoInfo.getContentType();
		if( null==contentType )
			warningMsg( kFName, "Null content-type / mime-type" );
		else if( ! contentType.startsWith("text/") )
			warningMsg( kFName, "Binary content-type / mime-type '" + contentType + "'" );
		else if( contentType.startsWith("text/xml") )
			warningMsg( kFName, "XML content-type / mime-type - but still using text parsing, which is probably still OK" );
		else if( ! contentType.startsWith("text/html") )
			warningMsg( kFName, "Non HTML/XML content-type / mime-type '" + contentType + "' - which is unusual but may still parse OK" );

		return parseResults_static(
			inDoc, getMainConfig().getSearchLogger(), getUseGoogleInstead()
			);
	}

	public static List parseResults_static( String inDoc, nie.sn.SearchLogger inSearchLogger,
			boolean inUseGoogleInstead )
	{
		return parseResults_static( inDoc, inSearchLogger,
			inUseGoogleInstead, true
			);
	}
	public static List parseResults_static( String inDoc, nie.sn.SearchLogger inSearchLogger,
			boolean inUseGoogleInstead, boolean inDoParseWarnings
	) {
		final String kFName = "parseResults_static";
		// Google is " of about <b>"
		// and then "</b> - did not match any documents.  <br><br>"
		// statusMsg( kFName, inDoc );

		if( inUseGoogleInstead ) {
			// Google public engine
			List tmpList = nie.sn.SearchLogger.staticParseValuesFromDocument(
				inDoc,
				GOOGLE_MATCH_PATTERN_1,			// Google matched, the "of about" version
				GOOGLE_NO_MATCH_PATTERN,		// Google no match
				nie.sn.SearchLogger.DEFAULT_DOCS_FOUND_LOOK_AHEAD,
				false,
				nie.sn.SearchLogger.DEFAULT_DOCS_FOUND_MAX_SEARCHED_LOOK_AHEAD,
				inDoParseWarnings
				);
			// Double check for the secondary Google pattern
			// If we got nothing for the first
			if( null==tmpList || tmpList.isEmpty() || ((Integer)tmpList.get(0)).intValue() < 0 )
			{
				statusMsg( kFName, "Trying SEONDARY Google pattern." );
				return nie.sn.SearchLogger.staticParseValuesFromDocument(
						inDoc,
						GOOGLE_MATCH_PATTERN_2,			// Google matched, the second version that just says "of"
						null,
						nie.sn.SearchLogger.DEFAULT_DOCS_FOUND_LOOK_AHEAD,
						false,
						nie.sn.SearchLogger.DEFAULT_DOCS_FOUND_MAX_SEARCHED_LOOK_AHEAD,
						inDoParseWarnings
						);			
			}
			// Else just return whatever we had
			else {
				// statusMsg( kFName, "Sticking with primary Google pattern, tmpList[0] =" + tmpList.get(0) );
				return tmpList;
			}
		}
		// Else just use the regular engine
		else {
			// Local engine
			return nie.sn.SearchLogger.staticParseValuesFromDocument( inDoc, inSearchLogger, inDoParseWarnings );
		}
	}

	
	///////////////////////////////////////////////////////////
	//
	// Set a given IP number to "unknown"
	//
	///////////////////////////////////////////////////////////

	public boolean updateRecord(
		String inQueryText, int inCount, int optSearchedCount
		)
	{
		final String kFName = "updateRecord";

		inQueryText = NIEUtil.trimmedLowerStringOrNull( inQueryText );
		if( null==inQueryText ) {
			errorMsg( kFName, "Null search passed in, returning false." );
			return false;
		}
		if( inCount < 0 ) {
			errorMsg( kFName, "Negative count passed in, returning false." );
			return false;
		}

		boolean debug = shouldDoDebugMsg( kFName );
		boolean info = shouldDoInfoMsg( kFName );

		boolean outSuccess = false;
    	
		int updateCount = 0;
		String lastSql = null;
		try {
			
			String updateSql = (null!=inQueryText)
				? kUpdateSQL_1 : kUpdateSQL_2;
			if( fOperatingMode == kOverwriteAllMode )
				updateSql += kUpdateSQL_Suffix;

			// Put the actual values in the update statement
			// TODO: we could use a prepared statement instead
			updateSql = replaceAll( updateSql, VALUE_MARKER_1, ""+inCount );
			if( optSearchedCount >= 0 ) {
				updateSql = replaceAll( updateSql, VALUE_MARKER_2, ""+optSearchedCount );
			}
			else {
				updateSql = replaceAll( updateSql, VALUE_MARKER_2, "NULL" );
			}
			if( null!=inQueryText )
				updateSql = replaceAll( updateSql, KEY_MARKER, NIEUtil.sqlEscapeString(inQueryText,true) );

			if( debug )
				debugMsg( kFName,
					"Trying to update using SQL statement: \""
					+ updateSql + "\""
					);
			lastSql = updateSql;
			cStatementUpdate.execute( updateSql );

			updateCount = cStatementUpdate.getUpdateCount();
			if( debug )
				debugMsg( kFName, "Update count = " + updateCount );

			
			if( updateCount > 0 )
				outSuccess = true;
			/***
			else {
				// If update didn't work, try inserting
				if( debug )
					debugMsg( kFName,
						"Update failed, will try insert."
						);

				// Work on the INSERT statement

				// Host name
				String lSQLInsertText =
					replaceAll( insertSql, kHOSTNAME, inHostName );
				// IP address
				lSQLInsertText =
					replaceAll( lSQLInsertText, kIPNUMBER, inIPNumber );

				lSQLInsertText =
					replaceAll( lSQLInsertText, kRESOLVED, kWAS_NOT_RESOLVED );

				// Resolved yes / no (actually 0/1)
				lSQLInsertText = replaceAll(
					lSQLInsertText, kRESOLVED,
					( inWasResolved ? kWAS_RESOLVED : kWAS_NOT_RESOLVED )
					);

				if( debug )
					debugMsg( kFName,
						"Trying to insert using SQL statement: \""
						+ lSQLInsertText + "\""
						);
				lastSql = lSQLInsertText;
				cStatementUpdate.execute( lSQLInsertText );
				updateCount = cStatementUpdate.getUpdateCount();
				if( debug )
					debugMsg( kFName, "Insert count = " + updateCount );

				if( updateCount > 0 )
					outSuccess = true;
				else {
					outSuccess = false;
					debugMsg( kFName, "Insert also failed; returning false" );
				}
			}
			***/

		}
		catch( SQLException se )
		{
			outSuccess = false;
			errorMsg( kFName,
				"Error executing SQL \"" + lastSql + "\""
				+ "; returning false."
				+ " SQL Exception: " + se
				);

			// fatalErrorMsg( kFName, msg );
			//System.exit( -1 );
		}

		return outSuccess;

	}

	///////////////////////////////////////////////////////////
	private static final void __Utility__() {}


    ///////////////////////////////////////////////////////////
    //
    // Early Java VMs (like the one that is distributed with 
    // Oracle 9i server) is 1.2.x, which is missing numerous
    // functions.  These are replacements.
    //
    ///////////////////////////////////////////////////////////

	// TODO: replace with call to standard NIEUtil equivalent
    static String replaceAll( String inSourceString,
    	String inSearchString, String inReplacementString
    	)
    {
		int lPosition = inSourceString.indexOf( inSearchString );
		if( lPosition >= 0 )
	    {
			String lPrefixString = inSourceString.substring( 0, lPosition );
			String lPostString = replaceAll( inSourceString.substring( lPosition + inSearchString.length() ), inSearchString, inReplacementString );
			return lPrefixString + inReplacementString + lPostString;
	    }
		else
		    return inSourceString;
    }

	///////////////////////////////////////////////////////////
	private static final void __Simple_Getters_and_Setters__() {}

	boolean getUseGoogleInstead()
	{
		return mUseGoogleInstead;
	}
	void setUseGoogleInstead()
	{
		setUseGoogleInstead( true );
	}
	void setUseGoogleInstead( boolean inFlag )
	{
		mUseGoogleInstead = inFlag;
	}

	String getSitePrefix()
	{
		return mSitePrefix;
	}
	void setSitePrefix( String inPrefx )
	{
		mSitePrefix = NIEUtil.trimmedStringOrNull( inPrefx );
	}

	public static String getLogTableName()
	{
		return DBConfig.LOG_TABLE;
	}



    ///////////////////////////////////////////////////////////
    //
    // Build a tree set from a result set.  If the input result
    // set is null, then we return an initialized, but empty,
    // tree set.
    //
    ///////////////////////////////////////////////////////////
    
    // private TreeSet buildIPTreeSet( ResultSet inResultSet )
    private Hashtable _buildIPTreeSet( ResultSet inResultSet )
    {
		final String kFName = "buildIPTreeSet";
		boolean debug = shouldDoDebugMsg( kFName );

		// if( gComparator == null )
		//	gComparator = new DotNotationComparator();
	
		// TreeSet lTreeSet = new TreeSet( gComparator );
		Hashtable outHash = new Hashtable();

		if( inResultSet != null )
		{
		    try
		    {
				while( inResultSet.next() )
				{
				    String lIPNumberString;
				    
				    lIPNumberString = inResultSet.getString( 1 );
				    if( debug )
						debugMsg( kFName,
							"Converting '" + lIPNumberString + "'"
							);
				    // DotNotation lDotNotation =
				    //	new DotNotation( lIPNumberString );
				    // lTreeSet.add( lDotNotation );
				    // lTreeSet.add( lIPNumberString );
				    outHash.put( lIPNumberString, lIPNumberString );
				}
		    }
		    catch( SQLException se )
		    {
				fatalErrorMsg( kFName,
					"SQL Exception caught: " + se
					);
				se.printStackTrace();
				System.exit( -1 );
		    }
		}
		// return lTreeSet;
		return outHash;
	}

	// Needs to operate from command line or as a thread under main app
	DBConfig getDBConfig()
	{
	    final String kFName = "getDBConfig";
	    // if( null!=fDBConf )
		//	return fDBConf;
		if( null!=getMainConfig() )
			return getMainConfig().getDBConfig();
		return null;
	}
	public void setMainConfig( SearchTuningConfig inMainConfig ) {
		mMainConfig = inMainConfig;
	}
	SearchTuningConfig getMainConfig() {
		return mMainConfig;
	}
	void setDBConfig( DBConfig inDB ) {
		fDBConf = inDB;
	}

	///////////////////////////////////////////////////////////
	private static final void __Logging__() {}

	// This gets us to the logging object
	private static RunLogInterface getRunLogObject()
	{
		return RunLogBasicImpl.getRunLogObject();
	}

	// This gets us essentially the same thing, but casted
	// to let us to implementation specific things like parse
	// command line options
	private static RunLogBasicImpl getRunLogImplObject()
	{
		// return RunLogBasicImpl.getRunLogObject();
		return RunLogBasicImpl.getRunLogImplObject();
	}

	protected boolean stackTrace( String inFromRoutine, Exception e, String optMessage )
	{
		return getRunLogObject().stackTrace( kClassName, inFromRoutine,
			e, optMessage
			);
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

	private static boolean shouldDoInfoMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoInfoMsg( kClassName, inFromRoutine );
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


	///////////////////////////////////////////////////////////
	private static final void __Fields_and_Constants__() {}

	// private JDOMHelper fMainElem;

  

    
    ///////////////////////////////////////////////////////////
    //
    // Private members...
    //
    ///////////////////////////////////////////////////////////

	SearchTuningConfig mMainConfig;

	String mSitePrefix;
	
	Hashtable mDocCountCache;

	boolean mUseGoogleInstead = DEFAULT_USE_GOOGLE_INSTEAD;
	static final boolean DEFAULT_USE_GOOGLE_INSTEAD = false;

	public static final String GOOGLE_MATCH_PATTERN_1 =	" of about <b>";
	public static final String GOOGLE_MATCH_PATTERN_2 =	"</b> of <b>";
	public static final String GOOGLE_NO_MATCH_PATTERN = "</b> - did not match any documents.  <br><br>";
	
	boolean mHadError;
	private String fConfigFileURI;
	String _mErrorMsg;

	// The query we will use to pull records
	String fSQL;

	// The primary database configuration
	DBConfig fDBConf;
	Statement cStatementRead;
	Connection cConnectionRead;
	Statement cStatementUpdate;
	Connection cConnectionUpdate;

	// We want to retain information about the search results
	AuxIOInfo mIntermediateIoInfo;

	// Which search engine to use, we do NOT always use the
	// main configured one, for example we might use Google instead
	SearchEngineConfig mTargetSearchEngineConfig;

	int fOperatingMode = DEFAULT_MODE;

//	Statement cStatement;

    // String fDriverName;
    // String fPassword = "kklop";
    // String fAccount = "kklop";
    // String fPassword;
    // String fAccount;
    
    // private static Comparator gComparator = null;

	public static final String GOOGLE_CONFIG_URI =
		AuxIOInfo.SYSTEM_RESOURCE_PREFIX
		+ "static_files/predefined_configs/search_engine_google_public.xml";
	public static final String SYSTEM_RESOURCE_BASE_CLASS =
		nie.config_ui.Configurator2.kFullClassName;
		// = "nie.config_ui.Configurator2";

    
    static final int kErrorMode = 0;
    static final int kNewOnlyMode = 1;
    static final int _kRetryMode = 2;
    static final int _kRefreshMode = 3;
    static final int kOverwriteAllMode = 4;
    static final int DEFAULT_MODE = kNewOnlyMode;
 
	boolean fOverwriteGoodWithNotResolved = false;

    
    ////////////////////////////////////////////////////////////
    //
    // SQL Statements...
    //
    ////////////////////////////////////////////////////////////

	static final String SQL_NEW_ONLY =
		// "SELECT UNIQUE client_host FROM " + getLogTableName() + " log"
		"SELECT DISTINCT original_query FROM " + getLogTableName() + " log"
		+ " WHERE num_results IS NULL"
		;

	static final String SQL_ALL_RECORDS =
		"SELECT DISTINCT original_query FROM " + getLogTableName()
		;
    
    static final String KEY_MARKER = "VAR_KEY";
    static final String VALUE_MARKER_1 = "VAR_VALUE_1";
    static final String VALUE_MARKER_2 = "VAR_VALUE_2";
    
    // no inserts in this util:
    // static final String kInsertDomainNameSQL =
    
    static final String kUpdateSQL_1 =
    	"UPDATE " + getLogTableName()
    	+ " SET num_results = " + VALUE_MARKER_1
    	+ ", num_searched = " + VALUE_MARKER_2
    	+ " WHERE original_query = '" + NIEUtil.sqlEscapeString( KEY_MARKER, true ) + "'"
    	;
    static final String kUpdateSQL_2 =
    	"UPDATE " + getLogTableName()
    	+ " SET num_results = " + VALUE_MARKER_1
    	+ ", num_searched = " + VALUE_MARKER_2
    	+ " WHERE original_query IS NULL"
    	;
    static final String kUpdateSQL_Suffix =
    	" AND num_results IS NULL"
    	; 	
    
    // public static boolean _gDebug = false;
}
