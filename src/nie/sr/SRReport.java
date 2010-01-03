package nie.sr;

import java.sql.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.text.DateFormat;

public abstract class SRReport
{
    /*******************************************
     *******************************************
     **
     ** All Reports must override these routines.
     **
     ******************************************
     *****************************************/

    abstract public String prepareSQL();
    abstract public String getReportTitle();
    abstract public String getDefaultDirection();
    abstract public String formatResults( ResultSet inResultSet ) throws SRException;
    public int getRowCount() { return fRowCount; };

    /******************************************
     ******************************************
     **
     ** Constructor for a report
     **
     ******************************************
     *****************************************/

    public SRReport( modelMain inModel )
    {
		setModel( inModel );
		fRowNumber = -1;
		fRowCount = 0;
	}

    public boolean hasNextPage()
    {
		return getRowCount() >= getModel().getPageSize();
    }

    /*****************************************
     *****************************************
     **
     ** Main entry point
     **
     *****************************************
     ****************************************/

    public void process( SRFilter inFilter ) throws SRException
    {
		/*************
		 * Call the subclass' functions to generate the basic SQL, passing it on to the filter
		 * class for processing.
		 ***/
	
		String lSQLString = prepareSQL();
		Hashtable lFieldHash = inFilter.prepareSQL();
	
		/*************
		 * Put the model and view into local variables for convenience
		 ***/
	
		modelMain lModel = getModel();
		viewMain lView = lModel.getMainView();
	
		/*************
		 * Set up the other fields that may be inserted into the report's basic SQL
		 ***/
	
		lFieldHash.put( kStartRowMarker, "" + (lModel.getPageStart()) );
		lFieldHash.put( kEndRowMarker, "" + (lModel.getEndRow()) );
		if( lModel.getDirection() == null )
		    lModel.setDirection( getDefaultDirection() );
		lFieldHash.put( kDirectionMarker, lModel.getDirection() );
	
		/*************
		 * Create the finished SQL string
		 ***/

		lSQLString = SRView.createOutputStringFromTemplate( lSQLString, lFieldHash );
		SRConfig.doDebugMsg( "SRReport", "process", "Executing SQL:\n'" + lSQLString + "'" );
	
		/*************
		 * Perform the SQL operations, format the result list, and output it.
		 ***/

		Connection lConnection = SRConfig.getConfigInstance().getConnection();
		try
		{
		    Statement lStatement = lConnection.createStatement();
		    ResultSet lResultSet = lStatement.executeQuery( lSQLString );
	
		    String lPreviousButtonString;
		    String lNextButtonString = "";
		    String lReportTextString;
		    String lPreviousLinkString;
		    String lNextLinkString = "";
	
		    /*************
		     * Create the "next" and "previous" buttons and links for use in the output template
		     ***/
	
		    lReportTextString = formatResults( lResultSet );
	
		    lFieldHash.put( kReportMarker, lReportTextString );
		    lFieldHash.put( kReportTitleMarker, getReportTitle() );
		    lFieldHash.put( kReportIDMarker, lModel.getReportCode() );
		    lFieldHash.put( kFilterIDMarker, lModel.getFilterCode() );
	
		    /*************
		     * If we're on the first page, then the previous button and link are empty
		     ***/

		    if( lModel.getPageStart() > 1 )
		    {
				lPreviousButtonString = "<form action=\"main.jsp\">\n" +
				    "<input type=\"hidden\" name=\"reportCode\" value=\"" + lModel.getReportCode() + "\">\n" + 
				    "<input type=\"hidden\" name=\"filterCode\" value=\"" + lModel.getFilterCode() + "\">\n" +
				    "<input type=\"hidden\" name=\"direction\" value=\"" + lModel.getDirection() + "\">\n" +
				    "<input type=\"hidden\" name=\"pageStart\" value=\"" + ( lModel.getPrevPageStart() ) + "\">\n" +
				    "<input type=\"hidden\" name=\"pageSize\" value=\"" + lModel.getPageSize() + "\">\n";

				if( lModel.getSearchTerm() != null )
				    lPreviousButtonString += "<input type=\"hidden\" name=\"searchTerm\" value=\"" + lModel.getSearchTerm() + "\">\n";
	
				lPreviousButtonString += "<input type=\"submit\" value=\"Previous\">\n" +
				    "</form>\n";
	
				lPreviousLinkString = "<a href=\"main.jsp?reportCode=" + lModel.getReportCode() +
				    "&filterCode=" + lModel.getFilterCode() +
				    "&direction=" + lModel.getDirection() +
				    "&pageStart=" + (lModel.getPageStart() - lModel.getPageSize() < 0 ? 0 : (lModel.getPageStart() - lModel.getPageSize())) +
				    "&pageSize=" + (lModel.getPageSize());
	
				if( lModel.getSearchTerm() != null )
					lPreviousLinkString += "&searchTerm=" + URLEncoder.encode( lModel.getSearchTerm() );

				lPreviousLinkString += "\">Previous</a>";

	    	}
	    	else	// Else NOT on the first page
	    	{
				lPreviousButtonString = "&nbsp;";
				lPreviousLinkString = "&nbsp;";
		    }

		    /*************
		     * Save the previous button and link text into the fields for the templating engine
		     ***/
	
		    lFieldHash.put( kPreviousButtonMarker, lPreviousButtonString );
		    lFieldHash.put( kPreviousLinkMarker, lPreviousLinkString );
	
		    /*************
		     * Generate the next button and link
		     ***/

		    if( hasNextPage() )
		    {
				lNextButtonString = "<form action=\"main.jsp\">" +
				    "<input type=\"hidden\" name=\"reportCode\" value=\"" + lModel.getReportCode() + "\">" +
				    "<input type=\"hidden\" name=\"direction\" value=\"" + lModel.getDirection() + "\">" +
				    "<input type=\"hidden\" name=\"filterCode\" value=\"" + lModel.getFilterCode() + "\">" +
				    "<input type=\"hidden\" name=\"pageStart\" value=\"" + (lModel.getNextPageStart()) + "\">" +
				    "<input type=\"hidden\" name=\"pageSize\" value=\"" + lModel.getPageSize() +  "\">";
	
				if( lModel.getSearchTerm() != null )
				    lNextButtonString += "<inpt type=\"hidden\" name=\"searchTerm\" value=\"" + lModel.getSearchTerm() + "\">";
		 
				lNextButtonString += "<input type=\"submit\" value=\"Next\"></form>";
		
				lNextLinkString = "<a href=\"main.jsp?reportCode=" + lModel.getReportCode() +
				    "&filterCode=" + lModel.getFilterCode() +
				    "&direction=" + lModel.getDirection() +
				    "&pageStart=" + (lModel.getNextPageStart() ) +
				    "&pageSize=" + lModel.getPageSize();

				if( lModel.getSearchTerm() != null )
				{
				    try
					{
					    lNextLinkString += "&searchTerm=" + URLEncoder.encode( lModel.getSearchTerm() );
					}
					catch( Exception e )
					{
					    SRConfig.doErrorMsg( "SRReport", "process", "" + e );
					    return;
					}
				}

				lNextLinkString += "\">Next</a>";
	    	}	// End if has Next Page


		    /*************
		     * Put them into the field list for the templating engine
		     ***/
		   
		    lFieldHash.put( kNextButtonMarker, lNextButtonString );
		    lFieldHash.put( kNextLinkMarker, lNextLinkString );
		    lFieldHash.put( kStartRowMarker, "" + lModel.getPageStart() );
		    lFieldHash.put( kEndRowMarker, "" + lModel.getEndRow() );
	
		    /*************
		     * Display the file.
		     ***/
	
		    SRView.displayTemplateFile( lView.getWriter(), kReportTemplateFileName, lFieldHash );

		}
		catch( SQLException se )
		{
		    SRConfig.doErrorMsg( "SRReport", "process", "" + se );
		    SRView.internalError( lModel.getView().getWriter() );
		    throw new SRException( se );
		}

    }	// end method void process()


    /*****************************************
     * Format something that is able to be clicked
     * for a drill-down report
     ****************************************/

    public String drillDown( String inDestinationReportName, String inSearchTermColumn, String inSearchTerm )
    {
	    if( inSearchTerm == null )
			inSearchTerm = "";

	    return "<a href='main.jsp?reportCode=" + inDestinationReportName +
			"&filterCode=" + getModel().getFilterCode() +
			"&pageStart=" + getModel().getPageStart() +
			"&pageSize=" + getModel().getPageSize() +
			"&searchTerm=" + URLEncoder.encode( inSearchTerm ) + "'>"
			;
	}

    public String endDrillDown( String inSearchTermColumn, String inSearchTerm)
    {
		return "</a>";
    }

    /*****************************************
     *****************************************
     **
     ** Getters and Setters
     **
     *****************************************
     ****************************************/

    public String getFilterCode() { return getModel().getFilterCode(); };
    public String getDirection() { return getModel().getDirection(); };
    public String getFunctionCode() { return getModel().getFunctionCode(); };

    public void setModel( modelMain inModel ) { fModel = inModel; }
    public modelMain getModel( ) { return fModel; }

    public String getHeaderRowBGColour() { return "#004C91"; };
    public String getHeaderRowBGColor() { return getHeaderRowBGColour(); };
    public String getHeaderRowFontColour() { return "white"; };
    public String getHeaderRowFontColor() { return getHeaderRowFontColour(); };
    public String getOddRowColour() { return "white"; };
    public String getOddRowColor() { return getOddRowColour(); };
    public String getEvenRowColour() { return "#CECECE"; };
    public String getEvenRowColor() { return getEvenRowColour(); };

    public String getBGColor()
    {
	if( fRowNumber == 0 )
	    return getEvenRowColour();
	return getOddRowColour();
    };

    public String getBGColour() { return getBGColor(); };

    /********************************************************************
     * Advance to the next row
     *******************************************************************/

    public void nextRow() { fRowNumber = (byte)( (fRowNumber + 1) % 2); };

    /*******************************************************************
     * Format something to be p;laced in the header row of a table and
     * links to another report.
     ******************************************************************/

    public String formatReportLinkHeaderItem( String inItem, String inAlignment, String inReportID, String inDirection )
    {
		String retString = "<th bgcolor=\"" +
		    getHeaderRowBGColour() +
		    "\" align=\"" +
		    inAlignment +
		    "\"><a style=\"vlink: white; alink: white; link: white;\" href=\"main.jsp?reportCode=" + inReportID + "&direction=" + inDirection + "&filterCode=" + getModel().getFilterCode() + "\">" +
		    "<font color=\"" +
		    getHeaderRowFontColour() +
		    "\">" +
		    inItem +
		    "</font></a>" +
		    "</th>";
	
		return retString;
	    }

    /*******************************************************************
     * Format something to be placed in the header row of a table
     ******************************************************************/

    public String formatHeaderItem( String inItem, String inAlignment )
    {
	String retString = "<th bgcolor=\"" +
	    getHeaderRowBGColour() +
	    "\" align=\"" +
	    inAlignment +
	    "\"><font color=\"" +
	    getHeaderRowFontColour() +
	    "\">" +
	    inItem +
	    "</font>" +
	    "</th>";

	return retString;
    }

    /******************************************************************
     * Format a date
     *****************************************************************/

    static public String formatDate( java.sql.Timestamp inTimeStamp )
    {
//	int lMonth = inTimeStamp.getMonth() + 1;
//	int lDay = inTimeStamp.getDate();
//	int lYear = inTimeStamp.getYear() + 1900;
//	int lHour = inTimeStamp.getHours();
//	int lMinutes = inTimeStamp.getMinutes();
//	int lSeconds = inTimeStamp.getSeconds();
//
//	return formatDigits( lMonth, 2 ) + "/" + formatDigits( lDay, 2 ) + "/" + formatDigits( lYear, 4 ) + " " +
//	    formatDigits( lHour, 2 ) + ":" + formatDigits( lMinutes, 2 ) + ":" + formatDigits( lSeconds, 2 );
		DateFormat myFormatter = DateFormat.getDateTimeInstance(
			DateFormat.SHORT, DateFormat.LONG
			);
		String myStr = myFormatter.format( inTimeStamp );
		return myStr;
    }

    static public String formatDigits( int inNumber, int inNumDigits )
    {
		String lOutString = "" + inNumber;
		for( int i = lOutString.length(); i < inNumDigits; i++ )
		    lOutString = "0" + lOutString;
		return lOutString;
    }

    /******************************************************************
     * Format the thing that governs the sort order in the table
     *****************************************************************/

    public String formatSortableHeaderItem( String inItem, String inAlignment )
    {
		String retString = "<th bgcolor=\"" +
		    getHeaderRowBGColour() +
		    "\" align=\"" +
		    inAlignment +
		    "\">" +
		    "<a href=\"main.jsp?" +
			"reportCode=" + getModel().getReportCode() +
			"&filterCode=" + getModel().getFilterCode() +
			"&pageStart=" + getModel().getPageStart() +
			"&pageSize=" + getModel().getPageSize() +
		    "&direction=" + getModel().getOppositeDirection();

		if( getModel().getSearchTerm() != null )
		{
		    try
		    {
				retString += "&searchTerm=" + URLEncoder.encode( getModel().getSearchTerm() );
		    }
		    catch( Exception e )
		    {
				SRConfig.doErrorMsg( "SRReport", "formatSortableHeaderItem", "URLEncoder threw an exception: " + e );
		    }
		}

		retString +=
		    "\">" +
		    "<font color=\"" +
		    getHeaderRowFontColour() +
		    "\">" +inItem +
		    "&nbsp;<img src=\"images/" + getModel().getDirection() + "_arrow.gif\">" +
		    "</font></a>" +
		    "</th>";

		return retString;
    }


	public static String getLogTableName()
	{
		// return LOG_TABLE;

		// *** SEE ALSO nie.sn.SearchLogger.getLogTableName(), they should match

		return nie.core.DBConfig.LOG_TABLE;
	}

	// The main table we will use
	// static final String LOG_TABLE = "nie_log";
	// moved to DBConfig


    /*****************************************
     *****************************************
     **
     ** Markers used in constructing a SQL
     ** template that can be paged
     **
     *****************************************
     ****************************************/

    static public final String kStartRowMarker = "<start_row>";
    static public final String kEndRowMarker = "<end_row>";
    static public final String kReportMarker = "<report_goes_here>";
    static public final String kPreviousButtonMarker = "<previous_page_button>";
    static public final String kNextButtonMarker = "<next_page_button>";
    static public final String kPreviousLinkMarker = "<previous_page_link>";
    static public final String kNextLinkMarker = "<next_page_link>";
    static public final String kReportTitleMarker = "<report_title>";
    static public final String kReportIDMarker = "<report_id>";
    static public final String kFilterIDMarker = "<filter_id>";
    static public final String kDirectionMarker = "<direction>";

    // static public final String kReportTemplateFileName = "html" + File.separator + "report.html";
    static public final String kReportTemplateFileName = "html/report.html";

    /*****************************************
     *****************************************
     **
     ** Instance variables
     **
     ****************************************
     ***************************************/

    modelMain fModel;
    byte fRowNumber;
    protected int fRowCount;
}
