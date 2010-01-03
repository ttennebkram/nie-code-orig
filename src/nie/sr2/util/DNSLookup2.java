package nie.sr2.util;

/*
	DNS Updater for the SearchTrack system.
	v1 Written by Kevin-Neil Klop
	v2 revisions by Mark L Bennett
	Copyright 2002 - 2008 by New Idea Engineering, Inc., All Rights Reserved.
	
*/

import java.sql.*;
import java.util.*;
import java.net.*;
import nie.core.*;
import nie.sn.SearchTuningConfig;

public class DNSLookup2 implements nie.sn.CronLiteJob // Runnable
{

	private final static String kClassName = "DNSLookup2";

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
		DNSLookup2 lLookup = new DNSLookup2();
		lLookup.parseCommandLine( inArgs );
		try
		{
			// lLookup.commonInit();
			// ^^^ moved to top of run()
			lLookup.setupDBConfigFromURI();
		}
		catch( DNSException e )
		{
			fatalErrorMsg( kFName,
				"Error initializing, exiting."
				+ " Error: " + e
				);
			System.exit( 2 );
		}
		lLookup.run();
    }

    
    ////////////////////////////////////////////////
    //
    // Constructors.
    //
    // All constructors should call commonInit()
    //
    ////////////////////////////////////////////////
    
    public DNSLookup2()
    //	throws DNSException
    {
		// commonInit();
		// ^^^ moved to AFTER we parsed the command line, so
		// we have a config file to use

    }

	public DNSLookup2( SearchTuningConfig inMainConfig )
	{
		this();
		setMainConfig( inMainConfig );
	}

	public boolean hadError() {
		return mHadError;
	}


	public void setupDBConfigFromURI()
		 throws DNSException
	{
		final String kFName = "setupDBConfigFromURI";
		final String kExTag = kClassName + '.' + kFName + ": ";
		try
		{
			fDBConf = new DBConfig( fConfigFileURI );
		}
		catch( Exception e )
		{
			throw new DNSException( kExTag
				+ "Error intializing config/data from URI \"" + fConfigFileURI + "\""
				+ " Error was: " + e
				);
		}
	}

    public void commonInit()
    	throws Exception
    {
		final String kFName = "commonInit";
		final String kExTag = kClassName + '.' + kFName + ": ";

		if( null==getDBConfig() )
			throw new Exception( kExTag +
				"Null DB config."
				);

		// fOperatingMode = kMinimumMode;
		// fDriverName = kDefaultDBURL;

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
		    throw new DNSException( kExTag +
				"Error caching statement: " + e
				);
		}
	

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
					if( lFlag.equals( "new" ) )
						fOperatingMode = kNewOnlyMode;
				    // else if( lFlag.equals( "intermediate") )
					else if( lFlag.equals( "retry") )
						fOperatingMode = kRetryMode;
				    else if( lFlag.equals( "refresh" ) ) 
						fOperatingMode = kRefreshMode;
				    // else if( lFlag.equals( "full" ) ) 
					else if( lFlag.equals( "overwrite" ) ) 
						fOperatingMode = kOverwriteAllMode;
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
		// + "-minimum (default)" + NIEUtil.NL
		+ "-new (default)" + NIEUtil.NL
		+ "	Process only IP numbers that have not" + NIEUtil.NL
		+ "	been seen before on any run." + NIEUtil.NL
		+ NIEUtil.NL
		// + "-intermediate" + NIEUtil.NL
		+ "-retry" + NIEUtil.NL
		+ "	process only IP numbers that have not been" + NIEUtil.NL
		+ "	seen before OR that have not been successfully" + NIEUtil.NL
		+ "	resolved before." + NIEUtil.NL
		+ NIEUtil.NL
		+ "-refresh" + NIEUtil.NL
		+ "	process all IP numbers, regardless of whether" + NIEUtil.NL
		+ "	they've been seen before or not.  Note that this" + NIEUtil.NL
		+ "	will NOT change a resolved host to an unresolved" + NIEUtil.NL
		+ "	one." + NIEUtil.NL
		+ NIEUtil.NL
		// + "-full" + NIEUtil.NL
		+ "-overwrite" + NIEUtil.NL
		+ "	process all IP Numbers, regardless of whether" + NIEUtil.NL
		+ "	they've been seen before or not.  Note that this" + NIEUtil.NL
		+ "	WILL change a resolved host to an unresolved one." + NIEUtil.NL
		+ NIEUtil.NL
		+ "These flags are mutually exclusive - the one encountered" + NIEUtil.NL
		+ "LAST on the command  line will be the one used." + NIEUtil.NL
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






    
    ////////////////////////////////////////////////////
    //
    // And here's where we actually run.  What this
    // does is set up the SQL connections, then build three
    // lists of IP numbers:
    //
    //	List 1: All IP Numbers that are in the "source"
    //			table.
    //	List 2: All IP Numbers that shouldnot be resolved.
    //	List 3:	All IP Numbers that should not be modified IF
    //			they could not be resolved on this run.
    //
    ////////////////////////////////////////////////////
    
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

		    ///////
		    //
		    // Get the list of candidate IP numbers
		    //
		    ///////



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
			doProcessIPNumbers( lCandidateIPNumbersResultSet );	    

			// Commit if needed
			if( getDBConfig().getVendorNeedsCommitByDefault() )
				cConnectionUpdate.commit();

		}
		catch( DNSException de )
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
    

	void setupQuery()
	{
		final String kFName = "setupQuery";

		switch( fOperatingMode )
		{

		case kNewOnlyMode:
			debugMsg( kFName,
				"Running in minimum / new_only mode"
				+ " (processing only IP numbers in log that do not"
				+ " apepar in domainnames)"
				);
			fSQL = SQL_NEW_ONLY;
			break;

		    
		case kRetryMode:
			debugMsg( kFName,
				"Running in retry mode"
				+ " (IP numbers that we've not seen before OR that"
				+ " have not been previously successfully resolved)"
				);
			fSQL = SQL_NEW_AND_RETRY;
			break;

			
		case kRefreshMode:
			debugMsg( kFName,
				"Running in refresh mode"
				+ " (process all IP numbers in log."
				+ " This will NOT change an already identified IP"
				+ " number to unidentified but will update it if"
				+ " we can resolve it during this run)"
				);
			fSQL = SQL_ALL_IPS;
			break;

				
		case kOverwriteAllMode:
			debugMsg( kFName,
				"Running in overwrite_all mode (process all IP numbers"
				+ " in log.  This WILL change an already"
				+ " identified IP number to unidentified if we"
				+ " can't update it during this run)"
				);
			fSQL = SQL_ALL_IPS;
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
	void doProcessIPNumbers( ResultSet inCandidateResults )
		throws SQLException
	{
    	final String kFName = "doProcessIPNumbers";
		final int kReportInterval = 1000;

		if( null == inCandidateResults )
		{
			errorMsg( kFName,
				"Null results set, exiting method."
				);
			return;
		}


		// int lExpectedTotal = inCandidates.size();
		int lCurrScannedCount = 0;
		// int lSkippedCount = 0;
		int lDNSCheckedCount = 0;
		int lResolvedCount = 0;
		int lDidNotResolveCount = 0;
		// statusMsg( kFName, "Will check " + lExpectedTotal + " records" );

		debugMsg( kFName, "inCandidateResults=" + inCandidateResults );

		// For each candidate IP address
		while( inCandidateResults.next() )
		{
			String lIPNumberString = inCandidateResults.getString( 1 );
			traceMsg( kFName,
				"Main loop: Checking '" + lIPNumberString + "'"
				);
			lIPNumberString = NIEUtil.trimmedLowerStringOrNull( lIPNumberString );
			if( null==lIPNumberString ) {
				errorMsg( kFName, "Null IP Address field, skipping this record." );
				continue;
			}


			lCurrScannedCount++;
			if( lCurrScannedCount % kReportInterval == 0 )
				statusMsg( kFName,
					"Checking record " + lCurrScannedCount
					+ " Previously:"
					// + " skipped=" + lSkippedCount
					+ " DNS-checked=" + lDNSCheckedCount
					+ ", resolved=" + lResolvedCount
					+ ", not resolved=" + lDidNotResolveCount
					+ ". (stats do NOT include the current record)"
					);

			// Or count it as checked
			// Errors will show up in unresolved total
			// and error/warning messages
			lDNSCheckedCount++;

			// Convert an IP number string to a true IP Address Object
		    InetAddress lAddress = null;
		    try
		    {
		    	lAddress = InetAddress.getByName(
		    		lIPNumberString
		    		);
			}
			catch( UnknownHostException uhe )
			{
				errorMsg( kFName,
					"Unable to create IP address object from string"
					+ " \"" + lIPNumberString + "\", setting as unknown."
					+ " Will continue looking at any subsequent addresses."
					+ " Error was: " + uhe
					);
			    setUnknown(
					lIPNumberString
			    	);
			    lDidNotResolveCount++;
				continue;
			}
			if( null == lAddress )
			{
				errorMsg( kFName,
					"Got back NULL IP address object from string"
					+ " \"" + lIPNumberString + "\", setting as unknown."
					+ " Will continue looking at any subsequent addresses."
					);
			    setUnknown(
					lIPNumberString
			    	);
			    lDidNotResolveCount++;
				continue;						
			}

			// Using the true IP Address object, get it's host name
			String lHostName = lAddress.getHostName();

			// Tweak to fix local host
			if( lHostName.equalsIgnoreCase( lIPNumberString ) 
				&& lIPNumberString.equals( "127.0.0.1" )
				)
					lHostName = "localhost";


			if( lHostName.equalsIgnoreCase( lIPNumberString ) ) {
				infoMsg( kFName,
					"Got back same string from getHostName for string"
					+ " \"" + lIPNumberString + "\", setting as unknown."
					+ " Will continue looking at any subsequent addresses."
					);
		    	setUnknown(
					lIPNumberString
		    		);
		    	lDidNotResolveCount++;
			}
			else {
		    	boolean result = setWasResolved(
					lIPNumberString,
		    		lHostName
		    		);
		    	lResolvedCount++;
			}

			traceMsg( kFName,
				"Bottom of main loop"
				);

	    }	// End for each candidate IP number

		statusMsg( kFName,
			"Done."
			+ " Final statistics:"
    		+ " # candidates=" + lCurrScannedCount
			// + ", skipped=" + lSkippedCount
			+ ", DNS-checked=" + lDNSCheckedCount
			+ ", resolved=" + lResolvedCount
			+ ", not resolved=" + lDidNotResolveCount
			);

		/***
		if( lExpectedTotal != lCurrScannedCount )
			errorMsg( kFName,
				"# scanned records not equal number expected:"
				+ " expected=" + lExpectedTotal
				+ ", actual candidates scanned=" + lCurrScannedCount
				);

		if( lSkippedCount + lDNSCheckedCount != lCurrScannedCount )
			errorMsg( kFName,
				"# records checked+skipped != # scanned candidate records:"
				+ " scanned candidate=" + lCurrScannedCount
				+ ", DNS-checked=" + lDNSCheckedCount
				+ "+ skipped=" + lSkippedCount
				);
		***/

		if( lResolvedCount + lDidNotResolveCount != lDNSCheckedCount )
			errorMsg( kFName,
				"# records resolved + not resolved != # DNS-checked records:"
				+ " DNS checks=" + lDNSCheckedCount
				+ ", resolved=" + lResolvedCount
				+ "+ not resolved=" + lDidNotResolveCount
				);

    }
    
    ///////////////////////////////////////////////////////////
    //
    // Set a given IP number to "unknown"
    //
    ///////////////////////////////////////////////////////////

    public boolean setUnknown( String inIPNumber )
    {
		final String kFName = "setUnknown";
		boolean debug = shouldDoDebugMsg( kFName );

		inIPNumber = NIEUtil.trimmedLowerStringOrNull( inIPNumber );
		if( null==inIPNumber ) {
			errorMsg( kFName, "Null IP number passed in, returning false." );
			return false;
		}

    	boolean shouldDoIt = false;

		// If we're in Overwrite mode we will trash even the valid records
		if( fOperatingMode == kOverwriteAllMode ) {
			shouldDoIt = true;
			if(debug) debugMsg( kFName, "In OVERWRITE mode, so will always do it." );
		}
		// Otherwise, BE CAREFUL
		// We only make "not resolved" entries if it will not overwrite
		// a previously good resolved entry
		else {
			if(debug) debugMsg( kFName, "Not in Overwrite mode, so need to check." );
			// So do we have any good entries?
			String sql = "SELECT count(*) FROM " + getDomainTableName()
				+ " WHERE client_host='" + NIEUtil.sqlEscapeString( inIPNumber, true ) + "'"
				+ " AND was_resolved=" + kWAS_RESOLVED
				;
			// find out if we already have resolved records, which would NOT want
			// to overwrite
			int existingCount = getDBConfig().simpleCountQuery( sql, true, true );

			if(debug) debugMsg( kFName,
				"Query \"" + sql + "\" gave back " + existingCount
				);
			// Do this only if no good entries
			shouldDoIt = existingCount < 1;
			if(debug) debugMsg( kFName, "After check shouldDoIt=" + shouldDoIt );
		}

		if( shouldDoIt )
			return updateDnsTable( inIPNumber, null, false );
		else
			return false;
	}

	public boolean setWasResolved( String inIPNumber, String inHostName )
	{
		return updateDnsTable( inIPNumber, inHostName, true );
	}

	// public void doSetUnknown( Statement inStatement,  DotNotation inIPNumber, TreeSet inDoNotUpdateToUnknown )
	// public void doSetUnknown( Statement inStatement,  String inIPNumber, TreeSet inDoNotUpdateToUnknown )
	public void _setUnknown(
		// Statement inStatement,
		String inIPNumber //,
		// Hashtable inDoNotUpdateToUnknown
		)
	{
		final String kFName = "setUnknown";
	
		// String lIPNumber = inIPNumber.asString();
		String lIPNumber = inIPNumber;
	
		boolean debug = shouldDoDebugMsg( kFName );
		
		if( debug )
	    	debugMsg( kFName,
	    		"Unknown IP: " + lIPNumber
	    		);
	
		// fOverwriteGoodWithNotResolved
	
		String insertSql = kInsertDomainNameSQL;
		String updateSql = kUpdateDomainNameSQL;
	
		String sysdateStr = getDBConfig().getVendorSysdateString();
		if( ! sysdateStr.equalsIgnoreCase("SYSDATE") ) {
			insertSql = NIEUtil.simpleSubstitution( insertSql, "SYSDATE", sysdateStr );
			updateSql = NIEUtil.simpleSubstitution( updateSql, "SYSDATE", sysdateStr );
		}
	
		// String lNewHostName = "Unknown " + inIPNumber.asString();
		String lNewHostName = "Unknown " + lIPNumber;
		String lSQLInsertText =
			// replaceAll( kInsertDomainNameSQL, kHOSTNAME, lNewHostName );
			replaceAll( insertSql, kHOSTNAME, lNewHostName );
		lSQLInsertText =
			replaceAll( lSQLInsertText, kIPNUMBER, lIPNumber );
		lSQLInsertText =
			replaceAll( lSQLInsertText, kRESOLVED, kWAS_NOT_RESOLVED );
		
		if( debug ) {
			debugMsg( kFName,
				"Trying to insert using SQL statement: \""
				+ lSQLInsertText + "\""
				);
	    }
	
		try {
	    	// inStatement.execute( lSQLInsertText );
	    	cStatementUpdate.execute( lSQLInsertText );
		}
		catch( SQLException se )
		{
		    // Perhaps the record is already in there and we need to update it.
		    // Check against the DoNotUpdateToUnknown list before doing that!!
		    
				// String lSQLUpdateText = replaceAll( kUpdateDomainNameSQL, kHOSTNAME, lNewHostName );
				String lSQLUpdateText = replaceAll( updateSql, kHOSTNAME, lNewHostName );
				lSQLUpdateText = replaceAll( lSQLUpdateText, kIPNUMBER, lIPNumber );
				lSQLUpdateText = replaceAll( lSQLUpdateText, kRESOLVED, kWAS_NOT_RESOLVED );
		
				if( debug ) {
				   	debugMsg( kFName,
				   		"Trying to update using SQL statement: \""
				   		+ lSQLUpdateText + "\""
				   		);
				}
		
				try {
				    // inStatement.execute( lSQLUpdateText );
				    cStatementUpdate.execute( lSQLUpdateText );
				}
				catch( SQLException se1 ) {
					String seStr = NIEUtil.exceptionToString( se );
					String se1Str = NIEUtil.exceptionToString( se1 );
	
					String msg =
				    	"Could not update database"
				    	+ " - got two SQL exceptions in a row."
				    	+ " First SQL Statement was: \""
				    	+ lSQLInsertText + "\""
				    	+ " First exception was: \"" + seStr + "\""
				    	+ " Second SQL Statement was: \""
				    	+ lSQLUpdateText + "\""
				    	+ " Second exception was: " + se1Str
				    	;
					fatalErrorMsg( kFName, msg );
				    System.exit( -1 );
				}
		}
	}
    
	///////////////////////////////////////////////////////////
	//
	// Set a given IP number to resolved... We did it!
	//
	///////////////////////////////////////////////////////////
	
	// public void doSetHost( Statement inStatement, InetAddress inIPNumber )
	public void _setWasResolved(
		String inIPNumber, String inHostName
		)
	{
		final String kFName = "setWasResolved";
		boolean debug = shouldDoDebugMsg( kFName );
	
		// String lHostName = inIPNumber.getHostName();
		// String lIPNumber = inIPNumber.getHostAddress();
		String lHostName = inHostName;
		String lIPNumber = inIPNumber;
	
		String insertSql = kInsertDomainNameSQL;
		String updateSql = kUpdateDomainNameSQL;
	
		String sysdateStr = getDBConfig().getVendorSysdateString();
		if( ! sysdateStr.equalsIgnoreCase("SYSDATE") ) {
			insertSql = NIEUtil.simpleSubstitution( insertSql, "SYSDATE", sysdateStr );
			updateSql = NIEUtil.simpleSubstitution( updateSql, "SYSDATE", sysdateStr );
		}
	
	
	
		// String lSQLInsertText = replaceAll( kInsertDomainNameSQL, kHOSTNAME, lHostName );
		String lSQLInsertText = replaceAll( insertSql, kHOSTNAME, lHostName );
		lSQLInsertText = replaceAll( lSQLInsertText, kIPNUMBER, lIPNumber );
		lSQLInsertText = replaceAll( lSQLInsertText, kRESOLVED, kWAS_RESOLVED );
		
		if( debug )
		    debugMsg( kFName,
		    	"Trying to insert using SQL statement: \""
		    	+ lSQLInsertText + "\""
		    	);
		
		try {
	    	// inStatement.execute( lSQLInsertText );
	    	cStatementUpdate.execute( lSQLInsertText );
		}
		catch( SQLException se1 )
		{
		    String lSQLUpdateText =
		    	// replaceAll( kUpdateDomainNameSQL, kHOSTNAME, lHostName );
				replaceAll( updateSql, kHOSTNAME, lHostName );
		    lSQLUpdateText =
		    	replaceAll( lSQLUpdateText, kIPNUMBER, lIPNumber );
		    lSQLUpdateText =
		    	replaceAll( lSQLUpdateText, kRESOLVED, kWAS_RESOLVED );
	    
		    if( debug )
				debugMsg( kFName,
					"Trying to update using SQL statement: \""
					+ lSQLUpdateText + "\""
					);
	
		    try {
				// inStatement.execute( lSQLUpdateText );
				cStatementUpdate.execute( lSQLUpdateText );
	    	}
	    	catch( SQLException se2 )
	    	{
				String se1Str = NIEUtil.exceptionToString( se1 );
				String se2Str = NIEUtil.exceptionToString( se2 );
	
				String msg =
					"Could not update database"
					+ " - got two SQL exceptions in a row."
					+ " First SQL Statement was: \""
					+ lSQLInsertText + "\""
					+ " Exception: \"" + se1Str + "\""
					+ " Second SQL Statement was: \""
					+ lSQLUpdateText + "\""
					+ " Exception: \"" + se2Str + "\""
					;
				fatalErrorMsg( kFName, msg );
	
				System.exit( -1 );
	    	}
		}
	
		if( debug )
		    debugMsg( kFName,
		    	"Resolved " + lIPNumber
		    	+ " to " + lHostName
		    	);
	}


	///////////////////////////////////////////////////////////
	//
	// Set a given IP number to "unknown"
	//
	///////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////
	//
	// Set a given IP number to "unknown"
	//
	///////////////////////////////////////////////////////////
	
	///////////////////////////////////////////////////////////
	//
	// Set a given IP number to "unknown"
	//
	///////////////////////////////////////////////////////////
	
	///////////////////////////////////////////////////////////
	//
	// Set a given IP number to "unknown"
	//
	///////////////////////////////////////////////////////////
	
	// public void doSetUnknown( Statement inStatement,  DotNotation inIPNumber, TreeSet inDoNotUpdateToUnknown )
	// public void doSetUnknown( Statement inStatement,  String inIPNumber, TreeSet inDoNotUpdateToUnknown )
	public boolean updateDnsTable_v1(
		String inIPNumber, String inHostName, boolean inWasResolved
		)
	{
		final String kFName = "updateDnsTable_v1";
	
		inIPNumber = NIEUtil.trimmedLowerStringOrNull( inIPNumber );
		if( null==inIPNumber ) {
			errorMsg( kFName, "Null IP number passed in, returning false." );
			return false;
		}
		inHostName = NIEUtil.trimmedStringOrNull( inHostName );
		if( null==inHostName ) {
			if( inWasResolved ) {
				errorMsg( kFName, "Null host name passed in, returning false." );
				return false;
			}
			else
				inHostName = "(unknown " + inIPNumber + ")";
		}
	
		boolean debug = shouldDoDebugMsg( kFName );
		boolean info = shouldDoInfoMsg( kFName );
	
		boolean outSuccess = false;
		
		if( debug || info )
			if( inWasResolved )
				infoMsg( kFName, "Registering IP address \"" + inIPNumber + "\" to name \"" + inHostName + "\"" );
			else
				infoMsg( kFName, "Registering UNTRANSLATED IP address \"" + inIPNumber + "\"" );
	
		int updateCount = 0;
		String lastSql = null;
		try {
	
			String updateSql = kUpdateDomainNameSQL;
			String insertSql = kInsertDomainNameSQL;
	
			// Fix the dates
			String sysdateStr = getDBConfig().getVendorSysdateString();
			if( ! sysdateStr.equalsIgnoreCase("SYSDATE") ) {
				insertSql = NIEUtil.simpleSubstitution( insertSql, "SYSDATE", sysdateStr );
				updateSql = NIEUtil.simpleSubstitution( updateSql, "SYSDATE", sysdateStr );
			}
	
			// Work on the UPDATE statement
	
			// Host name
			String lSQLUpdateText = replaceAll( updateSql, kHOSTNAME, inHostName );
			// IP address
			lSQLUpdateText = replaceAll( lSQLUpdateText, kIPNUMBER, inIPNumber );
			// Resolved yes / no (actually 0/1)
			lSQLUpdateText = replaceAll(
				lSQLUpdateText, kRESOLVED,
				( inWasResolved ? kWAS_RESOLVED : kWAS_NOT_RESOLVED )
				);
		
			if( debug )
				debugMsg( kFName,
					"Trying to update using SQL statement: \""
					+ lSQLUpdateText + "\""
					);
			lastSql = lSQLUpdateText;
			cStatementUpdate.execute( lSQLUpdateText );
	
			updateCount = cStatementUpdate.getUpdateCount();
			if( debug )
				debugMsg( kFName, "Update count = " + updateCount );
	
			// If update didn't work, try inserting
			if( updateCount > 0 )
				outSuccess = true;
			else {
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



	// public void doSetUnknown( Statement inStatement,  DotNotation inIPNumber, TreeSet inDoNotUpdateToUnknown )
	// public void doSetUnknown( Statement inStatement,  String inIPNumber, TreeSet inDoNotUpdateToUnknown )
	public boolean updateDnsTable(
		String inIPNumber, String inHostName, boolean inWasResolved
		)
	{
		return staticUpdateDnsTable(
				getDBConfig(), cStatementUpdate,
				inIPNumber, inHostName, inWasResolved
				);
	}

	// public void doSetUnknown( Statement inStatement,  DotNotation inIPNumber, TreeSet inDoNotUpdateToUnknown )
	// public void doSetUnknown( Statement inStatement,  String inIPNumber, TreeSet inDoNotUpdateToUnknown )
	public static boolean staticUpdateDnsTable(
		DBConfig inConfig, Statement ioStmt,
		String inIPNumber, String inHostName, boolean inWasResolved
		)
	{
		final String kFName = "staticUpdateDnsTable";

		inIPNumber = NIEUtil.trimmedLowerStringOrNull( inIPNumber );
		if( null==inIPNumber ) {
			errorMsg( kFName, "Null IP number passed in, returning false." );
			return false;
		}
		inHostName = NIEUtil.trimmedStringOrNull( inHostName );
		if( null==inHostName ) {
			if( inWasResolved ) {
				errorMsg( kFName, "Null host name passed in, returning false." );
				return false;
			}
			else
				inHostName = "(unknown " + inIPNumber + ")";
		}

		boolean debug = shouldDoDebugMsg( kFName );
		boolean info = shouldDoInfoMsg( kFName );

		boolean outSuccess = false;
    	
		if( debug || info )
			if( inWasResolved )
				infoMsg( kFName, "Registering IP address \"" + inIPNumber + "\" to name \"" + inHostName + "\"" );
			else
				infoMsg( kFName, "Registering UNTRANSLATED IP address \"" + inIPNumber + "\"" );

		int updateCount = 0;
		String lastSql = null;
		try {

			String updateSql = kUpdateDomainNameSQL;
			String insertSql = kInsertDomainNameSQL;

			// Fix the dates
			String sysdateStr = inConfig.getVendorSysdateString();
			if( ! sysdateStr.equalsIgnoreCase("SYSDATE") ) {
				insertSql = NIEUtil.simpleSubstitution( insertSql, "SYSDATE", sysdateStr );
				updateSql = NIEUtil.simpleSubstitution( updateSql, "SYSDATE", sysdateStr );
			}

			// Work on the UPDATE statement

			// Host name
			String lSQLUpdateText = replaceAll( updateSql, kHOSTNAME, inHostName );
			// IP address
			lSQLUpdateText = replaceAll( lSQLUpdateText, kIPNUMBER, inIPNumber );
			// Resolved yes / no (actually 0/1)
			lSQLUpdateText = replaceAll(
				lSQLUpdateText, kRESOLVED,
				( inWasResolved ? kWAS_RESOLVED : kWAS_NOT_RESOLVED )
				);
		
			if( debug )
				debugMsg( kFName,
					"Trying to update using SQL statement: \""
					+ lSQLUpdateText + "\""
					);
			lastSql = lSQLUpdateText;
			ioStmt.execute( lSQLUpdateText );

			updateCount = ioStmt.getUpdateCount();
			if( debug )
				debugMsg( kFName, "Update count = " + updateCount );

			// If update didn't work, try inserting
			if( updateCount > 0 ) {
				outSuccess = true;
			}
			else {
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

				// lSQLInsertText =
				//	replaceAll( lSQLInsertText, kRESOLVED, kWAS_NOT_RESOLVED );

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
				ioStmt.execute( lSQLInsertText );
				updateCount = ioStmt.getUpdateCount();
				if( debug )
					debugMsg( kFName, "Insert count = " + updateCount );

				if( updateCount > 0 ) {
					outSuccess = true;
				}
				else {
					outSuccess = false;
					debugMsg( kFName, "Insert also failed; returning false" );
				}
			}

			// Finishing up
			if( outSuccess )
			{
				debugMsg( kFName, "Finishing up with success." );
				if( inConfig.getVendorNeedsCommitByDefault() )
				{
					debugMsg( kFName, "Vendor DOES need explicit commit." );
					ioStmt.getConnection().commit();
				}
				else {
					debugMsg( kFName, "Vendor does auto commit." );
				}
			}
			else {
				debugMsg( kFName, "Finishing up with OUT success." );
			}
		}
		catch( SQLException se )
		{
			outSuccess = false;
			errorMsg( kFName,
				"Error executing SQL \"" + lastSql + "\""
				+ " (or commit); returning false."
				+ " SQL Exception: " + se
				);

			// fatalErrorMsg( kFName, msg );
			//System.exit( -1 );
		}
		

		return outSuccess;

	}



    ///////////////////////////////////////////////////////////
    //
    // Early Java VMs (like the one that is distributed with 
    // Oracle 9i server) is 1.2.x, which is missing numerous
    // functions.  These are replacements.
    //
    ///////////////////////////////////////////////////////////

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


	public static String getLogTableName()
	{
		return DBConfig.LOG_TABLE;
	}
	public static String getDomainTableName()
	{
		return DBConfig.DOMAIN_TABLE;
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
	    if( null!=fDBConf )
			return fDBConf;
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



	// private JDOMHelper fMainElem;

  

    
    ///////////////////////////////////////////////////////////
    //
    // Private members...
    //
    ///////////////////////////////////////////////////////////

	SearchTuningConfig mMainConfig;

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
  
    int fOperatingMode = DEFAULT_MODE;

//	Statement cStatement;

    // String fDriverName;
    // String fPassword = "kklop";
    // String fAccount = "kklop";
    // String fPassword;
    // String fAccount;
    
    // private static Comparator gComparator = null;
    
    static final int kErrorMode = 0;
    static final int kNewOnlyMode = 1;
    static final int kRetryMode = 2;
    static final int kRefreshMode = 3;
    static final int kOverwriteAllMode = 4;
    static final int DEFAULT_MODE = kNewOnlyMode;
    
    // static final String kDefaultDBURL =
    //	"jdbc:oracle:thin:@bigmomma:1521:stack"
    //	;
    
    ////////////////////////////////////////////////////////////
    //
    // SQL Statements...
    //
    ////////////////////////////////////////////////////////////

	static final String SQL_NEW_ONLY =
		// "SELECT UNIQUE client_host FROM " + getLogTableName() + " log"
		"SELECT DISTINCT client_host FROM " + getLogTableName() + " log"
		+ " WHERE client_host not in ("
		+ "	SELECT client_host from " + getDomainTableName() + " dns"
		+ "	WHERE log.client_host = dns.client_host )"
		;

	static final String SQL_NEW_AND_RETRY =
		// "SELECT UNIQUE client_host FROM " + getLogTableName() + " log"
		"SELECT DISTINCT client_host FROM " + getLogTableName() + " log"
		+ " WHERE client_host not in ("
		+ "	SELECT client_host from " + getDomainTableName() + " dns"
		+ "	WHERE log.client_host = dns.client_host"
		+ "		AND dns.was_resolved=1 )"
		;

	static final String SQL_ALL_IPS =
		// "SELECT UNIQUE client_host FROM " + getLogTableName()
		"SELECT DISTINCT client_host FROM " + getLogTableName()
		;

	/****
    static final String kGetCandidateIPNumbers =
		"SELECT DISTINCT client_host"
		+ " FROM " + getLogTableName()
		+ " WHERE client_host IS NOT NULL"
		+ " ORDER BY client_host"
		;
    
    static final String kBasicModeShouldNotResolveSQLText =
		"SELECT DISTINCT client_host"
		+ " FROM " + getDomainTableName()
		+ " WHERE client_host IS NOT NULL"
		+ " ORDER BY client_host"
		;
    
    static final String kBasicDoNotChangeToUnknownSQLText = null;
    
    static final String kIntermediateModeShouldNotResolveSQLText =
		"SELECT DISTINCT client_host"
		+ " FROM " + getDomainTableName()
		+ " WHERE resolved = 1"
		+ "  AND client_host IS NOT NULL"
		+ " ORDER BY dn_client_host"
		;
    
    static final String kIntermediateDoNotChangeToUnknownSQLText = null;
    
    static final String kRefreshModeShouldNotResolveSQLText = null;
    
    static final String kRefreshDoNotChangeToUnknownSQLText =
		"SELECT DISTINCT client_host"
		+ " FROM " + getDomainTableName()
		+ " WHERE resolved = 1"
		+ "  AND client_host IS NOT NULL"
		+ " ORDER BY client_host"
		;
	***/

	boolean fOverwriteGoodWithNotResolved = false;

    // static final String kFullModeShouldNotResolveSQLText = null;
    
    // static final String kFullDoNotChangeToUnknownSQLText = null;
    
    static final String kHOSTNAME = "VAR_HOSTNAME";
    static final String kIPNUMBER = "VAR_IPNUMBER";
    static final String kRESOLVED = "VAR_RESOLVED";
    static final String kWAS_RESOLVED = "1";
    static final String kWAS_NOT_RESOLVED = "0";
    
    static final String kInsertDomainNameSQL =
    	"INSERT INTO " + getDomainTableName()
    	+ "( client_host, dns_name, lookup_date, was_resolved )"
    	+ " VALUES ('" + kIPNUMBER + "'"
    	+ ",'" + kHOSTNAME + "'"
    	+ ", SYSDATE"
    	+ ", " + kRESOLVED + " ) "
    	;
    
    static final String kUpdateDomainNameSQL =
    	"UPDATE " + getDomainTableName()
    	+ " SET dns_name = '" + kHOSTNAME + "'"
    	+ ", lookup_date = SYSDATE"
    	+ ", was_resolved = " + kRESOLVED
    	+ " WHERE client_host = '" + kIPNUMBER + "'"
    	;
    	
    public static boolean _gDebug = false;
}
