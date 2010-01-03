package nie.sr2.util;

/*
	DNS Updater for the SearchTrack system.
	Written by Kevin-Neil Klop
	Copyright 2002 by  New Idea Engineering, Inc., All Rights Reserved.
	
	If you're reading this without NIE's permission, then please put this
	back where you found it - you're not being nice.
	
*/

import java.sql.*;
import java.util.*;
import java.net.*;
import nie.core.*;
import nie.sn.SearchTuningConfig;

public class RollDates
{

	private final static String kClassName = "RollDates";


    ////////////////////////////////////////////////
    //
    // In case we're run as a stand-alone program
    // instead of being incorporated in another program
    //
    /////////////////////////////////////////////////
	
    static public void main( String[] inArgs )
	{
		final String kFName = "main";
		RollDates proc = new RollDates();
		proc.parseCommandLine( inArgs );
		try
		{
			// lLookup.commonInit();
			// ^^^ moved to top of run()
			proc.setupDBConfigFromURI();
		}
		catch( DNSException e )
		{
			fatalErrorMsg( kFName,
				"Error initializing, exiting."
				+ " Error: " + e
				);
			System.exit( 2 );
		}
		proc.run();
    }

    
    ////////////////////////////////////////////////
    //
    // Constructors.
    //
    // All constructors should call commonInit()
    //
    ////////////////////////////////////////////////
    
    public RollDates()
    //	throws DNSException
    {
		// commonInit();
		// ^^^ moved to AFTER we parsed the command line, so
		// we have a config file to use

    }

	public RollDates( SearchTuningConfig inMainConfig )
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
			fDBConf = new DBConfig( getDBConfigURI() );
		}
		catch( Exception e )
		{
			throw new DNSException( kExTag
				+ "Error intializing config/data from URI \"" + getDBConfigURI() + "\""
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
		// fDBConf = null;
		// fOperatingMode = -1;

		// statusMsg( kFName, "dbconf=" + getDBConfig() );

		// Configure the database and cache a statement
		// try
		// {
			// fDBConf = new DBConfig( fConfigFileURI );
			// ^^^ moved to setupDBConfigFromURI()

			// cStatement = getDBConfig().createStatement();
			Object [] objs = getDBConfig().createStatement();
			cStatementRead = (Statement) objs[0];
			cConnectionRead = (Connection) objs[1];

			objs = getDBConfig().createStatement();
			cStatementUpdate = (Statement) objs[0];
			cConnectionUpdate = (Connection) objs[1];
		/***
		}
		catch( Exception e )
		{
			throw new Exception( kExTag
				+ " Error caching statement: " + e
				);
		}
		***/	

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

		statusMsg( kFName, "CommandLine=" + inArgs.length + " arg(s)" );

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

					/***
					Only one mode anyway
					if( lFlag.equals( "roll" ) )
						fOperatingMode = kRollMode;
				    // else if( lFlag.equals( "intermediate") )
					else
					***/
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
					fDbConfigFileURI = inArgs[i];
					haveSeenConfigURI = true;
					getRunLogObject().debugMsg( kClassName, kFName,
						"Command line option " + (i+1)
						+ " is config file name \"" + fDbConfigFileURI + "\"."
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

		/***
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
		***/

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

			Timestamp maxDate = getMaxDate();
			Timestamp currDate = getCurrentDbDate();
			statusMsg( kFName, "Current DATABASE system date is: '" + currDate );
			statusMsg( kFName, "Max table entry date is: '" + maxDate );

			// Do we have valid Dates?
			if( null!=maxDate && null!=currDate ) {
				double delta = calcDelta( maxDate, currDate );

				// Do we have a valid Delta period?
				if( delta > 0.0 ) {

					// MAIN LOGIC HERE
					// ====================================					

					statusMsg( kFName, "Rolling dates forward by " + delta + " days" );
					rollRecordsForward( delta );

					// Commit if needed
					if( getDBConfig().getVendorNeedsCommitByDefault() )
						cConnectionUpdate.commit();

					// Report Cache Cleanup
					if( null!=getDBConfigURI() ) {
						statusMsg( kFName, "Clearing report cache relative to DB config ..." );
						nie.sr2.java_reports.ActivityTrend.clearReportCache( getDBConfigURI() );
					}
					else if( null!=getMainConfigURIOrNull() ) {
						statusMsg( kFName, "Clearing report cache relative to Main config ..." );
						nie.sr2.java_reports.ActivityTrend.clearReportCache( getMainConfigURIOrNull() );
					}
					else {
						statusMsg( kFName, "No main config file URI available - not able to clear report cache." );
					}
					// END of MAIN LOGIC HERE
					// ====================================

				}
				// Invalid 1
				else if( 0.0 == delta ) {
					warningMsg( kFName,
						"Search log records appear to be up to date, skipping update."
						+ " current log max date = '" + maxDate + "'"
						+ ", current database system date = '" + currDate + "'"
						+ ", delta = '" + delta + "' days"
						);
				}
				// Invalid 2
				else {
					errorMsg( kFName,
						"Trouble calculating delta from dates; got negtive value."
						+ " current log max date = '" + maxDate + "'"
						+ ", current database system date = '" + currDate + "'"
						+ ", delta = '" + delta + "' day(s)"
						);
				}
			}
			// We do NOT have valid dates
			else {
				errorMsg( kFName,
					"Trouble getting one or more dates from database:"
					+ " current log max date = '" + maxDate + "'"
					+ ", current database system date = '" + currDate + "'"
					+ ( null==maxDate ?
							" Log max date could be null if there are no records"
							+ ", or if all the values in " + DATE_FIELD + " are null."
							: ""
						)
					);
			}

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
  

	Timestamp getMaxDate( )
	{
		final String kFName = "getMaxDate";
		String maxQry = getQuery1();
		// Timestamp max = fDBConf.simpleDateQuery( maxQry, true, true );
		Timestamp max = getDBConfig().simpleDateQuery( maxQry, true, true );
		infoMsg( kFName, "Max Date = " + max );
		return max;
	}
	Timestamp getCurrentDbDate( ) {
		final String kFName = "getCurrentDbDate";
		String sysQry = getQuery2();
		// Timestamp sys = fDBConf.simpleDateQuery( sysQry, true, true );
		Timestamp sys = getDBConfig().simpleDateQuery( sysQry, true, true );
		infoMsg( kFName, "Sys Date = " + sys );
		return sys;
	}
	double calcDelta( Timestamp time1, Timestamp time2 ) {
		final String kFName = "calcDelta";
		long diff = time2.getTime() - time1.getTime();
		// String diffStr = NIEUtil.formatTimeIntervalFancyMS( diff );
		// infoMsg( kFName, "Delta = " + diffStr );

		double diffDays = (double)diff / (double)NIEUtil.MS_PER_DAY;
		double diffDays2 = NIEUtil.roundDown( diffDays, 6 );
		infoMsg( kFName, "Delta = " + diffDays + " ( " + diffDays2 + " )" );

		return diffDays2;		
	}

	void rollRecordsForward( double inDelta )
		throws SQLException
	{
		final String kFName = "rollRecordsForward";

		/***		
		String sql3 = NIEUtil.simpleSubstitution(
			SQL_TEST_DATE_MATH, kDELTA, ""+diffDays2
			);
		statusMsg( kFName, "SQL = " + sql3 );
		Timestamp newMax = getDBConfig().simpleDateQuery( sql3, true, true );
		statusMsg( kFName, "New Max = " + sys );
		***/
		
		String sqlUpt = getUpdateSql( DATE_FIELD, inDelta );
		// done earlier - statusMsg( kFName, "Update SQL =\n\t" + sqlUpt );
		cStatementUpdate.execute( sqlUpt );
		sqlUpt = getUpdateSql( DATE_FIELD2, inDelta );
		infoMsg( kFName, "Update = " + sqlUpt );
		cStatementUpdate.execute( sqlUpt );

	}

	String getQuery1()
	{
		String ans = SQL_FIND_MAX_DATE;
		String sysdateStr = getDBConfig().getVendorSysdateString();
		if( ! sysdateStr.equalsIgnoreCase("SYSDATE") ) {
			ans = NIEUtil.simpleSubstitution( ans, "SYSDATE", sysdateStr );
		}
		return ans;
	}
	String getQuery2()
	{
		String ans = SQL_FIND_SYSDATE;
		String sysdateStr = getDBConfig().getVendorSysdateString();
		if( ! sysdateStr.equalsIgnoreCase("SYSDATE") ) {
			ans = NIEUtil.simpleSubstitution( ans, "SYSDATE", sysdateStr );
		}
		return ans;
	}

	String getUpdateSql( String inDateField, double inDelta )
	{
		final String kFName = "getUpdateSql";
		// statusMsg( kFName, "DB vendor = '" + getDBConfig().getConfiguredVendorTag() + "'" );
		String ans = SQL_UPDATE_DATE;
		String deltaStr = "" + inDelta;

		// Override for PostgreSQL
		if( getDBConfig().getConfiguredVendorTag().equalsIgnoreCase(DBConfig.VENDOR_TAG_POSTGRESQL) ) {
			ans = SQL_UPDATE_DATE_POSTGRESQL;
		}
		// Override for MySQL, both Template AND Delta calculation
		else if( getDBConfig().getConfiguredVendorTag().equalsIgnoreCase(DBConfig.VENDOR_TAG_MYSQL) ) {
			// ans = SQL_UPDATE_DATE_MYSQL_3;
			ans = SQL_UPDATE_DATE_MYSQL_2;
			// Convert to Seconds (not Microseconds)
			double newDelta = inDelta * 3600 * 24; // NO * 1000;
			// double newDelta = inDelta * 3600 * 24 * 1000;
			// inDelta = (float) Math.round( inDelta );
			// Format to a rounded string, with no scientific notation
			java.text.NumberFormat nf = java.text.NumberFormat.getInstance();
			nf.setGroupingUsed( false );
			nf.setMinimumFractionDigits( 0 );
			nf.setMaximumFractionDigits( 0 );
			deltaStr = nf.format( newDelta );
		}
		statusMsg( kFName, "Template SQL for vendor '" + getDBConfig().getConfiguredVendorTag() + "' = \n\t" + ans );
		String sysdateStr = getDBConfig().getVendorSysdateString();
		if( ! sysdateStr.equalsIgnoreCase("SYSDATE") ) {
			ans = NIEUtil.simpleSubstitution( ans, "SYSDATE", sysdateStr );
		}
		// statusMsg( kFName, "template=" + ans );
		// statusMsg( kFName, "inDateField=" + inDateField );
		// statusMsg( kFName, "kFIELD=" + kFIELD );

		// Need to call subst more than once
		// TODO: fix this when NIEUtil's method supports doing them all
		ans = NIEUtil.simpleSubstitution( ans, kFIELD, inDateField );
		ans = NIEUtil.simpleSubstitution( ans, kFIELD, inDateField );
		ans = NIEUtil.simpleSubstitution( ans, kFIELD, inDateField );
		ans = NIEUtil.simpleSubstitution( ans, kFIELD, inDateField );

		ans = NIEUtil.simpleSubstitution( ans, kDELTA, ""+deltaStr );
		ans = NIEUtil.simpleSubstitution( ans, kDELTA, ""+deltaStr );

		statusMsg( kFName, "Final SQL = \n\t" + ans );

		return ans;
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
	void _doProcess( ResultSet inCandidateResults )
		throws SQLException
	{
    	final String kFName = "doProcess";
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
//			    setUnknown(
//					lIPNumberString
//			    	);
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
//			    setUnknown(
//					lIPNumberString
//			    	);
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
//		    	setUnknown(
//					lIPNumberString
//		    		);
		    	lDidNotResolveCount++;
			}
			else {
//		    	boolean result = setWasResolved(
//					lIPNumberString,
//		    		lHostName
//		    		);
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
    

    static String _replaceAll( String inSourceString,
    	String inSearchString, String inReplacementString
    	)
    {
		int lPosition = inSourceString.indexOf( inSearchString );
		if( lPosition >= 0 )
	    {
			String lPrefixString = inSourceString.substring( 0, lPosition );
			String lPostString = _replaceAll( inSourceString.substring( lPosition + inSearchString.length() ), inSearchString, inReplacementString );
			return lPrefixString + inReplacementString + lPostString;
	    }
		else
		    return inSourceString;
    }


	public static String getLogTableName()
	{
		return DBConfig.LOG_TABLE;
	}




	// private JDOMHelper fMainElem;
	
	// Needs to operate from command line or as a thread under main app
	DBConfig getDBConfig()
	{
		if( null!=fDBConf )
			return fDBConf;
		if( null!=getMainConfig() )
			return getMainConfig().getDBConfig();
		return null;
	}

	String getDBConfigURI()
	{
		return fDbConfigFileURI;
	}

	public void setMainConfig( SearchTuningConfig inMainConfig ) {
		mMainConfig = inMainConfig;
	}


	SearchTuningConfig getMainConfig() {
		return mMainConfig;
	}


	public String getMainConfigURIOrNull()
	{
		// if( null!=fConfigFileURI )
		//	return fConfigFileURI;
		if( null!=getMainConfig() )
			return getMainConfig().getConfigFileURI();
		return null;
	}


	void setDBConfig( DBConfig inDB ) {
		fDBConf = inDB;
	}




	private String fDbConfigFileURI;


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

	SearchTuningConfig mMainConfig;

	boolean mHadError;
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
    
    // static final int kErrorMode = 0;
    static final int kRollMode = 1;
    static final int DEFAULT_MODE = kRollMode;
    
     
    ////////////////////////////////////////////////////////////
    //
    // SQL Statements...
    //
    ////////////////////////////////////////////////////////////

	public static final String DATE_FIELD = "start_time";
public static final String DATE_FIELD2 = "end_time";

	static final String kDELTA = "VAR_DELTA";
	static final String kFIELD = "VAR_DATE_FIELD";


	static final String SQL_FIND_MAX_DATE =
		"SELECT max(" + DATE_FIELD + ") FROM " + getLogTableName()
		+ " WHERE " + DATE_FIELD + " <= SYSDATE"
		;

	static final String SQL_FIND_SYSDATE =
		"SELECT max(SYSDATE) FROM " + getLogTableName();

	static final String SQL_TEST_DATE_MATH =
		"SELECT max(" + DATE_FIELD + ") + " + kDELTA + " FROM " + getLogTableName();

	/***
	static final String kInsertDomainNameSQL =
    	"INSERT INTO " + getDomainTableName()
    	+ "( client_host, dns_name, lookup_date, was_resolved )"
    	+ " VALUES ('" + kIPNUMBER + "'"
    	+ ",'" + kHOSTNAME + "'"
    	+ ", SYSDATE"
    	+ ", " + kRESOLVED + " ) "
    	;
	***/

	// Generic
	static final String SQL_UPDATE_DATE =
    	"UPDATE " + getLogTableName()
    	+ " SET " + kFIELD + " = " + kFIELD + " + " + kDELTA
		+ " WHERE"
		+ " " + kFIELD + " IS NOT NULL"
		+ " AND " + kFIELD + " + " + kDELTA + " <= SYSDATE"
		;

	// PostgreSQL with factional dates and times
	// select extract(epoch from your_column)::integer from your_table;
	// select now() + 0.2*interval '1 day';
	static final String SQL_UPDATE_DATE_POSTGRESQL =
    	"UPDATE " + getLogTableName()
    	+ " SET " + kFIELD + " = " + kFIELD + " + (" + kDELTA + " * INTERVAL '1 day')"
		+ " WHERE"
		+ " " + kFIELD + " IS NOT NULL"
		+ " AND " + kFIELD + " + (" + kDELTA + " * INTERVAL '1 day') <= SYSDATE"
		;
	
	// MySQL bs...
	/***
	static final String SQL_UPDATE_DATE_MYSQL_1 =
    	"UPDATE " + getLogTableName()
    	+ " SET " + kFIELD + " = DATE_ADD(" + kFIELD + ", INTERVAL '" + kDELTA + "' DAY)"
		+ " WHERE"
		+ " " + kFIELD + " IS NOT NULL"
		+ " AND DATE_ADD(" + kFIELD + ", INTERVAL '" + kDELTA + "' DAY) <= SYSDATE"
		;
	***/
	static final String SQL_UPDATE_DATE_MYSQL_2 =
    	"UPDATE " + getLogTableName()
    	+ " SET " + kFIELD + " = FROM_UNIXTIME( UNIX_TIMESTAMP(" + kFIELD + ")+" + kDELTA + ")"
		+ " WHERE"
		+ " 2>1 OR "
		+ " " + kFIELD + " IS NOT NULL"
		+ " AND FROM_UNIXTIME( UNIX_TIMESTAMP(" + kFIELD + ")+" + kDELTA + ") <= SYSDATE"
		;
		// FROM_UNIXTIME(AVG(UNIX_TIMESTAMP(START_TIME)))
	/***
	static final String SQL_UPDATE_DATE_MYSQL_3 =
    	"UPDATE " + getLogTableName()
    	+ " SET " + kFIELD + " = DATE_ADD(" + kFIELD + ", " + kDELTA + " * (INTERVAL '1' DAY) )"
		+ " WHERE"
		+ " " + kFIELD + " IS NOT NULL"
		+ " AND DATE_ADD(" + kFIELD + ", " + kDELTA + " * (INTERVAL '1' DAY) ) <= SYSDATE"
		;
	***/

	public static boolean _gDebug = false;
}
