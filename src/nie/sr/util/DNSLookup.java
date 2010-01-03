package nie.sr.util;

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

public class DNSLookup implements Runnable
{

    ////////////////////////////////////////////////
    //
    // In case we're run as a stand-alone program
    // instead of being incorporated in another program
    //
    /////////////////////////////////////////////////
	
    static public void main( String[] inArgs )
    {
	DNSLookup lLookup = new DNSLookup();
	lLookup.parseArgs( inArgs );
	lLookup.run();
    }
    
    ////////////////////////////////////////////////
    //
    // Constructors.
    //
    // All constructors should call commonInit()
    //
    ////////////////////////////////////////////////
    
    public DNSLookup()
    {
	commonInit();
    };
    
    public void commonInit()
    {
	fOperatingMode = kMinimumMode;
	fDriverName = kDefaultDBURL;
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
    
    public void parseArgs( String[] inArgs )
    {
	for( int i = 0; i < inArgs.length; i++ )
	{
	    if( (inArgs[i].compareTo( "-m" ) == 0) ||
		(inArgs[i].compareTo( "--minimum") == 0) )
		fOperatingMode = kMinimumMode;
	    
	    else if( (inArgs[i].compareTo( "-i" ) == 0) ||
		     (inArgs[i].compareTo( "--intermediate") == 0) )
		fOperatingMode = kIntermediateMode;
	    
	    else if( (inArgs[i].compareTo( "-r" ) == 0) ||
		     (inArgs[i].compareTo( "--refresh" ) == 0) )
		fOperatingMode = kRefreshMode;
	    
	    else if( (inArgs[i].compareTo( "-f" ) == 0) ||
		     (inArgs[i].compareTo( "--full" ) == 0) )
		fOperatingMode = kFullMode;
	    
	    else if( (inArgs[i].compareTo( "-d" ) == 0) ||
		     (inArgs[i].compareTo( "--driver" ) == 0) )
		fDriverName = inArgs[++i];
	    
	    else if( (inArgs[i].compareTo( "-p" ) == 0) ||
		     (inArgs[i].compareTo( "--password" ) == 0) )
		fPassword = inArgs[++i];
	    
	    else if( (inArgs[i].compareTo( "-a" ) == 0) ||
		     (inArgs[i].compareTo( "--account" ) == 0) )
		fAccount = inArgs[++i];
	    
	    else if( (inArgs[i].compareTo( "-g" ) == 0) ||
		     (inArgs[i].compareTo("--debug" ) == 0) )
	    {
		System.out.println( "Enabling debug messages" );
		System.out.flush();
		gDebug = true;
	    }

	    else
		fOperatingMode = kErrorMode;
	};
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
	TreeSet lCandidateIPNumbersTreeSet = null;
	TreeSet lShouldNotResolveTreeSet = null;
	TreeSet lDoNotChangeToUnknownTreeSet = null;
	
	try
	{
	    ///////
	    //
	    // Connect to the database
	    //
	    ///////
			
	    System.out.println( "Connecting using URL: " + fDriverName );
	    Connection lConnection =
		DriverManager.getConnection( fDriverName,
					     fAccount,
					     fPassword );
	    Statement lStatement = lConnection.createStatement();
			
	    ///////
	    //
	    // Get the list of candidate IP numbers
	    //
	    ///////
	    
	    if( gDebug )
	    {
		System.out.println( "Executing SQL:\n" + kGetCandidateIPNumbers );
		System.out.flush();
	    }

	    ResultSet lCandidateIPNumbersResultSet =
		lStatement.executeQuery( kGetCandidateIPNumbers );

	    if( gDebug )
	    {
		System.out.println( "Result Set returned." );
		System.out.flush();
	    }

	    lCandidateIPNumbersTreeSet =
		buildIPTreeSet( lCandidateIPNumbersResultSet );

	    lCandidateIPNumbersResultSet = null;
	    
	    ///////
	    //
	    // For the various operating  modes, get the lists of:
	    //		IP Numbers that should not be resolved
	    //		IP Numbers that should not be set to unknown
	    //
	    ///////
	    
	    //
	    // Strings to hold the SQL statement to get the above lists
	    //
	    
	    String lShouldNotResolveSQLText = null;
	    String lDoNotChangeToUnknownSQLText = null;
	    
	    //
	    // Set the strings to the right values for different
	    // operating modes.
	    //
	    
	    switch( fOperatingMode )
	    {
		case kMinimumMode:
		    if( gDebug )
		    {
			System.out.println( "Running in minimum mode. (processing only IP numbers in log that do not apepar in domainnames)" );
			System.out.flush();
		    }

		    lShouldNotResolveSQLText =
			kBasicModeShouldNotResolveSQLText;
		    lDoNotChangeToUnknownSQLText =
			kBasicDoNotChangeToUnknownSQLText;
		    break;
		    
	        case kIntermediateMode:
		    if( gDebug )
		    {
			System.out.println( "Running in intermediate mode (IP numbers that we've not seen before OR that have not been previously successfully resolved)" );
			System.out.flush();
		    }

		    lShouldNotResolveSQLText =
			kIntermediateModeShouldNotResolveSQLText;
		    lDoNotChangeToUnknownSQLText =
			kIntermediateDoNotChangeToUnknownSQLText;
		    break;
			
	        case kRefreshMode:
		    if( gDebug )
		    {
			System.out.println( "Running in refresh mode (process all IP numbers in log.  This will NOT change an already identified IP number to unidentified but will update it if we can resolve it during this run)" );
			System.out.flush();
		    }

		    lShouldNotResolveSQLText =
			kRefreshModeShouldNotResolveSQLText;
		    lDoNotChangeToUnknownSQLText =
			kRefreshDoNotChangeToUnknownSQLText;
		    break;
				
	        case kFullMode:
		    if( gDebug )
		    {
			System.out.println( "Running in full mode (process all IP numbers in log.  This WILL change an already identified IP number to unidentified if we can't update it during this run)" );
			System.out.flush();
		    }

		    lShouldNotResolveSQLText =
			kFullModeShouldNotResolveSQLText;
		    lDoNotChangeToUnknownSQLText =
			kFullDoNotChangeToUnknownSQLText;
		    break;
				
	        default:
		    if( gDebug )
		    {
			System.out.println( "Operating mode = " + fOperatingMode );
			System.out.flush();
		    }

		    System.err.println( "Invalid operating mode!!" );
		    System.exit(-1);
		    break;
	    }
			
	    //
	    // Set up the list of IP numbers that should be ignored.
	    // This MAY be an empty tree set.
	    
	    if( gDebug )			
		System.out.println( "Executing Query:\n" + lShouldNotResolveSQLText );
	    
	    if( lShouldNotResolveSQLText != null )
		lCandidateIPNumbersResultSet =
		    lStatement.executeQuery( lShouldNotResolveSQLText );

	    lShouldNotResolveTreeSet =
		buildIPTreeSet( lCandidateIPNumbersResultSet );
	    
	    // Set up the list of IP numbers whose server should not be
	    // changed to unknown if they already exist.
	    
	    if( gDebug )
		System.out.println( "Executing Query: " + lDoNotChangeToUnknownSQLText );

	    if( lDoNotChangeToUnknownSQLText != null )
		lCandidateIPNumbersResultSet =
		    lStatement.executeQuery( lDoNotChangeToUnknownSQLText );

	    lDoNotChangeToUnknownTreeSet =
		buildIPTreeSet( lCandidateIPNumbersResultSet );
	    
	    // Now that the lists are set up, do the actual work.
	    // We'll use a method for this as it's possible that
	    // someone else might want to call this externally after
	    // setting up their own lists.
	    
	    doProcessIPNumbers( lConnection,
				lCandidateIPNumbersTreeSet,
				lShouldNotResolveTreeSet,
				lDoNotChangeToUnknownTreeSet );
	    
	} catch( SQLException se ) {
	    System.err.println( "SQL Exception caught:" );
	    System.err.println( se );
	    se.printStackTrace();
	} catch( Exception e ) {
	    System.err.println( "Mundane exception caught:" );
	    System.err.println( e );
	    e.printStackTrace();
	    System.exit(-1);
	};
    }
    
    ///////////////////////////////////////////////////////////
    //
    // Process the three lists, updating the domainnames table.
    //
    ///////////////////////////////////////////////////////////
    
    void doProcessIPNumbers( Connection inDBConnection,
			     TreeSet inCandidates,
			     TreeSet inDoNotResolve,
			     TreeSet inDoNotUpdateToUnknown )
    {
	Iterator lCandidateIPNumbersIterator = inCandidates.iterator();
	Object lCandidateIPObject = null;
	try
	{
	    Statement lStatement = inDBConnection.createStatement();
	    while( lCandidateIPNumbersIterator.hasNext() )
	    {
		lCandidateIPObject = lCandidateIPNumbersIterator.next();
		DotNotation lCandidateIPNumber = (DotNotation)lCandidateIPObject;
		if( !inDoNotResolve.contains( lCandidateIPNumber ) )
		{
		    //	
		    // Resolve the IP number in lCandidateIPNumber
		    //	
					
		    InetAddress lAddress =
			lCandidateIPNumber.asInetAddress();
		    if( lAddress != null )
		    {
			String lHostName = lAddress.getHostName();
			if( lHostName.compareTo( lCandidateIPNumber.asString() ) == 0 )
			    doSetUnknown( lStatement, lCandidateIPNumber, inDoNotUpdateToUnknown );
			else if( lHostName.compareTo( lCandidateIPNumber.asString() ) != 0 )
			    doSetHost( lStatement, lAddress );
		    } else
			doSetUnknown( lStatement, lCandidateIPNumber, inDoNotUpdateToUnknown );
		}
	    }
	} catch (SQLException se) {
	    System.err.println( "Caught SQL Exception: " + se );
	    se.printStackTrace();
	    System.exit( -1 );
	}
	return;
    }
    
    ///////////////////////////////////////////////////////////
    //
    // Set a given IP number to "unknown"
    //
    ///////////////////////////////////////////////////////////

    public void doSetUnknown( Statement inStatement,  DotNotation inIPNumber, TreeSet inDoNotUpdateToUnknown )
    {
    	String lIPNumber = inIPNumber.asString();
    	
    	if( gDebug )
	    System.out.println( "Unknown IP: " + lIPNumber );

    	String lNewHostName = "Unknown " + inIPNumber.asString();
    	String lSQLInsertText = replaceAll( kInsertDomainNameSQL, kHOSTNAME, lNewHostName );
    	lSQLInsertText = replaceAll( lSQLInsertText, kIPNUMBER, lIPNumber );
    	lSQLInsertText = replaceAll( lSQLInsertText, kRESOLVED, kWAS_NOT_RESOLVED );
    	
    	if( gDebug )
	    {
		System.out.println( "Trying to insert using SQL statement: " );
		System.out.println( lSQLInsertText );
	    };
	
    	try
	{
	    inStatement.execute( lSQLInsertText );
	} catch( SQLException se ) {
	    // Perhaps the record is already in there and we need to update it.
	    // Check against the DoNotUpdateToUnknown list before doing that!!
	    
	    if( ! inDoNotUpdateToUnknown.contains( inIPNumber ) )
	    {
		String lSQLUpdateText = replaceAll( kUpdateDomainNameSQL, kHOSTNAME, lNewHostName );
		lSQLUpdateText = replaceAll( lSQLUpdateText, kIPNUMBER, lIPNumber );
		lSQLUpdateText = replaceAll( lSQLUpdateText, kRESOLVED, kWAS_NOT_RESOLVED );
		
		if( gDebug )
		{
		    System.out.println( "Trying to update using SQL statement: " );
		    System.out.println( lSQLUpdateText );
		}
		
		try
		{
		    inStatement.execute( lSQLUpdateText );
		}
		catch( SQLException se1 )
		{
		    System.err.println( "Could not update database - got two SQL exceptions in a row." );
		    System.err.println( "First SQL Statement was: \n" + lSQLInsertText );
		    System.err.println( "First exception was: " + se );
		    se.printStackTrace();
		    System.err.println( "------------------------" );
		    System.err.println( "Second SQL Statement was\n" + lSQLUpdateText );
		    System.err.println( "Second exception was: " + se1 );
		    se1.printStackTrace();
		    System.exit( -1 );
		}
	    }
	}
    }
    
    ///////////////////////////////////////////////////////////
    //
    // Set a given IP number to resolved... We did it!
    //
    ///////////////////////////////////////////////////////////
    
    public void doSetHost( Statement inStatement, InetAddress inIPNumber )
    {
    	String lHostName = inIPNumber.getHostName();
    	String lIPNumber = inIPNumber.getHostAddress();
    	
    	String lSQLInsertText = replaceAll( kInsertDomainNameSQL, kHOSTNAME, lHostName );
    	lSQLInsertText = replaceAll( lSQLInsertText, kIPNUMBER, lIPNumber );
    	lSQLInsertText = replaceAll( lSQLInsertText, kRESOLVED, kWAS_RESOLVED );
    	
    	if( gDebug )
    	{
	    System.out.println( "Trying to insert using SQL statement: " );
	    System.out.println( lSQLInsertText );
	}
		
    	try
    	{
	    inStatement.execute( lSQLInsertText );
    	} catch( SQLException se1 ) {
	    String lSQLUpdateText = replaceAll( kUpdateDomainNameSQL, kHOSTNAME, lHostName );
	    lSQLUpdateText = replaceAll( lSQLUpdateText, kIPNUMBER, lIPNumber );
	    lSQLUpdateText = replaceAll( lSQLUpdateText, kRESOLVED, kWAS_RESOLVED );
	    
	    if( gDebug )
	    {
		System.out.println( "Trying to update using SQL statement: " );
		System.out.println( lSQLUpdateText );
	    }
	    
	    try
    	    {
		inStatement.execute( lSQLUpdateText );
	    } catch( SQLException se2 ) {
		System.err.println( "Could not update database - got two SQL exceptions in a row." );
		System.err.println( "First SQL Statement was: \n" + lSQLInsertText );
		se1.printStackTrace();
		System.err.println( "------------------------" );
		System.err.println( "Second SQL Statement was\n" + lSQLUpdateText );
		se2.printStackTrace();
		System.exit( -1 );
	    }
	}
		
	if( gDebug )
	    System.out.println( "Resolved " + lIPNumber + " to " + lHostName );
    }

    ///////////////////////////////////////////////////////////
    //
    // Early Java VMs (like the one that is distributed with 
    // Oracle 9i server) is 1.2.x, which is missing numerous
    // functions.  These are replacements.
    //
    ///////////////////////////////////////////////////////////

    static String replaceAll( String inSourceString, String inSearchString, String inReplacementString )
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
    //
    // Build a tree set from a result set.  If the input result
    // set is null, then we return an initialized, but empty,
    // tree set.
    //
    ///////////////////////////////////////////////////////////
    
    private TreeSet buildIPTreeSet( ResultSet inResultSet )
    {
	if( gComparator == null )
	    gComparator = new DotNotationComparator();
	
	TreeSet lTreeSet = new TreeSet( gComparator );
	
	if( inResultSet != null )
	{
	    try
	    {
		while( inResultSet.next() )
		{
		    String lIPNumberString;
		    
		    lIPNumberString = inResultSet.getString( 1 );
		    if( gDebug ) {
			System.out.println( "Converting '" + lIPNumberString + "'" );
			System.out.flush();
		    }
		    DotNotation lDotNotation = new DotNotation( lIPNumberString );
		    lTreeSet.add( lDotNotation );
		}
	    } catch( SQLException se ) {
		System.err.println( "SQL Exception caught: " + se );
		se.printStackTrace();
		System.exit( -1 );
	    }
	}
	return lTreeSet;
    }
    
    ///////////////////////////////////////////////////////////
    //
    // Private members...
    //
    ///////////////////////////////////////////////////////////
    
    int fOperatingMode = kMinimumMode;
    String fDriverName = null;
    String fPassword = "kklop";
    String fAccount = "kklop";
    
    private static Comparator gComparator = null;
    
    static final int kErrorMode = 0;
    static final int kMinimumMode = 1;
    static final int kIntermediateMode = 2;
    static final int kRefreshMode = 3;
    static final int kFullMode = 4;
    
    static final String kDefaultDBURL = "jdbc:oracle:thin:@bigmomma:1521:stack";
    
    ////////////////////////////////////////////////////////////
    //
    // SQL Statements...
    //
    ////////////////////////////////////////////////////////////
    
    static final String kGetCandidateIPNumbers =
	"SELECT DISTINCT client_host " +
	"FROM log " +
	"WHERE client_host IS NOT NULL " +
	"ORDER BY client_host " +
	"";
    
    static final String kBasicModeShouldNotResolveSQLText =
	"SELECT DISTINCT client_host " +
	"FROM domainnames " +
	"WHERE client_host IS NOT NULL " +
	"ORDER BY client_host " +
	"";
    
    static final String kBasicDoNotChangeToUnknownSQLText = null;
    
    static final String kIntermediateModeShouldNotResolveSQLText =
	"SELECT DISTINCT client_host " +
	"FROM domainnames " +
	"WHERE resolved = 1 " +
	"  AND client_host IS NOT NULL " +
	"ORDER BY dn_client_host " +
	"";
    
    static final String kIntermediateDoNotChangeToUnknownSQLText = null;
    
    static final String kRefreshModeShouldNotResolveSQLText = null;
    
    static final String kRefreshDoNotChangeToUnknownSQLText =
	"SELECT DISTINCT client_host " +
	"FROM domainnames " +
	"WHERE resolved = 1 " +
	"  AND client_host IS NOT NULL " +
	"ORDER BY client_host " +
	"";
    
    static final String kFullModeShouldNotResolveSQLText = null;
    
    static final String kFullDoNotChangeToUnknownSQLText = null;
    
    static final String kHOSTNAME = "VAR_HOSTNAME";
    static final String kIPNUMBER = "VAR_IPNUMBER";
    static final String kRESOLVED = "VAR_RESOLVED";
    static final String kWAS_RESOLVED = "1";
    static final String kWAS_NOT_RESOLVED = "0";
    
    static final String kInsertDomainNameSQL =
    	"INSERT INTO domainnames " +
    	"VALUES ('" + kIPNUMBER + "','" + kHOSTNAME + "', SYSDATE, " + kRESOLVED + " ) " +
    	"";
    
    static final String kUpdateDomainNameSQL =
    	"UPDATE domainnames " +
    	"SET dns_name = '" + kHOSTNAME + "', " +
    	"    lookup_date = SYSDATE, " +
    	"    resolved = " + kRESOLVED + " " +
    	"WHERE client_host = '" + kIPNUMBER + "' " +
    	"";
    	
    public static boolean gDebug = false;
}
