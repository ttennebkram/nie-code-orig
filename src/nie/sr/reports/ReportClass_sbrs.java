package nie.sr.reports;

import nie.sr.*;
import java.sql.*;
import java.util.*;

public class ReportClass_sbrs extends SRReport
{
    /************************************************************
     * Constructor - required in order to call the super class
     * with the proper parameters
     ***********************************************************/

    public ReportClass_sbrs( modelMain inModel )
    {
	super( inModel );
    }

    /***********************************************************
     * Return the title of this report
     **********************************************************/

    public String getReportTitle() { return "Searches by result size"; };

    /***********************************************************
     * Prepare the report's main SQL Statement
     **********************************************************/

    public String prepareSQL()
    {
	return "SELECT *\n" +
	       "FROM (\n" +
	       "	SELECT rownum AS thePosition, normalized_query, search_term_indicator, avg_result_size, most_recent, theCount\n" +
	       "	FROM (\n" +
	       "		SELECT normalized_query,\n" +
	       "			max(was_search_names_term) as search_term_indicator,\n" +
	       "			avg(num_results) as avg_result_size,\n" +
	       "       			max( start_time ) as most_recent,\n" +
	       "	      	    	count(*) as theCount\n" +
	       // "		FROM log \n" +
	       "		FROM " + getLogTableName() + "\n" +
	       "		" + SRFilter.kWhereMarker + "\n" +
	       "		GROUP BY normalized_query\n" +
	       "		ORDER BY avg_result_size " + SRReport.kDirectionMarker + ", normalized_query ASC\n" +
	       "	)\n" +
	       ") \n" +
	       "WHERE (thePosition >=" + SRReport.kStartRowMarker + " AND thePosition < " + SRReport.kEndRowMarker + "+1) " + 
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
	    "		" + formatSortableHeaderItem("Average result size", "center" ) +
	    "		" + formatHeaderItem("Term", "center") +
	    "           " + formatHeaderItem("Most Recent Search", "center") +
	    "           " + formatHeaderItem("# Searches", "center") +
	    "	</tr>\n";
	try
	{
	    String lQuery;
	    Timestamp lMostRecent;
	    String lQueryFont;
	    String lQueryFontEnd;
	    int lWasSearchNamesTerm;
	    int lAverageResultSize;
	    Timestamp lMostRecentQuery;
	    int lSearchCount;
	    fRowCount = 0;

	    while( inResultSet.next() )
	    {
		fRowCount++;
		lQuery = inResultSet.getString( "normalized_query" );
		lWasSearchNamesTerm = inResultSet.getInt( "search_term_indicator" );
		lAverageResultSize = inResultSet.getInt( "avg_result_size" );
		lMostRecentQuery = inResultSet.getTimestamp( "most_recent" );
		lSearchCount = inResultSet.getInt( "theCount" );

		if( lWasSearchNamesTerm > 0 )
		{
		    lQueryFont = "<strong><i>";
		    lQueryFontEnd = "</i></strong>";
		} else {
		    lQueryFont = "";
		    lQueryFontEnd = "";
		}

		retString += "	<tr>\n" +
		    "		<td bgcolor=\"" + getBGColor() + "\" align=\"center\">" + lAverageResultSize + "</td>\n" +
		    "		<td bgcolor=\"" + getBGColor() + "\">" + drillDown( "query_detail", "normalized_query", lQuery ) + lQueryFont + lQuery + lQueryFontEnd + endDrillDown( "normalized_query", lQuery ) + "</td>\n" +
		    "           <td bgcolor=\"" + getBGColor() + "\" align=\"center\">" + formatDate( lMostRecentQuery ) + "</td>\n" +
		    "           <td bgcolor=\"" + getBGColor() + "\" align=\"center\">" + lSearchCount + "</td>\n" +
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
