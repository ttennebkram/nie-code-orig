package nie.sr.reports;

import nie.sr.*;
import java.sql.*;
import java.util.*;

public class FilterClass_d30 extends SRFilter
{
    public FilterClass_d30( modelMain inModel )
    {
	super( inModel );
    }

    public Hashtable prepareSQL( Hashtable inHashtable )
    {
	String lCalendarString = getDateString( -30 );

	if( inHashtable == null )
	    inHashtable = initHashtable();

	inHashtable.remove( kWhereMarker );

	String lConditionString = "start_time >= cast( '" + lCalendarString + "' as date)";
	inHashtable.put( kWhereMarker, "WHERE " + lConditionString );
	inHashtable.put( kAdditionalWhereMarker, "AND (" + lConditionString + ")" );
	return inHashtable;
    }
}
