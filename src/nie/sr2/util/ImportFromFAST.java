package nie.sr2.util;

import java.sql.*;
import java.util.*;
import java.io.*;
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

// Imports search acrtivity data from FAST
// would be nice to have Best Bets importer too....

public class ImportFromFAST
{

	private final static String kClassName = "ImportFromFAST";

	static final long _MY_INTERVAL = nie.sn.CronLite.HOUR;

	// How often to run
	public long _getRunIntervalInMS() {
		// return MY_INTERVAL;
		return -1L;
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
		ImportFromFAST util = new ImportFromFAST();
		util.parseCommandLine( inArgs );

		int res = 0;
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
			e.printStackTrace( System.err );
			System.exit( 2 );
		}
		util.run();
		statusMsg( kFName, "Recorded " + res + " total entries from all log files" );

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
    
    private /*public*/ ImportFromFAST()
    //	throws UtilException
    {
    	// If you use this one, you must also then call
    	// .setupConfigFromURI() before running
    }

    // NOTE: This constructor may not be called
    // Instead, they may call the null arg version
    // and then call .setupConfigFromURI()
	public ImportFromFAST( SearchTuningConfig inMainConfig )
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
					// roll records forward to current date and time
					else if( flag.startsWith("roll") ) {
						setDoRollDates( true );
					}
					else if( flag.startsWith("no") && flag.indexOf("roll") > 0 ) {
						setDoRollDates( false );
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
		// + "-days int (how many days to spread the searches over, DEFAULT=" + DEFAULT_WINDOW_DAYS + ")" + NIEUtil.NL
		// + NIEUtil.NL
		+ "-preview[_only] (just show what you would do, but don't do it)" + NIEUtil.NL
		+ NIEUtil.NL
		// + "-back[_fill] (Back fill match counts)" + NIEUtil.NL
		// + "-no_back[_fill] turn off, default=" + DEFAULT_DO_BACK_FILL + NIEUtil.NL
		// + "-use_google (instead for adding match counts, IMPLIES -back_fill)" + NIEUtil.NL
		// + "-site_prefix something.com (site prefix for using with Google back fill)" + NIEUtil.NL
		// + NIEUtil.NL
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

			statusMsg( kFName, "Looking for files starting at '" + getDataURI() + "'" );
			List files = findLogFiles( getDataURI() );
			statusMsg( kFName, "******************** Found " + files.size() + " log files." );
			int count = processFiles( files );

			// nukeTablesIfRequested();
			
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
		initRollerNeeded();
		// openDataFile();  NOT here, we process multiple files
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


	private static final void __Directory_Level_Logic__(){}
	//////////////////////////////////////////////////////////

	static List findLogFiles( )
		throws IOException
	{
		return findLogFiles( "." );
	}

static List findLogFiles( String startDir )
	throws IOException
{
	if( null==startDir || startDir.trim().equals("") )
		startDir = ".";
	return findLogFiles( new File( startDir.trim() ) );
}

static List findLogFiles( List dirRoots )
	throws IOException
{
	if( null==dirRoots || dirRoots.isEmpty() ) {
		dirRoots = new ArrayList();
		dirRoots.add( "." );
	}

	List outFiles = new ArrayList();

	// For each root to search
	for( Iterator rit = dirRoots.iterator() ; rit.hasNext() ; ) {
		Object entry = rit.next();

		// List newFiles = findLogFiles( entry );

		List newFiles = null;
		if( entry instanceof File )
			newFiles = findLogFiles( (File) entry );
		else if( entry instanceof String )
			newFiles = findLogFiles( (String) entry );
		else {
	        throw new IOException(
                "Don't know how to scan directory entry of type '"
				+ entry.getClass().getName()
				+ "' for entry '" + entry + "'"
				);
		}

		outFiles.addAll( newFiles );
	}	// end for each root to search

	return outFiles;
}


static List findLogFiles( File inFile )
	throws IOException
{

	// System.err.println( "Scanning '" + inFile + "'" );

	List outFiles = new ArrayList();
	if( inFile.isFile() )
	{
		// Name should start with "query_log."
		String tmpName = inFile.getName();
		if( tmpName.startsWith( "query_log." ) )
			outFiles.add( inFile );
	}
	else if( inFile.isDirectory() ) {

// System.err.println( "Directory '" + inFile + "'" );
// System.err.println( "\tisDir=" + inFile.isDirectory()
// 	+ ", isDot=" + (""+inFile).startsWith(".")
// 	);

		// Now search that root
		File [] entries = inFile.listFiles();
		for( int i=0; i < entries.length ; i++ )
		{
			File entry = entries[i];
	// System.err.println( "\tentry: '" + entry + "'" );
			Object name = entry.getName();
	// System.err.println( "\tnamme: '" + name + "'" );

			// if( entry.isDirectory() && (""+entry).startsWith(".") )
			// 	continue;
			// File fullEntry = new File( inFile, ""+entry );
			// File fullEntry = null;
			// if( inFile.isDirectory() && (""+inFile).startsWith(".") )
			// 	fullEntry = entry;
			// else
			// 	fullEntry = new File( inFile, ""+entry );
			File fullEntry = entry;

	// System.err.println( "\tfull entry: '" + fullEntry + "'" );

			// if(i>5)
			//	break;

			List newFiles = findLogFiles( fullEntry );
			outFiles.addAll( newFiles );

		}
	}
	else {
		throw new IOException(
			"Entry is not a file nor directory: '"
			+ inFile + "'"
			);
	}
	return outFiles;
}

// Fields
// -------
// ClientIP
// - -
// [TimeStamp]
// "HTTPRequest"
// StatusCode
// ContentLength
// "Referer"
// "Agent"
// RequestTime
// SearchTime
// DocSumTime
// NumHits
// [Extra]
//
// Also I added:
// HTTPPath
// HTTPPathTrunc
// HTTPRequest
// FullQueryClause
// Query
//
// Where Extra is
// [(webcluster: VIEW() QUERY() MANAGED() STATS())]
// [
//   (webcluster:
//     VIEW(alphasppublished)
//     QUERY(FQL, nnn, <andnot>...</andnot>)
//     MANAGED(1, 0)
//     STATS(0.0012, 0.0899, 522)
//   )
// ]
//
// OR
// ... RequestTime (space)(space) - (space) []
// Presumably skipping SearchTime and DocSumTime
// and giving a dash for Hit Count


	private static final void __File_Level_Logic__(){}
	//////////////////////////////////////////////////////////
	
	int processFiles( List inFiles )
		throws IOException, DBConfigException
	{
		int counter = 0;
		for( Iterator it = inFiles.iterator(); it.hasNext(); )
		{
			File file = (File) it.next();
			int thisCount = processFile( file );
			counter += thisCount;
		}
		return counter;
	}

	int processFile( String inFileName )
		throws IOException, DBConfigException
	{
		return processFile( new File( inFileName ) );
	}

	int processFile( File inFile )
		throws IOException, DBConfigException
	{
		final String kFName = "processFile";
		/***
		int lineCounter = 0;
		int goodRecCounter = 0;
		int badRecCounter = 0;
		int fieldCounter = 0;
		***/
		// BufferedReader fin = new BufferedReader( new FileReader( inFile ) );
		mDataIn = NIEUtil.openURIReadChar( inFile.toString() );

		int count = processRecords( mDataIn );
/***
		String line = null;
		while( (line=mDataIn.readLine()) != null )
		{
processRecord
			lineCounter++;
			Map fields = parseLogLine( line );
			if( null!=fields && ! fields.isEmpty() ) {
				try {
					fieldCounter += recordFields( fields );
					goodRecCounter++;
				}
				catch( IOException ioe ) {
					rethrow ioe;
				}
				catch( Exception e ) {
					
				}
			}
			else {
				// System.err.println( "WARNING: No fields for line # '" + lineCounter + "' in file '" + inFileName + "', line='" + line + "'" );
				badRecCounter++;
			}
		}
***/
		mDataIn.close();
		/***
		System.err.println( "Lines:" + lineCounter
			+ " Good:" + goodRecCounter
			+ " Bad:" + badRecCounter
			+ ", Total Fields:" + fieldCounter
			);
		***/
		// return lineCounter;
		return count;
	}

	int processRecords( LineNumberReader fin )
		throws IOException, DBConfigException
	{
		final String kFName = "processRecords";
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

		String line = null;
		// For each line in the file
		while( null != (line = fin.readLine()) ) {
			lineCounter = fin.getLineNumber();
			// lineCounter++;

			line = NIEUtil.trimmedStringOrNull( line );
			
			// Some sanity checking
			if( null==line ) {
				continue;
			}
		
			// If the line starts with #, then it's a comment
			if( line.startsWith("#") ) {
				statusMsg( kFName, "Skipping comment line # " + lineCounter );
				continue;
			}

			try {
				recordCounter += processRecord( line );
			}
			catch( java.text.ParseException e ) {
				warningMsg( kFName, "Parse error on line " + lineCounter + ", ignoring entire line, error='" +  e + "', line='" + line + "'" );
			}
		}	// End of While lines in file
		debugMsg( kFName,
			"Done."
			+ " Final statistics:"
			+ " # lines read = " + lineCounter
			+ ", entries added = " + recordCounter
			);
		return recordCounter;
	}
	
	private static final void __Record_Level_Logic__(){}
	//////////////////////////////////////////////////////////

	int processRecord( String inLine )
		throws DBConfigException, java.text.ParseException
	{
		Map fields = parseLogLine( inLine );
		if( null==fields )
			return 0;
		return recordFields( fields );
	}

	static Map parseLogLine( String inLine )
	{
		final String kFName = "parseLogLine";

		final String DASHES = " - - ";
		// final String ERRF = "ParseErrorField";
		// final String ERRM = "ParseErrorMsg";
		int dashesAt = inLine.indexOf( DASHES );
		if( dashesAt < 0 ) {
			warningMsg( kFName, "No dashes" );
			return null;
		}
		Map outMap = new Hashtable();
	
		// Client IP
		String clientIP = inLine.substring( 0, dashesAt ).trim();
		if( clientIP.length() > 0 ) {
			outMap.put( "ClientIP", clientIP );
		}
		else {
			warningMsg( kFName, "Empty client IP." );
			// return null;
		}
	
		// Timestamp
		int b1 = inLine.indexOf( '[', dashesAt+DASHES.length() );
		if( b1 < 0 ) {
			warningMsg( kFName, "No starting timestamp" );
			return null;
		}
		int b2 = inLine.indexOf( ']', b1+1 );
		if( b2 < 0 ) {
			warningMsg( kFName, "No ending timestamp" );
			return null;
		}
		String timeStamp = inLine.substring( b1+1, b2 ).trim();
		if( timeStamp.length() > 0 ) {
			outMap.put( "TimeStamp", timeStamp );
		}
		else {
			warningMsg( kFName, "Empty timestamp" );
			return null;
		}
	
		// HTTP Line
		int q1 = inLine.indexOf( '"', b2+1 );
		if( q1 < 0 ) {
			warningMsg( kFName, "No starting HTTP" );
			return null;
		}
		int q2 = inLine.indexOf( '"', q1+1 );
		if( q2 < 0 ) {
			warningMsg( kFName, "No ending HTTP" );
			return null;
		}
		String httpLine = inLine.substring( q1+1, q2 ).trim();
		String httpPath = null; // We need this in scope
		String httpPathTrunc = null; // without any ? suffix
		if( httpLine.length() > 0 )
		{
			outMap.put( "HTTPRequest", httpLine );
			// HTTP Path
			int s1 = httpLine.indexOf( ' ' );
			if( s1 < 0 ) {
				warningMsg( kFName, "No starting http PATH" );
				return null;
			}
			int s2 = httpLine.indexOf( ' ', s1+1 );
			if( s2 < 0 ) {
				warningMsg( kFName, "No ending http PATH" );
				return null;
			}
			/*String*/ httpPath = httpLine.substring( s1+1, s2 ).trim();
			if( httpPath.length() > 0 )
			{
				outMap.put( "HTTPPath", httpPath );
				// And we'd also like it without the trailing ?
				int qMarkAt = httpPath.indexOf( '?' );
				httpPathTrunc = qMarkAt < 0 ? httpPath
					: httpPath.substring( 0, qMarkAt ).trim()
					;
				// Not critical, so no error checking
				outMap.put( "HTTPPathTrunc", httpPathTrunc );
			}
			else {
				warningMsg( kFName, "Empty http PATH" );
				return null;
			}
		}
		// Else EMPTY HTTP request, just ""
		// This appears to be normal
		// 10.97.0.6 - - [02/Oct/2007... -0700] "" 400 0 "" "" 0.0012  - []
		// ->                                   ^^here
		else {
			// System.err.println( "WARNING: parseLine: empty HTTP" );
			// System.err.println( inLine );
			return null;
		}
	
		// StatusCode
		int scMinAt = q2 + 2;
		int scEndSpaceAt = inLine.indexOf( ' ', scMinAt );
		if( scEndSpaceAt < 0 ) {
			warningMsg( kFName, "No ending for status code" );
			return null;
		}
		String statusCode = inLine.substring( scMinAt, scEndSpaceAt ).trim();
		if( statusCode.length() > 0 )
		{
			outMap.put( "StatusCode", statusCode );
		}
		else {
			warningMsg( kFName, "Empty status code" );
			return null;
		}
	
		// ContentLength
		int clMinAt = scEndSpaceAt + 1;
		int clEndSpaceAt = inLine.indexOf( ' ', clMinAt );
		if( clEndSpaceAt < 0 ) {
			warningMsg( kFName, "No ending for content length." );
			return null;
		}
		String contentLength = inLine.substring( clMinAt, clEndSpaceAt ).trim();
		if( contentLength.length() > 0 )
		{
			outMap.put( "ContentLength", contentLength );
		}
		else {
			warningMsg( kFName, "Empty content length." );
			return null;
		}
	
		// Referer
		int r1 = inLine.indexOf( '"', clEndSpaceAt+1 );
		if( r1 < 0 ) {
			warningMsg( kFName, "No starting referer" );
			return null;
		}
		int r2 = inLine.indexOf( '"', r1+1 );
		if( r2 < 0 ) {
			warningMsg( kFName, "No ending referer" );
			return null;
		}
		String referer = inLine.substring( r1+1, r2 ).trim();
		if( referer.length() > 0 )
		{
			outMap.put( "Referer", referer );
		}
		else {
			// System.err.println( "WARNING: parseLine: empty referer" );
			// return null;
		}
	
		// Client
		int t1 = inLine.indexOf( '"', r2+1 );
		if( t1 < 0 ) {
			warningMsg( kFName, "No starting client" );
			return null;
		}
		int t2 = inLine.indexOf( '"', t1+1 );
		if( t2 < 0 ) {
			warningMsg( kFName, "No ending client" );
			return null;
		}
		String client = inLine.substring( t1+1, t2 ).trim();
		if( client.length() > 0 )
		{
			outMap.put( "Client", client );
		}
		else {
			// System.err.println( "WARNING: parseLine: empty client" );
			// return null;
		}
	
		// RequestTime
		int rtMinAt = t2 + 2;
		int rtEndSpaceAt = inLine.indexOf( ' ', rtMinAt );
		if( rtEndSpaceAt < 0 ) {
			warningMsg( kFName, "No ending space for request time." );
			return null;
		}
		String requestTime = inLine.substring( rtMinAt, rtEndSpaceAt ).trim();
		if( requestTime.length() > 0 )
		{
			outMap.put( "RequestTime", requestTime );
		}
		else {
			warningMsg( kFName, "Epty request time" );
			return null;
		}
	
		// SearchTime
		final String FQL_ERR_PAT =
			"MANAGED(0, 0) STATS(0.0000, 0.0000, 0) ERROR(1201))]"
			;
		// Have seen both
		// MANAGED(0, 0) STATS(0.0000, 0.0000, 0) ERROR(1102))]
		// MANAGED(0, 1) STATS(0.0000, 0.0000, 0) ERROR(1102))]
		final String COMM_ERR_PAT =
			// "MANAGED(0, 0) STATS(0.0000, 0.0000, 0) ERROR(1102))]";
			"STATS(0.0000, 0.0000, 0) ERROR(1102))]";
		final String ESP_ERR3_PAT =
			"STATS(0.0000, 0.0000, 0) ERROR(1018))]";
		final String ESP_ERR4_PAT =
			"STATS(0.0000, 0.0000, 0) ERROR(1003))]";
		int stMinAt = rtEndSpaceAt + 1;
		int stEndSpaceAt = inLine.indexOf( ' ', stMinAt );
		if( stEndSpaceAt < 0 ) {
			System.err.println( "WARNING: parseLine: no ending space for search time." );
			return null;
		}
		String searchTime = inLine.substring( stMinAt, stEndSpaceAt ).trim();
		if( searchTime.length() > 0 )
		{
			outMap.put( "SearchTime", searchTime );
		}
		else {
			// Warn them unless this is a /configuration line
			if( null==httpPath
				|| (
					! httpPath.equalsIgnoreCase( "/configuration" )
					&& ! httpPath.startsWith( "/get?" )
					&& ! httpPath.equals( "/" )
					&& inLine.indexOf( FQL_ERR_PAT ) < 0
					&& inLine.indexOf( COMM_ERR_PAT ) < 0
					&& inLine.indexOf( ESP_ERR3_PAT ) < 0
					&& inLine.indexOf( ESP_ERR4_PAT ) < 0
				   )
			) {
				warningMsg( kFName, "Empty search time"
					+ ". Path='" + httpPath + "'"
					+ '\n' + inLine + '\n'
					);
			}
			return null;
		}
	
		// DocSumTime
		int dtMinAt = stEndSpaceAt + 1;
		int dtEndSpaceAt = inLine.indexOf( ' ', dtMinAt );
		if( dtEndSpaceAt < 0 ) {
			warningMsg( kFName, "No ending space for document summary time." );
			return null;
		}
		String docSumTime = inLine.substring( dtMinAt, dtEndSpaceAt ).trim();
		if( docSumTime.length() > 0 )
		{
			outMap.put( "DocSumTime", docSumTime );
		}
		else {
			warningMsg( kFName, "Empty document summary time" );
			return null;
		}
	
		// NumHits
		int nhMinAt = dtEndSpaceAt + 1;
		int nhEndSpaceAt = inLine.indexOf( ' ', nhMinAt );
	
		// TODO: Revisit how strict to be
		// On the one hand, we can't do much if it doesn't have "extra"
		// On the other, I think it's an optional field, not on by default
		if( nhEndSpaceAt < 0 ) {
			warningMsg( kFName, "No ending space for number of hits (or no Extra query data)." );
			return null;
		}
		String numHits = inLine.substring( nhMinAt, nhEndSpaceAt ).trim();
		if( numHits.length() > 0 ) {
			outMap.put( "NumHits", numHits );
		}
		else {
			warningMsg( kFName, "Empty number of hits (or premature end of line)" );
			return null;
		}
	
		/***
		String numHits = null;
		if( nhEndSpaceAt < 0 ) {
			System.err.println( "WARNING: parseLine: no ending space for number of hits (or no Extra query data)." );
			// return null;
			numHits = inLine.substring( nhMinAt ).trim();
		}
		else {
			numHits = inLine.substring( nhMinAt, nhEndSpaceAt ).trim();
		}
		if( numHits.length() > 0 )
		{
			outMap.put( "NumHits", numHits );
		}
		else {
			System.err.println( "WARNING: parseLine: empty number of hits." );
			return null;
		}
		if( nhEndSpaceAt < 0 ) {
			System.err.println( "WARNING: parseLine: no extended / extra data found, will not have query field" );
			return outMap;
		}
		***/
	
		// [Extra]
		//
		// Where Extra is
		// [(webcluster: VIEW() QUERY() MANAGED() STATS())]
		// [
		//   (webcluster:
		//     VIEW(alphasppublished)
		//     QUERY(FQL, nnn, <andnot>...</andnot>)
		//     MANAGED(1, 0)
		//     STATS(0.0012, 0.0899, 522)
		//   )
		// ]
	
		// Extra
		int b3 = inLine.indexOf( '[', nhEndSpaceAt+1 );
		if( b3 < 0 ) {
			warningMsg( kFName, "No starting for extra data." );
			return null;
		}
		int b4 = inLine.lastIndexOf( ']' );
		if( b4 < 0 || b4 < b3 ) {
			warningMsg( kFName, "No ending for extra data." );
			return null;
		}
		String extra = inLine.substring( b3+1, b4 ).trim();
		if( extra.length() > 0 ) {
			outMap.put( "Extra", extra );
		}
		else {
			warningMsg( kFName, "Empty extra data." );
			return null;
		}
	
		final String QRY_PAT_START = "QUERY(FQL, ";
		// final String QRY_PAT_END = ") MANAGED(0, 0) STATS(";
		final String QRY_PAT_END = ") MANAGED(";
		// Obsolete syntax, we will ignore
		final String OBS_PAT = "QUERY(AQL, ";
		int outerQueryStartsAt = extra.indexOf( QRY_PAT_START );
		if( outerQueryStartsAt < 0 ) {
			if( null!=extra && extra.indexOf( OBS_PAT ) < 0 ) {
				warningMsg( kFName, "Could not find start of QUERY clause in extra data.\n" + inLine );
				// System.exit(3);
			}
			return null;
		}
		// Skip over the length
		final String QRY_PAT2 = ", ";
		int nextQueryPatAt = extra.indexOf(
			QRY_PAT2, outerQueryStartsAt + QRY_PAT_START.length()
			);
		if( nextQueryPatAt < 0 ) {
			warningMsg( kFName, "Could not find secondary start of QUERY clause in extra data." );
			return null;
		}
		int innerQueryStartsAt = nextQueryPatAt + QRY_PAT2.length();
		// int queryEndParenAt = extra.indexOf( ')', innerQueryStartsAt );
		// Parens CAN be in there, so need longer pattern
		int queryEndParenAt = extra.indexOf( QRY_PAT_END, innerQueryStartsAt );
		// if( nextQueryPatAt < 0 ) {
		if( queryEndParenAt < 0 ) {
			warningMsg( kFName, "Could not find end of QUERY clause in extra data.\n" + inLine + '\n' );
			return null;
		}
		// System.err.println( "queryEndParenAt=" + queryEndParenAt );
		String fullQueryClause = extra.substring(
			innerQueryStartsAt, queryEndParenAt ).trim();
		if( fullQueryClause.length() > 0 ) {
			outMap.put( "FullQueryClause", fullQueryClause );
			// System.out.println( fullQueryCluase );
		}
		else {
			warningMsg( kFName, "Empty QUERY clause in extra data." );
			return null;
		}
	
		final String FINAL_QRY_PAT =
			"<in><scope text=\"content\"/><or><or><string text=\"";
		// And some we don't want
		final String IGNORE_QRY_PAT_1 = "<filter>";
		final String IGNORE_QRY_PAT_2 =
			"<andnot><and><in><scope text=\"urls\"/><or><or><string text=\"";
		final String IGNORE_QRY_PAT_3 =
			"<in><scope text=\"content\"/><string text=\".\">";
		final String IGNORE_QRY_PAT_4 =
			"<in><scope text=\"content\"/><string text=\"/\">";
		final String IGNORE_QRY_PAT_5 =
			"<in><scope text=\"content\"/><string text=\"''\">";
		final String IGNORE_QRY_PAT_6 =
			"<in><scope text=\"content\"/><string text=\"'\">";
		final String IGNORE_QRY_PAT_7 =
			"<in><scope text=\"meta.contentid\"/><equals><string text=\"";
		final String IGNORE_QRY_PAT_8 =
			"<string text=\"aliveCheck\"><token text=\"alivecheck\"/></string>";
		final String IGNORE_QRY_PAT_9 =
			"<andnot><and><in><scope text=\"anchortext\"/><or><or><string text=\"";
		final String IGNORE_QRY_PAT_10 =
			"<andnot><and><in><scope text=\"generic1\"/><or><or><string text=\"";
		final String IGNORE_QRY_PAT_11 =
			"<and><in><scope text=\"generic1\"/><string text=\"";
		final String IGNORE_QRY_PAT_12 =
			"<andnot><and><in><scope text=\"title\"/><or><or><string text=\"";
		final String IGNORE_QRY_PAT_13 =
			"<andnot><and><in><scope text=\"urlkeywords\"/><or><or><string text=\"";
	
		final String ATYPICAL_START_PAT =
			"<in><scope text=\"content\"/><string text=\"";
	
		int finQryPatAt = fullQueryClause.indexOf( FINAL_QRY_PAT );
		if( finQryPatAt < 0 ) {
			if( ! fullQueryClause.startsWith( IGNORE_QRY_PAT_1 )
				&& ! fullQueryClause.startsWith( IGNORE_QRY_PAT_2 )
				&& ! fullQueryClause.startsWith( IGNORE_QRY_PAT_3 )
				&& ! fullQueryClause.startsWith( IGNORE_QRY_PAT_4 )
				&& ! fullQueryClause.startsWith( IGNORE_QRY_PAT_5 )
				&& ! fullQueryClause.startsWith( IGNORE_QRY_PAT_6 )
				&& ! fullQueryClause.startsWith( IGNORE_QRY_PAT_7 )
				&& ! fullQueryClause.startsWith( IGNORE_QRY_PAT_8 )
				&& ! fullQueryClause.startsWith( IGNORE_QRY_PAT_9 )
				&& ! fullQueryClause.startsWith( IGNORE_QRY_PAT_10 )
				&& ! fullQueryClause.startsWith( IGNORE_QRY_PAT_11 )
				&& ! fullQueryClause.startsWith( IGNORE_QRY_PAT_12 )
				&& ! fullQueryClause.startsWith( IGNORE_QRY_PAT_13 )
				&& ! fullQueryClause.startsWith( ATYPICAL_START_PAT )
			) {
				warningMsg( kFName, "Could not find start of final query in extra data."
					+ "Pattern: " + FINAL_QRY_PAT
					+ "extraAt: " + fullQueryClause
					);
				// System.exit(3);
			}
			return null;
		}
		int finQryEndQuoteAt = fullQueryClause.indexOf( '"', finQryPatAt+FINAL_QRY_PAT.length() );
		if( finQryEndQuoteAt < 0 ) {
			warningMsg( kFName, "Could not find end of final query in extra data."
					+ "extraAt: " + fullQueryClause
					+ '\n' + inLine
					);
			// System.exit(3);
			return null;
		}
		String query = fullQueryClause.substring( 
			finQryPatAt+FINAL_QRY_PAT.length(),
			finQryEndQuoteAt
			).trim();
		if( query.length() > 0 ) {
			outMap.put( "Query", query );
			// System.out.println( query );
		}
		else {
			System.err.println( "WARNING: parseLine: empty final query in extra data." );
			return null;
		}
	
		return outMap;
	}



	int recordFields( Map inFields )
		// throws Exception
		throws DBConfigException, java.text.ParseException
	{
		// System.out.println( "Recording fields with " + inFields.keySet().size() + " items" );

		// ClientIP
		// TimeStamp
		// StatusCode
		// NumHits
		// Query
		// HTTPPath
		// HTTPPathTrunc
		// HTTPRequest
		// FullQueryClause

		return insertLogRecord(
				(String) inFields.get("Query"),
				NIEUtil.stringToIntOrDefaultValue( ((String) inFields.get("NumHits")), -1, true, true ),  // Matched
				0,	// int optSearched,
				NIEUtil.stringToSqlTimestamp( (String) inFields.get("TimeStamp") ),
				(String) inFields.get("ClientIP")
			);
	}


	// Based losely on nie.sn.SearchLogger
	int insertLogRecord(
		String inQuery, int optMatched, int optSearched,
		java.sql.Timestamp inTimestamp,
		String clientIPToUse //, String clientNameToUse
	)
		// throws Exception
		throws DBConfigException
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
	
	private static final void __OLD__(){}
	/////////////////////////////////////////

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

	void _openDataFile()
		throws IOException
	{
		final String kFName = "openDataFile";

		statusMsg( kFName,
				"Opening data file: " + getDataURI()
				);
		mDataIn = NIEUtil.openURIReadChar( getDataURI() );
		
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

	String _mSitePrefix;

	private String fConfigFileURI;
	private String mDataURI;
	private LineNumberReader mDataIn;
	private String _mErrorMsg;

	BackfillMatchCounts _mBackFiller;
	RollDates mRoller;

	boolean _mUseGoogleInstead = DEFAULT_USE_GOOGLE_INSTEAD;
	static final boolean DEFAULT_USE_GOOGLE_INSTEAD = false;

	boolean mDoPreviewOnly = false;

	boolean _mDoBackFill = DEFAULT_DO_BACK_FILL;
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

    int _mDaysInWindow = DEFAULT_WINDOW_DAYS;
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
