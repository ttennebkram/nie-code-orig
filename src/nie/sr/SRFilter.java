package nie.sr;

import java.util.*;

abstract public class SRFilter
{
    /*******************************************
     *******************************************
     **
     ** All filters must override prepareSQL
     ** PrepareSQL creates a has that will be
     ** fed in as the fields to insert into the SQL
     ** to the templating engine.
     **
     *******************************************
     ******************************************/

    public abstract Hashtable prepareSQL( Hashtable inHashtable );
    public Hashtable prepareSQL()
    {
	Hashtable lHashtable = initHashtable();
	return prepareSQL( lHashtable );
    };

    /*******************************************
     *******************************************
     **
     ** Constructor for SRFilter
     **
     *******************************************
     ******************************************/

    public SRFilter( modelMain inModel )
    {
	setModel( inModel );
    }

    /*******************************************
     *******************************************
     **
     ** Initialize a hashtable so that it
     ** can safely be passed to a filterable
     ** report without gumming up the works.
     ** initializes all required fields to
     ** empty (i.e. will remove them from the
     ** report SQL )
     **
     *******************************************
     ******************************************/

    public static Hashtable initHashtable()
    {
	Hashtable lHashtable = new Hashtable();
	lHashtable.put( kWhereMarker, "" );
	lHashtable.put( kGroupByMarker, "" );
	lHashtable.put( kAdditionalWhereMarker, "" );
	lHashtable.put( kAdditionalGroupByMarker, "" );
	return lHashtable;
    }

    /*******************************************
     * Get a date string representing the date
     * x days from now...
     ******************************************/

    public static String getDateString( int inOffset )
    {
	Calendar lCalendar = Calendar.getInstance();
	lCalendar.add( Calendar.DATE, inOffset );
	int lDay = lCalendar.get(Calendar.DAY_OF_MONTH);
	int lMonth = lCalendar.get(Calendar.MONTH);
	int lYear = lCalendar.get(Calendar.YEAR) % 100;

	String retString = "" + lDay + "-" + kMonthName[lMonth] + "-";

	if( lYear < 10 )
	    retString += "0";

	retString = retString + lYear;

	return retString;
    }

    /*******************************************
     *******************************************
     **
     ** Getters and Setters
     **
     *******************************************
     ******************************************/

    public void setModel( modelMain inModel ) { fModel = inModel; }
    public modelMain getModel() { return fModel; }

    /*******************************************
     *******************************************
     **
     ** Markers used in setting up a SQL template
     ** that can be modified by filters.
     **
     *******************************************
     ******************************************/

    static public String kWhereMarker = "<where_clause>";
    static public String kAdditionalWhereMarker = "<additional_where_clause>";
    static public String kGroupByMarker = "<group_by_clause>";
    static public String kAdditionalGroupByMarker = "<additional_group_by_marker>";
    static final String kMonthName[] = {
	"JAN",	"FEB",	"MAR",	"APR",	"MAY",	"JUN",
	"JUL",	"AUG",	"SEP",	"OCT",	"NOV",	"DEC"
    };


    /******************************************
     ******************************************
     **
     ** Instance variables
     **
     ******************************************
     *****************************************/

    modelMain fModel;
}
