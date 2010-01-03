package nie.sr.reports;

import nie.sr.*;
import java.sql.*;
import java.util.*;

public class ReportClass_query_detail extends SRReport
{
    /************************************************************
     * Constructor - required in order to call the super class
     * with the proper parameters
     ***********************************************************/

    public ReportClass_query_detail( modelMain inModel )
    {
	super( inModel );
    }

    /***********************************************************
     * Return the title of this report
     **********************************************************/

    public String getReportTitle() { return "Query Detail Report"; };

    /***********************************************************
     * Prepare the report's main SQL Statement
     **********************************************************/

    public String prepareSQL()
    {
	String lSearchTerm = getModel().getSearchTerm();
	SRConfig.getSoleInstance().doDebugMsg( "ReportClass_query_detail", "prepareSQL", "getSearchTerm returned " + lSearchTerm );
	return
	    "SELECT *\n" +
	    "FROM (\n" +
	    "	SELECT rownum AS thePosition,\n" +
	    "		original_query,\n" +
	    "		a.client_host,\n" +
	    "		referer,\n" +
	    "		num_results,\n" +
	    "		start_time,\n" +
	    "		was_search_names_term as search_term_indicator,\n" +
	    "		dns_name\n" +
	    // "	FROM log a, domainnames b\n" +
	    "	FROM " + getLogTableName() + " a, domainnames b\n" +
	    "	WHERE normalized_query='" + getModel().getSearchTerm() + "' " + SRFilter.kAdditionalWhereMarker + "\n" +
	    "	  AND a.client_host = b.client_host (+)\n" +
	    "	ORDER BY start_time " + SRReport.kDirectionMarker + "\n" +
	    ")\n" +
	    "WHERE (thePosition >= " + SRReport.kStartRowMarker + " AND thePosition < " + SRReport.kEndRowMarker + "+1) " +
	    "ORDER BY thePosition ASC " +
	    "";
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
	    "		" + formatHeaderItem("Term", "left") + "\n" +
	    "		" + formatSortableHeaderItem( "Search Time", "center") + "\n" +
	    "		" + formatHeaderItem( "Client Host", "left") + "\n" +
	    "		" + formatHeaderItem( "Referrer", "left" ) + "\n" +
	    "		" + formatHeaderItem( "Num Results", "center") + "\n" +
	    "	</tr>\n";
	try
	{
	    String lQuery;
	    String lQueryFont;
	    String lQueryFontEnd;
	    String lHost;
	    String lReferer;
	    int lNumResults;
	    String lDateStamp;
	    int lWasSearchNamesTerm;
	    fRowCount = 0;

	    while( inResultSet.next() )
	    {
		fRowCount++;
		lQuery = inResultSet.getString( "original_query" );
		lHost = inResultSet.getString( "dns_name" );
		if( (lHost == null) || (lHost.compareTo( "" ) == 0) )
		    lHost = inResultSet.getString( "client_host" );
		lReferer = inResultSet.getString( "referer" );
		lNumResults = inResultSet.getInt( "num_results" );
		lDateStamp = formatDate(inResultSet.getTimestamp( "start_time" ));
		lWasSearchNamesTerm = inResultSet.getInt( "search_term_indicator" );

		if( lWasSearchNamesTerm > 0 )
		{
		    lQueryFont = "<strong><i>";
		    lQueryFontEnd = "</i></strong>";
		} else {
		    lQueryFont = "";
		    lQueryFontEnd = "";
		}

		retString += "	<tr>\n" +
		    "		<td bgcolor=\"" + getBGColor() + "\">" + lQueryFont + lQuery + lQueryFontEnd + "</td>\n" +
		    "		<td bgcolor=\"" + getBGColor() + "\">" + lDateStamp + "</td>\n" +
		    "		<td bgcolor=\"" + getBGColor() + "\">" + lHost + "</td>\n" +
		    "		<td bgcolor=\"" + getBGColor() + "\">" + lReferer + "</td>\n" +
		    "		<td bgcolor=\"" + getBGColor() + "\">" + lNumResults +"</td>\n" +
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
