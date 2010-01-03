package nie.sr.util;

import nie.sr.*;

import java.sql.*;
import java.io.*;
import java.util.*;

public class trendReport
{
    /******************************************************
     * Entry point so that we can run it stand-alone
     *****************************************************/

    public static void main(String inArgs[] )
    {
	if( inArgs.length == 0 )
	{
	    System.out.println( "Usage:\n    trendReport interval [config_file]\n\n     where:\n\n          interval is the number of days in the interval\n          config_file is the optional path to the configuration file.\n" );
	    System.exit( 0 );
	}

	trendReport lTrendReport = new trendReport();
	lTrendReport.setInterval( inArgs[0] );

	lTrendReport.setConfig( new SRConfig() );

	try
	{
	    if( inArgs.length > 1 )
		lTrendReport.getConfig().SRConfigInit( inArgs[1] );
	    else
		lTrendReport.getConfig().SRConfigInit( kDefaultConfigFileName );
	} catch( nie.sr.SRException sce ) {
	    SRConfig.doErrorMsg( "trendReport", "process", "Exception loading configuration file '" +
				kDefaultConfigFileName + "'\n" +
				"Error string is: \n\n" +
				sce +"\n" );
	    return;
	}

	if( lTrendReport.getConfig() == null )
	{
	    nie.sr.SRConfig.doErrorMsg( "trendReport", "process", "fConfig is null" );
	    return;
	}

	lTrendReport.process( System.out );
    }

    /****************************************************
     * Set the number of days to use in the interval
     ***************************************************/

    /****************************************************
     * Constructor
     ***************************************************/

    public trendReport()
    {
    }

    /*******************************************************
     * Get dates into a string to deal with Oracle's
     * fershlugginer handling of time zones and such
     ******************************************************/

    String getDateString( Calendar inCalendar )
    {
	int lDay = inCalendar.get(Calendar.DAY_OF_MONTH);
	int lMonth = inCalendar.get(Calendar.MONTH);
	int lYear = inCalendar.get(Calendar.YEAR) % 100;

	String retString = "" + lDay + "-" + kMonthName[lMonth] + "-";

	if( lYear < 10 )
	    retString += "0";

	retString = retString + lYear;

	return retString;
    }

    /*******************************************************
     * Main processing section
     ******************************************************/

    public void process( PrintStream inOutput )
    {
	Calendar lCalendar = Calendar.getInstance();
	String lCurrentIntervalEnd = getDateString( lCalendar );

	lCalendar.add( Calendar.DATE, -getInterval() );
	String lCurrentIntervalStart = getDateString( lCalendar );

	lCalendar.add( Calendar.DATE, -getInterval() );
	String lPreviousIntervalStart = getDateString( lCalendar );

	if( getConfig() == null )
	{
	    return;
	}

	try
	{
	    Connection lConnection = getConfig().getConnection();

	    String lSQL =
		"SELECT *\n" +
		"FROM (\n" +
		"	SELECT  query,\n" +
		"		num_requests,\n" +
		"		ROWNUM as thePosition\n" +
		"	FROM (\n" +
		"		SELECT normalized_query AS query,\n" +
		"			count(*) AS num_requests\n" +
		"		FROM log\n" +
		"		WHERE start_time >= '" + lCurrentIntervalStart + "'\n" +
		"				     AND start_time <='" + lCurrentIntervalEnd + "'\n" +
		"		GROUP BY normalized_query\n" +
		"		ORDER BY count(*) DESC, normalized_query\n" +
		"	)\n" +
		")\n" +
		"WHERE thePosition < 11\n" +
		"";

	    SRConfig.doDebugMsg( "trendReport", "process", lSQL );

	    Statement lThisTimeIntervalStatement = lConnection.createStatement();
	    SRConfig.doDebugMsg( "trendReport", "process", "got connection." );
	    Statement lPreviousTimeIntervalStatement = lConnection.createStatement();
	    SRConfig.doDebugMsg( "trendReport", "process", "got statement." );
	    ResultSet lThisTimeIntervalResultSet = lThisTimeIntervalStatement.executeQuery( lSQL );
	    SRConfig.doDebugMsg( "trendReport", "process", "executed query." );

	    inOutput.println( "<table>\n" +
			      "	<tr>\n" +
			      "		<td colspan=4 align='center'>\n" +
			      "			<font size='-1'>Popular Searches For Last " + getInterval() + " Days</font>\n" +
			      "		</td>\n" +
			      "	</tr>\n" +
			      "	<tr>\n" +
			      "		<td align='left' valign='bottom'>\n" +
			      "			<font size='-1'>Search Term</font><br><hr>\n" +
			      "		</td>\n" +
			      "		<td align='center' valign='bottom'>\n" +
			      "			<font size='-1'>Change</font><br><hr>\n" +
			      "		</td>\n" +
			      "		<td align='center' valign='bottom'>\n" +
			      "			<font size='-1'>Last " + getInterval() + " days</font><br><hr>\n" +
			      "		</td>\n" +
			      "		<td align='center' valign='bottom'>\n" +
			      "			<font size='-1'>Prev " + getInterval() + " days</font><br><hr>\n" +
			      "		</td>\n" +
			      "		<td>\n" +
			      "			&nbsp;&nbsp;&nbsp;\n" +
			      "		</td>\n" +
			      "	</tr>\n" );
	    inOutput.flush();

	    SRConfig.doDebugMsg( "trendReport", "process", "starting loop" );

	    while( lThisTimeIntervalResultSet.next() )
	    {
		SRConfig.doDebugMsg( "trendReport", "process", "Inside while loop." );
		String lQueryTerm = lThisTimeIntervalResultSet.getString( "query" );
		SRConfig.doDebugMsg( "trendReport", "process", "Query Term = '" + lQueryTerm + "'" );
		int lNumResults = lThisTimeIntervalResultSet.getInt( "num_requests" );
		SRConfig.doDebugMsg( "trendReport", "process", "Num Results = '" + lNumResults + "'" );
		int lCurrentPosition = lThisTimeIntervalResultSet.getInt( "thePosition" );
		SRConfig.doDebugMsg( "trendReport", "process", "Current Position = '" + lCurrentPosition + "'" );

		lSQL =
		    "SELECT *\n" +
		    "FROM (\n" +
		    "	SELECT  query,\n" +
		    "		num_requests,\n" +
		    "		ROWNUM as thePosition\n" +
		    "	FROM (\n" +
		    "		SELECT normalized_query AS query,\n" +
		    "			count(*) AS num_requests\n" +
		    "		FROM log\n" +
		    "		WHERE start_time BETWEEN '" + lPreviousIntervalStart + "'\n" +
		    "				     AND '" + lCurrentIntervalStart + "'\n" +
		    "		GROUP BY normalized_query\n" +
		    "		ORDER BY count(*) DESC, normalized_query\n" +
		    "	)\n" +
		    ")\n" +
		    "WHERE query='" + lQueryTerm + "'\n" +
		    "";

		SRConfig.doDebugMsg( "trendReport", "process", lSQL );
	    
		ResultSet lOldTimeIntervalResultSet = lPreviousTimeIntervalStatement.executeQuery( lSQL );
		SRConfig.doDebugMsg( "trendReport", "process", "returned from inner sql execution" );

		int lOldPosition;
		String lOldPositionString = "";
		String lGraphicName;
		String lGraphicDescription;

		try
		{
		    lOldTimeIntervalResultSet.next();
		    lOldPosition = lOldTimeIntervalResultSet.getInt( "thePosition" );
		    lOldPositionString = "" + lOldPosition;

		    if( lOldPosition > lCurrentPosition )
		    {
			lGraphicName = "moving-up.gif";
			lGraphicDescription = "Moved Up";
		    }
		    else if( lOldPosition < lCurrentPosition )
		    {
			lGraphicName = "moving-down.gif";
			lGraphicDescription = "Moved Down";
		    } else {
			lGraphicName = "did-not-move.gif";
			lGraphicDescription = "No Movement";
		    }
		} catch( Exception e ) {
		    lGraphicName = "new-1.gif";
		    lGraphicDescription = "New";
		    lOldPositionString = "-";
		}

		if( lQueryTerm.length() > 22 )
		    lQueryTerm = lQueryTerm.substring( 0, 22 ) + "...";

		inOutput.println( "	<tr>\n" +
				  "		<td align='left'><font size='-1'>" + lQueryTerm + "</font></td>\n" +
				  "		<td align='center'><img src='images/" + lGraphicName + "' alt='" + lGraphicDescription + "'></td>\n" +
				  "		<td align='center'><font size='-1'>" + lCurrentPosition + "</font></td>\n" +
				  "		<td align='center'><font size='-1'>" + lOldPositionString + "</font></td>\n" +
				  "	</tr>\n" +
				  "" );
	    }

	    SRConfig.doDebugMsg( "trendReport", "process", "Completed Loop." );
     
	    inOutput.println( "	<tr>\n" +
			      "		<td align='center' colspan='4'>\n" +
			      "			<center><hr width='70%'></center>\n" +
			      "			<img src='images/legend.gif' alt='Legend'>\n" +
			      "		</td>\n" +
			      "	</tr>\n" +
			      "</table>\n" +
			      "" );
	} catch (SQLException se) {
	    SRConfig.doErrorMsg( "trendReport", "process", "" + se );
	    StringWriter lStringWriter = new StringWriter();
	    PrintWriter lPrintWriter = new PrintWriter( lStringWriter );

	    lPrintWriter.println( "Caught SQL Exception: " + se );
	    se.printStackTrace( lPrintWriter );
	    SRConfig.doErrorMsg( "trendReport", "process", lStringWriter.getBuffer().toString() );
	}
    }

    /***************************************************************
     * Setters and Getters
     **************************************************************/

    public void setConfig( SRConfig inConfig ) {
	fConfig = inConfig;
    };

    public SRConfig getConfig() { return fConfig; };

    public void setInterval( String inInterval ) { setInterval( Integer.parseInt( inInterval ) ); }
    public void setInterval( int inInterval )    { fInterval = inInterval; }
    public int getInterval() { return fInterval; };

    /***************************************************************
     * Constants
     **************************************************************/

    static final String kMonthName[] = {
	"JAN",	"FEB",	"MAR",	"APR",	"MAY",	"JUN",
	"JUL",	"AUG",	"SEP",	"OCT",	"NOV",	"DEC" };

    static final String kDefaultConfigFileName = "/tmp/sr_config.xml";

    /***************************************************************
     * Instance variables
     **************************************************************/

    int fInterval;
    SRConfig fConfig;
}
