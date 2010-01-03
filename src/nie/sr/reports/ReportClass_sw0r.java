package nie.sr.reports;

import nie.sr.*;
import java.sql.*;
import java.util.*;

public class ReportClass_sw0r extends SRReport
{
    /************************************************************
     * Constructor - required in order to call the super class
     * with the proper parameters
     ***********************************************************/

    public ReportClass_sw0r( modelMain inModel )
    {
		super( inModel );
    }

    /***********************************************************
     * Return the title of this report
     **********************************************************/

    public String getReportTitle() { return "Searches with no results"; };

    /***********************************************************
     * Prepare the report's main SQL Statement
     **********************************************************/

    public String prepareSQL()
    {
		return "SELECT *\n" +
	       "FROM (\n" +
	       "	SELECT rownum AS thePosition,\n" +
	       "               normalized_query,\n" +
	       "               latest_search,\n" +
	       "               search_term_indicator,\n" +
	       "               number_of_queries,\n" +
	       "	       max_num_results\n" +
	       "	FROM (\n" +
	       "		SELECT normalized_query,\n" +
	       "			max(start_time) as latest_search,\n" +
	       "			max(was_search_names_term) as search_term_indicator,\n" +
	       "			count(*) as number_of_queries,\n" +
	       "		   	max(num_results) as min_num_results,\n" +
	       "			min(num_results) as max_num_results\n" +
	       // "		FROM log \n" +
	       "		FROM " + getLogTableName() + "\n" +
	       "		" + SRFilter.kWhereMarker + "\n" +
	       "		GROUP BY normalized_query\n" +
	       "		ORDER BY normalized_query " + SRReport.kDirectionMarker + "\n" +
	       "	)\n" +
	       "	WHERE (min_num_results = 0)\n" +
	       ") \n" +
	       "WHERE (thePosition >=" + SRReport.kStartRowMarker + " AND thePosition < " + SRReport.kEndRowMarker + "+1) " + 
	       "ORDER BY thePosition ASC" +
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
	    "		" + formatSortableHeaderItem("Term", "center") +
	    "		" + formatHeaderItem("Number Of Searches", "center" ) +
	    "		" + formatHeaderItem("Most recent search", "left") +
	    "		" + formatHeaderItem("Had Non-Zero Search", "center" ) +
	    "	</tr>\n";
	try
	{
	    String lQuery;
	    Timestamp lMostRecent;
	    String lQueryFont;
	    String lQueryFontEnd;
	    int lWasSearchNamesTerm;
	    int lNumberOfSearches;
	    boolean lHadNonZeroSearch;
	    fRowCount = 0;

	    while( inResultSet.next() )
	    {
		fRowCount++;
		lQuery = inResultSet.getString( "normalized_query" );
		lMostRecent = inResultSet.getTimestamp( "latest_search" );
		lWasSearchNamesTerm = inResultSet.getInt( "search_term_indicator" );
		lNumberOfSearches = inResultSet.getInt( "number_of_queries" );
		lHadNonZeroSearch = inResultSet.getInt( "max_num_results" ) > 0;

		if( lWasSearchNamesTerm > 0 )
		{
		    lQueryFont = "<strong><i>";
		    lQueryFontEnd = "</i></strong>";
		} else {
		    lQueryFont = "";
		    lQueryFontEnd = "";
		}

		retString += "	<tr>\n" +
		    "		<td bgcolor=\"" + getBGColor() + "\">" + drillDown( "query_detail", "normalized_query", lQuery ) + lQueryFont + lQuery + lQueryFontEnd + endDrillDown( "normalized_query", lQuery ) + "</td>\n" +
		    "		<td bgcolor=\"" + getBGColor() + "\" align=\"center\">" + lNumberOfSearches + "</td>\n" +
		    "		<td bgcolor=\"" + getBGColor() + "\">" + formatDate(lMostRecent) + "</td>\n" +
		    "		<td bgcolor=\"" + getBGColor() + "\" align=\"center\">" + (lHadNonZeroSearch ? "<img src=\"images/checkmark.gif\" alt=\"YES\">" : "&nbsp;") + "</td>\n" +
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
