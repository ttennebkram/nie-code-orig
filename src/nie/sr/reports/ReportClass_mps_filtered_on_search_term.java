package nie.sr.reports;

import nie.sr.*;
import java.sql.*;
import java.util.*;

public class ReportClass_mps_filtered_on_search_term extends SRReport
{
    /************************************************************
     * Constructor - required in order to call the super class
     * with the proper parameters
     ***********************************************************/

    public ReportClass_mps_filtered_on_search_term( modelMain inModel )
    {
	super( inModel );
    }

    /***********************************************************
     * Return the title of this report
     **********************************************************/

    public String getReportTitle() { return "Most Popular Search Terms (filtered on search term)"; };

    /***********************************************************
     * Prepare the report's main SQL Statement
     **********************************************************/

    public String prepareSQL()
    {
	return "SELECT *\n" +
	    "FROM (\n" +
	    "	SELECT rownum as thePosition,\n" +
	    "		search_count,\n" +
	    "		search_term_indicator,\n" +
	    "		average_returns,\n" +
	    "		most_recent,\n" +
	    "		normalized_query\n" +
	    "	FROM (\n" +
	    "		SELECT  count(*) as search_count,\n" +
	    "			max(was_search_names_term) as search_term_indicator,\n" +
	    "			avg(num_results) as average_returns,\n" +
	    "			max(start_time) as most_recent,\n" +
	    "			normalized_query\n" +
	    "		FROM (\n" +
	    "			SELECT normalized_query,\n" +
	    "				was_search_names_term,\n" +
	    "				num_results,\n" +
	    "				start_time\n" +
	    // "			FROM log\n" +
	    "			FROM " + getLogTableName() + "\n" +
	    "			WHERE (normalized_query like '%" + ((modelMain)getModel()).getSearchTerm() + "%')\n" +
	    "				" + SRFilter.kAdditionalWhereMarker + "\n" +
	    "		)\n" +
	    "		GROUP BY normalized_query\n" +
	    "		ORDER BY search_count " + SRReport.kDirectionMarker + ", normalized_query\n" +
	    "	)\n" +
	    ")\n" +
	    "WHERE (thePosition BETWEEN " + SRReport.kStartRowMarker + " AND " + SRReport.kEndRowMarker + ")\n" +
	    "ORDER BY thePosition\n" +
	    "";
	/*
	return "SELECT *\n" +
	       "FROM (\n" +
	       "	SELECT rownum AS thePosition, normalized_query, count(*) as search_count, search_term_indicator,\n" +
	       "		average_returns, most_recent\n" +
	       "	FROM (\n" +
	       "		SELECT normalized_query,\n" +
	       "			count(*) as search_count,\n" +
	       "			max(was_search_names_term) as search_term_indicator,\n" +
	       "			avg(num_results) as average_returns,\n" +
	       "       			max(start_time) as most_recent\n" +	
	       "		FROM log \n" +
	       "		WHERE (normalized_query like '%" + ((modelMain)getModel()).getSearchTerm() + "%')" +
	       "			" + SRFilter.kAdditionalWhereMarker + "\n" +
	       "		GROUP BY normalized_query\n" +
	       "		ORDER BY search_count " + SRReport.kDirectionMarker + ", normalized_query\n" +
	       "	)\n" +
	       ") \n" +
	       "WHERE (thePosition >=" + SRReport.kStartRowMarker + " AND thePosition <= " + SRReport.kEndRowMarker + ") " + 
	       "ORDER BY thePosition " +
	       "";
	*/
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
	    "		" + formatSortableHeaderItem("Count", "center") + "\n" +
	    "		" + formatReportLinkHeaderItem("Search Term", "left", "abst", "ASC" ) + "\n" +
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