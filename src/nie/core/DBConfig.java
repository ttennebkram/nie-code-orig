package nie.core;

import java.util.*;
import java.io.*;
import nie.core.*;
import nie.sn.SearchTuningConfig;
import org.jdom.Element;
import org.jdom.Document;
import org.jdom.transform.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;

import java.sql.*;

public class DBConfig
{

	// Notes about ADDING NEW DRIVERS
	//
	// Get the appropriate driver files, and check dist rights
	//
	// Change this file, LOOK FOR:
	//  "Add Driver Marker"
	// That's typeically where you need to add stuff.
	// Hint: Start at the bottom of the file and define the contstants

	// Todo: given all the if vendor == this else if vendor == that it would
	// seem this is a good candidate for subclassing,
	// this code will offend purists


	private final static String kClassName = "DBConfig";

	// Force debug
	static {
		/***
		((RunLogBasicImpl)getRunLogObject()).setVerbosityByString(
			"-debug:" + kClassName + ".closeConnection", true
			);
		***/
	}

	////// Main ////////////////////////////////////////////////////
	private static final void ___sep__M_A_I_N__(){}
	//////////////////////////////////////////////////////////////////////


	public static final String kSyntaxMsg =
		NIEUtil.NL
		+ NIEUtil.NL
		+ "Syntax is: java " + kClassName + " db_config_uri.xml|-?|-help [options...]" + NIEUtil.NL
		// + " [sql_test_statement]"
		+ NIEUtil.NL
		+ "With just db_config_uri.xml specified, a quick check of the database connection configuration is performed." + NIEUtil.NL
		+ NIEUtil.NL
		+ "REMINDER: config.xml must come before any -args."
		+ NIEUtil.NL
		+ "Table checking options include:" + NIEUtil.NL
		+ "[ -check_table[s] tablename|-all [-create[_table]_if_missing]"
		+ " [-overwrite_table[s]]" + NIEUtil.NL
		+ "    [-load_sample_data [-overwrite_data]"
		+ " [-data_directory csv_dir] ]"  + NIEUtil.NL
		+ "]" + NIEUtil.NL
		+ NIEUtil.NL
		+ "Exporting tables:" + NIEUtil.NL
		+ "-export_table[s] tablename|-all"
		+ " -data_directory csv_dir"  + NIEUtil.NL
		+ " -create_dir_if_missing" + NIEUtil.NL
		+ NIEUtil.NL
		+ "Importing tables:" + NIEUtil.NL
		+ "-import_table[s] tablename|-all"
		+ " -data_directory csv_dir"  + NIEUtil.NL
		+ NIEUtil.NL
		+ "Deleting tables:" + NIEUtil.NL
		+ "[ -delete_table[s] tablename|-all ]" + NIEUtil.NL
		+ NIEUtil.NL
		+ "BE CAREFUL with the -delete and -overwrite options!!!"+ NIEUtil.NL
		+ NIEUtil.NL
		+ "To just purge all existing records use -overwrite_table[s]"+ NIEUtil.NL
		+ "(vs. -overwrite_data, which only used when loading sample data)"+ NIEUtil.NL
		+ NIEUtil.NL
		;


	//////////////////////////////////////////////////////////////////////
	public static void main(String[] args)
	{
		final String kFName = "main";
		// statusMsg( kFName, "Starting" );

		if( args.length < 1 )
		{
			errorMsg( kFName,
				"Syntax error, missing arg1 (config file name)."
				+ kSyntaxMsg
				);
			System.exit( 1 );
		}
		String configFile = args[0];
		String tmpStr = configFile.toLowerCase();
		if( tmpStr.equals("-?")
			|| tmpStr.equals("-h") || tmpStr.equals("-help")
			)
		{
			statusMsg( kFName, "Syntax reminder:" + kSyntaxMsg );
			System.exit(0);
		}

		String lTargetTable = null;
		String lDataDirectory = null;
		boolean lDoCheck = false;
		boolean lDoCreateIfNeeded = false;
		boolean lDoCreateDataDirIfNeeded = false;
		boolean lDoOverwriteData = false;
		boolean lDoOverwriteTables = false;
		boolean lDoDeleteTables = false;
		boolean lDoLoadData = false;
		boolean lDoExportData = false;
		boolean lDoAllTables = false;
		// boolean lDoCheckAllTables = false;
		// look at the args AFTER 
		for( int i=1; i<args.length; i++ )
		{
			String arg = args[i];
			tmpStr = arg.toLowerCase();
			debugMsg( kFName, "arg[" + i + "]=\"" + tmpStr + "\"" );


			if( tmpStr.equals( "-check_table" )
					|| tmpStr.equals( "-check_tables" )
				)
			{
				debugMsg( kFName, "Check table flag" );
				if( null != lTargetTable || lDoAllTables )
				{
					errorMsg( kFName,
						"-check_table/-delete_table/-export_table set more than once (1), not allowed."
						+ kSyntaxMsg
						);
					System.exit( 1 );
				}

				lDoCheck = true;

				// Look at the next arg on the command line
				i++;
				// If this is the end of the command line
				if( i >= args.length )
				{
//					errorMsg( kFName,
//						"No table name given after -table"
//						+ kSyntaxMsg
//						);
//					System.exit( 1 );
					statusMsg( kFName,
						"No table name given after -check_table"
						+ ", so will check all defined system tables."
						+ kSyntaxMsg
						);
					break;
				}
				// Else not the end of the line
				else
				{
					String tmpStr2 = args[i];
					debugMsg( kFName, "looking at next item after -check...\"" + tmpStr2 + "\"" );
					// Does the next arg start with a -?
					if( tmpStr2.startsWith( "-" ) )
					{
						debugMsg( kFName, "dashed option" );
						String tmpStr3 = tmpStr2.toLowerCase();
						// If it's -all, then we do all tables
						if( tmpStr3.equals( "-all" ) )
						{
							lDoAllTables = true;
							lTargetTable = null;
						}
						// Else it's some other option
						// push this back on the command line and
						// let the higher level logic handle it
						else
						{
							i--;
							continue;
						}
					}
					// Else dones't end with -, so assume it's a table name
					else
					{
						lTargetTable = tmpStr2;
						debugMsg( kFName, "check specific table \"" + lTargetTable + "\"" );
					}
				}
			}
			else if( tmpStr.equals( "-delete_table" )
					|| tmpStr.equals( "-delete_tables" )
					|| tmpStr.equals( "-drop_table" )
					|| tmpStr.equals( "-drop_tables" )
				)
			{
				debugMsg( kFName, "DELETE table flag" );
				if( null != lTargetTable || lDoAllTables )
				{
					errorMsg( kFName,
						"-check_table/-delete_table/-export_table set more than once (2), not allowed."
						+ kSyntaxMsg
						);
					System.exit( 1 );
				}

				lDoDeleteTables = true;

				// Look at the next arg on the command line
				i++;
				// If this is the end of the command line
				if( i >= args.length )
				{
//					errorMsg( kFName,
//						"No table name given after -table"
//						+ kSyntaxMsg
//						);
//					System.exit( 1 );
					statusMsg( kFName,
						"No table name given after -delete_table"
						// + ", so will check all defined system tables."
						+ kSyntaxMsg
						);
					break;
				}
				// Else not the end of the line
				else
				{
					String tmpStr2 = args[i];
					debugMsg( kFName, "looking at next item after -delete...\"" + tmpStr2 + "\"" );
					// Does the next arg start with a -?
					if( tmpStr2.startsWith( "-" ) )
					{
						debugMsg( kFName, "dashed option" );
						String tmpStr3 = tmpStr2.toLowerCase();
						// If it's -all, then we do all tables
						if( tmpStr3.equals( "-all" ) )
						{
							lDoAllTables = true;
							lTargetTable = null;
						}
						// Else it's some other option
						// push this back on the command line and
						// let the higher level logic handle it
						else
						{
							i--;
							continue;
						}
					}
					// Else dones't end with -, so assume it's a table name
					else
					{
						lTargetTable = tmpStr2;
						debugMsg( kFName, "delete specific table \"" + lTargetTable + "\"" );
					}
				}
			}
			else if( tmpStr.equals( "-export_table" )
					|| tmpStr.equals( "-export_tables" )
					|| tmpStr.equals( "-export_data" )
				)
			{
				debugMsg( kFName, "Export table flag" );
				if( null != lTargetTable || lDoAllTables )
				{
					errorMsg( kFName,
						"-check_table/-delete_table/-export_table set more than once (3), not allowed."
						+ kSyntaxMsg
						);
					System.exit( 1 );
				}

				lDoExportData = true;

				// Look at the next arg on the command line
				i++;
				// If this is the end of the command line
				if( i >= args.length )
				{
//					errorMsg( kFName,
//						"No table name given after -table"
//						+ kSyntaxMsg
//						);
//					System.exit( 1 );
					statusMsg( kFName,
						"No table name given after -export_table"
						+ ", so will check all defined system tables."
						+ kSyntaxMsg
						);
					lDoAllTables = true;
					break;
				}
				// Else not the end of the line
				else
				{
					String tmpStr2 = args[i];
					debugMsg( kFName, "looking at next item after -export...\"" + tmpStr2 + "\"" );
					// Does the next arg start with a -?
					if( tmpStr2.startsWith( "-" ) )
					{
						debugMsg( kFName, "dashed option" );
						String tmpStr3 = tmpStr2.toLowerCase();
						// If it's -all, then we do all tables
						if( tmpStr3.equals( "-all" ) )
						{
							lDoAllTables = true;
							lTargetTable = null;
						}
						// Else it's some other option
						// push this back on the command line and
						// let the higher level logic handle it
						else
						{
							i--;
							continue;
						}
					}
					// Else dones't end with -, so assume it's a table name
					else
					{
						lTargetTable = tmpStr2;
						debugMsg( kFName, "export specific table \"" + lTargetTable + "\"" );
					}
				}
			}
			// We just lump them all together
			else if( tmpStr.equals( "-data_directory" )
				|| tmpStr.equals( "-data_dir" )
				|| tmpStr.equals( "-dir" )
				|| tmpStr.equals( "-import_directory" )
				|| tmpStr.equals( "-export_directory" )
				|| tmpStr.equals( "-import_dir" )
				|| tmpStr.equals( "-export_dir" )
				|| tmpStr.equals( "-csv_directory" )
				|| tmpStr.equals( "-csv_dir" )
				)
			{
				debugMsg( kFName, "data directory flag" );
				if( null != lDataDirectory )
				{
					errorMsg( kFName,
						"-data_directory set more than once, not allowed."
						+ kSyntaxMsg
						);
					System.exit( 1 );
				}

				// Look at the next arg on the command line
				i++;
				// If this is the end of the command line
				if( i >= args.length )
				{
//					errorMsg( kFName,
//						"No table name given after -table"
//						+ kSyntaxMsg
//						);
//					System.exit( 1 );
					statusMsg( kFName,
						"No data directory name given after -data_directory"
						+ ", so will check all defined system tables."
						+ kSyntaxMsg
						);
					break;
				}
				// Else not the end of the line
				else
				{
					String tmpStr2 = args[i];
					debugMsg( kFName, "looking at next item after -data_dir...\"" + tmpStr2 + "\"" );
					// Does the next arg start with a -?
					if( tmpStr2.startsWith( "-" ) )
					{
						debugMsg( kFName, "dashed option" );
						String tmpStr3 = tmpStr2.toLowerCase();
						// If it's -all, then we do all tables
						if( tmpStr3.equals( "-system" ) )
						{
							lDataDirectory = null;
						}
						// Else it's some other option
						// push this back on the command line and
						// let the higher level logic handle it
						else
						{
							i--;
							continue;
						}
					}
					// Else dones't end with -, so assume it's a table name
					else
					{
						lDataDirectory = tmpStr2;
						debugMsg( kFName, "import data from \"" + lDataDirectory + "\"" );
					}
				}
			}
			else if( tmpStr.equals( "-create_if_missing" )
				|| tmpStr.equals( "-create_table_if_missing" )
				|| tmpStr.equals( "-create_tables_if_missing" ) )
			{
				lDoCreateIfNeeded = true;
			}
			else if( tmpStr.equals( "-create_data_dir_if_missing" )
				|| tmpStr.equals( "-create_data_directory_if_missing" )
				|| tmpStr.equals( "-create_dir_if_missing" )
				|| tmpStr.equals( "-create_directory_if_missing" ) )
			{
				lDoCreateDataDirIfNeeded = true;
			}
			else if( tmpStr.equals( "-overwrite_data" ) )
			{
				lDoOverwriteData = true;
			}
			else if( tmpStr.startsWith( "-overwrite_table" ) )
			{
				lDoOverwriteTables = true;
			}
			else if( tmpStr.equals( "-load_sample_data" )
					|| tmpStr.equals( "-import_sample_data" )
					|| tmpStr.equals( "-load_data" )
					|| tmpStr.equals( "-import_data" )
					|| tmpStr.equals( "-import_table" )
					|| tmpStr.equals( "-import_tables" )
				)
			{
				debugMsg( kFName, "Import table flag" );
				if( null != lTargetTable || lDoAllTables )
				{
					errorMsg( kFName,
						"-check_table/-delete_table/-import_table/-export_table set more than once (4), not allowed."
						+ " Extra flag was: '" + tmpStr + "'"
						+ kSyntaxMsg
						);
					System.exit( 1 );
				}

				lDoLoadData = true;

				// Look at the next arg on the command line
				i++;
				// If this is the end of the command line
				if( i >= args.length )
				{
//					errorMsg( kFName,
//						"No table name given after -table"
//						+ kSyntaxMsg
//						);
//					System.exit( 1 );
					statusMsg( kFName,
						"No table name given after -import_table"
						+ ", so will check all defined system tables."
						+ kSyntaxMsg
						);
					lDoAllTables = true;
					break;
				}
				// Else not the end of the line
				else
				{
					String tmpStr2 = args[i];
					debugMsg( kFName, "looking at next item after -export...\"" + tmpStr2 + "\"" );
					// Does the next arg start with a -?
					if( tmpStr2.startsWith( "-" ) )
					{
						debugMsg( kFName, "dashed option" );
						String tmpStr3 = tmpStr2.toLowerCase();
						// If it's -all, then we do all tables
						if( tmpStr3.equals( "-all" ) )
						{
							lDoAllTables = true;
							lTargetTable = null;
						}
						// Else it's some other option
						// push this back on the command line and
						// let the higher level logic handle it
						else
						{
							i--;
							continue;
						}
					}
					// Else dones't end with -, so assume it's a table name
					else
					{
						lTargetTable = tmpStr2;
						debugMsg( kFName, "import specific table \"" + lTargetTable + "\"" );
					}
				}
			}
			else
			{
				errorMsg( kFName,
					"Unknown or misplaced command line option \"" + arg + "\"."
					+ kSyntaxMsg
					);
				System.exit( 1 );
			}

		}


		// Some additional command line checks
		// -------------------------------------
		// Variables defined above
		//	String lTargetTable = null;
		//	String lDataDirectory = null;
		//	boolean lDoCheck = false;
		//	boolean lDoCreateIfNeeded = false;
		//	boolean lDoOverwriteData = false;
		//	boolean lDoOverwriteTables = false;
		//	boolean lDoDeleteTables = false;
		//	boolean lDoLoadData = false;
		//	boolean lDoAllTables = false;

		// Sanity check, delete flag is incompatible with most everything else
		if( lDoDeleteTables ) {
			// Should not be combined with most other options
			if( lDoCheck
				|| lDoExportData
				|| lDoCreateIfNeeded
				|| lDoOverwriteData
				|| lDoOverwriteTables
				|| lDoLoadData
				|| null!=lDataDirectory
			) {
				errorMsg( kFName,
					"-delete_table flag incorrectly combined with non-delete option(s)."
					+ " -delete_table(s) only accepts a target table name or -all."
					+ " BE CAREFUL WITH THIS OPTION!!!"
					+ " Perhaps you meant to use -overwrite_table(s) ?"
					+ " Exiting program (error code 1)."
					+ kSyntaxMsg
					);
				System.exit( 1 );
			}

			if( null==lTargetTable && ! lDoAllTables ) {
				errorMsg( kFName,
					"-delete_table must specify a target table or -all."
					+ " BE CAREFUL WITH THIS OPTION!!!"
					+ " You may also want to check out -overwrite_table(s)"
					+ " Exiting program (error code 1)."
					+ kSyntaxMsg
					);
				System.exit( 1 );
			}
		}

		
		// Sanity check, export flag is incompatible with most everything else
		if( lDoExportData ) {
			// Should not be combined with most other options
			if( lDoCheck
				|| lDoCreateIfNeeded
				|| lDoOverwriteData
				|| lDoOverwriteTables
				|| lDoLoadData
				// || null!=lDataDirectory
			) {
				errorMsg( kFName,
					"-export_table flag incorrectly combined with non-export option(s)."
					+ " -export_table(s) only accepts a target table name or -all"
					+ " and a data directory."
					+ " Exiting program (error code 1)."
					+ kSyntaxMsg
					);
				System.exit( 1 );
			}

			if( null==lTargetTable && ! lDoAllTables ) {
				errorMsg( kFName,
					"-export_table must specify a target table or -all."
					+ " Exiting program (error code 1)."
					+ kSyntaxMsg
					);
				System.exit( 1 );
			}
		}


		// If you set a data directory, clearly you are trying to import/export
		if( null!=lDataDirectory && (!lDoLoadData && !lDoExportData) ) {
			lDoLoadData = true;
			infoMsg( kFName, "-load_data/-export_data implicitely set because -data_directory was set.");
		}
		// Sanity check
		if( lDoCreateDataDirIfNeeded && ! lDoExportData ) {
			errorMsg( kFName,
					"-create_data_directory_if_missing is only valid when exporting data."
					+ " Exiting program (error code 1)."
					+ kSyntaxMsg
					);
				System.exit( 1 );
		}
		// If you set a data directory, does it exist?
		if( null!=lDataDirectory ) {
			File tmpDir = new File( lDataDirectory );
			if( ! tmpDir.exists() ) {
				if( lDoLoadData || ! lDoCreateDataDirIfNeeded ) {
					errorMsg( kFName,
						"Import/export directory doesn't exist '" + lDataDirectory + "'"
						+ " Exiting program (error code 1)."
						+ kSyntaxMsg
						);
					System.exit( 1 );				
				}
				// OK, create it
				if( tmpDir.mkdirs() ) {
					statusMsg( kFName,
						"Created export directory '" + lDataDirectory + "'"
						);
				}
				else {
					errorMsg( kFName,
						"Error creating import/export directory '" + lDataDirectory + "'"
						+ " Exiting program (error code 1)."
						+ kSyntaxMsg
						);
					System.exit( 1 );								
				}
			}
			else if( ! tmpDir.isDirectory() ) {
				errorMsg( kFName,
						"Import/export dir is not a directory '" + lDataDirectory + "'"
						+ kSyntaxMsg
						);
					System.exit( 1 );				
			}
		}
		
		
		// If you told us to overwrite tables, clearly you want them (re)created
		if( lDoOverwriteTables && ! lDoCreateIfNeeded ) {
			lDoLoadData = true;
			infoMsg( kFName, "-create_if_missing implicitely set because -overwrite_tables was set.");
		}


		// The check flag is turned on if any of the other options are turned on
		// and it also carries the target with it
		if( ! lDoCheck && ! lDoDeleteTables && ! lDoExportData &&
			(lDoCreateIfNeeded || lDoOverwriteData || lDoOverwriteTables || lDoLoadData /*|| null!=lDataDirectory*/ )
		) {
			lDoCheck = true;
			infoMsg( kFName, "-check_table activated by one or more implicit table related options.");
		}

		// Checking also requires that a table was set, or that all tables were set
		if( lDoCheck && ! ( lDoAllTables || null!=lTargetTable ) ) {
			errorMsg( kFName,
				"Table checking operations require you to specify a target table or -all."
				+ " Exiting program (error code 1)."
				+ kSyntaxMsg
				);
			System.exit( 1 );
		}



		// Now try reading the config
		statusMsg( kFName,
			"Will read config URI \"" + configFile + "\""
			);
		DBConfig myDB = null;
		try {
			myDB = new DBConfig( configFile );
		}
		catch (DBConfigException e) {
			errorMsg( kFName,
				"Unable to construct DB Config object"
				+ " Exception = " + e
				+ " Exiting program (error code 2)."
				);
			System.exit( 2 );
		}
		statusMsg( kFName,
			"Was able to read config."
			);


		// First we consider trashing tables
		if( lDoDeleteTables || lDoOverwriteTables ) {
			boolean tmpWasOK = false;
			if( null!=lTargetTable ) {
				// do not ignore warnings if we're trying to specifically delete it
				tmpWasOK = myDB.dropASpecificDBTable( lTargetTable, ! lDoDeleteTables );
			}
			else if( lDoAllTables ) {
				tmpWasOK = myDB.dropAllDBTables( ! lDoDeleteTables );
			}
			else {
				errorMsg( kFName,
					"No tables to remove!"
					+ " Exiting program (error code 4)."
					);
				System.exit( 4 );
			}

			if( ! tmpWasOK ) {
				if( lDoDeleteTables ) {
					errorMsg( kFName,
						"Did not successfully delete table(s)"
						+ " Exiting program (error code 3)."
						);
					System.exit( 3 );
				}
				else
					warningMsg( kFName,
						"Did not successfully remove pre-existing table(s) for overwrite operation."
						+ " Perhaps -overwrite_tables was specified but not all the tables"
						+ " exist yet (for example if this is the first run against a new database)."
						+ " Will continue to attempt further operations."
						);
			}
			else
				statusMsg( kFName, "Previously existing table(s) successfully deleted/dropped." );
		}

		// Should we check system tables
		boolean isAllOK = false;

		// Other table items
		if( lDoCheck )
		{
			statusMsg( kFName,
				"Will now check presence of predefined system tables."
				);
			if( null!=lTargetTable ) {
				isAllOK = myDB.verifyASpecificDBTable( lTargetTable,
					true, lDoCreateIfNeeded
					);
			}
			else if( lDoAllTables ) {
				isAllOK = myDB.verifyAllDBTables( true, lDoCreateIfNeeded );
			}
			else {
				errorMsg( kFName,
					"No tables to check!"
					+ " Exiting program (error code 4)."
					);
				System.exit( 4 );
			}

		}

		// Should we load system tables
		if( lDoLoadData )
		{
			if( ! lDoCheck ) {
				errorMsg( kFName,
					"Can't load data without checking table."
					);
				System.exit( 1 );
			}

			if( ! isAllOK ) {
				errorMsg( kFName,
					"Can't load data because previous table checks failed."
					);
				System.exit( 3 );
			}

			statusMsg( kFName,
				"Will now load data into predefined system tables."
				);
			if( null!=lTargetTable ) {
				myDB.loadASpecificDBTable( lTargetTable,
					/* true, lDoCreateIfNeeded,*/ lDoOverwriteData,
					lDataDirectory
					);
			}
			else if( lDoAllTables ) {
				myDB.loadAllDBTables( /* true, lDoCreateIfNeeded,*/
					lDoOverwriteData, lDataDirectory );
			}
			else {
				errorMsg( kFName,
					"No tables to import to!"
					+ " Exiting program (error code 4)."
					);
			}

		}

		// Should we export?
		if( lDoExportData )
		{
			statusMsg( kFName,
				"Will now export data from predefined system tables."
				);
			if( null!=lTargetTable ) {
				myDB.saveASpecificDBTable( lTargetTable,
					lDataDirectory
					);
			}
			else if( lDoAllTables ) {
				myDB.saveAllDBTables( lDataDirectory );
			}
			else {
				errorMsg( kFName,
					"No tables to export from!"
					+ " Exiting program (error code 4)."
					);
			}

		}
	}

	//////////////////////////////////////////////////////////////////////
	private static void ___sep__Construction_and_Initialization__() {}
	///////////////////////////////////////////////////////////////////////

	// This constructor is usually used for testing
	// can accept a URI
	public DBConfig( String inURI )
		throws DBConfigException
	{
		final String kFName = "constructor(1)";
		final String kExTag = kClassName + ':' + kFName + ": ";

		// create jdom element and store info
		// Sanity checks
		if( inURI == null )
			throw new DBConfigException( kExTag,
				"Constructor was passed in a NULL URI (file name, url, etc)."
				);

		// Instantiate and store the main JDOMHelper a
		fConfigTree = null;
		try
		{
			// fConfigTree = new JDOMHelper( inURI );
			// use the one that handles includes
			fConfigTree = new JDOMHelper( inURI, null, 0, null );

		}
		catch (JDOMHelperException e)
		{
			throw new DBConfigException( kExTag,
				"Got JDOMHelper Exception (1): "
				+ e );
		}

		if( ! fConfigTree.getElementName().equals(MAIN_ELEMENT_NAME) ) {
			// Try the secondary path
			Element tmpElem = fConfigTree.findElementByPath( PATH2 );
			// Or maybe the REALLY old tertiary path
			if( null==tmpElem ) {
				tmpElem = fConfigTree.findElementByPath( PATH3 );
				if( null!=tmpElem )
					warningMsg( kFName,
						"Using deprecated database config path \"" + PATH3 + "\""
						+ " Should be using config path \"" + PATH2 + "\""
						);
			}
			// IF we found a node...
			if( null!=tmpElem )
			{
				statusMsg( kFName,
					"Examining Embedded Database Configuration in file \"" + inURI + "\""
					);
				try
				{
					// fConfigTree = new JDOMHelper( inURI );
					// use the one that handles includes
					fConfigTree = new JDOMHelper( tmpElem );

				}
				catch (JDOMHelperException e)
				{
					throw new DBConfigException( kExTag,
						"Got JDOMHelper Exception (2): "
						+ e );
				}
			}
			else
				throw new DBConfigException( kExTag,
					"Not a valid database or application configuration file."
					+ " File/URL = \"" + inURI + "\""
					+ " Unknown top level node is \"" + fConfigTree.getElementName() + "\""
					);
		}
		else {
			statusMsg( kFName,
				"Examining Stand-Alone Database Configuration file \"" + inURI + "\""
				);
		}


		// Do comoon init stuff, it will throw an exception if it
		// isn't happy
		finishInit();

	}

	// construct from an element, perhaps part of a larger tree
	// public DBConfig( Element inElement )
	public DBConfig( Element inElement, SearchTuningConfig optMainConfig )
		throws DBConfigException
	{
		final String kFName = "constructor(2)";
		final String kExTag = kClassName + ':' + kFName + ": ";

		// create jdom element and store info
		// Sanity checks
		if( inElement == null )
			throw new DBConfigException( kExTag,
				"Constructor was passed in a NULL element."
				);

		// Instantiate and store the main JDOMHelper a
		fConfigTree = null;
		try
		{
			fConfigTree = new JDOMHelper( inElement );
		}
		catch (JDOMHelperException e)
		{
			throw new DBConfigException( kExTag,
				"Got JDOMHelper Exception: "
				+ e );
		}

		fMainConfig = optMainConfig;

		// Do comoon init stuff, it will throw an exception if it
		// isn't happy
		finishInit();
	}

	public DBConfig(
			String inVendorTag,
			String inServer,
			String inPort,
			String inDB,
			String inUser,
			String inPwd
		)
			throws DBConfigException
	{
		this(
			argsToElement(
				inVendorTag,
				inServer,
				inPort,
				inDB,
				inUser,
				inPwd
				)
			, null
			);
	}
	static Element argsToElement(
		String inVendorTag,
		String inServer,
		String inPort,
		String inDB,
		String inUser,
		String inPwd
		)
	{
		final String kFName = "argsToElement";
		Element outElem = new Element( MAIN_ELEMENT_NAME );
		if( null!=inVendorTag )
			outElem.setAttribute( VENDOR_TAG_ATTR, inVendorTag );
		if( null!=inServer )
			outElem.setAttribute( SERVER_NAME_ATTR, inServer );
		if( null!=inPort )
			outElem.setAttribute( SERVER_PORT_ATTR, inPort );
		if( null!=inDB )
			outElem.setAttribute( DB_NAME_ATTR, inDB );
		if( null!=inUser )
			outElem.setAttribute( USERNAME_ATTR, inUser );
		if( null!=inPwd )
			outElem.setAttribute( PASSWORD_ATTR, inPwd );
statusMsg( kFName,
	"tmpElem=" + NIEUtil.NL
	+ JDOMHelper.JDOMToString( outElem, false )
	);
		return outElem;
	}

	// Second half of constructors
	private void finishInit()
		throws DBConfigException
	{
		final String kFName = "finishInit";
		final String kExTag = kClassName + ':' + kFName + ": ";

		if( fConfigTree == null )
			throw new DBConfigException( kExTag,
				"Got back a NULL xml tree when trying to create"
				+ " a Database Configuration object."
				);

		// Helper for logic about failure detection
		fDoingInit = true;

		// Force us to read all get methods once, and store results
		reinitFieldCache();

		// Check that a couple critical items exist
		String tmpStr;
		int tmpInt;

		// Must always have vendor tag
		tmpStr = getConfiguredVendorTag();
		if( tmpStr == null )
			throw new DBConfigException( kExTag,
				"Must specifiy the vendor tag"
				+ " (ex: " + VENDOR_TAG_ORACLE + ") in config."
				+ " This is true even if specifying a native connect string."
				);

		// Check DB settings
		// Will throw exception if it is VERY unhappy
		// Also can give some warnings
		// Think of this as elaborate "syntax" checking
		quickCheckDBSettings();

		// Check actual DB connection (if not in test mode)
		// Will also throw exception if it is unhappy
		// Think of this as the actual test drive
		try
		{
			connectionDBCheck();
		}
		// We should not be getting this exception here,
		// the db should be up during startup for us to work
		// Todo: unless we decide to revisit this
		catch( DBConfigInServerReconnectWait e )
		{
			throw new DBConfigException( kExTag,
				"Unable to connect to database at initialization time."
				+ " Returning init failure to application."
				+ " Error: " + e
				);
		}

		synchronized(fStateLock)
		{
			fIsWorking = true;
		}
		fDoingInit = false;
	}

	// Mostly "syntax" checking
	// This routine obsesses about things that they SPECIFICALLY set
	private void quickCheckDBSettings()
		throws DBConfigException
	{
		final String kFName = "quickCheckDBSettings";
		final String kExTag = kClassName + ':' + kFName + ": ";

		// A quick warning about username and password
		if( getUsername() == null || getPassword() == null )
			warningMsg( kFName,
				"No Database username and/or password specified in config file."
				+ " If you have trouble connecting you might want to check that."
				+ " Will continue with database connection setup."
				);

		// This should always give us a value
		String type = getDBType();
		if( type == null || ( !type.equals(TYPE_JDBC) && !type.equals(TYPE_ODBC) ) )
			throw new DBConfigException( kExTag,
				"Invalid type \"" + type + "\"."
				+ " Valid types: "
				+ TYPE_JDBC
				+ ", " + TYPE_ODBC
				+ " The attribute is " + DB_TYPE_ATTR
				);

		// A couple status flags
		// boolean isJDBC = getDBType().equals(TYPE_JDBC);
		// boolean isODBC = getDBType().equals(TYPE_ODBC);
		boolean isJDBC = isJDBC();
		boolean isODBC = isODBC();

		String vendor = getConfiguredVendorTag();

		// We require a vendor
		if( isJDBC && vendor == null )
			throw new DBConfigException( kExTag,
				"Must specifiy the database vendor tag."
				+ " In some cases, such as with Microsoft, this is actually"
				+ " the specific database product tag."
				+ " The attribute is " + VENDOR_TAG_ATTR
				);


		// If it's JDBC, there are only particular vendors we can handle
		if( isJDBC )
		{
			// If they did not set a driver, we require a recognized vendor
			if( getConfiguredDriverClassString() == null )
			{
				// If it's not a recognized vendor
				if( ! isJDBCDriver(vendor) )
				{
					// Maybe they accidently said odbc?
					if( type.equals( BOGUS_JDBC_VENDOR_ODBC ) )
					{
						throw new DBConfigException( kExTag,
							"You have mistakenly set the vendor tag to ODBC."
							+ " To use ODBC data sources, please set "
							+ DB_TYPE_ATTR + "=\"" + TYPE_ODBC + "\""
							+ " and then set " + VENDOR_TAG_ATTR + " to the type of ODBC"
							+ " source you want to access, for example \"excel\" or \"access\"."
							+ " With a type of ODBC and a vendor tag set, we will generate"
							+ " the appropriate connection string of jdbc:odbc:vendor...etc..."
							);
					}
					// Else generic exception
					else
					{
						throw new DBConfigException( kExTag,
							"Unsupported JDBC database vendor tag in config = \"" + vendor + "\"."
							+ " The attribute is " + VENDOR_TAG_ATTR
							+ " Currently our JDBC only supports: "
							+ listJDBCVendors()
							);
					}   // End else generic driver
				}   // End if not a JDBC dirver
			}   // End if driver class was null
		}   // End if type is JDBC
		// Todo: put checks in for ODBC

		// Get a bunch of flags set right up front to make logic
		// easier to follow, etc.
		boolean hasVendor = getConfiguredVendorTag() != null;
		boolean hasType = getConfiguredDBType() != null;
		boolean hasDBName = getDBName() != null;
		boolean hasServer = getConfiguredServerName() != null;
		boolean hasPort = getConfiguredPort() > 0;
		boolean hasParms = getExtraParameters() != null;

		// Did they use the short form
		boolean hasNativeURL = getConfiguredConnectionString() != null;

		// For the most part, if they specified this, then we're about done
		// checking, and it's their funeral if they got it wrong
		if( hasNativeURL )
		{
			statusMsg( kFName,
				"Found and will use optional native connection string"
				+ " \"" + getConfiguredConnectionString() + "\""
				+ " set with attribute " + NATIVE_CONNECT_STRING_ATTR
				+ " FYI: Use of this advanced attribute bypasses"
				+ " some of the other long-form"
				+ " attributes, and also bypasses some syntax error checking."
				);


			// a meta field for long form attributes
			boolean hasLongFormAttrs = hasType || hasServer || hasPort
				|| hasDBName || hasParms;

			if( hasLongFormAttrs )
			{
				errorMsg( kFName,
					"A native connection string is configured, but some other"
					+ " attributes have also been specified.  When a native connection"
					+ " stirng is set, many of the long form attributes are IGNORED."
					+ " The database configuration will continue despite this error message."
					+ " At least one of these values has been set and will be ignored:"
					+ " " + DB_TYPE_ATTR + "=\"" + getConfiguredDBType() + "\""
					+ ", " + DB_NAME_ATTR + "=\"" + getDBName() + "\""
					+ ", " + SERVER_NAME_ATTR + "=\"" + getConfiguredServerName() + "\""
					+ ", " + SERVER_PORT_ATTR + "=\"" + getConfiguredPort() + "\""
					+ ", " + EXTRA_PARAMETERS_ATTR + "=\"" + getExtraParameters() + "\""
					);
			}
			// We're done, we don't check any other syntax when native connect is set
			return;
		}


		// For most JDBC connections we do require a server
//		if( isJDBC && ! hasServer && ! vendor.equals(VENDOR_TAG_POSTGRESQL) )
//			throw new DBConfigException( kExTag,
//				"Must specifiy the database server name or IP address"
//				+ " for JDBC vendor " + vendor
//				);
		if( isJDBC && ! hasServer )
			warningMsg( kFName,
				"Did not specifiy the database server name or IP address"
				+ " for JDBC vendor " + vendor + "."
				+ " Most JDBC connections will default to local machine."
				);

		// For most JDBC connections we need a database name
		if( isJDBC && ! hasDBName && ! vendor.equals(VENDOR_TAG_MS_SQL) )
			throw new DBConfigException( kExTag,
				"Must specifiy the database name"
				+ " for JDBC vendor " + vendor
				);

		// For all ODBC connections we need a database name
		if( isODBC && ! hasDBName )
			throw new DBConfigException( kExTag,
				"Must specifiy the database or DSN name"
				+ " for ODBC connections."
				+ " Please set attribute " + DB_NAME_ATTR
				);

		// Check the port, this includes default values for quite a
		// few of the JDBC databases
		int port = getPort();
		if( isJDBC && port < 1 )
			throw new DBConfigException( kExTag,
				"Must specifiy the database port for the host machine for JDBC."
				+ " And it must be a positive integer."
				);

		if( isJDBC && hasParms )
			warningMsg( kFName,
				"Extra parameters are not supported for JDBC connections"
				+ "; currently only supported for ODBC connections."
				+ " Attribute " + EXTRA_PARAMETERS_ATTR
				+ " was set to \"" + getExtraParameters() + "\"."
				+ " This setting will be ignored."
				);

		// And the rest is handled by methods
		debugMsg( kFName, "Long form Syntax check finished." );

	}


	// Setup an actual connection, take it for a test drive
	private void connectionDBCheck()
		throws DBConfigException,
			DBConfigInServerReconnectWait
	{
		final String kFName = "connectionDBCheck";
		final String kExTag = kClassName + ':' + kFName + ": ";

		// We only peform the rest of the tests if the connection is active
		// which it normally is
		if( isActive() )
		{
			// Test the connection
			try
			{
				debugMsg( kFName, "About to do test connection." );
				Connection tmpConn = getConnection();
				debugMsg( kFName, "Back from test connection." );
			}
			catch (SQLException e)
			{
				throw new DBConfigException( kExTag,
					"Got an exception connecting to the database: " + e
					+ " Unable to construct a valid DB Config."
					);
			}

			// Should we do a test query
			if( shouldDoATestQuery() )
			{
				String testQry = getTestQuery();
		//		statusMsg( kFName,
		//			"Found test query \"" + testQry + "\"."
		//			);
				if( testQry != null )
				{
					statusMsg( kFName,
						"Running test query \"" + testQry + "\"."
						);
					if( isTestQueryASimpleCount() )
						infoMsg( kFName,
							"FYI: This is a count(*) style query"
							+ " so will return the actual count field."
							);
					// Setup special state so they can optionally display the first row
					fInTestMode = true;
					int numRows = testQueryNumRows( testQry, isTestQueryASimpleCount() );
					fInTestMode = false;
					// Negative number means an error
					if( numRows < 0 )
						throw new DBConfigException( kExTag,
							"Test Query Failed."
							+ " Query = \"" + testQry + "\"."
							+ " If you'd rather not run a test query, then please"
							+ " comment it out or remove it from your configuration."
							);
					// We should at least warn if there were zero
					if( numRows == 0 )
					{
						warningMsg( kFName,
							"The Test Query returned zero rows."
							+ " Query = \"" + testQry + "\"."
							+ " This may be normal for some applications."
							+ " If the query had an error, or the table didn't exist,"
							+ " you would be seeing a different, more serious message."
							+ " You are likely connected to the databse."
							+ " Query = \"" + testQry + "\"."
							+ " If you'd rather not run a test query, then please"
							+ " comment it out or remove it from your configuration."
							);
					}
					else    // Else there was at least one row
					{
						statusMsg( kFName,
							"Test query returned " + numRows + " row(s)."
							);
					}

				}
				else
				{
					debugMsg( kFName, "No test query was configured." );
				}
			}   // End if should do test query
		}
		else    // Not active
		{
			debugMsg( kFName,
				"Database has been deactivated, so no connection or query tests will be run."
				);
		}


	}

	// Force us to read all get methods once, and store results
	private void reinitFieldCache()
		throws DBConfigException
	{
		// Get everything in to the cache
		// First, specifically turn caching off (should be false anyway via Java init)
		mUseCache = false;
		// Now call each getter once, populates cached values
		isActive();
		getDBType();    // Will also call getConfigureedDBType()
		// getServerString();  // Will also call getConfiguredServerName()
		// getPort();  // Will also call getConfiguredPort()
		getConfiguredDriverClassString();
		// getNetworkProtocal();
		// getConfiguredVendorTag();
		_getVendorString();
		// getDBName();
		getMainTable();
		getUsername();
		getPassword();
		getExtraParameters();
		getDriverClassName();
		shouldDoATestQuery();
		isAutomaticTestQuery();
		doShowFirstRowFromTestQuery();
		getTestQuery();
		isTestQueryASimpleCount();
		// getConfiguredConnectionString();
		calculateConnectionString();
		doShowFirstRowFromTestQuery();
		getRetryWaitInterval();

		// we cache our transform
		if( null!=getMainConfigOrNull() )
			getCompiledXSLTDoc();

		getVendorAliasString();
		getVendorSysdateString();
		getVendorDoesNormalOuterJoin();
		getVendorNeedsCommitByDefault();
		getVendorNullValueMethodName();
		getVendorEpochCorrectionInMS();


		// Now turn caching on
		mUseCache = true;
	}

	private void showRow( ResultSet inResults )
	{
		final String kFName = "showRow";
		final String NL = "\r\n";
		// final int maxCount = 50;
		ResultSetMetaData meta = null;
		try
		{
			meta = inResults.getMetaData();
			int numCols = meta.getColumnCount();
			StringBuffer buff = new StringBuffer();
			buff.append( NL );
			for( int i=1; i<=numCols; i++ )
			{
				// if( i>1 )
				//	buff.append( ',' ).append( ' ' );
				// buff.append( NL );
				buff.append('[').append(i).append("] ");
				buff.append( meta.getColumnName(i) );
				String className = meta.getColumnClassName(i);
				String fieldType = meta.getColumnTypeName(i);
				buff.append(" (").append( className );
				buff.append('/').append( fieldType ).append(')');
				if( className!=null && className.equals("java.lang.String") )
					buff.append(" = \"").append( inResults.getString(i) ).append('"');
				else if( fieldType!=null && fieldType.equals("NUMBER") )
					buff.append(" = ").append( inResults.getBigDecimal(i) );
				else if( fieldType!=null && fieldType.equals("INTEGER") )
					buff.append(" = ").append( inResults.getInt(i) );
				else if( fieldType!=null && fieldType.startsWith("TIMESTAMP") )
					buff.append(" = '").append( inResults.getTimestamp(i) ).append('\'');
				else if( fieldType!=null && fieldType.startsWith("TIME") )
					buff.append(" = '").append( inResults.getTime(i) ).append('\'');
					// buff.append( '*' );
				buff.append( NL );
				// statusMsg( kFName, new String(buff) );
				// buff = new StringBuffer();
			}
			statusMsg( kFName, new String(buff) );
		}
		catch(SQLException sqle)
		{
			errorMsg( kFName,
				"Unable to get meta data to display row of data"
				+ " Exception=" + sqle
				);
		}
		finally {
			// quickie cleanup!
			// meta = closeResults( meta, kClassName, kFName, false );
			// No, can't close meta data results
		}
	}

	////////////////////////////////////////////////////////////////////
	private static void ___sep__Higher_Level_Logic__() {}

	public void closeAndClearAnyManualCommitConnections( Connection optConn ) {
		final String kFName = "closeAndClearAnyManualCommitConnections";
		if( null!=optConn ) {
			try {
				if( ! optConn.isClosed() )
					optConn.close();
			}
			catch( Exception e ) {
				errorMsg( kFName,
					"Error closing passed in connection " + optConn
					);
			}
		}
		if( null!=mDBConnectionManualCommit ) {
			try {
				if( ! mDBConnectionManualCommit.isClosed() )
					mDBConnectionManualCommit.close();
			}
			catch( Exception e ) {
				errorMsg( kFName,
					"Error closing member connection " + mDBConnectionManualCommit
					);
			}
			mDBConnectionManualCommit = null;
		}
	}


	// Convenience method for closing a connection
	// also returns null so can be used in assignment statement
	public static Connection closeConnection( Connection inConn,
		String optClassName, String optMethodName, boolean inDoWarnOnNull
	) {
		final String kFName = "closeConnection";
		boolean hasCallerInfo = null!=optClassName || null!=optMethodName;
		// Don't bother if null
		if( null==inConn ) {
			if( inDoWarnOnNull )
				errorMsg( kFName,
					( hasCallerInfo ? "(for " + optClassName + "." + optMethodName + "): " : "" )
					+ "Null Connection passed in, nothing to close."
					);
			return null;
		}
		// OK, try and close it!
		debugMsg( kFName,
			( hasCallerInfo ? "(for " + optClassName + "." + optMethodName + "): " : "" )
			+ "Closing."
			);
		try {
			if( ! inConn.isClosed() )
				inConn.close();
		}
		catch( Exception e ) {
			errorMsg( kFName,
				( hasCallerInfo ? "(for " + optClassName + "." + optMethodName + "): " : "" )
				+ "Got exception while closing Connection: " + e
				);
		}
		finally {
			inConn = null;	// who knows, maybe inspire garbage collection / finalize
		}
		// always return null, so that caller can call us in assignment statement
		return null;
	}


	// Convenience method for closing results sets
	// also returns null so can be used in assignment statement
	public static ResultSet closeResults( ResultSet inResults,
		String optClassName, String optMethodName, boolean inDoWarnOnNull
	) {
		final String kFName = "closeResults";
		boolean hasCallerInfo = null!=optClassName || null!=optMethodName;
		// Don't bother if null
		if( null==inResults ) {
			if( inDoWarnOnNull )
				errorMsg( kFName,
					( hasCallerInfo ? "(for " + optClassName + "." + optMethodName + "): " : "" )
					+ "Null Result Set passed in, nothing to close."
					);
			return null;
		}
		// OK, try and close it!
		debugMsg( kFName,
			( hasCallerInfo ? "(for " + optClassName + "." + optMethodName + "): " : "" )
			+ "Closing."
			);
		try {
			// if( ! inResults.isClosed() )
				inResults.close();
		}
		catch( Exception e ) {
			errorMsg( kFName,
				( hasCallerInfo ? "(for " + optClassName + "." + optMethodName + "): " : "" )
				+ "Got exception while closing Result Set: " + e
				);
		}
		finally {
			inResults = null;	// who knows, maybe inspire garbage collection / finalize
		}
		// always return null, so that caller can call us in assignment statement
		return null;
	}


	// Convenience method for closing statements
	// also returns null so can be used in assignment statement
	public static Statement closeStatement( Statement inStatement,
		String optClassName, String optMethodName, boolean inDoWarnOnNull
	) {
		final String kFName = "closeStatement(1)";
		boolean hasCallerInfo = null!=optClassName || null!=optMethodName;
		// Don't bother if null
		if( null==inStatement ) {
			if( inDoWarnOnNull )
				errorMsg( kFName,
					( hasCallerInfo ? "(for " + optClassName + "." + optMethodName + "): " : "" )
					+ "Null Statement passed in, nothing to close."
					);
			return null;
		}
		// OK, try and close it!
		debugMsg( kFName,
			( hasCallerInfo ? "(for " + optClassName + "." + optMethodName + "): " : "" )
			+ "Closing."
			);
		try {
			// if( ! inStatement.isClosed() )
				inStatement.close();
		}
		catch( Exception e ) {
			errorMsg( kFName,
				( hasCallerInfo ? "(for " + optClassName + "." + optMethodName + "): " : "" )
				+ "Got exception while closing Statement: " + e
				);
		}
		finally {
			inStatement = null;	// who knows, maybe inspire garbage collection / finalize
		}
		// always return null, so that caller can call us in assignment statement
		return null;
	}

	// 2nd version handles prepared statements
	public static PreparedStatement closeStatement( PreparedStatement inStatement,
		String optClassName, String optMethodName, boolean inDoWarnOnNull
	) {
		final String kFName = "closeStatement(2)";
		boolean hasCallerInfo = null!=optClassName || null!=optMethodName;
		// Don't bother if null
		if( null==inStatement ) {
			if( inDoWarnOnNull )
				errorMsg( kFName,
					( hasCallerInfo ? "(for of " + optClassName + "." + optMethodName + "): " : "" )
					+ "Null Prepared Statement passed in, nothing to close."
					);
			return null;
		}
		// OK, try and close it!
		debugMsg( kFName,
			( hasCallerInfo ? "(for " + optClassName + "." + optMethodName + "): " : "" )
			+ "Closing."
			);
		try {
			inStatement.close();
		}
		catch( Exception e ) {
			errorMsg( kFName,
				( hasCallerInfo ? "(for " + optClassName + "." + optMethodName + "): " : "" )
				+ "Got exception while closing Prepared Statement: " + e
				);
		}
		finally {
			inStatement = null;	// who knows, maybe inspire garbage collection / finalize
		}
		// always return null, so that caller can call us in assignment statement
		return null;
	}


	void registerDriverIfNeeded()
		throws DBConfigException
	{
		final String kFName = "registerDriverIfNeeded";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// If it hasn't been initialized yet
		if( ! mHasDriverBeenRegistered ) {

			String myClassName = null;
			try {
				// OLD: This caused compile problems and was inflexible
				//	DriverManager.registerDriver(
				//		new oracle.jdbc.OracleDriver()
				//		);
			
				// Get the name of the driver and check it
				// This will pick the default for this vendor, or allow for
				// an optional supplied driver in the config file
				myClassName = getDriverClassName();
				if( myClassName == null )
				{
					throw new DBConfigException( kExTag
						 + "Unknown driver for unknown vendor \"" + getConfiguredVendorTag() + "\""
						);
				}
			
				// Now get the class and driver instance
				Class myDriverClass = Class.forName( myClassName );
				Driver myDriverInstance = (Driver)myDriverClass.newInstance();
				// MS shows this as one step
				// Driver d = (Driver)Class.forName("com.microsoft.jdbc.sqlserver.SQLServerDriver")
				// (cont)  .newInstance();
				// And now register the driver
				DriverManager.registerDriver( myDriverInstance );
	
				mHasDriverBeenRegistered = true;

			}
			// There are 3 types of exceptions that the enclosed block
			// could have thrown
			catch(ClassNotFoundException cnf)
			{
				reportError();	// This also closes connections and sets nulls
				// stillBroken = true;

				throw new DBConfigException( kExTag
					+ "Problem loading database driver (1);"
					+ " perhaps check config file and class path."
					+ " Vendor = \"" + getConfiguredVendorTag() + "\""
					+ ", Driver Class = \"" + myClassName + "\""
					+ ", Error/Exception = \"" + cnf + "\""
					);
			}
			catch(InstantiationException inst)
			{
				reportError();	// This also closes connections and sets nulls
				// stillBroken = true;

				throw new DBConfigException( kExTag
					+ "Problem loading database driver (2):"
					+ " perhaps check config file and class path."
					+ " Vendor = \"" + getConfiguredVendorTag() + "\""
					+ ", Driver Class = \"" + myClassName + "\""
					+ ", Error/Exception = \"" + inst + "\""
					);
			}
			catch(IllegalAccessException ill)
			{
				reportError();	// This also closes connections and sets nulls
				// stillBroken = true;

				throw new DBConfigException( kExTag
					+ "Problem loading database driver (3);"
					+ " perhaps check config file and class path."
					+ " Vendor = \"" + getConfiguredVendorTag() + "\""
					+ ", Driver Class = \"" + myClassName + "\""
					+ ", Error/Exception = \"" + ill + "\""
					);
			}
			catch(SQLException eload)
			{
				reportError();	// This also closes connections and sets nulls

				throw new DBConfigException( kExTag
					+ "Problem loading database driver (4):"
					+ " perhaps check config file and class path."
					+ " Vendor = \"" + getConfiguredVendorTag() + "\""
					+ ", Driver Class = \"" + myClassName + "\""
					+ ", Error/Exception = \"" + eload + "\""
					);
			}

			mHasDriverBeenRegistered = true;

		}

	}




	public Connection getConnection()
		throws SQLException,
			DBConfigException,
			DBConfigInServerReconnectWait
	{
		return getConnection( true );
	}



	// Get or create a connection
	// This can also REconnect if the connect was marked as down
	// Most errors are SQLException
	// DBConfigException notes that the connection is currently down
	// and we did NOT try to reconnect, because we're waiting for
	// the retry interval to elapse
	public Connection getConnection( boolean inAutoCommit )
		throws SQLException,
			DBConfigException,
			DBConfigInServerReconnectWait
	{
		final String kFName = "getConnection";
		final String kExTag = kClassName + ':' + kFName + ": ";
		final boolean debug = shouldDoDebugMsg( kFName );

		// Is it even configured to be active?
		// boolean stillBroken = false;
		if( isActive() )
		{
			if(debug) debugMsg( kFName, "Is active." );

			// Remember this state as it appears now
			// We don't do this if we're initializing
			boolean lWasBroken = !fDoingInit && !fIsWorking;

			// If we've currently noted a problem
			if( lWasBroken )
			{
				if(debug) debugMsg( kFName, "Was marked as broken." );
				// Current time
				long curr = System.currentTimeMillis();
				if(debug) debugMsg( kFName, "curr time = " + curr );
				// How long since the last check
				long currInterval = fLastCheckedTime > 0 ? curr - fLastCheckedTime : 0;
				if(debug) debugMsg( kFName, "curr interval = " + currInterval );
				// How long we should really wait
				long maxWait = getRetryWaitInterval();
				if(debug) debugMsg( kFName, "max wait = " + maxWait );

				// Of if we DO NOT WANT retries
				if( maxWait <= DISABLE_RETRY )
				{
					if(debug) debugMsg( kFName, "Retries are DISABLED." );

					mDBConnection = DBConfig.closeConnection( mDBConnection, kClassName, kFName, false );
					mDBConnectionManualCommit = DBConfig.closeConnection( mDBConnectionManualCommit, kClassName, kFName, false );

					throw new DBConfigException( kExTag,
						"The database connection went down"
//						+ " and retries have been disabled."
//						+ " Setting was " + RETRY_WAIT_MS_ATTR
//						+ "=\"" + DISABLE_RETRY + "\"."
						);
				}

				// If it's NOT yet time to retry
				// Note: when an error is initially reported the last
				// check is set to ZERO, so we WILL retry right away on the
				// immediately subsequent connection, in case it's already
				// back up
				// Note: If we're told to NOT wait, then it IS always time to retry
				if( fLastCheckedTime > 0L && maxWait > 0L && currInterval < maxWait )
				{
					if(debug) debugMsg( kFName, "Not time for retry yet." );

					mDBConnection = DBConfig.closeConnection( mDBConnection, kClassName, kFName, false );
					mDBConnectionManualCommit = DBConfig.closeConnection( mDBConnectionManualCommit, kClassName, kFName, false );

					throw new DBConfigInServerReconnectWait( kExTag,
						"The database connection is currently down."
						+ " Have waited " + currInterval + " of " + maxWait
						+ " ms since last retry."
						);
				}
				else
					if(debug) debugMsg( kFName,
						"Apparently time to retry: "
						+ "fLastCheckedTime=" + fLastCheckedTime
						+ ", currInterval=" + currInterval
						+ ", maxWait=" + maxWait
						);


				// Remember when we checked it last
				fLastCheckedTime = curr;

				// Force the code below to recreate a connection by
				// nulling out whatever might be there
				mDBConnection = DBConfig.closeConnection( mDBConnection, kClassName, kFName, false );
				mDBConnectionManualCommit = DBConfig.closeConnection( mDBConnectionManualCommit, kClassName, kFName, false );
			}
			else	// Else was not marked as broken
				if(debug) debugMsg( kFName, "Was not marked as broken." );

			// At this point we may try to get a connection again
			// and it may be that it was currently marked as broken but
			// it's time to try again

			// Now we set about to potentially create a new connection
			Connection tmpConnection = null;
			String connectionStr = null;
			try
			{

				// If it's null we need to do something about it
				if( (inAutoCommit && (null==mDBConnection || mDBConnection.isClosed() ) )
						||
					(! inAutoCommit && (null==mDBConnectionManualCommit || mDBConnectionManualCommit.isClosed()) )
					)
				{
					if(debug) debugMsg( kFName, "Connection is Null or closed." );

					// Stuff that should only be done once
					registerDriverIfNeeded();

					// We need a connection string to get a connection
					connectionStr = calculateConnectionString();

					String msg = "Connecting to database with connection string \""
						+ connectionStr + "\""
						+ " as user \"" + getUsername() + "\"."
						;

					/***
					// Auto commits are our normal connection, which should
					// be infrequent, so do tell them
					if( inAutoCommit )
						statusMsg( kFName, msg );
					// Whereas EVERY database update asks for a manual
					// commit connection, so do not flood the log with those
					else
						infoMsg( kFName, msg );
					***/

					// We're still getting too many from normal activity, so
					// now only do once
					if( ! mHaveReportedDBConnection ) {
						statusMsg( kFName, msg );
						mHaveReportedDBConnection = true;
					}
					else {
						infoMsg( kFName, msg );
					}

					// And now get the connection
					tmpConnection = DriverManager.getConnection(
						connectionStr,
						getUsername(), getPassword()
						);
					tmpConnection.setAutoCommit( inAutoCommit );

					// Cache/Store it to the correct member field
					if( inAutoCommit )
						mDBConnection = tmpConnection;
					else
						mDBConnectionManualCommit = tmpConnection;

				}	// End if we need a connection
				else
					if(debug) debugMsg( kFName, "Connection was NOT null." );

				debugMsg( kFName,
					"New connection, auto-commit="
					+ mDBConnection.getAutoCommit()
					);

				// At this point we either have a connection or have
				// thrown an exception


			}   // End big try block
			// catch(DBConfigException dbce) <= let this just flow up
			catch(SQLException eload)
			{
				reportError();	// This also closes connections and sets nulls

				throw new DBConfigException( kExTag
					+ "Error creating database connection"
					+ " with connection string \""
					+ connectionStr + "\""
					+ " as user \"" + getUsername() + "\"."
					+ " and vendor = \"" + getConfiguredVendorTag() + "\""
					+ " Error/Exception: \"" + eload + "\""
					);
			}




			// If we're recovering from a broken connection, the constructor
			// and init stuff won't get a chance to run and mark the connection
			// as being up, so we should assume we're back up and mark it
			// here.  If we're wrong, it's OK, the caller will re-report an errror.
			if( lWasBroken /*&& ! stillBroken*/ )
			{
				synchronized(fStateLock)
				{
					if(debug) debugMsg( kFName, "Broken connection marked as working." );
					fIsWorking = true;
				}
			}

			// Return the answer
			if( inAutoCommit )
				return mDBConnection;
			else
				return mDBConnectionManualCommit;
		}
		else    // Else database is NOT active
		{
			String connectionStr = calculateConnectionString();
			String tmpMsg = "Database was DEACTIVATED in the configuration file."
				+ " Can not connect if database is not configured to be active."
				+ " Would have connected to the database with connection string \""
				+ connectionStr + "\""
				+ " as user \"" + getUsername() + "\"."
				;
			throw new SQLException( kExTag + tmpMsg );
		}
	}
	public Connection getConnectionOrNull()
	{
		final String kFName = "getConnectionOrNull";

		Connection answer = null;
		try
		{
			answer = getConnection();
		}
		catch (Exception e)
		{
			errorMsg( kFName,
				"Got an exception connecting to the database: " + e
				+ " Will return null."
				);
			answer = DBConfig.closeConnection( answer, kClassName, kFName, false );
			return null;
		}
		return answer;
	}

	// public Statement createStatement()
	// returns [ newStatement, connectionUsedToCreateStatement ]
	public Object [] createStatement()
		throws SQLException,
			DBConfigException,
			DBConfigInServerReconnectWait
	{
		final String kFName = "createStatement";
		final String kExTag = kClassName + '.' + kFName + ": ";
		Connection myConnection = getConnection();
		// ^^^ will bail and close any connection if there's a problem
		if( null==myConnection )
			throw new DBConfigException( kExTag +
				"Got Null connection."
				);
		if( myConnection.isClosed() ) {
			throw new DBConfigException( kExTag +
				"System gave me a closed connection."
				);
			// TODO: Could report this as an error...
			// but worried about contention loops, and system should still do
			// the right thing
			// reportError();
		}

		Statement myStatement = null;
		try {
			myStatement = myConnection.createStatement();
		}
		catch( SQLException e ) {
			myStatement = DBConfig.closeStatement( myStatement, kFName, kExTag, false );
			// I have to also close the connection because the
			// caller will have no way to do it
			myConnection = DBConfig.closeConnection( myConnection, kFName, kExTag, false );
			// Also, this is a hard failur, since there was no actual SQL
			// involved, there must be something pretty serious going on
			reportError();
			throw new SQLException( kExTag +
				"Got exception creating statement: " + e
				);
		}

		Object outAry[] = { myStatement, myConnection };
		return outAry;
	}


	public Object [] createStatementOrNull()
	// public Statement createStatementOrNull()
	{
		final String kFName = "createStatementOrNull";

		// Statement answer = null;
		Object [] answer = null;
		try
		{
			answer = createStatement();
		}
		catch (Exception e)
		{
			errorMsg( kFName,
				"Got an exception creating a new statement: \"" + e + "\""
				+ " Will return null."
				);
			return null;
		}
		return answer;
	}

	// public Statement createStatementWithConnection( Connection inConn )
	public Object [] createStatementWithConnection( Connection inConn )
		throws SQLException,
			DBConfigException
			//, DBConfigInServerReconnectWait
	{
		final String kFName = "createStatementWithConnection";
		final String kExTag = kClassName + '.' + kFName + ": ";
		if( null==inConn )
			throw new DBConfigException( kExTag +
				"Null connection passed in."
				);
		if( inConn.isClosed() ) {
			throw new DBConfigException( kExTag +
				"Was passed in a closed connection."
				);
			// TODO: Could report this as an error...
			// but worried about contention loops, and system should still do
			// the right thing
			// reportError();
		}

		Statement myStatement = null;
		try {
			myStatement = inConn.createStatement();
		}
		catch( SQLException sqe ) {
			// attempt to close our statement, it's probably null anyway
			myStatement = DBConfig.closeStatement( myStatement, kFName, kExTag, false );
			// I do NOT close the connection, that's the callers job,
			// they have it since they told us what it is
			// ^^^ No, on second thought, no harm in closing it,
			// and we're about to call reportError(), which will also zap all
			// cached connections
			// It's odd, but "sql" errors can also mean connectivity errors, and in
			// a running app, there should be no syntax-caused errors, so presumably
			// ALL are of a connection failure nature
			inConn = DBConfig.closeConnection( inConn, kFName, kExTag, false );
			reportError();
			throw new SQLException( kExTag + "Got SQL Exception: " + sqe );
		}

		return new Object [] { myStatement, inConn };
	}


	// public PreparedStatement createPreparedStatement( String inSQL )
	public Object [] createPreparedStatement( String inSQL )
		throws SQLException,
			DBConfigException,
			DBConfigInServerReconnectWait
	{
		return createPreparedStatement( inSQL, null );
	}

	// For performance reasons, we allow you to pass us a prepared connection
	// public PreparedStatement createPreparedStatement(
	public Object [] createPreparedStatement(
			String inSQL,
			Connection optUseThisConnection
		)
		throws SQLException,
			DBConfigException,
			DBConfigInServerReconnectWait
	{
		final String kFName = "createPreparedStatement";
		final String kExTag = kClassName + '.' + kFName + ": ";
		boolean isLocalConnection = false;
		Connection myConnection = optUseThisConnection;
		if( null == myConnection ) {
			myConnection = getConnection();
			// ^^^ will bail and close any connection if there's a problem

			// Be paranoid
			if( null==myConnection )
				throw new DBConfigException( kExTag +
					"System gave me a null connection."
					);
			if( myConnection.isClosed() ) {
				throw new DBConfigException( kExTag +
					"System gave me a closed connection."
					);
				// TODO: Could report this as an error...
				// but worried about contention loops, and system should still do
				// the right thing
				// reportError();
			}

			isLocalConnection = true;
		}
		PreparedStatement myStatement = null;
		// Need to do a better job of setting us up for a retry
		try {
			myStatement = myConnection.prepareStatement( inSQL );
		}
		catch( SQLException e ) {
			myStatement = DBConfig.closeStatement( myStatement, kClassName, kFName, false );
			myConnection = DBConfig.closeConnection( myConnection, kClassName, kFName, false );
			reportError();
			throw e;
		}
		// return myStatement;
		return new Object [] { myStatement, myConnection };
	}
	// public PreparedStatement createPreparedStatementOrNull( String inSQL )
	public Object [] createPreparedStatementOrNull( String inSQL )
	{
		final String kFName = "prepareStatementOrNull";

		// PreparedStatement answer = null;
		Object [] answer = null;
		try
		{
			answer = createPreparedStatement( inSQL );
		}
		catch (Exception e)
		{
			errorMsg( kFName,
				"Got an exception creating a new prepared statement: \"" + e + "\""
				+ " Will return null."
				);
			return null;
		}
		return answer;
	}


	// public ResultSet runQuery( String inQuery )
	// Returns Object [ Results, Statement, Connection ]
	// Will close everything if there's a problem
	// Ideally, when we return, we'd like to have already automatically
	// closed the statement and connection, but that would break
	// their retrieval of results
	// If there's no problem, we return all 3, but the caller
	// is RESPONSIBLE for closing that statement and connection
	// If there's problem, we will close everything we have
	// If there's a potential connectivity failure, we will also
	// do a reportError()
	public Object [] runQuery(
			String inQuery,
			boolean inEscalateSQLErrors
		)
		throws SQLException,
			DBConfigException,
			DBConfigInServerReconnectWait
	{
		final String kFName = "runQuery(1)";
		final String kExTag = kClassName + ':' + kFName + ": ";

		// create jdom element and store info
		// Sanity checks
		if( inQuery == null )
			throw new SQLException( kExTag,
				"Was passed in a NULL Query."
				);
//			throw new DBConfigException( kExTag,
//				"Was passed in a NULL Query."
//				);

		// Connection myConnection = getConnection();
		// Statement myStatement = myConnection.createStatement();
		// Use our built in method
		// Statement myStatement = createStatement();
		Object [] myStateAndConnAry = createStatement();
		if( null == myStateAndConnAry || myStateAndConnAry.length < 1 )
			throw new DBConfigException( kExTag,
				"System gave me a null/empty object array while trying to create a statement."
				);
		// So we have at least a one element item
		Statement myStatement = (Statement)myStateAndConnAry[0];
		if( myStateAndConnAry.length < 2 ) {
			myStatement = DBConfig.closeStatement(myStatement, kClassName, kFName, false );
			throw new DBConfigException( kExTag,
				"System gave me truncated object array while trying to create a statement."
				);
		}
		Connection myConnection = (Connection)myStateAndConnAry[1];

		// Sanity check for nulls
		if( null==myStatement || null==myConnection ) {
			String msg = " myConnection=" + myConnection + ", myStatement=" + myStatement;
			myStatement = DBConfig.closeStatement( myStatement, kClassName, kFName, false );
			myConnection = DBConfig.closeConnection( myConnection, kClassName, kFName, false );
			throw new DBConfigException( kExTag,
				"System gave me object array with null(s) while trying to create a statement."
				+ msg
				);
		}
		// Sanity check for closed objects
		if( myConnection.isClosed() ) {
			myStatement = DBConfig.closeStatement( myStatement, kClassName, kFName, false );
			// a bit redundant, but what the heck!
			myConnection = DBConfig.closeConnection( myConnection, kClassName, kFName, false );
			throw new DBConfigException( kExTag,
				"System gave me a closed connection while trying to create a statement."
				);
		}

		// maybe a shortcut for simple counts, but won't compile
		// int tmpCount = myStatement.executeUpdate();
		// statusMsg( kFName, "tmpCount=" + tmpCount );

		ResultSet myResultSet = null;

		try {
			if( inQuery.toLowerCase().startsWith( "select" ) )
				myResultSet = myStatement.executeQuery( inQuery );
			else
				myStatement.executeUpdate( inQuery );
		}
		catch( SQLException se ) {
			myResultSet = DBConfig.closeResults( myResultSet, kClassName, kFName, false );
			myStatement = DBConfig.closeStatement( myStatement, kClassName, kFName, false );
			if( inEscalateSQLErrors ) {
				myConnection = DBConfig.closeConnection( myConnection, kClassName, kFName, false );
				reportError();
			}
			throw se;
		}

		// return myResultSet;
		return new Object [] { myResultSet,  myStatement, myConnection };
	}


	// public ResultSet runQueryWithConnection( String inQuery, Connection inConn )
	// TODO: the calling order of this one and the other one wihout
	// the connection should be switched around maybe...
	public Object [] runQueryWithConnection(
			String inQuery,
			Connection inConn,
			boolean inEscalateSQLErrors
		)
		throws SQLException,
			DBConfigException,
			DBConfigInServerReconnectWait
	{
		final String kFName = "runQueryWithConnection";
		final String kExTag = kClassName + ':' + kFName + ": ";

		// create jdom element and store info
		// Sanity checks
		if( inQuery == null )
			throw new SQLException( kExTag,
				"Was passed in a NULL Query."
				);
//			throw new DBConfigException( kExTag,
//				"Was passed in a NULL Query."
//				);

		// Sanity check for nulls
		if( null==inConn )
			throw new DBConfigException( kExTag, "Was passed in a NULL connection." );

		// Sanity check for closed objects
		if( inConn.isClosed() ) {
			// a bit redundant, but what the heck!
			inConn = DBConfig.closeConnection( inConn, kClassName, kFName, false );
			throw new DBConfigException( kExTag,
				"Was passed in a closed connection while trying to create a statement."
				);
		}


		// Statement myStatement = createStatementWithConnection( inConn );
		Object [] myStateAndConnAry = createStatementWithConnection( inConn );
		// TODO: we're going to rely on them to close our connection if there's
		// an issue, so we don't have to catch it here
		// createStatementWithConnection() is also dutifully paranoid

		// Sanity check, though this should not be possible
		if( null == myStateAndConnAry || myStateAndConnAry.length < 1 )
			throw new DBConfigException( kExTag,
				"System gave me a null/empty object array while trying to create a statement."
				);
		// So we have at least a one element item
		Statement myStatement = (Statement)myStateAndConnAry[0];
		if( myStateAndConnAry.length < 2 ) {
			myStatement = DBConfig.closeStatement(myStatement, kClassName, kFName, false );
			throw new DBConfigException( kExTag,
				"System gave me truncated object array while trying to create a statement."
				);
		}
		// We STILL sanity check the connection we got back
		Connection myConnection = (Connection)myStateAndConnAry[1];

		// Sanity check for nulls
		if( null==myStatement || null==myConnection ) {
			String msg = " myConnection=" + myConnection + ", myStatement=" + myStatement;
			myStatement = DBConfig.closeStatement( myStatement, kClassName, kFName, false );
			myConnection = DBConfig.closeConnection( myConnection, kClassName, kFName, false );
			throw new DBConfigException( kExTag,
				"System gave me object array with null(s) while trying to create a statement."
				+ msg
				);
		}
		// Sanity check for closed objects
		if( myConnection.isClosed() ) {
			myStatement = DBConfig.closeStatement( myStatement, kClassName, kFName, false );
			// a bit redundant, but what the heck!
			myConnection = DBConfig.closeConnection( myConnection, kClassName, kFName, false );
			throw new DBConfigException( kExTag,
				"System gave me a closed connection while trying to create a statement."
				);
		}

		// And a final check, it should be what we gave it before
		// it would be really weird if it wasn't the same... seems impossible,
		// but no harm checking anyway
		if( myConnection != inConn ) {
			myStatement = DBConfig.closeStatement( myStatement, kClassName, kFName, false );
			myConnection = DBConfig.closeConnection( myConnection, kClassName, kFName, false );
			inConn = DBConfig.closeConnection( inConn, kClassName, kFName, false );
			throw new DBConfigException( kExTag,
				"System gave back a different connection from what we asked it to use when trying to create a statement."
				);
		}

		ResultSet myResultSet = null;

		try {
			if( inQuery.toLowerCase().startsWith( "select" ) )
				myResultSet = myStatement.executeQuery( inQuery );
			else
				myStatement.executeUpdate( inQuery );
		}
		catch( SQLException se ) {
			myResultSet = DBConfig.closeResults( myResultSet, kClassName, kFName, false );
			myStatement = DBConfig.closeStatement( myStatement, kClassName, kFName, false );
			if( inEscalateSQLErrors ) {
				myConnection = DBConfig.closeConnection( myConnection, kClassName, kFName, false );
				reportError();
			}
			throw se;
		}

		// return myResultSet;
		return new Object [] { myResultSet,  myStatement, myConnection };
	}
	// Does not throw an exception
	// public ResultSet runQueryOrNull( String inQuery )
	public Object [] runQueryOrNull(
		String inQuery,
		boolean inEscalateSQLErrors
		)
	{
		return runQueryOrNull( inQuery, inEscalateSQLErrors, true );
	}
	// public ResultSet runQueryOrNull( String inQuery,
	public Object [] runQueryOrNull(
			String inQuery,
			boolean inEscalateSQLErrors,
			boolean inDoDisplayErrors
		)
	{
		final String kFName = "runQueryOrNull";

		// ResultSet answer = null;
		Object [] answer = null;
		try
		{
			answer = runQuery( inQuery, inEscalateSQLErrors );
		}
		catch (Exception e)
		{
			if( inDoDisplayErrors )
				errorMsg( kFName,
					"Got an exception running the query \""
					+ inQuery + "\"."
					+ " Exception: " + e
					+ " Will return null."
					);
			return null;
		}
		return answer;
	}

	public int testQueryNumRows(
			String inQuery,
			boolean inEscalateSQLErrors
		)
	{
		return testQueryNumRows( inQuery, false, inEscalateSQLErrors );
	}
	public int testQueryNumRows(
			String inQuery,
			boolean inIsCountQuery,
			boolean inEscalateSQLErrors
		)
	{
		return testQueryNumRows( inQuery, inIsCountQuery, inEscalateSQLErrors, true );
	}


	public int testQueryNumRows(
		String inQuery,
		boolean inIsCountQuery,
		boolean inEscalateSQLErrors,
		boolean inDoDisplayErrors
		)
	{
		final String kFName = "testQueryNumRows";

		// int tmpCount =

		// ResultSet theResults = runQueryOrNull( inQuery, inDoDisplayErrors );
		Object [] parts = runQueryOrNull( inQuery, inEscalateSQLErrors, inDoDisplayErrors );

		if( null==parts || parts.length != 3 ) {
			// If you know you're querying against a table that might
			// not exisit, we will have got a null object array back
			// at this point
			if( inDoDisplayErrors )
				errorMsg( kFName,
					"Invalid results object array for test query; returning -1."
					+ " parts=" + parts
					);
			return -1;
		}

		ResultSet theResults = (ResultSet)parts[0];
		// We will still need to close these as well
		Statement theStatement = (Statement)parts[1];
		Connection theConnection = (Connection)parts[2];

		if( theResults == null )
		{
			debugMsg( kFName, "Results were null." );

			if( inDoDisplayErrors )
				errorMsg( kFName,
					"Got back a null Result Set, returning -1."
					);
			theStatement = closeStatement( theStatement, kClassName, kFName, false );
			theConnection = closeConnection( theConnection, kClassName, kFName, false );

			return -1;
		}

		if( inIsCountQuery )
			debugMsg( kFName,
				"Told that this is a count(*) style test query"
				+ " so will return first field of first row as integer"
				+ " vs doing the normal row counting."
				);


		int lRowCount = 0;
		boolean doBreak = false;
		try
		{
			// while( ! oracleResultSet.isAfterLast() )
			while( theResults.next() )
			{
				lRowCount++;
//				String field = oracleResultSet.getString( 1 );
//				statusMsg( kFName,
//					"Field was \"" + field + "\""
//					);

				if( inIsCountQuery )
				{
					lRowCount = theResults.getInt(1);
					doBreak = true;
				}

				if( lRowCount == 1 && fInTestMode && doShowFirstRowFromTestQuery() )
					showRow( theResults );

				if( doBreak )
					break;

			};
		}
		catch (SQLException e)
		{
			if( inDoDisplayErrors )
				errorMsg( kFName,
					"Error while reading through records: " + e
					+ " Returning -1."
					);
			if( inEscalateSQLErrors )
				reportError();
			return -1;
		}
		finally {
			// quickie cleanup!
			theResults = closeResults( theResults, kClassName, kFName, false );
			theStatement = closeStatement( theStatement, kClassName, kFName, false );
			// theConnection = closeConnection( theConnection, kClassName, kFName, false );
		}

		debugMsg( kFName,
			"We fetched " + lRowCount + " rows."
			);

		return lRowCount;
//		System.out.println( "SQL executed was:" );
//		System.out.println( kSQLStatement );
//		if( null != oracleConnection )
//			System.out.println( "Success... " + oracleConnection );
//		else
//			System.out.println( "Failed." );


	}


	public int simpleCountQuery(
			String inQuery,
			boolean inEscalateSQLErrors,
			boolean inDoDisplayErrors
		)
	{
		return testQueryNumRows(
			inQuery, true, inEscalateSQLErrors, inDoDisplayErrors
			);
	}

	public java.sql.Timestamp simpleDateQuery(
			String inQuery,
			boolean inEscalateSQLErrors,
			boolean inDoDisplayErrors
		)
	{
		final String kFName = "simpleDateQuery";

		// int tmpCount =

		// ResultSet theResults = runQueryOrNull( inQuery, inDoDisplayErrors );
		Object [] parts = runQueryOrNull( inQuery, inEscalateSQLErrors, inDoDisplayErrors );

		if( null==parts || parts.length != 3 ) {
			// If you know you're querying against a table that might
			// not exisit, we will have got a null object array back
			// at this point
			if( inDoDisplayErrors )
				errorMsg( kFName,
					"Invalid results object array for test query; returning -1."
					+ " parts=" + parts
					);
			return null;
		}

		ResultSet theResults = (ResultSet)parts[0];
		// We will still need to close these as well
		Statement theStatement = (Statement)parts[1];
		Connection theConnection = (Connection)parts[2];

		if( theResults == null )
		{
			debugMsg( kFName, "Results were null." );

			if( inDoDisplayErrors )
				errorMsg( kFName,
					"Got back a null Result Set, returning -1."
					);
			theStatement = closeStatement( theStatement, kClassName, kFName, false );
			theConnection = closeConnection( theConnection, kClassName, kFName, false );

			return null;
		}

		java.sql.Timestamp answer = null;
		boolean doBreak = false;
		try
		{
			// while( ! oracleResultSet.isAfterLast() )
			while( theResults.next() )
			{

				// answer = theResults.getDate(1);
				answer = theResults.getTimestamp(1);
				doBreak = true;

				if( fInTestMode && doShowFirstRowFromTestQuery() )
					showRow( theResults );

				if( doBreak )
					break;

			};
		}
		catch (SQLException e)
		{
			if( inDoDisplayErrors )
				errorMsg( kFName,
					"Error while reading through records: " + e
					+ " Returning -1."
					);
			if( inEscalateSQLErrors )
				reportError();
			return null;
		}
		finally {
			// quickie cleanup!
			theResults = closeResults( theResults, kClassName, kFName, false );
			theStatement = closeStatement( theStatement, kClassName, kFName, false );
			// theConnection = closeConnection( theConnection, kClassName, kFName, false );
		}

		debugMsg( kFName,
			"We fetched " + answer + " rows."
			);

		return answer;


	}


	public boolean deleteAllRecordsFromTable( String inTableName )
	{
		final String kFName = "deleteAllTableData";

		/***
		boolean hasTable = verifyASpecificDBTable( inTableName, true, false );
		if( ! hasTable ) {
			errorMsg( kFName, "Database does not have table \"" + inTableName + "\"; nothing to do." );
			return false;
		}
		***/

		inTableName = NIEUtil.trimmedStringOrNull( inTableName );
		if( null==inTableName ) {
			errorMsg( kFName, "Null/empty table name passed in; nothing to do." );
			return false;
		}
		String sql = "DELETE FROM " + inTableName;

		try {
			executeStatement( sql, true );
		}
		catch( Exception e ) {
			errorMsg( kFName, "Error deleting records: " + e );
			return false;
		}
		return true;
	}

	public boolean executeStatement(
			String inQuery,
			boolean inEscalateSQLErrors
		)
		throws SQLException,
			DBConfigException,
			DBConfigInServerReconnectWait
	{
		return executeStatementWithConnection(
			inQuery, null, inEscalateSQLErrors
			);
	}

	public boolean executeStatementWithConnection(
			String inQuery,
			Connection optConn,
			boolean inEscalateSQLErrors
		)
		throws SQLException,
			DBConfigException,
			DBConfigInServerReconnectWait
	{
		final String kFName = "executeStatement";
		final String kExTag = kClassName + ':' + kFName + ": ";

		// create jdom element and store info
		// Sanity checks
		if( inQuery == null )
			throw new SQLException( kExTag,
				"Was passed in a NULL Query."
				);
//			throw new DBConfigException( kExTag,
//				"Was passed in a NULL Query."
//				);

		Connection myConnection = null;
		Statement myStatement = null;
		if( null!=optConn ) {
			if( optConn.isClosed() ) {
				throw new DBConfigException( kExTag +
					"Was passed in a closed connection."
					);
				// TODO: Could report this as an error...
				// but worried about contention loops, and system should still do
				// the right thing
				// reportError();
			}

			myConnection = optConn;
			try {
				myStatement = optConn.createStatement();
			}
			catch( SQLException sqe ) {
				// attempt to close our statement, it's probably null anyway
				myStatement = DBConfig.closeStatement( myStatement, kFName, kExTag, false );
				// I do NOT close the connection, that's the callers job,
				// they have it since they told us what it is
				// ^^^ No, on second thought, no harm in closing it,
				// and we're about to call reportError(), which will also zap all
				// cached connections
				// It's odd, but "sql" errors can also mean connectivity errors, and in
				// a running app, there should be no syntax-caused errors, so presumably
				// ALL are of a connection failure nature
				optConn = DBConfig.closeConnection( optConn, kFName, kExTag, false );
				// if( inEscalateSQLErrors )
				// Since no query involved, we always report an error here
				reportError();
				throw new SQLException( kExTag + "Got SQL Exception(1): " + sqe );
			}
		}
		// Else no connection passed in
		else {
			// Connection myConnection = getConnection();
			// Statement myStatement = myConnection.createStatement();
			// Use our built in method
			// Statement myStatement = createStatement();

			// And with all these below, any "SQL" exception will
			// be correctly treated as an escalated exception

			Object [] myStateAndConnAry = createStatement();
			if( null == myStateAndConnAry || myStateAndConnAry.length < 1 )
				throw new DBConfigException( kExTag,
					"System gave me a null/empty object array while trying to create a statement."
					);
			// So we have at least a one element item
			myStatement = (Statement)myStateAndConnAry[0];
			if( myStateAndConnAry.length < 2 ) {
				myStatement = DBConfig.closeStatement(myStatement, kClassName, kFName, false );
				throw new DBConfigException( kExTag,
					"System gave me truncated object array while trying to create a statement."
					);
			}
			myConnection = (Connection)myStateAndConnAry[1];

			// Sanity check for nulls
			if( null==myStatement || null==myConnection ) {
				String msg = " myConnection=" + myConnection + ", myStatement=" + myStatement;
				myStatement = DBConfig.closeStatement( myStatement, kClassName, kFName, false );
				myConnection = DBConfig.closeConnection( myConnection, kClassName, kFName, false );
				throw new DBConfigException( kExTag,
					"System gave me object array with null(s) while trying to create a statement."
					+ msg
					);
			}
			// Sanity check for closed objects
			if( myConnection.isClosed() ) {
				myStatement = DBConfig.closeStatement( myStatement, kClassName, kFName, false );
				// a bit redundant, but what the heck!
				myConnection = DBConfig.closeConnection( myConnection, kClassName, kFName, false );
				throw new DBConfigException( kExTag,
					"System gave me a closed connection while trying to create a statement."
					);
			}
		}

		// OK, so now we have a statement and connection

		// maybe a shortcut for simple counts, but won't compile
		// int tmpCount = myStatement.executeUpdate();
		// statusMsg( kFName, "tmpCount=" + tmpCount );

		boolean answer = false;

		try {
			answer = myStatement.execute( inQuery );
		}
		catch( SQLException e ) {
			// We only close the connection if there was a problem
			if( inEscalateSQLErrors ) {
				myConnection = closeConnection( myConnection, kClassName, kFName, false );
				reportError();
			}
			throw e;
		}
		finally {
			myStatement = closeStatement( myStatement, kClassName, kFName, false );
			// Close the connection if we opened it
			/*** No, why should we?
			if( null!=optConn ) {
				myConnection = closeConnection( myConnection, kClassName, kFName, false );
				optConn = null;
			}
			***/
		}

		return answer;
	}

	// Does not throw an exception
	public boolean executeStatementOrFalse(
		String inQuery,
		boolean inEscalateSQLErrors
		)
	{
		return executeStatementOrFalse( inQuery, inEscalateSQLErrors, true );
	}

	public boolean executeStatementOrFalse(
		String inQuery,
		boolean inEscalateSQLErrors,
		boolean inDoDisplayErrors
		)
	{
		final String kFName = "executeStatementOfFalse";

		boolean answer = false;
		try
		{
			answer = executeStatement( inQuery, inEscalateSQLErrors );
		}
		catch (Exception e)
		{
			if( inDoDisplayErrors )
				errorMsg( kFName,
					"Got an exception executing the statement \""
					+ inQuery + "\"."
					+ " Exception: " + e
					+ " Will return null."
					);
			return false;
		}
		return answer;
	}






	// return TRUE if everything went OK
	// Does not escalate sql errors
	public boolean runXmlSqlStatements( Element inStatements )
	{
		final String kFName = "runXmlSqlStatements";
		if( null==inStatements )
		{
			errorMsg( kFName, "Null input, nothing to do" );
			return false;
		}

		List statements = JDOMHelper.findElementsByPath(
			inStatements, "/statements/statement"
			);
		if( null==statements || statements.size() < 1 )
		{
			errorMsg( kFName, "No statements found to run." );
			return false;
		}
		boolean outEverythingOK = true;
		for( Iterator it = statements.iterator(); it.hasNext(); )
		{
			Element stmtElem = (Element)it.next();
			String stmtStr = stmtElem.getText();
			stmtStr = NIEUtil.trimmedStringOrNull( stmtStr );
			boolean ignoreErrors = JDOMHelper.getBooleanFromAttribute(
				stmtElem, "ignore_error", false
				);
			// Was the statement null?
			if( null==stmtStr )
			{
				if( ! ignoreErrors )
				{
					errorMsg( kFName,
						"Null/empty SQL statement, abandoning commands."
						);
					outEverythingOK = false;
					break;
				}
				else
				{
					continue;
				}
			}
			// OK, we have a statement, execute it!
			boolean result = false;
			try
			{
				// Statement myStatement = createStatement();
				// result = myStatement.execute( stmtStr );
				result = executeStatement( stmtStr, false );
				// result is not really that interesting after all
			}
			catch( Exception sqle )
			{
				if( ! ignoreErrors )
				{
					errorMsg( kFName,
						"Error excuting SQL statement"
						+ "\"" + stmtStr + "\"."
						+ " Error: \"" + sqle + "\"."
						+ " Abandoning commands."
						);
					outEverythingOK = false;
					break;
				}
				else
				{
					continue;
				}
			}		
		}

		return outEverythingOK;
	}


	///// Higher Level Logic ////////////////////////////////////////////
	// Still uses caching
	public String calculateConnectionString()
	{
		final String kFName = "calculateConnectionString";

		if( ! mUseCache )
		{
			// Did they give us a native one?
			cFinalConnectionString = getConfiguredConnectionString();
			// If not, calculate it with the components
			if( cFinalConnectionString == null )
			{
				// We want something that looks like:
				// jdbc:oracle:thin:@mchine:nnnn:dbname
				// default port for Oracle is 1521
				//
				// For PostgreSQL
				// jdbc:postgresql://host:port/db
				// jdbc:postgresql://host/db (uses host:5432)
				// jdbc:postgresql:db (uses localhost:5432)
				// jdbc:postgresql:SearchTrack
				// default port for Postgres is 5432
				//
				// For SQL server, I've also seen connect strings like:
				// jdbc:microsoft:sqlserver://localhost:8080;DataBaseName ='MyDataBase;User=sa;passowrd= (empty)
				// jdbc:microsoft:sqlserver://server_name:port
				// jdbc:microsoft:sqlserver://server_name:port;Databasename
				// jdbc:sqlserver://localhost:1433;databaseName=test
				// default port for MS is 1433
				//
				// jdbc:mysql://9999.9.9.99:3306/mydb
				// jdbc:mysql://localhost/dbname
				//
				// For ODBC, see ODBC section

				// final String lType = getDBType();
				final String lVendor = getConfiguredVendorTag();
				// The answer we will create
				StringBuffer buff = new StringBuffer();

				// Some markers we will use
				// final boolean isJDBC = isJDBCDriver( lVendor );
				// final boolean isODBC = isODBCDriver( lVendor );
				boolean isJDBC = isJDBC();
				boolean isODBC = isODBC();

				// And the DEFAULT class name
				String defaultDriverClassName = null;

				// Note on meaning of "driver class"
				// "driver class" for JDBC means a real java class name
				// "driver class" for ODBC means gunk we put in the string;
				// for ODBC, we always use the same JDBC-ODBC gateway

				// WARNING: This assumes default type is JDBC
				// If you change that, change the order here
//				if( isJDBC )
//				{
//					defaultDriverClassName =
//						getJDBCDriverNameForVendor( lVendor )
//						;
//				}
				/*else*/ if( isODBC )
				{
					defaultDriverClassName =
						expandOdbcVendorTagToDriverName( lVendor )
						;
				}
				// At this point lVendor and defaultDriverClassName
				// should be set

				// Defaults are handled by these
				int port = getPort();
				String server = getServerString();

				String dbName = getDBName();

				// Is it JDBC?
				if( isJDBC )
				{
					// This shouldn't happen, but checking beats
					// a null pointer exception later
					if( lVendor == null )
					{
						errorMsg( kFName,
							"No vendor for JDBC, returning null."
							);
						return null;
					}
					// Get the thing that goes right after the
					// jdbc: in the URL, which is *usually* the same
					// as the vendor string
					// so usually jdbc:oracle:..., jdbc:sqlserver: , etc.
					String proto = getJDBCProtocolForVendor( lVendor );

					// They all start with jdbc:proto:
					buff.append( "jdbc:" );
					buff.append( proto );
					buff.append( ':' );

					// Add Driver Marker

					// Is it Oracle?  Special syntax for Oracle
					if( lVendor!=null && lVendor.equals(VENDOR_TAG_ORACLE) )
					{
						debugMsg( kFName, "JDBC Oracle start" );
						// jdbc:oracle:thin:@mchine:nnnn:dbname
						buff.append( "thin:@" );
					}
					// Else everybody else uses the same URL syntax
					// If they don't, put another special case above
					else
					{
						debugMsg( kFName, "JDBC general start" );
						// jdbc:postgresql://host:port/db
						// jdbc:postgresql://host/db (uses host:5432)
						// jdbc:postgresql:db (uses localhost:5432)
						buff.append( "//" );
					}

					// Server info
					buff.append( server );
					if( port > 0 )
					{
						buff.append( ':' );
						buff.append( port );
					}

					// Add DB Name
					if( dbName != null )
					{
						// SQL Server has odd syntax, ;databaseName=db
						if( lVendor!=null && lVendor.equals(VENDOR_TAG_MS_SQL) )
						{
							debugMsg( kFName, "SQL Server DB Name" );
							// jdbc:microsoft:sqlserver://server_name:port;Databasename
							// Database name is optional
							buff.append( ";databaseName=" );    // Semicolon
							buff.append( dbName );
						}
						// Else Oracle gets :dbname
						else if( lVendor!=null && lVendor.equals(VENDOR_TAG_ORACLE) )
						{
							debugMsg( kFName, "Oracle DB Name" );
							buff.append( ':' );     // colon
							buff.append( dbName );
						}
						// Else everybody else gets /dbname
						else
						{
							debugMsg( kFName, "generic DB Name" );
							buff.append( '/' );     // slash
							buff.append( dbName );
						}
					}
					else    // Else the DB name WAS null
					{
						if( lVendor!=null && lVendor.equals(VENDOR_TAG_MS_SQL) )
						{
							statusMsg( kFName,
								"No database name was given."
								+ " This is not required for SQL Server"
								+ " but if things don't correctly you might want"
								+ " to check that."
								);
						}
						else    // Else it's NOT Microsoft, so NOT optional
						{
							errorMsg( kFName,
								"No database name was given."
								+ " This is required for vendor \"" + lVendor + "\"."
								+ " Returning null."
								);
							return null;
						}
					}   // End Else the DB name was null
				}   // End if it's JDBC
				// Is it ODBC?
				else if( isODBC )
				{
					// For ODBC
					//
					// Note:
					// Remote server not currently supported via ODBC
					// Maybe works by file system drive mapping
					// Maybe works with UNC file path, but maybe not
					// See doc below for RMI-JDBC-ODBC
					//
					// jdbc:odbc://source_name
					// "jdbc:odbc:Driver={MicroSoft Access Driver (*.mdb)};DBQ=D:test.mdb"
					// jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)};DBQ=C:\saint.mdb
					// "jdbc:odbc:DRIVER={SQL Server};DATABASE=iweb;UID=" +userid+ ";PWD=" +passwd+ ";";
					// OR they claim:
					// "jdbc:odbc:DRIVER={SQL Server};SERVER=123.456.789.123:1433;DATABASE=iweb";
					// Connection con = DriverManager.getConnection(conString, userid, passwd);
					// Maybe:
					// jdbc:odbc://whitehouse.gov:5000/CATS;PWD=Hillary
					// jdbc:odbc:Driver={SQL Server};Server=whitehouse.gov;Database=CATS

					// jdbc:odbc:Driver={Microsoft Excel Driver (*.xls)};DBQ=c:/excel/mySheet.xls;DriverID=22;READONLY=false
					// jdbc:odbc:Excel Files;DBQ=C:\\java\\data.xls

					// Maybe not
					// jdbc:odbc:Driver=MicroSoft Text Driver (*.txt; *.csv);Database=MyDatabase", "", ""

					// We INSIST on a DB Name at this point
					if( dbName == null )
					{
						errorMsg( kFName,
							"No Database Name for ODBC source"
							+ ", where db name is either a proper database name"
							+ ", or a DSN, or a data file name."
							+ " Returning null."
							);
						return null;
					}

					// We always start off with this
					buff.append( "jdbc:odbc:" );

					// Now it gets interesting
					if( defaultDriverClassName != null
						&& ! defaultDriverClassName.equals(ODBC_VENDOR_DSN)
						)
					{
						buff.append( "Driver=" );
						buff.append( defaultDriverClassName );
						buff.append( ";DBQ=" );
						buff.append( dbName );
						statusMsg( kFName,
							"ODBC Setup: assuming attribute " + DB_NAME_ATTR
							+ "=\"" + dbName + "\" refers to a database name"
							+ " or an actual file name, for example, an Excel file."
							);
						infoMsg( kFName,
							"ODBC Setup reminder:"
							+ " If you get an error and are referring to a file,"
							+ " you might want to specify an absolute path to the"
							+ " file and make sure to use only single backslashes."
							);
					}
					else    // Null driver class, assume DSN URL
					{
						buff.append( "//" );
						buff.append( dbName );
						statusMsg( kFName,
							"ODBC Setup: treating attribute " + DB_NAME_ATTR
							+ "=\"" + dbName + "\" as a predefined DSN"
							+ " since " + VENDOR_TAG_ATTR + " was not set at all"
							+ ", or was specifically set to \"" + ODBC_VENDOR_DSN + "\"."
							);
						infoMsg( kFName,
							"ODBC Setup reminder:"
							+ " If you get an error you may want to double check"
							+ " the DSN's name and configuration."
							+ " On Windows this is done in Control Panel / ODBC."
							);
					}

					// Add any extra parameters
					String parms = getExtraParameters();
					if( parms != null )
					{
						if( ! parms.startsWith( ";" ) )
							buff.append( ';' );
						buff.append( parms );
					}   // End If there were extra parameters

					if( (server!=null && ! server.equals(DEFAULT_SERVER_NAME) )
						|| port > 0
						)
					{
						errorMsg( kFName,
							"Server name and port are currently ignored"
							+ " in ODBC connections."
							+ " Will continue."
							+ " server=\"" + server + "\", port=\"" + port + "\""
							);
					}

				}
				// Else it's NOT jdbc NOR odbc
				else
				{
					errorMsg( kFName,
						"Unknown or missing connection type " // + lType
						+ " Valid choices are: "
						+ "\"" + TYPE_JDBC + "\""
						+ ", \"" + TYPE_ODBC
						+ ", default is \"" + DEFAULT_TYPE + "\"."
						+ " Will return null connection string."
						);
				}   // End else not Jdbc NOR Odbc


				// Save the results, if any
				if( buff.length() > 0 )
					cFinalConnectionString = new String( buff );
				// And just to double check
				else
					cFinalConnectionString = null;
					// We've already complained

			}   // End if getConfiguredConnectionString() was null

			// Else we already have it cached

		}   // End if not using cached value

		// Return cache, which we may have just calculated
		return cFinalConnectionString;

	}








//	private boolean ;
//	private boolean ;
//
//TEST_QUERY_PATH
//
//IS_A_SIMPLE_COUNT_ATTR
//SHOW_FIRST_ROW_ATTR




	private static void __sep__JDBC_and_ODBC__() {}





	// Check for Microsoft's funky driver
	// We really only want to do this check once
	// and we're using it's own caching mechanism
	private boolean hasMSSqlJDBCDriver()
	{
		return hasMSSqlJDBCDriver( false );
	}

	private boolean hasMSSqlJDBCDriver( boolean inWarnIfNotThere )
	{
		final String kFName = "hasMSSqlJDBCDriver(2)";
		boolean outAnswer = false;
		if( fHasCheckedForMSSqlDriver )
		{
			outAnswer = cHasMSSqlDriver;
		}
		else    // Else we have not checked before
		{
			// Do the actual check
			outAnswer = false;
			try
			{
				Class tmpClass = Class.forName( MS_SQL_CLASS_NAME );
				outAnswer = true;
			}
			catch(Exception exc)
			{
				// We will complain below
				debugMsg( kFName, "Class loader exeption: " + exc );
				// Redundant but clear:
				outAnswer = false;
			}

			// We should let them know at least once that we did not find
			// this driver.
			if( ! outAnswer )
			{
				String tmpMsg = "Microsoft SQL Server JDBC Driver not found."
					+ " This can be downloaded for FREE from Microsoft"
					+ " or you might consider using the ODBC driver."
					+ " The system will automatically fall back to that by default"
					+ " unless you specifically tell it otherwise."
					+ " If you want to download and use the JDBC driver"
					+ " *** PLEASE READ *** the file "
					+ MS_TROUBLESHOTTING_INSTRUCTIONS_FILE
					;
				// Complain loudly or softly
				if( inWarnIfNotThere )
					warningMsg( kFName, tmpMsg );
				else
					infoMsg( kFName, tmpMsg );
			}
			// Save the answer
			cHasMSSqlDriver = outAnswer;
			// And note that we HAVE NOW CHECKED
			fHasCheckedForMSSqlDriver = true;
		}

		// And we're done
		return outAnswer;
	}



	private static boolean isJDBCDriver( String inVendorTag )
	{
		final String kFName = "isJDBCDriver";
		// This shouldn't be possible since we use static init
		if( fJDBCDriverNameHash == null )
		{
			errorMsg( kFName,
				"Map not initialized, returning false."
				);
			return false;
		}
		String lVendorTag = NIEUtil.trimmedLowerStringOrNull( inVendorTag );
		if( lVendorTag == null )
		{
			errorMsg( kFName,
				"Passed in null/empty vendor tag, returning false."
				);
			return false;
		}
		return fJDBCDriverNameHash.containsKey( lVendorTag );
	}

	private static String getJDBCDriverNameForVendor( String inVendorTag )
	{
		return getJDBCDriverNameForVendor( inVendorTag, true );
	}

	private static String getJDBCDriverNameForVendor(
		String inVendorTag, boolean inDoWarnings
		)
	{
		final String kFName = "getJDBCDriverNameForVendor";
		boolean hasIt = isJDBCDriver( inVendorTag );
		if( hasIt )
		{
			String lVendorTag = NIEUtil.trimmedLowerStringOrNull( inVendorTag );
			return (String) fJDBCDriverNameHash.get( lVendorTag );
		}
		else
		{
			if( inDoWarnings )
				errorMsg( kFName,
					"No JDBC driver found for vendor \"" + inVendorTag + "\""
					+ ", returning null."
					);
			return null;
		}
	}

	// Always returns a string
	public static String listJDBCVendors()
	{
		// Will give appropriate whining if null
		return NIEUtil.hashKeysToSingleString( fJDBCDriverNameHash );
	}



	// We do give errors for improper usage or initialization
	private static boolean isODBCDriver( String inVendorTag )
	{
		final String kFName = "isODBCDriver";
		// This shouldn't be possible since we use static init
		if( fODBCDriverNameHash == null )
		{
			errorMsg( kFName,
				"Map not initialized, returning false."
				);
			return false;
		}
		String lVendorTag = NIEUtil.trimmedLowerStringOrNull( inVendorTag );
		if( lVendorTag == null )
		{
			errorMsg( kFName,
				"Passed in null/empty vendor tag, returning false."
				);
			return false;
		}
		return fODBCDriverNameHash.containsKey( lVendorTag );
	}

	private static String getODBCDriverNameForVendor( String inVendorTag )
	{
		return getODBCDriverNameForVendor( inVendorTag, true );
	}

	private static String getODBCDriverNameForVendor(
		String inVendorTag, boolean inDoWarnings
		)
	{
		final String kFName = "getODBCDriverNameForVendor";
		boolean hasIt = isODBCDriver( inVendorTag );
		if( hasIt )
		{
			String lVendorTag = NIEUtil.trimmedLowerStringOrNull( inVendorTag );
			return (String) fODBCDriverNameHash.get( lVendorTag );
		}
		else
		{
			if( inDoWarnings )
				errorMsg( kFName,
					"No ODBC driver found for vendor \"" + inVendorTag + "\""
					+ ", returning null."
					);
			return null;
		}
	}

	// always returns a string
	public static String listODBCVendors()
	{
		// Will give appropriate whining if null
		return NIEUtil.hashKeysToSingleString( fODBCDriverNameHash );
	}



	private static void __sep__Database_Tables__() {}

	////////////////////////////////////////////////////////////
	public static List getDBTableList()
	{
		return fDBTables;
	}


	public boolean verifyAllDBTables()
	{
		return verifyAllDBTables( false, false );
	}


	public boolean verifyAllDBTables(
		boolean inShowGoodTables, boolean inDoCreateIfMissing
		)
	{
		final String kFName = "verifyDBTables";
		List tables = getDBTableList();

		if( null == tables || tables.size() < 1 )
		{
			errorMsg( kFName,
				"Null/empty list of tables to check."
				);
			return false;
		}

		boolean outEverythingIsOK = true;

		// Loop through all the tables
		for( Iterator it = tables.iterator(); it.hasNext() ; )
		{
			String table = (String)it.next();
			boolean result = verifyASpecificDBTable(
				table, inShowGoodTables, inDoCreateIfMissing
				);
			if( ! result )
				outEverythingIsOK = false;
		}

		return outEverythingIsOK;

	}


	public boolean verifyASpecificDBTable(
		String inTableName,
		boolean inShowGoodTables, boolean inDoCreateIfMissing
		)
	{
		final String kFName = "verifyASpecificDBTable";
		inTableName = NIEUtil.trimmedStringOrNull( inTableName );
		if( null == inTableName )
		{
			errorMsg( kFName,
				"Null/empty table name to check."
				);
			return false;
		}

		boolean outEverythingOK = true;

		String tmpQry = "select count(*) from " + inTableName;
		int numRows = testQueryNumRows( tmpQry, true, false, false );
		if( numRows >= 0 )
		{
			if( inShowGoodTables )
			{
				String s = (1!=numRows) ? "s" : "";
				statusMsg( kFName,
					"Table \"" + inTableName + "\": Has " + numRows + " row" + s
					);
			}
		}
		// Else the table is missing
		else
		{
			// If we're not allowed to create it, then complain
			if( ! inDoCreateIfMissing )
			{
				warningMsg( kFName,
					"Table \"" + inTableName + "\": *** COULD NOT ACCESS TABLE ***"
					);
				outEverythingOK = false;
			}
			// Else we ARE allowed to create it
			else
			{
				outEverythingOK = createDBTable( inTableName );
			}
		}

		return outEverythingOK;

	}


	private boolean createDBTable( String inTableName )
	{
		final String kFName = "createDBTable";
		inTableName = NIEUtil.trimmedStringOrNull( inTableName );
		if( null == inTableName )
		{
			errorMsg( kFName,
				"Null/empty table name, don't know what to create."
				);
			return false;
		}

		// Get the table definition
		DBTableDef lTableDef = null;
		try
		{
			lTableDef = DBTableDef.getTableDef( inTableName, this );
		}
		catch( DBConfigException dbce )
		{
			errorMsg( kFName,
				"Unable to find defintion for table \"" + inTableName + "\"."
				+ " Exception was: " + dbce
				);
			return false;
		}

		// Now instruct the table to create itself in the actual database
		try
		{
			lTableDef.instantiateTableInDatabase();
		}
		catch( DBConfigException dbce2 )
		{
			errorMsg( kFName,
				"Unable to create database table \"" + inTableName + "\"."
				+ " Exception was: " + dbce2
				);
			return false;
		}

		return true;

	}


	public boolean loadAllDBTables(
		// boolean inShowGoodTables, boolean inDoCreateIfMissing,
		boolean inDoOverwriteData,
		String optCSVImportDirectory
		)
	{
		final String kFName = "loadAllDBTables";
		List tables = getDBTableList();

		if( null == tables || tables.size() < 1 )
		{
			errorMsg( kFName,
				"Null/empty list of tables to check."
				);
			return false;
		}

		boolean outEverythingIsOK = true;

		// Loop through all the tables
		for( Iterator it = tables.iterator(); it.hasNext() ; )
		{
			String table = (String)it.next();
			boolean result = loadASpecificDBTable(
				table, /* inShowGoodTables, inDoCreateIfMissing,*/
				inDoOverwriteData,
				optCSVImportDirectory
				);
			if( ! result )
				outEverythingIsOK = false;
		}

		return outEverythingIsOK;

	}

	public boolean saveAllDBTables(
		String optCSVImportDirectory
		)
	{
		final String kFName = "saveAllDBTables";
		List tables = getDBTableList();

		if( null == tables || tables.size() < 1 )
		{
			errorMsg( kFName,
				"Null/empty list of tables to check."
				);
			return false;
		}

		boolean outEverythingIsOK = true;

		// Loop through all the tables
		for( Iterator it = tables.iterator(); it.hasNext() ; )
		{
			String table = (String)it.next();
			boolean result = saveASpecificDBTable(
				table,
				optCSVImportDirectory
				);
			if( ! result )
				outEverythingIsOK = false;
		}
		return outEverythingIsOK;
	}

	public boolean loadASpecificDBTable(
		String inTableName,
		// boolean inShowGoodTables, boolean inDoCreateIfMissing,
		boolean inDoOverwriteData,
		String optCSVImportDirectory
		)
	{
		final String kFName = "loadASpecificDBTable";
		inTableName = NIEUtil.trimmedStringOrNull( inTableName );
		if( null == inTableName ) {
			errorMsg( kFName,
				"Null/empty table name to check."
				);
			return false;
		}
		// Get the table definition
		DBTableDef lTableDef = null;
		try {
			lTableDef = DBTableDef.getTableDef( inTableName, this );
		}
		catch( DBConfigException dbce )
		{
			errorMsg( kFName,
				"Unable to find defintion for table \"" + inTableName + "\"."
				+ " Exception was: " + dbce
				);
			return false;
		}

		// Now instruct the table to load its data into the database
		try {
			lTableDef.loadCsvDataIntoTable(
				null, optCSVImportDirectory,
				inDoOverwriteData
				);
		}
		catch( DBConfigException dbce2 )
		{
			errorMsg( kFName,
				"Unable to load database table \"" + inTableName + "\"."
				+ " Exception was: " + dbce2
				);
			return false;
		}
		catch( IOException ioe )
		{
			statusMsg( kFName, "No sample data found for table \"" + inTableName + "\"");
			return false;
		}

		return true;
	}

	public boolean saveASpecificDBTable(
		String inTableName,
		String inCSVExportDirectory
		)
	{
		final String kFName = "saveASpecificDBTable";
		inTableName = NIEUtil.trimmedStringOrNull( inTableName );
		if( null == inTableName ) {
			errorMsg( kFName,
				"Null/empty table name to export."
				);
			return false;
		}
		// Get the table definition
		DBTableDef lTableDef = null;
		try {
			lTableDef = DBTableDef.getTableDef( inTableName, this );
		}
		catch( DBConfigException dbce )
		{
			errorMsg( kFName,
				"Unable to find defintion for table \"" + inTableName + "\"."
				+ " Exception was: " + dbce
				);
			return false;
		}

		// Now instruct the table to create itself in the actual database
		try {
			lTableDef.saveCsvDataToFile(
				null, inCSVExportDirectory
				);
		}
		catch( DBConfigException dbce2 )
		{
			errorMsg( kFName,
				"Unable to save database table \"" + inTableName + "\"."
				+ " Exception was: " + dbce2
				);
			return false;
		}
		catch( IOException ioe )
		{
			errorMsg( kFName, "Unable to create CSV file for table \"" + inTableName + "\".  Error: '" + ioe + "'" );
			return false;
		}

		return true;
	}


	public boolean dropAllDBTables( boolean inIgnoreErrors )
	{
		final String kFName = "dropAllDBTables";
		List tableNames = getDBTableList();

		if( null == tableNames || tableNames.size() < 1 ) {
			if( ! inIgnoreErrors )
				errorMsg( kFName,
					"Null/empty list of tables to check."
					);
			return false;
		}

		boolean outEverythingIsOK = true;

		// Loop through all the tables
		for( Iterator it = tableNames.iterator(); it.hasNext() ; )
		{
			String tableName = (String)it.next();
			boolean result = dropASpecificDBTable( tableName, inIgnoreErrors );
			if( ! result )
				outEverythingIsOK = false;
		}

		return outEverythingIsOK;
	}



	public boolean dropASpecificDBTable( String inTableName, boolean inIgnoreErrors )
	{
		final String kFName = "dropASpecificDBTable";
		inTableName = NIEUtil.trimmedStringOrNull( inTableName );
		if( null == inTableName ) {
			errorMsg( kFName,
				"Null/empty table name to drop."
				);
			return false;
		}

		// Get the table definition
		DBTableDef lTableDef = null;
		try
		{
			lTableDef = DBTableDef.getTableDef( inTableName, this );
		}
		catch( DBConfigException dbce )
		{
			if( ! inIgnoreErrors )
				errorMsg( kFName,
					"Unable to find defintion for table \"" + inTableName + "\"."
					+ " Exception was: " + dbce
					);
			return false;
		}

		// Now instruct the table to create itself in the actual database
		try
		{
			lTableDef.dropTableFromDatabase( inIgnoreErrors );
		}
		catch( DBConfigException dbce2 )
		{
			if( ! inIgnoreErrors )
				errorMsg( kFName,
					"Unable to drop database table \"" + inTableName + "\"."
					+ " Exception was: " + dbce2
					);
			return false;
		}

		return true;
	}



	//// Gets and Simple Logic ///////////////////////////////////////////////
	private static void ___sep__XSLT_Transforms__() {}



	String getBaseURI()
	{
		final String kFName = "getBaseURI";
		// return getConfig().getConfigFileURI();
		// We want the DIRECTORY of where the main configuration was found

		if( null != getMainConfigOrNull() )
		{
			String tmpURI = getMainConfigOrNull().getConfigFileURI();
			debugMsg( kFName, "will return PARENT of \"" + tmpURI + "\"" );
			return NIEUtil.calculateDirOfURI( tmpURI );
		}
		else
		{
			errorMsg( kFName, "Null config, returning null." );
			return null;
		}
	}



	// This is only an error if there is a file name
	// present and we can't get at  it
	// If the file name is null, that's an expected event
	// results cached by calling routine
	byte [] getXSLTTextFromFileAsBytes()
		throws DBConfigException
	{
		final String kFName = "getXSLTTextFromFileAsBytes";
		String fileName = null;
		String baseURI = null;
		try
		{
			// fileName = getXSLTFileName();
			fileName = DB_VENDOR_FILTER_XSLT_SHEET;
			if( null == fileName )
				return new byte [0];
			baseURI = getBaseURI();
			if( null == baseURI )
				throw new DBConfigException(
					"Unable to get URI for base configuration file."
					);
			infoMsg( kFName,
				"Reading XSLT from file \"" + fileName + "\""
				);
			byte [] contents = NIEUtil.fetchURIContentsBin(
				fileName, baseURI
				);
			if( contents.length < 1 )
				throw new DBConfigException(
					"Unable to load XSLT file"
					+ ", filename=\"" + fileName + "\""
					+ ", base URI=\"" + baseURI + "\""
					);
			// we're done!
			return contents;
		}
		catch(Exception e)
		{
			throw new DBConfigException(
				"Error while loading XSLT file"
				+ ", filename=\"" + fileName + "\""
				+ ", base URI=\"" + baseURI + "\""
				+ ". Error: " + e
				);
		}
	}
	



	Transformer getCompiledXSLTDoc()
		throws DBConfigException
	{
		if( null == cTransformer )
		{
			final String kFName = "getCompiledXSLTDoc";
			try
			{
				byte [] contents = getXSLTTextFromFileAsBytes();
				cTransformer = JDOMHelper.compileXSLTString( contents );
			}
			catch(Exception e)
			{
				String msg = "Error getting / compiling XSLT: " + e;
				errorMsg( kFName, msg );
				throw new DBConfigException( msg );
			}
		}
		return cTransformer;
	}


	public JDOMHelper applyDbVendorFilter( JDOMHelper inXmlTree )
		throws DBConfigException
	{
		final String kFName = "applyDbVendorFilter";
		final String kExTag = kClassName + '.' + kFName + ": ";
	
		boolean debug = shouldDoDebugMsg( kFName );
	
		String lVendor = getConfiguredVendorTag();
		if( null == lVendor )
			throw new DBConfigException( kExTag
				+ "A vendor tag is required in order to generate database tables."
				);
	
	
		// create the parameters hash
		Hashtable lParamsHash = new Hashtable();
		// We need to tell the style sheet which flavor of SQL to output
		lParamsHash.put( "db", lVendor );
	
	
		try
		{
			/*** no caching of transform
			Document statements = JDOMHelper.xsltElementToDoc(
				inXmlTree,
				DB_VENDOR_FILTER_XSLT_SHEET,
				lParamsHash,
				false
				);
			***/
	
			if( debug )
				debugMsg( kFName,
					NIEUtil.NL + "BEFORE: (vendor=\"" + lVendor + "\")" + NIEUtil.NL
					+ inXmlTree.JDOMToString( true )
					);
	
			// Get compiled and cached XSLT
			Transformer formatter = getCompiledXSLTDoc();
			if( formatter == null )
			{
				throw new DBConfigException( kExTag
					+ "Could not obtain XSLT formatting rules"
					// + ", returning original document."
					);
			}
	
			debugMsg( kFName, "Formatting XML ..." );	
	
			// Now Transform it!
			// We have no parameters hash, and since we're already
			// working with CLONED data, it's OK to not have them reclone
			Document newJDoc = JDOMHelper.xsltElementToDoc(
				inXmlTree.getJdomElement(),
				formatter,
				lParamsHash,
				true
				);
			// throws JDOMHelperException?
			
			if( null == newJDoc )
				throw new DBConfigException( kExTag
					+ "Got back null document from XSLT formatter."
					);	
	
			Element tmpElem = newJDoc.getRootElement();
			if( null == tmpElem )
				throw new DBConfigException( kExTag
					+ "Got null root element from document produced XSLT formatter."
					);	
	
			JDOMHelper outElem = new JDOMHelper( tmpElem );
	
			if( debug )
				debugMsg( kFName,
					NIEUtil.NL + "AFTER: (vendor=\"" + lVendor + "\")" + NIEUtil.NL
					+ outElem.JDOMToString( true )
					);
	
			return outElem;
		}
		catch( Exception e )
		{
			throw new DBConfigException( kExTag
				+ "Unable to filter XML for this SQL dialect."
				+ " DB Vendor = \"" + lVendor + "\"."
				+ " Error: " + e
				);
		}
	
	
	}

	//// Gets and Simple Logic ///////////////////////////////////////////////
	private static void ___sep__DB_Vendor_Specific_Settings__() {}

	// do NOT use this for date math
	public String getVendorSysdateString() {
		if( null == cVendorSysdateString )
			if( getConfiguredVendorTag().startsWith(VENDOR_TAG_ORACLE) )
				cVendorSysdateString = "SYSDATE";
			else
				cVendorSysdateString = "CURRENT_TIMESTAMP";
		return cVendorSysdateString;
	}

	public String calculateDateTimeStringForNDaysPast( int inNDaysInPast,
			boolean inTruncToDateOnly, boolean inMakeFullSqlExpression
	) {
		final String kFName = "calculateDateTimeString";
	
		// We start with a calendar to help with date math
		Calendar lCalendar = Calendar.getInstance();
	
		// Now we SUBTRACT the number of days
		if( 0 != inNDaysInPast ) {
			lCalendar.add( Calendar.DATE, - inNDaysInPast );
			// not used below: java.util.Date startDate = lCalendar.getTime();
		}

		// Which format to use
		String lFormat = null;
		// If trucated
		if( inTruncToDateOnly ) {
			// For MySQL we have a special format
			if( getConfiguredVendorTag().equals( VENDOR_TAG_MYSQL ) ) {
				lFormat = nie.sr2.ReportConstants.DEFAULT_MYSQL_DATE_FORMAT;
			}
			// Else Oracle and everybody else!
			else {
				lFormat = nie.sr2.ReportConstants.DEFAULT_SQL_DATE_FORMAT;
			}
		}
		// Else NOT truncated
		else {
			// For MySQL we have a special format
			if( getConfiguredVendorTag().equals( VENDOR_TAG_MYSQL ) ) {
				lFormat = nie.sr2.ReportConstants.DEFAULT_MYSQL_DATETIME_FORMAT;
			}
			// Everybody else looks like Oracle
			else {
				lFormat = nie.sr2.ReportConstants.DEFAULT_ORACLE_DATETIME_FORMAT;
			}
		}

		// Convert to String
		String outDate = NIEUtil.formatDateToString(
			lCalendar.getTime(), lFormat, true
			);
		if( null==outDate ) {
			errorMsg( kFName, "Got back null for formatted date; returning null." );
			return null;
		}
	
		// Fix up string with SQL stuff if needed
		if( inMakeFullSqlExpression ) {
			outDate = "'" + outDate + "'";
	
			// For Oracle full date parsing, we need to wrap this in their to_date function
			if( getConfiguredVendorTag().startsWith(DBConfig.ORACLE_NAME) )
				outDate = "TO_DATE( " + outDate + ", '"
					+ nie.sr2.ReportConstants.DEFAULT_ORACLE_DATETIME_FORMAT
					+ "' )"
					;
		}
	
		return outDate;
	}

	public boolean getVendorDoesNormalOuterJoin() {
		if( ! mUseCache ) {
			cVendorNormalOuterJoin =
				! getConfiguredVendorTag().startsWith( ORACLE_NAME )
				&& ! getConfiguredVendorTag().equals( VENDOR_TAG_MS_SQL )
				;
		}
		return cVendorNormalOuterJoin;
	}

	public boolean getVendorNeedsCommitByDefault() {
		if( ! mUseCache ) {
			cVendorNeedsCommit =
				! getConfiguredVendorTag().equals( VENDOR_TAG_MYSQL )
				;
		}
		return cVendorNeedsCommit;
	}

	public String getVendorNullValueMethodName() {
		if( ! mUseCache ) {
			cVendorNullValueMethodName =
				( getConfiguredVendorTag().startsWith( VENDOR_TAG_ORACLE ) )
				? "NVL"
				: "COALESCE"
				;
		}
		// See:
		//	http://www.w3schools.com/SQL/sql_isnull.asp
		//	NVL - Oracle, Microsoft
		//	IFNULL - MySQL, Microsoft
		//	ISNULL - Microsoft, * MySQL has DIFFERENT syntax so use IFNULL
		//	COALESCE - MySQL, Microsoft (and PostgreSQL)
		
		return cVendorNullValueMethodName;
	}

	public long getVendorEpochCorrectionInMS() {
		if( ! mUseCache ) {
			final String kFName = "getVendorEpochCorrectionInMS";
			cEpochCorrection =
				getConfiguredVendorTag().equals( VENDOR_TAG_MS_SQL ) ?
					NIEUtil.EPOCH_CORRECTION_SQL_SERVER : 0
					;
	
			// We also correct for timezone and daylight savings time
			Calendar cal=Calendar.getInstance();
			cEpochCorrection -= cal.get(Calendar.ZONE_OFFSET);
			cEpochCorrection -= cal.get(Calendar.DST_OFFSET);
	
			/***
			statusMsg( kFName,
				"Offsets: "
				+ "Calendar.get(Calendar.ZONE_OFFSET)="
				+ cal.get(Calendar.ZONE_OFFSET)
			
				+ ", Calendar.get(Calendar.DST_OFFSET)="
				+ cal.get(Calendar.DST_OFFSET)
			);
			***/
	
		}
		return cEpochCorrection;
	}

	// This is a wrapper with quite a bit of logic when it needs
	// to figure out the type because it was not specifically stated
	// returns cType as either TYPE_JDBC or TYPE_ODBC (or maybe unknown...)
	public String getDBType()
	{
		if( ! mUseCache )
		{
			final String kFName = "getType";
			String reason = null;
	
			cType = getConfiguredDBType();
			String lVendor = null;
			if( cType == null )
			{
				reason = "no specific type set";
				// We have some checking for various vendor tags
				lVendor = getConfiguredVendorTag();
				lVendor = normalizeVendorTag( lVendor );
				// If it's null, take the default
				// others will complain, if needed, about there being no vendor
				if( lVendor == null )
				{
					cType = DEFAULT_TYPE;
					reason += ", null vendor, default type";
				}
				// If the vendor has braces, then it's ODBC
				else if( lVendor.indexOf('{')>=0 || lVendor.indexOf('}')>=0 )
				{
					cType = TYPE_ODBC;
					reason += ", found braces, indicates ODBC driver";
				}
				// If the vendor is MS SQL we have "special" rules for that
				else if( lVendor.equals(VENDOR_TAG_MS_SQL) )
				{
					reason += ", sql server indicated";
					// If we prefer JDBC
					if( DEFAULT_TYPE.equals(TYPE_JDBC) )
					{
						reason += ", desired default is JDBC";
						// and if we have the JDBC driver
						if( hasMSSqlJDBCDriver() )
						{
							cType = TYPE_JDBC;
							reason += ", and DOES have MS JDBC driver";
						}
						else    // Else don't have the jdbc driver
						{
							cType = TYPE_ODBC;
							reason += ", but does NOT have MS JDBC driver";
						}
					}
					// Else they prefer ODBC driver anyway
					else    // Default to ODBC
					{
						cType = DEFAULT_TYPE;
						reason += ", desired default is not JDBC, it's "
							+ DEFAULT_TYPE
							;
					}
				}
				else    // Else vendor is NOT Microsoft SQL
				{
	
					// Add Driver Marker
					// Change the order in which we check if you change
					// the default type
	
					// check for known JDBC vendors
					if( isJDBCDriver(lVendor) )
					{
						cType = TYPE_JDBC;
						reason = ", known JDBC vendor";
					}
					// Check for known ODBC vendors
					else if( isODBCDriver( lVendor ) )
					{
						cType = TYPE_ODBC;
						reason = ", known ODBC vendor";
					}
					else    // Else it's neither
					{
						// Give them the default
						cType = DEFAULT_TYPE;
						reason += ", default for misc vendor tag " + lVendor
							+ " is " + DEFAULT_TYPE
							;
					}
				}   // End else vendor was NOT microsoft SQL
			}
			else // Else a specific type was set
			{
				// cType is already set above
				reason = "specifically set to " + cType;
			}
			// Should not be null at this point
			if( cType != null )
			{
				// Normalize
				cType = cType.toLowerCase();
				// Let the know what's going on
				infoMsg( kFName,
					"Type is \"" + cType + "\" for vendor \"" + lVendor + "\""
					+ ", because " + reason + "."
					);
			}
			else    // Else it is still null !?
			{
				errorMsg( kFName,
					"Unable to determin type value.  Will return null!"
					);
			}
		}   // End if not cached
		return cType;
	}

	private String getConfiguredDBType()
	{
		return fConfigTree.getStringFromAttributeTrimOrNull( DB_TYPE_ATTR );
	}

	public boolean isJDBC()
	{
		String tmpStr = getDBType();
		if( tmpStr != null )
			return tmpStr.equals(TYPE_JDBC);
		else
			errorMsg( "isJDBC", "Null type, returning false." );
		return false;
	}

	public boolean isODBC()
	{
		String tmpStr = getDBType();
		if( tmpStr != null )
			return tmpStr.equals(TYPE_ODBC);
		else
			errorMsg( "isODBC", "Null type, returning false." );
		return false;
	}

	// OK to call with null, will return null and not complain
	private String expandOdbcVendorTagToDriverName( String inVendor )
	{
		final String kFName = "expandOdbcVendorTagToDriverName";
		// Lose the spaces
		String lVendor = NIEUtil.trimmedStringOrNull( inVendor );
		// If it's null, we're done
		if( lVendor == null )
		{
			debugMsg( kFName, "Null/empty vendor tag passed in, returning null." );
			return null;
		}
	
		// If it's got braces, leave it alone!
		if( lVendor.indexOf('{')>=0 || lVendor.indexOf('}')>=0 )
		{
			debugMsg( kFName, "Found braces, returning as-is (trimmed)." );
			return lVendor;
		}
	
		//  normalize for checking
		String compareVendor = lVendor.toLowerCase();
	
		if( compareVendor.equals( ODBC_VENDOR_DSN ) )
		{
			debugMsg( kFName,
				"It's just DSN, which has no driver string, returning null."
				);
			return null;
		}
	
		// OK, now go ahead and look it up
		// This will kick up warning if it's not found
		String outClassName = getODBCDriverNameForVendor( lVendor );
	
		// Return whatever it found
		return outClassName;
	}

	public int getPort()
	{
		final String kFName = "getPort";
	
		// If not using cache (during init), do the full calculation
		if( ! mUseCache )
		{
			int lPort = getConfiguredPort();
	
			// We'd like to have a default port
			// Some vendors do have that
			// Whether or not a port is needed AT ALL is decided in the
			// calculateConnect string method.
	
			// final String lType = getTypeString();
			final String lVendor = getConfiguredVendorTag();
	
			if( lPort <= 0 && isJDBCDriver( lVendor ) )
			{
	
				lPort = getDefaultPortForJDBCVendor( lVendor, false );
	
				// We don't warn here about the lack of a port, that's up to
				// the caller.
				// But as a courtesy, we DO give a status message when we
				// default to SOME vendors' default ports
				if( lPort > 0 )
					infoMsg( kFName,
						"Assuming default port \"" + lPort + "\""
						+ " for vendor \"" + lVendor + "\"."
						+ " This will be used if doing a remote or tcp/ip connection."
						);
	
			}   // End if port was <= 0
	
			// Cache the results
			cPort = lPort;
	
		}   // End if not caching
	
		// Return the answer
		return cPort;
	}

	private int getDefaultPortForJDBCVendor( String inVendor, boolean inDoWarn )
	{
		final String kFName = "getDefaultPortForODBCVendor";
		// This shouldn't be possible since we use static init
		if( fJDBCDefaultPorts == null )
		{
			errorMsg( kFName,
				"Map not initialized, returning -1."
				);
			return -1;
		}
		String lVendorTag = NIEUtil.trimmedLowerStringOrNull( inVendor );
		if( lVendorTag == null )
		{
			if( inDoWarn )
				errorMsg( kFName,
					"Passed in null/empty vendor tag, returning -1."
					);
			return -1;
		}
		// Grab the object and return the integer
		Integer obj = (Integer) fJDBCDefaultPorts.get( lVendorTag );
		return obj.intValue();
	}

	private int getConfiguredPort()
	{
		return fConfigTree.getIntFromAttribute( SERVER_PORT_ATTR, -1 );
	}

	private String getDefaultServerForJDBCVendor( String inVendor )
	{
		return DEFAULT_SERVER_NAME;
	}

	private String getJDBCProtocolForVendor( String inVendor )
	{
		final String kFName = "getJDBCProtocolForVendor";
		// This shouldn't be possible since we use static init
		if( fJDBCProtocolNameOverrides == null )
		{
			errorMsg( kFName,
				"Map not initialized, returning null."
				);
			return null;
		}
		String lVendorTag = NIEUtil.trimmedLowerStringOrNull( inVendor );
		if( lVendorTag == null )
		{
			errorMsg( kFName,
				"Passed in null/empty vendor tag, returning null."
				);
			return null;
		}
	
		// It's perfectly OK if there is no override
		if( fJDBCProtocolNameOverrides.containsKey( lVendorTag ) )
			return (String) fJDBCProtocolNameOverrides.get( lVendorTag );
		else
			return lVendorTag;
	}

	private String getDriverClassName()
	{
		if( ! mUseCache )
		{
			final String kFName = "getDriverClassName";
	
			// Did they set one specifically?
			// Most often NOT
			cDriverClassString = fConfigTree.getStringFromAttributeTrimOrNull(
				DRIVER_ATTR
				);
	
			// Most of the time we have to figure this out for ourselves
			// based on the vendor tag
			if( cDriverClassString == null )
			{
				String vTag = getConfiguredVendorTag();
	
				if( isJDBC() )
				{
					cDriverClassString = getJDBCDriverNameForVendor( vTag );
				}
				// All ODBC drivers use the same JDBC-ODBC class
				else if( isODBC() )
				{
					cDriverClassString = ODBC_CLASS_NAME;
				}
				else    // Else they DID set a specific driver class
				{
					statusMsg( kFName,
						"Will use User specified database driver class"
						+ " = \"" + cDriverClassString + "\""
						);
				}
			}
		}   // End if not using cache
		return cDriverClassString;
	}

	private String getConfiguredDriverClassString()
	{
		if( ! mUseCache )
			cDriverClassString = fConfigTree.getStringFromAttributeTrimOrNull( DRIVER_ATTR );
		return cDriverClassString;
	}

	private String getConfiguredConnectionString()
	{
		if( ! mUseCache )
			cNativeConnectionString = fConfigTree.getStringFromAttributeTrimOrNull(
				NATIVE_CONNECT_STRING_ATTR
				);
		return cNativeConnectionString;
	}

	//// Gets and Simple Logic ///////////////////////////////////////////////
	private static void ___sep__Gets_and_Simple_Logic__() {}

	public nie.sn.SearchTuningConfig getMainConfigOrNull()
	{
		return fMainConfig;
	}

	// The Vendor, short version
	public String getConfiguredVendorTag()
	{
		if( ! mUseCache )
		{
			final String kFName = "getConfiguredVendorTag";
	
			cVendorTag = fConfigTree.getStringFromAttributeTrimOrNull( VENDOR_TAG_ATTR );
	
			// If not null, do some normalization
			if( cVendorTag != null )
			{
				cVendorTag = cVendorTag.toLowerCase();
	
				// Do some normalizing on PostgreSQL
				// Currently VENDOR_TAG_POSTGRESQL = "postgresql" but could change
				if( cVendorTag.equals("postgresql")
					|| cVendorTag.equals("postgres") 
					|| cVendorTag.equals("postgress") 
					|| cVendorTag.equals("postgressql") 
					|| cVendorTag.equals("postgres sql") 
					|| cVendorTag.equals("postgres-sql") 
					|| cVendorTag.equals("postgres_sql") 
					)
				{
					if( ! cVendorTag.equals(VENDOR_TAG_POSTGRESQL) ) {
						infoMsg( kFName,
							"Correcting spelling of vendor tag PostgreSQL"
							+ " from \"" + cVendorTag + "\""
							+ " to \"" + VENDOR_TAG_POSTGRESQL + "\"."
							);
						cVendorTag = VENDOR_TAG_POSTGRESQL;
					}
				}
	
				// Some normalizing for Microsoft
				// Yes, I realize it's a bit much, but it's very popular and if
				// it saves even 5 support calls it's worth it!
				// Also, our config UI sometimes shows it long, with spaces.
				// Currently VENDOR_TAG_MS_SQL = "sqlserver" but could change
				else if(
						(
							cVendorTag.startsWith("s")
							|| cVendorTag.startsWith("m")
						) && (
							cVendorTag.equals("sql")
							|| cVendorTag.equals("sqlserver")
							|| cVendorTag.equals("mssql")
							|| cVendorTag.equals("msql")
							|| cVendorTag.equals("ms sql")
							|| cVendorTag.equals("ms-sql")
							|| cVendorTag.equals("ms_sql")
							|| cVendorTag.equals("microsoft sql")
							|| cVendorTag.equals("microsoft-sql")
							|| cVendorTag.equals("microsoft_sql")
							|| cVendorTag.equals("sql server")
							|| cVendorTag.equals("sql-server")
							|| cVendorTag.equals("sql_server")
							|| cVendorTag.equals("ms sqlserver")
							|| cVendorTag.equals("ms sql server")
							|| cVendorTag.equals("ms sql-server")
							|| cVendorTag.equals("microsoft sqlserver")
							|| cVendorTag.equals("microsoft sql server")
							|| cVendorTag.equals("microsoft sql-server")
						)
					)
				{
					if( ! cVendorTag.equals(VENDOR_TAG_MS_SQL) ) {
						infoMsg( kFName,
							"Correcting spelling of vendor tag for Microsoft SQL Server"
							+ " from \"" + cVendorTag + "\""
							+ " to \"" + VENDOR_TAG_MS_SQL + "\"."
							);
						cVendorTag = VENDOR_TAG_MS_SQL;
					}
				}
				// Oracle aliases
				else if( cVendorTag.equals("oracle")
					|| cVendorTag.equals("oracle8i")
					|| cVendorTag.equals("oracle 8i") 
					|| cVendorTag.equals("oracle-8i") 
					|| cVendorTag.equals("oracle_8i") 
					|| cVendorTag.equals("oracle9i")
					|| cVendorTag.equals("oracle 9i") 
					|| cVendorTag.equals("oracle-9i") 
					|| cVendorTag.equals("oracle_9i") 
					)
				{
					if( ! cVendorTag.equals(VENDOR_TAG_ORACLE) ) {
						infoMsg( kFName,
							"Correcting spelling of vendor tag Oracle"
							+ " from \"" + cVendorTag + "\""
							+ " to \"" + VENDOR_TAG_ORACLE + "\"."
							);
						cVendorTag = VENDOR_TAG_ORACLE;
					}
				}
				// MySQL
				// else if( cVendorTag.equals("mysql")
				else if( cVendorTag.startsWith("mysql")
					|| cVendorTag.equals("my sql") 
					|| cVendorTag.equals("my-sql") 
					|| cVendorTag.equals("my_sql") 
					)
				{
					if( ! cVendorTag.equals(VENDOR_TAG_MYSQL) ) {
						infoMsg( kFName,
							"Correcting spelling of vendor tag MySQL"
							+ " from \"" + cVendorTag + "\""
							+ " to \"" + VENDOR_TAG_MYSQL + "\"."
							);
						cVendorTag = VENDOR_TAG_MYSQL;
					}
				}
	
	
				// Todo: could normalize some of the lesser known ones
	
			}   // End if was not null
	
		}   // End if not using cache
	
		return cVendorTag;
	}

	// Longer version for display
	public String _getVendorString()
	{
		if( ! mUseCache )
			cVendorString = fConfigTree.getStringFromAttributeTrimOrNull( VENDOR_STRING_ATTR );
		return cVendorString;
	}

	// Returns " AS " or ""
	public String getVendorAliasString() {
		if( null == cVendorAliasString )
			cVendorAliasString = getConfiguredVendorTag().startsWith(VENDOR_TAG_ORACLE) ? " " : " AS ";
		return cVendorAliasString;
	}

	// OK to call with null, it will return null and not complain
	private String normalizeVendorTag( String inVendor )
	{
		final String kFName = "normalizeVendorTag";
		// Lose the spaces
		String lVendor = NIEUtil.trimmedStringOrNull( inVendor );
		// If it's null, we're done
		if( lVendor == null )
		{
			debugMsg( kFName, "Null/empty vendor tag passed in, returning null." );
			return null;
		}
		// If it's got braces, leave it alone!
		if( lVendor.indexOf('{')>=0 || lVendor.indexOf('}')>=0 )
		{
			debugMsg( kFName, "Found braces, returning as-is (trimmed)." );
			return lVendor;
		}
		// Else lower case it and return it
		debugMsg( kFName, "Returning trimmed and to lower case." );
		return lVendor.toLowerCase();
	}

	// Get what we wound up with, including appropriate defaults
	public String getServerString()
	{
	
		if( ! mUseCache )
		{
			final String kFName = "getServerString";
	
			String lServerName = getConfiguredServerName();
	
			// Some drivers have a default host name
			if( lServerName == null )
			{
				final String lVendor = getConfiguredVendorTag();
				lServerName = getDefaultServerForJDBCVendor( lVendor );
				// ^^^ for now it just returns localhost
			}   // End if it's null
	
			// Cache the results
			cServerName = lServerName;
		}
		return cServerName;
	}

		//	public String getNetworkProtocal()
	//	{
	//		if( ! mUseCache )
	//			cNetworkProtocol = fConfigTree.getStringFromAttributeTrimOrNull( NET_PROTO_ATTR );
	//		return cNetworkProtocol;
	//	}
		public String getDBName()
		{
			if( ! mUseCache )
				cDBName = fConfigTree.getStringFromAttributeTrimOrNull( DB_NAME_ATTR );
			return cDBName;
		}

	public boolean getIsWorking()
	{
		// We don't bother to sync this
		return fIsWorking;
	}

	public void reportError()
	{
		final String kFName = "reportError";

		synchronized(fStateLock)
		{
			if( fIsWorking )
			{
				fIsWorking = false;
				fLastCheckedTime = 0L;

				mDBConnection = DBConfig.closeConnection( mDBConnection, kClassName, kFName, false );
				mDBConnectionManualCommit = DBConfig.closeConnection( mDBConnectionManualCommit, kClassName, kFName, false );
			}
		}

	}


	// This specifies the CONFIGURATION of whether the database is active
	// or not, this is NOT the state of the database connection
	// See getIsWorking and reportError()
	public boolean isActive()
	{
		final String kFName = "isActive";
		if( ! mUseCache )
		{
			cIsActive = fConfigTree.getBooleanFromAttribute( ACTIVE_ATTR, true );
			if( ! cIsActive )
			{
				warningMsg( kFName,
					"Database access has been DEACTIVATED in the config file."
					+ " This is typically done for testing other parts of the system"
					+ " when a real database connection is not available."
					+ " No real database will be active"
					+ " and getConnection() will ALWAYS RETURN NULL."
					+ " You must still supply valid looking database config settings;"
					+ " that's often part of the test."
					);
			}
		}
		return cIsActive;
	}


	// Get what they actually entered
	private String getConfiguredServerName()
	{
		return fConfigTree.getStringFromAttributeTrimOrNull( SERVER_NAME_ATTR );
	}

	public long getRetryWaitInterval()
	{
		final String kFName = "getRetryWaitInterval";

		// If not using cache (during init), do the full calculation
		if( ! mUseCache )
		{
			cRetryWaitInterval = fConfigTree.getLongFromAttribute(
				RETRY_WAIT_MS_ATTR, DEFAULT_RETRY_WAIT_MS
				);

			debugMsg( kFName, "Initial value of retry wait = " + cRetryWaitInterval );

			// Is interval clearly negative
			if( cRetryWaitInterval < DISABLE_RETRY )
			{
				errorMsg( kFName,
					"Error setting retry wait interval \"" + cRetryWaitInterval + "\""
					+ ", this value must be >= " + DISABLE_RETRY + "."
					+ " Will set to default of \"" + DEFAULT_RETRY_WAIT_MS + "\"."
					);
				cRetryWaitInterval = DEFAULT_RETRY_WAIT_MS;
			}
			// -1 exactly means to disable retries completely
			else if( cRetryWaitInterval == DISABLE_RETRY )  // -1
			{
				statusMsg( kFName,
					"FYI: The retry wait interval " + RETRY_WAIT_MS_ATTR
					+ " has been set to " + DISABLE_RETRY
					+ " which indicates that NO RETRIES are to be performed, ever."
					+ " So if an error is encountered connecting to the database,"
					+ " NO attempts will be made to reconnect."
					+ " This will continue until the application has been restarted."
					);
			}
			// Warn if exactly zero, which means no waiting, always retry
			else if( cRetryWaitInterval == 0 )
			{
				statusMsg( kFName,
					"FYI: The retry wait interval " + RETRY_WAIT_MS_ATTR
					+ " has been set to zero"
					+ " which indicates that retries are to always be performed"
					+ ", without waiting for any time to pass."
					+ " This may cause many reconnection attempts."
					);
			}
			// Warn if suspiciously low number
			else if( cRetryWaitInterval < RETRY_WAIT_WARNING_THRESHOLD )
			{
				warningMsg( kFName,
					"FYI: The retry wait interval " + RETRY_WAIT_MS_ATTR
					+ " has been set to " + cRetryWaitInterval
					+ ", which is a rather short time."
					+ " This value is given in MILLISECONDS"
					+ " (thousandths of a second)"
					+ ", so for example a value of 1000 means 1 second"
					+ " and 60000 means 1 minute."
					+ " If you already know this and the value is what you intended"
					+ " then please disregard this warning message."
					+ " This warning is triggered when the value is less than "
					+ RETRY_WAIT_WARNING_THRESHOLD + "."
					);
			}

			// debugMsg( kFName, "End of cache, retry wait = " + cRetryWaitInterval );

		}   // End if not caching

		// Return the answer
		// debugMsg( kFName, "Returning cached retry wait = " + cRetryWaitInterval );
		return cRetryWaitInterval;
	}



	public String getMainTable()
	{
		if( ! mUseCache )
			cMainTable = fConfigTree.getStringFromAttributeTrimOrNull( MAIN_TABLE_ATTR );
		return cMainTable;
	}
	public String getUsername()
	{
		if( ! mUseCache )
			cUsername = fConfigTree.getStringFromAttributeTrimOrNull( USERNAME_ATTR );
		return cUsername;
	}
	public String getPassword()
	{
		if( ! mUseCache )
			cPassword = fConfigTree.getStringFromAttributeTrimOrNull( PASSWORD_ATTR );
		return cPassword;
	}
	// It's perfectly OK if there are none
	// Samples:
	// For Excel, you can have "readonly=false"
	public String getExtraParameters()
	{
		if( ! mUseCache )
			cExtraParameters = fConfigTree.getStringFromAttributeTrimOrNull(
				EXTRA_PARAMETERS_ATTR
				);
		return cExtraParameters;
	}
	public boolean shouldDoATestQuery()
	{
		if( ! hasTestQueryNode() )
			return false;
		if( getConfiguredTestQuery() != null )
			return true;
		return isAutomaticTestQuery();
	}

	public String getTestQuery()
	{
		final String kFName = "getTestQuery";
		if( ! hasTestQueryNode() )
		{
			infoMsg( kFName, "No test query node, returning null." );
			return null;
		}
		String tmp = getConfiguredTestQuery();
		if( tmp != null )
		{
			infoMsg( kFName, "Returning specifically configured test query." );
			return tmp;
		}
		if( ! isAutomaticTestQuery() )
		{
			infoMsg( kFName,
			"No specifically configured test query and no auto query, returning null."
			);
			return null;
		}
		String tableName = getMainTable();
		if( tableName == null )
		{
			infoMsg( kFName,
			"No specifically configured test query and no table name for auto-query, returning null."
			);
			return null;
		}
		String answer = null;
		if( ! doShowFirstRowFromTestQuery() )
		{
			infoMsg( kFName,
				"Returning auto-generated count(*) query from table name \"" + tableName + "\"."
				);
			answer = "select count(*) from " + tableName;
		}
		else    // We're showing the first row, we want to do an actual query
		{
			infoMsg( kFName,
				"Returning auto-generated select * query from table name \"" + tableName + "\"."
				+ " Not doing count(*) because we want to show the first row."
				);
			answer = "select * from " + tableName;
		}
		return answer;
	}

	// A query to run once we've connected
	private String getConfiguredTestQuery()
	{
		if( ! mUseCache )
		{
			if( ! hasTestQueryNode() )
				cTestQuery = null;
			else
				cTestQuery = fConfigTree.getTextByPathTrimOrNull( TEST_QUERY_PATH );
		}
		return cTestQuery;
	}
	public boolean isTestQueryASimpleCount()
	{
		if( ! mUseCache )
		{
			if( ! hasTestQueryNode() )
				cIsSimpleCountQuery = false;
			else
			{
				// They told us for sure
				if( fConfigTree.existsPathAttr(TEST_QUERY_PATH, IS_A_SIMPLE_COUNT_ATTR) )
				{
					cIsSimpleCountQuery = fConfigTree.getBooleanFromSinglePathAttr(
						TEST_QUERY_PATH, IS_A_SIMPLE_COUNT_ATTR, false
						);
				}
				// Time for guessing
				else
				{
					// If they have a query, we should not presume anything
					if( getConfiguredTestQuery() != null )
						cIsSimpleCountQuery = false;
					// Else if we're showing the first row, then no
					else if( doShowFirstRowFromTestQuery() )
						cIsSimpleCountQuery = false;
					// Else no query, but maybe using main table, try it!
					else if( getMainTable() != null )
						cIsSimpleCountQuery = true;
					// Else better to guess no
					else
						cIsSimpleCountQuery = false;
				}
			}
		}
		return cIsSimpleCountQuery;
	}
	private boolean isAutomaticTestQuery()
	{
		if( ! mUseCache )
		{
			final String kFName = "isAutomaticTestQuery";
			if( ! hasTestQueryNode() )
				cIsAutomaticQuery = false;
			// Else it does have a test query node
			else
			{
				// If it's specifically set
				if( fConfigTree.existsPathAttr(
						TEST_QUERY_PATH, GENERATE_AUTOMATIC_TEST_QUERY_ATTR
						)
					)
				{
					cIsAutomaticQuery = fConfigTree.getBooleanFromSinglePathAttr(
						TEST_QUERY_PATH, GENERATE_AUTOMATIC_TEST_QUERY_ATTR, false
						);
				}
				// Else we'll do it only if configured query is null and table not null
				else
				{
					cIsAutomaticQuery = getConfiguredTestQuery() == null
						&& getMainTable() != null;
				}

				// Do some error checking
				if( cIsAutomaticQuery )
				{
					if( getConfiguredTestQuery() != null )
					{
						errorMsg( kFName,
							"Predefined specific test query overrides automatic test query."
							+ " Setting this to false."
							);
						cIsAutomaticQuery = false;
					}
					else if( getMainTable() == null )
					{
						errorMsg( kFName,
							"Can't autogenerate test query if no main table defined."
							+ " Setting this to false."
							);
						cIsAutomaticQuery = false;
					}
				}   // End if is set to automatic query
			}   // End else it does have a test query node
		}   // End if not using cache
		return cIsAutomaticQuery;
	}
	public boolean doShowFirstRowFromTestQuery()
	{
		if( ! mUseCache )
			cShowFirstRow = fConfigTree.getBooleanFromSinglePathAttr(
				TEST_QUERY_PATH, SHOW_FIRST_ROW_ATTR, DEFAULT_SHOW_FIRST_ROW
				);
		return cShowFirstRow;
	}

	// We only want to check this once, independant of other caching
	private boolean hasTestQueryNode()
	{
		if( ! cHaveCheckedForQueryNode )
		{
			cHasQueryNode = fConfigTree.exists( TEST_QUERY_PATH );
			cHaveCheckedForQueryNode = true;
		}
		return cHasQueryNode;
	}



	///// Run Logging ///////////////////////////////////////
	private static void __sep__Run_Logging__() {}
	//////////////////////////////////////////////////////


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




	private static final void ___sep__Member_Fields_and_CONSTANTS__(){}
	//////////////////////////////////////////////////////////////////////

	// The main JDOM configuration tree
	private JDOMHelper fConfigTree;

	// Our main configuration
	private nie.sn.SearchTuningConfig fMainConfig;


	// The main JDBC connection
	private Connection mDBConnection;

	// The auxillary non-auto-commit JDBC connection
	private Connection mDBConnectionManualCommit;

	// An object for coordinated connection field access
	private Object fStateLock = new Object();
	private volatile boolean fIsWorking;
	private volatile boolean fDoingInit;
	private volatile long fLastCheckedTime;

	private Transformer cTransformer;

	private long cEpochCorrection;

	// See also Retry feilds

	// A table of short ODBC driver names to proper driver names
	// This is initialized in the static section BELOW
	private static Hashtable fJDBCDriverNameHash;
	private static Hashtable fJDBCDefaultPorts;
	private static Hashtable fJDBCProtocolNameOverrides;
	private static Hashtable fODBCDriverNameHash;

	private boolean fInTestMode;

	// Cached variables
	// =====================================================
	// Off by default
	private boolean mUseCache;

	private boolean mHasDriverBeenRegistered;

	private boolean mHaveReportedDBConnection;


	// Don't forget to call the routines that cache these values
	private boolean cIsActive;
	private String cServerName;
	private int cPort;
	private String cType;
	private String cDriverClassString;
	private String cNetworkProtocol;
	private String cDBName;
	private String cMainTable;
	private String cUsername;
	private String cPassword;
	private String cExtraParameters;
	private String cVendorTag;
	private String cVendorString;
	private String cTestQuery;
	private boolean cIsSimpleCountQuery;
	private boolean cIsAutomaticQuery;
	private boolean cShowFirstRow;
	private boolean cHasQueryNode;
	private boolean cHaveCheckedForQueryNode;
	private long cRetryWaitInterval;

	private String cNativeConnectionString;
	private String cFinalConnectionString;

	private String cVendorAliasString; // "" or " AS "
	private String cVendorSysdateString;
	private boolean cVendorNormalOuterJoin;
	private boolean cVendorNeedsCommit;
	private String cVendorNullValueMethodName;

	// All about "type", JDBC vs ODBC
	// =====================================
	public static final String MAIN_ELEMENT_NAME = "database";
	private static final String PATH2 = SearchTuningConfig.DB_CONFIG_PATH;
	private static final String PATH3 = SearchTuningConfig.OLD_DB_CONFIG_PATH;

	private static final String DB_TYPE_ATTR = "type";
	// If you add to these, you better sweep thorugh the code
	private static final String TYPE_JDBC = "jdbc";
	private static final String TYPE_ODBC = "odbc";
	// We default to JDBC for now
	private static final String DEFAULT_TYPE = TYPE_JDBC;
	// ^^^ WARNING!!!!
	// If you change this, you'll need to update the bottom of getDBType
	// Also better check other places DEFAULT is used, difficult to code
	// for all combinations, sorry


	// Attribute Names
	// ======================================================
	private static final String ACTIVE_ATTR =
		"active";
	private static final String SERVER_NAME_ATTR =
		"server_name";
	private static final String SERVER_PORT_ATTR =
		"port";
	private static final String DRIVER_ATTR =
		"driver_java_class";
	private static final String NET_PROTO_ATTR =
		"network_protocol";
	private static final String DB_NAME_ATTR =
		"database_name";
	private static final String MAIN_TABLE_ATTR =
		"main_table";
	private static final String USERNAME_ATTR =
		"username";
	private static final String PASSWORD_ATTR =
		"password";
	private static final String EXTRA_PARAMETERS_ATTR =
		"extra_parameters";
	private static final String VENDOR_TAG_ATTR =
		"vendor_tag";
	private static final String VENDOR_STRING_ATTR =
		"vendor_description";

	private static final String NATIVE_CONNECT_STRING_ATTR =
		"native_connection_string";

	private static final String TEST_QUERY_PATH =
		"test_query";
	private static final String GENERATE_AUTOMATIC_TEST_QUERY_ATTR =
		"count_main_table";
	private static final String IS_A_SIMPLE_COUNT_ATTR =
		"is_count_query";
	private static final String SHOW_FIRST_ROW_ATTR =
		"show_first_row";
	private static final boolean DEFAULT_SHOW_FIRST_ROW = false;
	// How long to wait before retrying a connection
	private static final String RETRY_WAIT_MS_ATTR =
		"retry_wait_ms";
	// How long to wait before retrying a connection
	// Starting with 2 minutes, this is in milliseconds
	// private static final long DEFAULT_RETRY_WAIT_MS = 120000L;
	// try 1 minute
	// private static final long DEFAULT_RETRY_WAIT_MS = 60000L;
	// 10 seconds
	private static final long DEFAULT_RETRY_WAIT_MS = 10000L;
	// A value that DISABLES retries
	private static final long DISABLE_RETRY = -1;
	// A threshold value that indicates they may be confused that
	// the unit of measure is MILLISECONDS
	private static final long RETRY_WAIT_WARNING_THRESHOLD = 2000L;

	// The default host name
	private static final String DEFAULT_SERVER_NAME = "localhost";

	// Add Driver Marker

	// Oracle
	public static final String VENDOR_TAG_ORACLE = "oracle"; // WARNING: IMPORTANT
	// ^^^ This value is used in MANY PLACES for various logic
	// both in code, and hard coded in XSLT
	// So if you ever want to make an oralce8i vs oracle11, etc. you'll need
	// to track down all of those
	private static final String ORACLE_NAME = "oracle";	// This will likely not change either
	// This is the DEFAULT driver for Oracle
	// private static final String ORACLE_CLASS_NAME = "oracle.jdbc.OracleDriver";
	// This should work with 8i
	private static final String ORACLE_CLASS_NAME =
		"oracle.jdbc.driver.OracleDriver";
	private static final int ORACLE_DEFAULT_PORT = 1521;
	// Older/fut/ssl 66, 1525, 1526, 1529?, 1575, 1830, 2483, 3872, etc
	// http://www.chebucto.ns.ca/~rakerman/oracle-port-table.html

	/*private*/ public static final String VENDOR_TAG_POSTGRESQL = "postgresql";
	private static final String POSTGRESQL_CLASS_NAME =
		"org.postgresql.Driver";
	private static final int POSTGRESQL_DEFAULT_PORT = 5432;
	private static final String POSTGRESQL_DEFAULT_SERVER = "localhost";
	public static final String POSTGRESQL_TROUBLESHOTTING_INSTRUCTIONS_FILE =
		"(nie-install-dir)/doc/postgresql-jdbc-troubleshooting.txt";

	private static final String VENDOR_TAG_MS_SQL = "sqlserver";
	private static final String MS_SQL_CLASS_NAME =
		"com.microsoft.jdbc.sqlserver.SQLServerDriver";
	// ^^^ For this driver I have seen paths like:
	// D:\Sql200JDBCDriver\lib\msbase.jar;D:\Sql200JDBCDriver\lib\msutil.jar
	// (cont) ;D:\Sql200JDBCDriver\lib\mssqlserver.jar
	private static final int MS_SQL_DEFAULT_PORT = 1433;
	// Some pain in the ass Microsoft requirements
	public static final String MS_DOWNLOAD_URL =
		"http://msdn.microsoft.com/downloads/default.asp?URL=/downloads/sample.asp?url=/MSDN-FILES/027/001/779/msdncompositedoc.xml"
		;
	public static final String MS_TROUBLESHOTTING_INSTRUCTIONS_FILE =
		"(nie-install-dir)/doc/microsoft-jdbc-troubleshooting.txt";

	// And yet more BS that we need to accommodate them
	// Whether or not we have checked for the driver
	private boolean fHasCheckedForMSSqlDriver;
	// The cached answer we get when we do check
	private boolean cHasMSSqlDriver;


	/*private*/ public static final String VENDOR_TAG_MYSQL = "mysql";
	private static final String MYSQL_CLASS_NAME =
		"com.mysql.jdbc.Driver";
		// OLD name: "org.gjt.mm.mysql.Driver";
		// Caucho driver: Also com.caucho.jdbc.mysql.Driver
		// For jdbc:mysql-caucho://localhost:3306/menagerie
	private static final int MYSQL_DEFAULT_PORT = 3306;



	// A place holder for something the user might accidently enter
	private static final String BOGUS_JDBC_VENDOR_ODBC = TYPE_ODBC;
	// When folks think they want dsn
	private static final String ODBC_VENDOR_DSN = "dsn";

	// private static final String BOGUS_VENDOR_TAG_ODBC = "odbc";
	private static final String ODBC_CLASS_NAME =
		"sun.jdbc.odbc.JdbcOdbcDriver";


	private static final void ___sep__Table_Names__(){}
	//////////////////////////////////////////////////////////////////////

	// Some Central Table Name constants
	// =====================================
	// Todo: revisit should this be centralized like this?
	public static final String TABLE_NAME_PREFIX = "nie_";
	// Name of the main logging table
	public static final String LOG_TABLE = TABLE_NAME_PREFIX + "log";	// nie_log
	// Supplementary info for the logging table
	public static final String LOG_META_TABLE = LOG_TABLE + "_meta_data";//nie_log_meta_data
	// The name of the login table
	public static final String USER_INFO_TABLE = TABLE_NAME_PREFIX + "user_info";
	// Where we keep the translations of DNS names
	public static final String DOMAIN_TABLE = TABLE_NAME_PREFIX + "domain_names";

	// Some other specific objects / entities

	// The configured sites that we know about
	public static final String SITE_TABLE = TABLE_NAME_PREFIX + "site";
	// these are the mappings, the actual entries, the actions, etc.
	// public static final String MAP_TABLE = TABLE_NAME_PREFIX + "term_map";
	public static final String MAP_TABLE = TABLE_NAME_PREFIX + "map";
	// specific terms, words, phrases, etc.
	public static final String TERM_TABLE = TABLE_NAME_PREFIX + "term";
	// the specific URLs we suggest, redirect to, advertise, etc.
	public static final String URL_TABLE = TABLE_NAME_PREFIX + "url";

	// Relationships
	// these all reprsent many-to-many relationships, with the map itself
	// as the central "hub"
	public static final String MAP_SITE_ASSOC = TABLE_NAME_PREFIX + "map_site_assoc";
	public static final String MAP_TERM_ASSOC = TABLE_NAME_PREFIX + "map_term_assoc";
	public static final String MAP_URL_ASSOC = TABLE_NAME_PREFIX + "map_url_assoc";
	public static final String MAP_RELATED_TERMS_ASSOC = TABLE_NAME_PREFIX + "map_relterm_assoc";

	// Misc object attributes that don't fit anywhere else
	public static final String META_DATA_TABLE = TABLE_NAME_PREFIX + "meta_data";


	private final static String DEFAULT_URI_XSLT_PREFIX =
		AuxIOInfo.SYSTEM_RESOURCE_PREFIX
		+ "system/db/transform/"
		;
	private final static String DB_VENDOR_FILTER_XSLT_SHEET =
		DEFAULT_URI_XSLT_PREFIX
		+ "db_vendor_filter.xslt";



	// And the static member table that holds them
	private static List fDBTables;



	private static final void ___sep__Static_Init__(){}
	//////////////////////////////////////////////////////////////////////

	// Add Driver Marker

	// Lists of JDBC and ODBC Drivers, table names, etc
	static
	{
		// Tables we use
		fDBTables = new Vector();
		fDBTables.add( LOG_TABLE );			// nie_log
		fDBTables.add( LOG_META_TABLE );	// nie_log_meta
		fDBTables.add( USER_INFO_TABLE );	// nie_user_info
		fDBTables.add( DOMAIN_TABLE );		// nie_domain_names

		fDBTables.add( SITE_TABLE );		// nie_site
		fDBTables.add( MAP_TABLE );			// nie_term_map
		fDBTables.add( TERM_TABLE );		// nie_term
		fDBTables.add( URL_TABLE );			// nie_url

		fDBTables.add( MAP_SITE_ASSOC );	// nie_map_site_assoc
		fDBTables.add( MAP_TERM_ASSOC );	// nie_map_term_assoc
		fDBTables.add( MAP_URL_ASSOC );		// nie_map_url_assoc
		fDBTables.add( MAP_RELATED_TERMS_ASSOC );	// nie_map_related_term

		fDBTables.add( META_DATA_TABLE );	// nie_meta_data



		// JDBC
		//////////////////////

		// The names of the Java driver
		fJDBCDriverNameHash = new Hashtable();

		// Oracle (also has ODBC)
		fJDBCDriverNameHash.put( VENDOR_TAG_ORACLE, ORACLE_CLASS_NAME );
		// PostgreSQL (also has ODBC)
		fJDBCDriverNameHash.put( VENDOR_TAG_POSTGRESQL, POSTGRESQL_CLASS_NAME );
		// Microsoft SQL (also has ODBC)
		fJDBCDriverNameHash.put( VENDOR_TAG_MS_SQL, MS_SQL_CLASS_NAME );
		// MySQL
		fJDBCDriverNameHash.put( VENDOR_TAG_MYSQL, MYSQL_CLASS_NAME );

		// We do NOT add the "bogus" ODBC placeholder here


		// Any default ports we allow
		/////////////////////////////////////
		fJDBCDefaultPorts = new Hashtable();
		// Oracle
		fJDBCDefaultPorts.put( VENDOR_TAG_ORACLE,
			new Integer(ORACLE_DEFAULT_PORT)
			);
		// PostgreSQL
		fJDBCDefaultPorts.put( VENDOR_TAG_POSTGRESQL,
			new Integer(POSTGRESQL_DEFAULT_PORT)
			);
		// Microsoft SQL
		fJDBCDefaultPorts.put( VENDOR_TAG_MS_SQL,
			new Integer(MS_SQL_DEFAULT_PORT)
			);
		// MySQL
		fJDBCDefaultPorts.put( VENDOR_TAG_MYSQL,
			new Integer(MYSQL_DEFAULT_PORT)
			);

		// Any OVERIDES to when the short name in the jdbc URL does not
		// match the tag we use
		///////////////////////////////////////////////////////////////
		fJDBCProtocolNameOverrides = new Hashtable();
		// Currently there are NO overrides needed
		// all the names match



		// ODBC
		/////////////////////

		// We have a number of shortcuts for ODBC Driver Names
		fODBCDriverNameHash = new Hashtable();

		// The _ODBC_ driver for SQL Server
		fODBCDriverNameHash.put( VENDOR_TAG_MS_SQL, "{SQL Server}" );

		// MS Access
		fODBCDriverNameHash.put( "access", "{MicroSoft Access Driver (*.mdb)}" );

		// Excel
		fODBCDriverNameHash.put( "excel", "{Microsoft Excel Driver (*.xls)}" );

		// dbase
		fODBCDriverNameHash.put( "dbase", "{Microsoft dBase Driver (*.dbf)}" );
		fODBCDriverNameHash.put( "dbase-word", "{Microsoft dBase VFP Driver (*.dbf)}" );

		// Foxpro
		fODBCDriverNameHash.put( "foxpro", "{Microsoft Visual FoxPro Driver (*.dbf)}" );
		fODBCDriverNameHash.put( "foxpro-word", "{Microsoft FoxPro VFP Driver (*.dbf)}" );

		// CSV
		fODBCDriverNameHash.put( "csv-text", "{Microsoft Text Driver (*.txt; *.csv)}" );

		// Oracle drivers
		fODBCDriverNameHash.put( VENDOR_TAG_ORACLE, "{Microsoft ODBC for Oracle}" );
		fODBCDriverNameHash.put( "oracle9", "{Oracle in OraHome92}" );

		// Postgres
		fODBCDriverNameHash.put( VENDOR_TAG_POSTGRESQL, "{PostgreSQL}" );

		// My SQL ? April '08
		fODBCDriverNameHash.put( VENDOR_TAG_MYSQL, "{MySQL ???}" );

		// Paradox
		// First is with a space, as shown
		fODBCDriverNameHash.put( "paradox", "{Microsoft Paradox Driver (*.db )}" );
		// They show it with a space, above, try again with no space
		fODBCDriverNameHash.put( "paradox2", "{Microsoft Paradox Driver (*.db)}" );

		// An entry for generic DSN, just so it shows up
		// This takes SPECIAL HANDLING
		fODBCDriverNameHash.put( ODBC_VENDOR_DSN, "Generic Windows ODBC DSN (User or System)" );


					// jdbc:odbc://source_name
					// "jdbc:odbc:Driver={MicroSoft Access Driver (*.mdb)};DBQ=D:test.mdb"
					// jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)};DBQ=C:\saint.mdb
					// "jdbc:odbc:DRIVER={SQL Server};DATABASE=iweb;UID=" +userid+ ";PWD=" +passwd+ ";";
					// OR they claim:
					// "jdbc:odbc:DRIVER={SQL Server};SERVER=123.456.789.123:1433;DATABASE=iweb";
					// Connection con = DriverManager.getConnection(conString, userid, passwd);
					// Maybe:
					// jdbc:odbc://whitehouse.gov:5000/CATS;PWD=Hillary
					// jdbc:odbc:Driver={SQL Server};Server=whitehouse.gov;Database=CATS

					// jdbc:odbc:Driver={Microsoft Excel Driver (*.xls)};DBQ=c:/excel/mySheet.xls;DriverID=22;READONLY=false
					// jdbc:odbc:Excel Files;DBQ=C:\\java\\data.xls

					// Maybe not
					// jdbc:odbc:Driver=MicroSoft Text Driver (*.txt; *.csv);Database=MyDatabase", "", ""


	}



	// Some Doc links
	private static final void ___sep__Misc_DOC__(){}
	//////////////////////////////////////////////////////////////////////

	// Add Driver Marker


    // Note: Use DISTINCT, not UNIQUE
    // The former is universal, and therefore we don't have
    // a method to get it, wherease the latter is old school Oracle


	// http://java.sun.com/docs/books/tutorial/jdbc/basics/index.html
	// http://java.sun.com/products/jdbc/faq.html

	// Postgres / PostgreSQL
	// They would like you to build it:
	//  http://jdbc.postgresql.org/doc.html
	// Compiled downloads
	//  http://jdbc.postgresql.org/download.html
	// We use 7.2 stable:  (7.3 was in beta as of 10/17/02)
	//      "The JDBC2 driver is intended for JDK 1.2 and JDK 1.3 environments
	//       but will run under later JDKs (1.4).
	//       It will not run under JDK 1.1. "

	// MySQL
	// http://www.mysql.com/downloads/index.html
	// http://www.mysql.com/downloads/api-jdbc.html
	// ^^^ See connector J2, was MM.MySQL or mmmsql
	// we are NOT currently using caucho
	// http://mmmysql.sourceforge.net/
	// http://www.mysql.com/downloads/download.php?file=Downloads/Connector-J/mysql-connector-java-2.0.14.tar.gz
	// http://www.mysql.com/documentation/index.html
	// TWO JDBC drivers
	// http://www.mysql.com/doc/en/Java.html

	// Microsoft
	// Tech Overview
	//  http://support.microsoft.com/default.aspx?scid=KB;EN-US;Q313100&
	// Downloading
	//  http://msdn.microsoft.com/downloads/default.asp?URL=/downloads/sample.asp?url=/MSDN-FILES/027/001/779/msdncompositedoc.xml
	//  http://download.microsoft.com/download/SQLSVR2000/Install/2.2.0022/NT5XP/EN-US/setup.exe
	//  http://download.microsoft.com/download/SQLSVR2000/Install/2.2.0022/UNIX/EN-US/mssqlserver.tar
	//  http://msdn.microsoft.com/MSDN-FILES/027/001/779/redistguide.htm

	// Misc ODBC Doc
	// Debug Info: java.sql.DriverManager.setLogStream(java.lang.System.out);
	// Gives output like:
	//trying driver[className=sun.jdbc.odbc.JdbcOdbcDriver,context=null,sun.jdbc.odbc.JdbcOdbcDriver@201cb63]<BR>
	//*Driver.connect (jdbc:odbc:WizKidJ)<BR>
	//JDBC to ODBC Bridge: Checking security<BR>
	//No SecurityManager present, assuming trusted application/applet<BR>
	//JDBC to ODBC Bridge 1.1001<BR>
	//Current Date/Time: Fri Feb 11 21:39:44 EST 2000<BR>
	//Loading JdbcOdbc library<BR>
	//Allocating Environment handle (SQLAllocEnv)<BR>
	//hEnv=551687240<BR>
	//Allocating Connection handle (SQLAllocConnect)<BR>
	//hDbc=551687408<BR>
	//Connecting (SQLDriverConnect), hDbc=551687408, szConnStrIn=DSN=WizKidJ;UID=;PWD=
	// String temps="jdbc:odbc:Excel Files;DBQ="+ClassLoader.getResource(pathToFileInJar).toString();
	// Network access via ODBC
	//You should look at the FAQ for JDBC. This answers your question.
	//Try installing MySQL it works fine and you can connect to it
	//remotely without the RMI-JDBC-ODBC.
	//5. How can I use the JDBC API to access a desktop database like Microsoft Access over the network?
	//Most desktop databases currently require a JDBC solution that uses ODBC underneath. This is because the vendors of these database products haven't implemented all-Java JDBC drivers.
	//The best approach is to use a commercial JDBC driver that supports ODBC and the database you want to use. See the JDBC drivers page for a list of available JDBC drivers.
	//The JDBC-ODBC bridge from Sun's Java Software does not provide network access to desktop databases by itself. The JDBC-ODBC bridge loads ODBC as a local DLL, and typical ODBC drivers for desktop databases like Access aren't networked. The JDBC-ODBC bridge can be used together with the RMI-JDBC bridge, however, to access a desktop database like Access over the net. This RMI-JDBC-ODBC solution is free.
	// http://forum.java.sun.com/thread.jsp?forum=48&thread=211735&start=2
	// http://forum.java.sun.com/thread.jsp?forum=48&thread=255167
	// Free driver for SQL JDBC!
	// https://sourceforge.net/projects/jtds/
	// http://www.ddtek.com

	//Runtime rt = Runtime.getRuntime();
	//rt.exec("ODBCSetup -drv \"Microsoft Excel Driver (*.xls)\" -dsn
	//testDrive -db D:\\TEMP\\sampla.xls");

}
