package nie.sr.reports;

import nie.sr.*;
import java.sql.*;
import java.util.*;

public class ReportClass_mfv extends SRReport
{
    /************************************************************
     * Constructor - required in order to call the super class
     * with the proper parameters
     ***********************************************************/

    public ReportClass_mfv( modelMain inModel )
    {
	super( inModel );
    }

    /***********************************************************
     * Return the title of this report
     **********************************************************/

    public String getReportTitle() { return "Most Frequent Visitors"; };

    /***********************************************************
     * Prepare the report's main SQL Statement
     **********************************************************/

    public String prepareSQL()
    {
	return "SELECT a.*, b.dns_name\n" +
	       "FROM (\n" +
	       "	SELECT client_host, visit_count, most_recent, rownum AS thePosition\n" +
	       "	FROM (\n" +
	       "		SELECT client_host,\n" +
	       "			count(*) AS visit_count,\n" +
	       "			max(start_time) AS most_recent\n" +
	       // "		FROM log\n" +
	       "		FROM " + getLogTableName() + "\n" +
	       "		" + SRFilter.kWhereMarker + "\n" +
	       "		GROUP BY client_host\n" +
	       "		ORDER BY visit_count " + SRReport.kDirectionMarker + ", client_host\n" +
	       "	)\n" +
	       ") a, domainnames b\n" +
	       "WHERE (thePosition >=" + SRReport.kStartRowMarker + " AND thePosition < " + SRReport.kEndRowMarker + "+1)\n" +
	       "  AND (a.client_host = b.client_host (+))\n" +
	       "ORDER BY visit_count " + SRReport.kDirectionMarker + ", a.client_host\n";
    }

    /************************************************************
     * Get the default direction for rows in the results.
     ***********************************************************/

    public String getDefaultDirection() { return "desc"; };

    /************************************************************
     * Format the result set for output
     ***********************************************************/

    public String formatResults( ResultSet inResultSet ) throws SRException
    {
	String retString = "<table border=0>\n" +
	    "	<tr>\n" +
	    "		" + formatSortableHeaderItem("Number Of Visits", "center") +
	    "		" + formatHeaderItem("Visitor", "center") +
	    "		" + formatHeaderItem("Most recent visit", "center") +
	    "	</tr>\n";
	try
	{
	    String lHostName;
	    int lNumVisits;
	    Timestamp lMostRecent;
	    fRowCount = 0;

	    while( inResultSet.next() )
	    {
		fRowCount++;
		lHostName = inResultSet.getString("dns_name");
		if( (lHostName == null) || (lHostName.compareTo("") == 0) )
		    lHostName = inResultSet.getString( "client_host" );
		lNumVisits = inResultSet.getInt( "visit_count" );
		lMostRecent = inResultSet.getTimestamp( "most_recent" );

		retString += "	<tr>\n" +
		    "		<td align=\"center\" bgcolor=\"" + getBGColor() + "\">" + lNumVisits + "</td>\n" +
		    "		<td bgcolor=\"" + getBGColor() + "\">" + drillDown( "host_detail", "host_name", lHostName ) + lHostName + endDrillDown( "host_name", lHostName ) + "</td>\n" +
		    "		<td bgcolor=\"" + getBGColor() + "\">" + formatDate(lMostRecent) + "</td>\n" +
		    "	</tr>\n";

		nextRow();
	    }

	    retString = retString +
		"	<tr>\n" +
		"</table>\n";

	    return retString;
	} catch( SQLException se ) {
	    throw new SRException( se );
	}
    }
}
