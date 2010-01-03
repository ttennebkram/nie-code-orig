package nie.sr2.util;

import java.sql.*;
import java.util.*;
import java.io.IOException;
import java.io.LineNumberReader;
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

// SEARCH ACTIVITY POPULATOR
// Makes up semi-random looking search activity
// Can use with date roller
// Driven by csv control file

public class Populator // implements nie.sn.CronLiteJob // Runnable
{

	private final static String kClassName = "Populator";

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
		Populator util = new Populator();
		util.parseCommandLine( inArgs );
		try
		{
			util.setupConfigFromURI();
		}
		catch( Exception e )
		{
			fatalErrorMsg( kFName,
				"Error initializing, exiting."
				+ " Error: " + e
				);
			System.exit( 2 );
		}
		util.run();
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
    
    private /*public*/ Populator()
    //	throws UtilException
    {
    	// If you use this one, you must also then call
    	// .setupConfigFromURI() before running
    }

    // NOTE: This constructor may not be called
    // Instead, they may call the null arg version
    // and then call .setupConfigFromURI()
	public Populator( SearchTuningConfig inMainConfig )
		throws Exception
	{
		this();
		setMainConfig( inMainConfig );
		// setupSearchEngineConfig();
	}


	public void setupConfigFromURI()
		 throws Exception
	{
		final String kFName = "setupDBConfigFromURI";
		final String kExTag = kClassName + '.' + kFName + ": ";
		try
		{
			// fDBConf = new DBConfig( fConfigFileURI );
			// mMainConfig = new SearchTuningConfig( fConfigFileURI, null );
			mMainConfig = new SearchTuningConfig( getMainConfigURI(), null );
		}
		catch( Exception e )
		{
			throw new UtilException( kExTag
				+ "Error intializing config/data from URI \"" + getMainConfigURI() + "\""
				+ " REMINDER: This needs to a FULL SearchTrack config, and NOT just a Database config"
				+ " because we need more than just database info to load search records."
				// ^^^ In particular we need search engine info and Site ID, and maybe some patterns
				+ " Error was: " + e
				);
		}
		// checkDataFile();
		// setupSearchEngineConfig();
	}


	public boolean hadError() {
		return mHadError;
	}

    
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
				String flag = inArgs[i].substring( 1 ).toLowerCase();

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
					if( flag.startsWith("nuke") )
					{
						if( flag.indexOf("log") > 0 ) {
							mNukeLog = true;
						}
						else if( flag.indexOf("dns") > 0 ) {
							mNukeDns = true;
						}
						else if( flag.indexOf("all")>0 || flag.indexOf("both")>0 ) {
							mNukeLog = mNukeDns = true;
						}
						else {
							bailOnBadSyntax( inArgs[i], "Must be -nuke_log, -nuke_dns, or -nuke_both" );
						}
					}
					// Where to read the data from
					else if( flag.startsWith("data") ) {
						if( i == inArgs.length-1 )
							bailOnBadSyntax( inArgs[i], "requires an argument" );
						String arg = inArgs[++i];
						mDataURI = NIEUtil.trimmedStringOrNull( arg );
						if( null==mDataURI )
							bailOnBadSyntax( arg, "data_file requires an argument" );
					}
					// Days / Window of time
					// mDaysInWindow
					else if( flag.equals("days") ) {
						if( i == inArgs.length-1 )
							bailOnBadSyntax( inArgs[i], "requires an argument" );
						String arg = inArgs[++i];
						mDaysInWindow = NIEUtil.stringToIntOrDefaultValue( arg, -1, true, true );
						if( mDaysInWindow < 1 )
							bailOnBadSyntax( arg , "Days must be positive int" );
					}
					// Site ID
					else if( flag.equals("site_id") || flag.equals("site-id") ) {
						if( i == inArgs.length-1 )
							bailOnBadSyntax( inArgs[i], "requires an argument" );
						String arg = inArgs[++i];
						mSiteId = NIEUtil.stringToIntOrDefaultValue( arg, -1, true, true );
						if( mSiteId < 1 )
							bailOnBadSyntax( arg , "site_id must be positive int" );
					}
					// Preview Only mode
					else if( flag.startsWith("preview") ) {
						// mDoPreviewOnly = true;
						setDoPreviewOnly();
					}
					// backfill missing records
					else if( flag.startsWith("back") ) {
						setDoBackFill( true );
					}
					// No backfill
					else if( flag.startsWith("no") && flag.indexOf("back") > 0 ) {
						setDoBackFill( false );
					}
					// roll records forward to current date and time
					else if( flag.startsWith("roll") ) {
						setDoRollDates( true );
					}
					else if( flag.startsWith("no") && flag.indexOf("roll") > 0 ) {
						setDoRollDates( false );
					}
					// Use Google
					else if( flag.equals("use_google") || flag.equals("usge-google") || flag.equals("google") ) {
						mUseGoogleInstead = true;
						setDoBackFill( true );
					}
					// Site prefix (for google)
					else if( flag.equals("site_prefix") || flag.equals("site-prefix") || flag.equals("prefix") ) {
						if( i == inArgs.length-1 )
							bailOnBadSyntax( inArgs[i], "requires an argument" );
						String arg = inArgs[++i];
						mSitePrefix = NIEUtil.trimmedStringOrNull( arg );
						if( null==mSitePrefix )
							bailOnBadSyntax( arg, "prefix requires an argument" );
						setDoBackFill( true );
					}
					else {
						// We don't know what it is
						bailOnBadSyntax( inArgs[i] );
					}
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

		String msg = "Utility to create sample searches in the logs, usually for a demo." + NIEUtil.NL
			+ "It also takes care of updating DNS cache, populating null match counts," + NIEUtil.NL
			+ "rolling records up to the current date and time, and clearing the report cache." + NIEUtil.NL
			+ NIEUtil.NL
			;

		/*String*/ msg = "Bad Command Line Syntax: ";
		if( optMsg != null )
			msg += optMsg;
		else
			msg += "Unknown option \"" + inOpt + "\"";
		msg += NIEUtil.NL;

		msg += "Required Parameter:" + NIEUtil.NL
			+ "\tdatabase_config_file_name_or_url.xml" + NIEUtil.NL
			+ NIEUtil.NL
			;

		msg += "REQUIRED Args:" + NIEUtil.NL
		+ NIEUtil.NL
		+ "config_file.xml (full SearchTrack, not just DB)" + NIEUtil.NL
		+ "-data[_file] tabbed_data.txt (REQUIRED!)" + NIEUtil.NL
		+ NIEUtil.NL
		
		+ "Primary Options:" + NIEUtil.NL
		+ NIEUtil.NL
		+ "-nuke_log" + NIEUtil.NL
		+ "-nuke_dns" + NIEUtil.NL
		+ "-nuke_both" + NIEUtil.NL
		+ NIEUtil.NL
		+ "-site_id int (OVERRIDE the site id in the main config)" + NIEUtil.NL
		+ NIEUtil.NL
		+ "-days int (how many days to spread the searches over, DEFAULT=" + DEFAULT_WINDOW_DAYS + ")" + NIEUtil.NL
		+ NIEUtil.NL
		+ "-preview[_only] (just show what you would do, but don't do it)" + NIEUtil.NL
		+ NIEUtil.NL
		+ "-back[_fill] (Back fill match counts)" + NIEUtil.NL
		+ "-no_back[_fill] turn off, default=" + DEFAULT_DO_BACK_FILL + NIEUtil.NL
		+ "-use_google (instead for adding match counts, IMPLIES -back_fill)" + NIEUtil.NL
		+ "-site_prefix something.com (site prefix for using with Google back fill)" + NIEUtil.NL
		+ NIEUtil.NL
		+ "-roll[_dates] (move up to current date and time)" + NIEUtil.NL
		+ "-no_roll[_dates] turn off, default=" + DEFAULT_DO_ROLL_DATES + NIEUtil.NL
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

		try
		{
			mHadError = false;

			commonInit();

			// nukeTablesIfRequested();
			
			// Now process them
			int count = doProcessRecords( mDataIn );
			statusMsg( kFName, "Inserted " + count + " records" );
	
			// Do we have any cleanup work to do?
			if( count>0 ) {
				if( getDoPreviewOnly() )
				{
					statusMsg( kFName, "Preview mode, so skipping clearing of report cache and any back filling results." );
				}
				// Yes, we really did something
				else {

					// Commit
					if( getDBConfig().getVendorNeedsCommitByDefault() )
					{
						cConnectionUpdate.commit();
					}

					// Backfill missing data
					if( getDoBackFill() )
					{
						statusMsg( kFName, "Backfilling any missing results ..." );
						mBackFiller.run();
					}
					else {
						statusMsg( kFName, "Configured to NOT Backfill missing results." );
					}
					
					// Roll random records right up to the minute
					// Since they were random, the lastest could be a few days old
					if( getDoRollDates() )
					{
						statusMsg( kFName, "Rolling record dates forward ..." );
						mRoller.run();
					}
					else {
						statusMsg( kFName, "Configured to NOT Roll record dates forward." );
					}
					
					statusMsg( kFName, "One last time... clearing report cache ..." );
					nie.sr2.java_reports.ActivityTrend.clearReportCache( getMainConfigURI() );

				}
			}
			// Else no sense doing those if no data added
			else {
				statusMsg( kFName, "No Records added, so skipping clearing of report cache and any back filling and rolling results." );
			}
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
			// lCandidateIPNumbersResultSet = DBConfig.closeResults( lCandidateIPNumbersResultSet, kClassName, kFName, false );
			// cStatementRead = DBConfig.closeStatement( cStatementRead, kClassName, kFName, false );
			// cConnectionRead = DBConfig.closeConnection( cConnectionRead, kClassName, kFName, false );
			// cStatementUpdate = DBConfig.closeStatement( cStatementUpdate, kClassName, kFName, false );
			// cConnectionUpdate = DBConfig.closeConnection( cConnectionUpdate, kClassName, kFName, false );
		}
    }
    
	// These are things that are done ONCE PER RUN
	// and this is NOT part of the constructor chain
    public void commonInit()
    	throws Exception
    {
		final String kFName = "commonInit";
		final String kExTag = kClassName + '.' + kFName + ": ";
		initDB();
		nukeTablesIfRequested();
		initBackFillerIfNeeded();
		initRollerNeeded();
		openDataFile();
    }

	public void initDB()
    	throws Exception
    {
		final String kFName = "initDB";
		final String kExTag = kClassName + '.' + kFName + ": ";

		if( null==getDBConfig() )
			throw new Exception( kExTag +
				"Null DB config."
				);

		statusMsg( kFName,
				"Opening Database ..."
				);
		// initDatabase();

		/***	
		// TODO: Nice idea from other util method
		String tableName = getTableName();
	
		if( ! getDBConfig().verifyASpecificDBTable( tableName, false, false ) )
			throw new DBConfigException( kExTag +
				"No such table \"" + tableName + "\""
				);
		***/


		// fOperatingMode = kMinimumMode;
		// fDriverName = kDefaultDBURL;

		// cStatementRead = null;
		// cConnectionRead = null;
		cStatementUpdate = null;
		cConnectionUpdate = null;

		// Configure the database and cache a statement
		try
		{
			// fDBConf = new DBConfig( fConfigFileURI );
			// ^^^ moved to setupDBConfigFromURI()

			// cStatement = getDBConfig().createStatement();
		    debugMsg( kFName, "getDBConfig()=" + getDBConfig() );
			// Object [] objs = getDBConfig().createStatement();
			// cStatementRead = (Statement) objs[0];
			// cConnectionRead = (Connection) objs[1];

			Object [] objs = getDBConfig().createStatement();
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
    }

	void initBackFillerIfNeeded()
		throws Exception
	{
		if( getDoBackFill() )
		{
			mBackFiller = new BackfillMatchCounts( getMainConfig() );
			mBackFiller.setUseGoogleInstead( getUseGoogleInstead() );
			mBackFiller.setSitePrefix( getSitePrefix() );
			// mBackFiller.commonInit();
			// ^^^ No, back.run() will take care of this
		}
	}
	void initRollerNeeded()
		throws Exception
	{
		if( getDoRollDates() )
		{
			mRoller = new RollDates( getMainConfig() );
		}
	}

	void nukeTablesIfRequested()
		throws Exception
	{
		final String kFName = "nukeTablesIfRequested";
		if( mNukeLog )
		{
			statusMsg( kFName, "Clearing old log records from " + getLogTableName() );
			String sql = "DELETE FROM " + getLogTableName();
			int results = cStatementUpdate.executeUpdate( sql );
			if( 0 == results )
				warningMsg( kFName, "No records deleted from search log "
					+ getLogTableName()
					);
			else
				statusMsg( kFName, "" + results
					+ " records deleted from search log "
					+ getLogTableName()
					);
		}
		else {
			statusMsg( kFName, "Combining with old log records from " + getLogTableName() );
		}
		
		if( mNukeDns )
		{
			statusMsg( kFName, "Clearing old dns records from " + DNSLookup2.getDomainTableName() );
			String sql = "DELETE FROM " + DNSLookup2.getDomainTableName();
			int results = cStatementUpdate.executeUpdate( sql );
			if( 0 == results )
				warningMsg( kFName, "No records deleted from DNS cache "
					+ DNSLookup2.getDomainTableName()
					);
			else
				statusMsg( kFName, "" + results
					+ " records deleted from DNS cache "
					+ DNSLookup2.getDomainTableName()
					);
		}
		else {
			statusMsg( kFName, "Combining with old DNS records from " + DNSLookup2.getDomainTableName() );
		}
	}

	void openDataFile()
		throws IOException
	{
		final String kFName = "openDataFile";

		statusMsg( kFName,
				"Opening data file: " + getDataURI()
				);
		mDataIn = NIEUtil.openURIReadChar( getDataURI() );
		// NIEUtil.fetchURIContentsLines( getDataURI() )
		// NIEUtil.fetchURIContentsChar( getDataURI() )
		
    }


	int doProcessRecords( LineNumberReader fin )
		throws Exception
	{
    	final String kFName = "doProcessRecords";
		final int kReportInterval = 100;

		if( null == fin )
		{
			errorMsg( kFName,
				"Null line reader passed in, exiting method."
				);
			return -1;
		}

		// Process the rest of the lines
		int lineCounter = 1;
		int recordCounter = 0;
		int lastCountReported = 0;
		String currClientIP = null;
		String currClientName = null;
		String currRandIPPrefix = null;
		String currRandNameSuffix = null;
		boolean randEveryRecord = false;
		String line = null;
		// For each line in the file
		while( null != (line = fin.readLine()) ) {
			lineCounter = fin.getLineNumber();
			// lineCounter++;

			// line = NIEUtil.trimmedStringOrNull( line );
			// DON'T TRIM at this point!
			// You will lose the initial indenting.
			// and Java doesn't have just a Right Trim rtrim
			
			// Some sanity checking
			if( null==line ) {
				currClientIP = currClientName = currRandIPPrefix = currRandNameSuffix = null;
				continue;
			}

			// If the line starts with #, then it's a comment
			if( line.startsWith("#") ) {
				statusMsg( kFName, "Skipping comment line # " + lineCounter );
				continue;
			}

			// Get the values
			// Vector values = NIEUtil.parseCSVLine( line );
			Vector values = NIEUtil.parseTabDelimLine( line );
	
			// Some sanity checking
			if( null==values || values.isEmpty() ) {
				statusMsg( kFName, "Skipping empty line (1) # " + lineCounter );
				currClientIP = currClientName = currRandIPPrefix = currRandNameSuffix = null;
				randEveryRecord = false;
				continue;
			}
				
			String val1 = NIEUtil.trimmedStringOrNull( (String) values.get(0) );
			// statusMsg( kFName, "val1 = '" + val1 + "'" );
			// Is this a client header line?
			if( null!=val1 )
			{
				// statusMsg( kFName, "Header Line # " + lineCounter );
				// Is it random client directive?
				if( val1.startsWith("*") || val1.startsWith("+") )
				{
					// Clear out explicit names, which we are no longer using
					currClientIP = currClientName = null;
					// And for now, also clear out the rendom seeds, which we are about to set with new values
					currRandIPPrefix = currRandNameSuffix = null;
					// Randomize EVERY record
					if( val1.startsWith("+") )
						randEveryRecord = true;
					// Assign the batch to a random person
					else
						randEveryRecord = false;
					// Is there a base name?
					if( val1.length()>1 )
					{
						// If this fails, it'll be null, and that's fine
						currRandNameSuffix = NIEUtil.trimmedStringOrNull( val1.substring(1) );
					}
					// Else totally random
					// Is there a seed TLD IP?
					// This is the 123 in 123.xx.xx.xx IP address
					// first part of an IPv4 class C address
					// 1 - 254 (0 and 255 are not valid either)
					if( values.size()>1 ) {
						// Skip trailing comments
						if( ((String)values.get(1)).trim().startsWith("#") )
							continue;
						if( null==currRandNameSuffix ) {
							errorMsg( kFName,
									"Can assign IP base for completely wildcarded domain, on line # " + lineCounter + " of file \"" + getDataURI() + "\" (1)"
									+ NIEUtil.NL
									+ " Must be one of either:"
									+ NIEUtil.NL
									+ " *.domain.com(tab)123"
									+ NIEUtil.NL
									+ " *.domain.com(end-of-line)"
									+ NIEUtil.NL
									+ " *(end-of-line)"
									+ NIEUtil.NL
									+ " Will continue reading any subsequent lines."
									+ " Line=\"" + line + "\""
									);
								currClientIP = currClientName = currRandIPPrefix = currRandNameSuffix = null;
								randEveryRecord = false;
								continue;					
						}
						// Get the number
						if( values.size()>2 ) {
							String tmpStr = NIEUtil.trimmedStringOrNull( (String) values.get(2) );
							if( null!=tmpStr && ! tmpStr.startsWith("#") )
								currRandIPPrefix = tmpStr;
						}
						if( null!=currRandIPPrefix ) {
							int tmpInt = NIEUtil.stringToIntOrDefaultValue( currRandIPPrefix, -1, false, true );
							if( tmpInt < 1 || tmpInt > 254 ) {
								currRandIPPrefix = null;
								errorMsg( kFName,
									"Invalid IP prefix on line # " + lineCounter + " of file \"" + getDataURI() + "\""
									+ " Must be in the range of 1 through 254 (the left field of a Class-C IPv4 address)"
									+ " Will just use default values, and will continue to read any subsequent lines."
									);
								currClientIP = currClientName = currRandIPPrefix = currRandNameSuffix = null;
								randEveryRecord = false;
								continue;
							}
						}
					}
					// Else totally random TLD IP prefix
					// Just leave them all null
				}
				// Else it's an explicit client
				// TODO: explicit clients are never stored!
				else {
					// Clear out the rendom seeds, which we are no longer using
					currRandIPPrefix = currRandNameSuffix = null;
					// And for now clear out explicit names, which we are about to set with new values
					currClientIP = currClientName = null;
					randEveryRecord = false;
				}
			}
			// Else it's data
			else {
				// statusMsg( kFName, "Data Line # " + lineCounter );
				if( values.size() < 2 ) {
					warningMsg( kFName,
						"Skipping apparently blank line # " + lineCounter + " of file \"" + getDataURI() + "\" (1)"
						+ " (it had an initial indent, but nothing else, comment out to avoid this warning)"
						+ " Will continue reading any subsequent lines."
						+ " Line=\"" + line + "\""
						);
					currClientIP = currClientName = currRandIPPrefix = currRandNameSuffix = null;
					continue;
				}
				// Query
				// -----
				// word is get(1)
				String query = NIEUtil.trimmedStringOrNull( (String) values.get(1) );
				if( null == query ) {
					warningMsg( kFName,
						"Skipping apparently blank line # " + lineCounter + " of file \"" + getDataURI() + "\" (2)"
						+ " (it had one or more indents, but nothing else, comment out to avoid this warning)"
						+ " Will continue reading any subsequent lines."
						+ " Line=\"" + line + "\""
						);
					currClientIP = currClientName = currRandIPPrefix = currRandNameSuffix = null;
					continue;
				}
				// Count (how many times to run it)
				// -----
				int count = -1;
				// count is get(2) if specified
				if( values.size() > 2 ) {
					String countStr = NIEUtil.trimmedStringOrNull( (String) values.get(2) );
					count = NIEUtil.stringToIntOrDefaultValue( countStr, -1, false, true );
				}
				// No explicit count given, try to calculate one
				if( count <= 0 ) {
					count = wordToNumber( query );
					// If still negative, complain
					if( count <= 0 ) {
						errorMsg( kFName,
							"No valid explicit nor implicit count for line # " + lineCounter + " of file \"" + getDataURI() + "\""
							+ " This is the NUMBER OF TIMES the query should be run."
							+ " If you want to show 0 RESULTS, please put it in the NEXT field."
							+ " Will continue reading any subsequent lines."
							+ " Line=\"" + line + "\""
							);
						continue;						
					}
				}
				// Matched (how many documents matched)
				// -------
				int matched = -1;
				// count is get(3) if specified
				if( values.size() > 3 ) {
					String matchedStr = NIEUtil.trimmedStringOrNull( (String) values.get(3) );
					matched = NIEUtil.stringToIntOrDefaultValue( matchedStr, -1, false, true );
				}
				// No explicit count given, we'll run the query
				// -1 means automatic
				// TODO: -2 means use google public, but would need prefix

				// Searched (how many documents were searched in total, many don't give this anyway)
				// --------
				int searched = -1;
				// number searched is get(4) if specified
				if( values.size() > 4 ) {
					String searchedStr = NIEUtil.trimmedStringOrNull( (String) values.get(4) );
					searched = NIEUtil.stringToIntOrDefaultValue( searchedStr, -1, false, true );
				}

				// At this point we have a query and a desired count
				// Values query and count are set

				// Now we need to "run" them, and attribute them to a client		
				int tmpCount = generateLogEntries(
										query, count, matched, searched,
										currClientIP, currClientName,
										currRandIPPrefix, currRandNameSuffix,
										randEveryRecord
										);
				// Tabulate
				if( tmpCount > 0 ) {
					recordCounter += tmpCount;
					if( recordCounter >= lastCountReported + kReportInterval ) {
						statusMsg( kFName,
							"Have added " + recordCounter + " so far."
							);
						lastCountReported = recordCounter;
					}
				}
				else {
					errorMsg( kFName,
						"No records generated for line # " + lineCounter + " of file \"" + getDataURI() + "\""
						+ " Will continue reading any subsequent lines."
						+ " Line=\"" + line + "\""
						);
					continue;											
				}
			}
		}	// End of While lines in file
		statusMsg( kFName,
			"Done."
			+ " Final statistics:"
    		+ " # lines read = " + lineCounter
			+ ", entries added = " + recordCounter
			);
		return recordCounter;
	}
			
	int generateLogEntries(
		String inQuery, int inCount, int optMatched, int optSearched,
		String inClientIP, String inClientName,
		String inRandIPPrefix, String inRandNameSuffix,
		boolean inRandomizeEveryRecord
		// int inWindowDays
		)
			throws Exception
	{
		final String kFName = "generateLogEntries";
		// Normalize the numeric prefix we will use for IPs
		String ipPrefix = NIEUtil.trimmedStringOrNull( inRandIPPrefix );
		if( null!=ipPrefix ) {
			if( ipPrefix.endsWith(".*") || ipPrefix.endsWith(".+") ) {
				if( ipPrefix.length()>2 )
					ipPrefix = NIEUtil.trimmedStringOrNull( ipPrefix.substring(0,ipPrefix.length()-2) );
				else
					ipPrefix = null;
			}
			else if( ipPrefix.endsWith(".") ) {
				if( ipPrefix.length()>1 )
					ipPrefix = NIEUtil.trimmedStringOrNull( ipPrefix.substring(0,ipPrefix.length()-1) );
				else
					ipPrefix = null;
			}	
		}
		if( null==ipPrefix )
			ipPrefix = randomIpSegment();

		// Normalize the Suffix we will use for names
		String nameSuffix = NIEUtil.trimmedStringOrNull( inRandNameSuffix );
		if( null!=nameSuffix ) {
			if( nameSuffix.startsWith("*.") || nameSuffix.startsWith("+.") ) {
				if( nameSuffix.length()>2 )
					nameSuffix = NIEUtil.trimmedStringOrNull( nameSuffix.substring(2) );
				else
					nameSuffix = null;
			}
			else if( nameSuffix.startsWith(".") ) {
				if( nameSuffix.length()>1 )
					nameSuffix = NIEUtil.trimmedStringOrNull( nameSuffix.substring(1) );
				else
					nameSuffix = null;
			}
		}
		if( null==nameSuffix )
			nameSuffix = numStringToWords( ipPrefix ) + ".com";

		// Need to get client to attribute this to
		String clientIPToUse = null;
		String clientNameToUse = null;

		// Have both, just use them
		if( null!=inClientIP && null!=inClientName )
		{
			// Good, just copy over
			clientIPToUse = inClientIP;
			clientNameToUse = inClientName;
		}
		// Have IP, but need to generate a name
		else if( null!=inClientIP )
		{
			clientIPToUse = inClientIP;
			String fragment = ipToNameFragment( inClientIP );
			if( null==fragment ) {
				errorMsg( kFName, "Could not get fragment, can't log (1)" );
				return -1;
			}
			clientNameToUse = fragment + '.' + nameSuffix;
		}
		// Else use suffix and prefix to generate IP and name
		// And those will have been set up top
		// ipPrefix nameSuffix
		else {
			String ipTriple = randomIpTriple();
			clientIPToUse = ipPrefix + '.' + ipTriple;
			String fragment = ipToNameFragment( clientIPToUse );
			if( null==fragment ) {
				errorMsg( kFName, "Could not get fragment, can't log (2)" );
				return -1;
			}
			clientNameToUse = fragment + '.' + nameSuffix;		
		}

		// The interval of time in milliseconds we'll spread out
		// the queries
		long dateRangeMs = (long) getNumDaysInWindow() * NIEUtil.MS_PER_DAY;

		int recsAdded = 0;

		// For the desired number of times...
		for( int i=1; i<=inCount; i++ )
		{
			// Generate a random date
			long subtractMs = (long)(  Math.random() * (double) dateRangeMs  );
			long newMs = NIEUtil.getCurrTimeMillis() - subtractMs;
			java.sql.Timestamp newDate = new java.sql.Timestamp( newMs );

			if( inRandomizeEveryRecord )
			{
				// Sanity check
				if( null!=inClientIP && null!=inClientName ) {
					warningMsg( kFName, "Can't randomize records when specific client IP and Name passed in, clearing flag." );
					inRandomizeEveryRecord = false;
				}
				// Have IP, but need to generate a name
				// TODO: for now line parsing can't get to this state but may do later
				else if( null!=inClientIP )
				{
					clientIPToUse = inClientIP;
					String fragment = ipToNameFragment( inClientIP );
					if( null==fragment ) {
						errorMsg( kFName, "Could not get fragment, can't log (1)" );
						return -1;
					}
					clientNameToUse = fragment + '.' + nameSuffix;
				}
				// Else use suffix and prefix to generate IP and name
				// And those will have been set up top
				// ipPrefix nameSuffix
				else {
					String ipTriple = randomIpTriple();
					clientIPToUse = ipPrefix + '.' + ipTriple;
					String fragment = ipToNameFragment( clientIPToUse );
					if( null==fragment ) {
						errorMsg( kFName, "Could not get fragment, can't log (2)" );
						return -1;
					}
					clientNameToUse = fragment + '.' + nameSuffix;		
				}
			}
			
			int thisResult = -1;
			if( getDoPreviewOnly() ) {
				statusMsg( kFName, "Would add record:" + NIEUtil.NL
					+ "\tQuery='" + inQuery + "'" + NIEUtil.NL
					+ "\tMatched='" + optMatched + "'" + NIEUtil.NL
					+ "\tSearched='" + optSearched + "'" + NIEUtil.NL
					+ "\tDate/time='" + newDate + "'" + NIEUtil.NL
					+ "\tIPaddr='" + clientIPToUse + "'" + NIEUtil.NL
					+ "\tIPname='" + clientNameToUse + "'"
					);
				thisResult = 1;
			}
			// Actually do it
			else {
				thisResult = insertLogRecord(
					inQuery, optMatched, optSearched, newDate,
					clientIPToUse, clientNameToUse
					);
				// Might need to do DNS for each record
				if( inRandomizeEveryRecord ) {
					if( thisResult > 0 ) {
						updateDnsTable( clientIPToUse, clientNameToUse );
					}
					else {
						warningMsg( kFName, "No records added for query '" + inQuery + "', no DNS to update (1)." );
					}
				}
			}
			if( thisResult >= 0 )
				recsAdded += thisResult;
			
		}  // End for requested number of times

		// Update DNS if we've added any records
		// And haven't done it already
		if( ! inRandomizeEveryRecord ) {
			if( recsAdded > 0 ) {
				statusMsg( kFName, "Adding DNS for query batch '" + inQuery + "' " + clientIPToUse + '/' + clientNameToUse );
				updateDnsTable( clientIPToUse, clientNameToUse );
			}
			else {
				warningMsg( kFName, "No records added for query '" + inQuery + "', no DNS to update (2)." );
			}
		}

		return recsAdded;

    }

	// Based losely on nie.sn.SearchLogger
	int insertLogRecord(
			String inQuery, int optMatched, int optSearched,
			java.sql.Timestamp inTimestamp,
			String clientIPToUse, String clientNameToUse
		)
			throws Exception
	{
		final String kFName = "insertLogRecord";
		nie.core.DBUpdateStmt statement = new nie.core.DBUpdateStmt(
				nie.core.DBConfig.LOG_TABLE, getDBConfig()
				);
		// Basic info
		// ==========
		// Site ID
		statement.setValue(
			nie.sn.SearchLogger.SITE_ID_DB_FIELD,
			getSiteIdToUse()
			);
		// Transaction Type
		statement.setValue(
				nie.sn.SearchLogger.TRANS_TYPE_DB_FIELD,
				nie.sn.SearchLogger.TRANS_TYPE_SEARCH
				);
		// Search Info
		// ============
		// We now support sentinels / sentinals that represent a NULL search
		// So look for those sentinels, and force to null
		if( getMainConfig().isNullSearch(inQuery) )
			inQuery = null;
		// Don't whine about nulls
		String normQuery = nie.sn.SearchLogger.normalizeString(
				inQuery, false
			);
		// Log them, if present
		if( null != inQuery )
			statement.setValue(
				nie.sn.SearchLogger.ORIGINAL_SEARCH_DB_FIELD_NAME,
				inQuery
				);
		if( null != normQuery )
			statement.setValue(
				nie.sn.SearchLogger.NORMALIZED_SEARCH_DB_FIELD_NAME,
				normQuery
				);
		// Conditionally log them
		// TODO: If not, backfill
		if( optMatched >= 0 )
			statement.setValue(
				nie.sn.SearchLogger.NUM_FOUND_DB_FIELD_NAME,
				optMatched
				);
		if( optSearched >= 0 )
			statement.setValue(
				nie.sn.SearchLogger.NUM_SEARCHED_DB_FIELD_NAME,
				optSearched
				);

		// No SearchNames Status
		
		// User Info
		// ====================
		// CLIENT_HOST
		if( null != clientIPToUse )
			statement.setValue(
				nie.sn.SearchLogger.CLIENT_IP_DB_FIELD_NAME,
				clientIPToUse
				);

		// No Advertising
		
		// No Click-Through

		// Date / Time Info
		// ====================
		// java.sql.Timestamp currentTime = new Timestamp(
		//		System.currentTimeMillis()
		//		);
		statement.setValue(
			nie.sn.SearchLogger.START_TIME_DB_FIELD_NAME,
			inTimestamp
			);
		statement.setValue(
			nie.sn.SearchLogger.END_TIME_DB_FIELD_NAME,
			inTimestamp
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
			// send the update
			numRows = statement.sendUpdate( true );
		}
		// This exception means we are just not trying right now
		// typically this would be caught above
		catch( Exception e )
		{
			errorMsg( kFName,
				"There was a problem executing the update,"
				+ " or perhaps the database connection is down"
				+ "; this may be temporary (2)."
				+ " This particular search will not be logged."
				+ " Error: " + e
				);
			return -1;
		}
	
		return numRows;
	}

	boolean updateDnsTable( String inIPNumber, String inHostName )
		throws Exception
	{
		final String kFName = "updateDnsTable";
		// Object [] parts = getDBConfig().createStatement();
		// Statement stmt = (Statement) parts[0];
		boolean result = false;
		if( getDoPreviewOnly() ) {
			statusMsg( kFName, "Would do update for '"
				+ inIPNumber + "'='" + inHostName + "'"
				);
			result = true;
		}
		else {
			result = DNSLookup2.staticUpdateDnsTable(
				getDBConfig(), cStatementUpdate, // stmt,
				inIPNumber, inHostName, true
				);
		}
		// getDBConfig().closeStatement( stmt, kClassName, kFName, true );
		return result;
	}



	public boolean _updateRecord(
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



	static String randomIpSegment()
	{
		return "" + ( (int) ( Math.random() * 254.0 ) + 1 );
	}
	static String randomIpTriple()
	{
		return randomIpSegment() + '.' + randomIpSegment() + '.' + randomIpSegment();
	}
	public static String ipToNameFragment( String inStr )
	{
		final String kFName = "ipToNameFragment";
		final String prefix = "ws-";
		inStr = NIEUtil.trimmedStringOrNull( inStr );
		if( null==inStr ) {
			errorMsg( kFName, "Null/empty string passed in, returning null." );
			return null;
		}
		return prefix + NIEUtil.replaceChars( inStr, '.' , '-' );
	}

	// Convert "123" into "onetwothree"
	// Doesn't need to be fancy, don't need "onehundredtwentythree"
	public static String numStringToWords( String inStr )
	{
		final String kFName = "numStringToWords";

		final String[] digitNames = {
			"zero", "one", "two", "three", "four",
			"five", "six", "seven", "eight", "nine"
			};
		inStr = NIEUtil.trimmedStringOrNull( inStr );
		if( null==inStr ) {
			errorMsg( kFName, "Null/empty string passed in, returning NULL" );
			return null;
		}
		StringBuffer buff = new StringBuffer();
		char [] digits = inStr.toCharArray();
		for( int i=0; i<digits.length; i++ )
		{
			char digit = digits[i];
			if( digit < '0' || digit > '9' ) {
				errorMsg( kFName, "Invalid digit at OFFSET " + i
						+ ", string=\""+inStr+'"'
						+ ", returning NULL"
						);
				return null;
			}
			int nameOffset = digit - '0';
			buff.append( digitNames[nameOffset] );
		}

		// statusMsg( kFName, "Input/output = "+inStr+'/'+new String(buff) );
		return new String( buff );
	}
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


    // Return a string in the range of 1 to 254, using modulo math
    // use the input word as a seed
    // Edge cases: a return value from wordToNumber of -1, 0 or 1 will
    // map to the string "1"
    public static String wordToIPSegmentNumberStr( String inWord )
    {
    	int val = wordToNumber( inWord );
    	val = val<0 ? val * -1 : val;
    	val = 0==val ? 1 : val;
    	val = ((val-1) % 254) + 1;
    	return "" + val;
    }
    // Given a word, calculate it's sum giving each letter
    // it's ordinal value from the alphabet, regardless of case
    // and ignore anything else
    // 0 or -1 indicates a word with no letters
    // "a" = 1, "b" = 2, "c" = 3, "aa" = 2, "bc" = 5 (2+3)
    public static int wordToNumber( String inWord )
    {
    	final String kFName = "wordToNumber";
    	inWord = NIEUtil.trimmedLowerStringOrNull( inWord );
    	if( null==inWord ) {
    		errorMsg( kFName, "Null/empty word passed in, returning -1" );
    		return -1;
    	}
    	int outVal = -1;
    	char [] symbols = inWord.toCharArray();
    	for( int i=0; i<symbols.length; i++ ) {
    		char c = symbols[i];
    		if( c < 'a' || c > 'z' )
    			continue;
    		outVal = outVal < 0 ? 0 : outVal;
    		outVal += ( c - 'a' + 1 );
    	}
    	if( outVal < 0 )
    		errorMsg( kFName, "Word had no chars between a and z, returning -1" );
    	return outVal;
    }

	///////////////////////////////////////////////////////////
	private static final void __Simple_Getters_and_Setters__() {}

	int getNumDaysInWindow()
	{
		return mDaysInWindow;
	}
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

	boolean getDoBackFill()
	{
		return mDoBackFill;
	}
	void setDoBackFill()
	{
		setDoBackFill( true );
	}
	void setDoBackFill( boolean inFlag )
	{
		mDoBackFill = inFlag;
	}

	boolean getDoRollDates()
	{
		return mDoRollDates;
	}
	void setDoRollDates()
	{
		setDoRollDates( true );
	}
	void setDoRollDates( boolean inFlag )
	{
		mDoRollDates = inFlag;
	}

	boolean getDoPreviewOnly()
	{
		return mDoPreviewOnly;
	}
	void setDoPreviewOnly()
	{
		setDoPreviewOnly( true );
	}
	void setDoPreviewOnly( boolean inFlag )
	{
		mDoPreviewOnly = inFlag;
	}
	
	String getDataURI()
	{
		return mDataURI;
	}
	public static String getLogTableName()
	{
		return DBConfig.LOG_TABLE;
	}

	int getSiteIdToUse()
	{
		if( mSiteId > 0 )
			return mSiteId;
		return getMainConfig().getSearchLogger().getConfiguredSiteID();
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
		// NOT return _fDBConf
	}
	public void setMainConfig( SearchTuningConfig inMainConfig ) {
		mMainConfig = inMainConfig;
	}
	SearchTuningConfig getMainConfig() {
		return mMainConfig;
	}
	String getMainConfigURI() {
		if( null!=fConfigFileURI )
			return fConfigFileURI;
		if( null != getMainConfig() )
			return getMainConfig().getConfigFileURI();
		return null;
	}
	void _setDBConfig( DBConfig inDB ) {
		_fDBConf = inDB;
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

	private String fConfigFileURI;
	private String mDataURI;
	private LineNumberReader mDataIn;
	private String _mErrorMsg;

	BackfillMatchCounts mBackFiller;
	RollDates mRoller;

	boolean mUseGoogleInstead = DEFAULT_USE_GOOGLE_INSTEAD;
	static final boolean DEFAULT_USE_GOOGLE_INSTEAD = false;

	boolean mDoPreviewOnly = false;

	boolean mDoBackFill = DEFAULT_DO_BACK_FILL;
	static final boolean DEFAULT_DO_BACK_FILL = true;

	boolean mDoRollDates = DEFAULT_DO_ROLL_DATES;
	static final boolean DEFAULT_DO_ROLL_DATES = true;

	int mSiteId = -1;  // Usually we use what's in the main config
	// int mSiteId = DEFAULT_SITE_ID;
	// static final int DEFAULT_SITE_ID = 10;

	boolean mHadError;

	private boolean mNukeDns = DEFAULT_NUKE_DNS;
	private boolean mNukeLog = DEFAULT_NUKE_LOG;
	public static final boolean DEFAULT_NUKE_DNS = false;
	public static final boolean DEFAULT_NUKE_LOG = false;
	
	// The query we will use to pull records
	String _fSQL;

	// The primary database configuration
	DBConfig _fDBConf;
	Statement _cStatementRead;
	Connection _cConnectionRead;
	Statement cStatementUpdate;
	Connection cConnectionUpdate;

	// Which search engine to use, we do NOT always use the
	// main configured one, for example we might use Google instead
	SearchEngineConfig _mTargetSearchEngineConfig;

	int fOperatingMode = DEFAULT_MODE;

	// static final String DEFAULT_NAME_SUFFIX = "somedomain.com";
	// static final String DEFAULT_IP_PREFIX = "198";

	public static final String _GOOGLE_CONFIG_URI =
		AuxIOInfo.SYSTEM_RESOURCE_PREFIX
		+ "static_files/predefined_configs/search_engine_google_public.xml";
	public static final String _SYSTEM_RESOURCE_BASE_CLASS =
		nie.config_ui.Configurator2.kFullClassName;
		// = "nie.config_ui.Configurator2";

    
    static final int kErrorMode = 0;
    static final int kNewOnlyMode = 1;
    static final int _kRetryMode = 2;
    static final int _kRefreshMode = 3;
    static final int kOverwriteAllMode = 4;
    static final int DEFAULT_MODE = kNewOnlyMode;

    int mDaysInWindow = DEFAULT_WINDOW_DAYS;
    static final int DEFAULT_WINDOW_DAYS = 95;
 
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
