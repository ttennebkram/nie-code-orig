package nie.sr.reports;

import nie.sr.*;
import java.sql.*;
import java.util.*;

public class ReportClass_absn extends SRReport
{
    /************************************************************
     * Constructor - required in order to call the super class
     * with the proper parameters
     ***********************************************************/

    public ReportClass_absn( modelMain inModel )
    {
	super( inModel );
    }

    /***********************************************************
     * Return the title of this report
     **********************************************************/

    public String getReportTitle() { return "Most Popular SearchNames Terms"; };

    /***********************************************************
     * Prepare the report's main SQL Statement
     **********************************************************/

    public String prepareSQL()
    {
	return "SELECT *\n" +
	       "FROM (\n" +
	       "	SELECT rownum AS thePosition, normalized_query, search_count, search_term_indicator,\n" +
	       "		average_returns, most_recent\n" +
	       "	FROM (\n" +
	       "		SELECT normalized_query,\n" +
	       "			count(*) as search_count,\n" +
	       "			max(was_search_names_term) as search_term_indicator,\n" +
	       "			avg(num_results) as average_returns,\n" +
	       "       			max(start_time) as most_recent\n" +	
	       // "		FROM log \n" +
	       "		FROM " + getLogTableName() + " \n" +
	       "		" + SRFilter.kWhereMarker + "\n" +
	       "		GROUP BY normalized_query\n" +
	       "		HAVING max(was_search_names_term) <> 0\n" +
	       "		ORDER BY normalized_query " + SRReport.kDirectionMarker + "\n" +
	       "	)\n" +
	       ") \n" +
	       "WHERE (thePosition >=" + SRReport.kStartRowMarker + " AND thePosition < " + SRReport.kEndRowMarker + "+1) " + 
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
	    "		" + formatReportLinkHeaderItem("Count", "center", "mpsns", "DESC") + "\n" +
	    "		" + formatSortableHeaderItem("Search Term", "left") + "\n" +
	    "		" + formatHeaderItem("Average Results", "center") + "\n" +
	    "           " + formatHeaderItem("Most Recent", "center") + "\n" +
	    "	</tr>\n";
	try
	{
	    String lQuery;
	    String lCount;
	    String lQueryFont;
	    String lQueryFontEnd;
	    int lWasSearchNamesTerm;
	    int lAverageReturns;
	    Timestamp lMostRecent;
	    fRowCount = 0;
	    while( inResultSet.next() )
	    {
		fRowCount++;
		lQuery = inResultSet.getString( "normalized_query" );
		lCount = inResultSet.getString( "search_count" );
		lAverageReturns = inResultSet.getInt( "average_returns" );
		lWasSearchNamesTerm = inResultSet.getInt( "search_term_indicator" );
		lMostRecent = inResultSet.getTimestamp( "most_recent" );

		if( lWasSearchNamesTerm > 0 )
		{
		    lQueryFont = "<strong><i>";
		    lQueryFontEnd = "</i></strong>";
		} else {
		    lQueryFont = "";
		    lQueryFontEnd = "";
		}

		retString += "	<tr>\n" +
		    "		<td align=\"center\" bgcolor=\"" + getBGColor() + "\">" + lCount + "</td>\n" +
		    "		<td bgcolor=\"" + getBGColor() + "\">" + drillDown( "query_detail", "normalized_query", lQuery) + lQueryFont + lQuery + lQueryFontEnd + endDrillDown( "normalized_query", lQuery ) + "</td>\n" +
		    "		<td align=\"center\" bgcolor=\"" + getBGColor() + "\">" + lAverageReturns + "</td>\n" +
		    "           <td align=\"left\" bgcolor=\"" + getBGColor() + "\">" + formatDate(lMostRecent) + "</td>\n" +
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
