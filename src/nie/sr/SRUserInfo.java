package nie.sr;

import java.sql.*;
import nie.core.*;

public class SRUserInfo
{
	static final String kClassName = "SRUserInfo";

    public SRUserInfo()
    {
		setWhichTrendReport( kWeeklyTrendReportNumber );
		setSecurityLevel( -1 );
    }

    /********************************************************
     * Fill out the user info from a record retrieved from the
     * database.
     *******************************************************/

	public void fillFromResultSet( ResultSet inResultSet )
	{
		final String kFName = "fillFromResultSet";
		String lString;
		if( inResultSet != null )
		{
		    try
		    {
				lString  = inResultSet.getString( "user_id" );
				debugMsg( kFName,
					"Read '" + lString + "' for the userID from the result set."
					);
				setUserID( Integer.parseInt( lString ) );
		
				lString = inResultSet.getString( "user_name" );
				debugMsg( kFName,
					"Read '" + lString + "' for the user_name from the result set."
					);
				setUserName( lString );
				
				lString = inResultSet.getString( "full_name" );
				debugMsg( kFName,
					"Read '" + lString + "' for the full_name from the result set."
					);
				if( lString != null )
				    setFullName( lString );
				else
				    setFullName( getUserName() );
		
				lString = inResultSet.getString( "security_level" );
				debugMsg( kFName,
					"Read '" + lString
					+ "' for the security_level from the result set."
					);
				setSecurityLevel( Integer.parseInt( lString ) );
		    }
		    catch( SQLException se )
		    {
		    	errorMsg( kFName, "SQL Exception: " + se );
		    }
		}
	}

    static void debugMsg( String inMethodName, String inMessage )
    {
		SRConfig.doDebugMsg( kClassName, inMethodName, inMessage );
    }
    static void errorMsg( String inMethodName, String inMessage )
    {
		SRConfig.doErrorMsg( kClassName, inMethodName, inMessage );
    }

    /**********************************************************
     * Status routines - like checking if the data really
     * has been loaded yet or not.
     *********************************************************/
    
    public boolean isLoaded()
    {
	return (getUserID() != 0) &&
	    (getFullName() != null);
    }

    /**********************************************************
     * Getters and Setters
     *********************************************************/
	 
    public void setSecurityLevel( int inSecurityLevel ) { fSecurityLevel = inSecurityLevel; }
    public int getSecurityLevel()						{ return fSecurityLevel; }
    public void setNumberOfTries( int inNumberOfTries ) { fNumberOfTries = inNumberOfTries; }
    public int getNumberOfTries()						{ return fNumberOfTries; }
    public void setUserName( String inUserName )		{ fUserAccountName = inUserName; };
    public String getUserName()							{ return fUserAccountName; };
    public void setFullName( String inFullName ) { fUserFullName = inFullName; };
    public String getFullName() { return fUserFullName; };
    public void setUserID( long inUserID ) { fUserID = inUserID; };
    public long getUserID() { return fUserID; };
    public void setWhichTrendReport( int inTrendReport ) { fWhichTrendReport = inTrendReport; };
    public int getWhichTrendReport() { return fWhichTrendReport; }

    /***********************************************************
     * Constants available to other people
     **********************************************************/

    public static final int kAdministratorSecurityLevel = 100;
    public static final int kDisabledSecurityLevel = -1;
    public static final int kUserSecurityLevel = 0;
    public static final int kDailyTrendReportNumber = 0;
    public static final int kWeeklyTrendReportNumber = 1;
    public static final int kMonthlyTrendReportNumber = 2;
    public static final int kMinTrendReportNumber = kDailyTrendReportNumber;
    public static final int kMaxTrendReportNumber = kMonthlyTrendReportNumber;
    
    /***********************************************************
     * Instance variables
     **********************************************************/
	 
    int fSecurityLevel;
    int fNumberOfTries;
    int fWhichTrendReport;
    long fUserID;
    String fUserFullName;
    String fUserAccountName;
}
