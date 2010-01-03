package nie.sr.util;

import java.util.*;

public class DotNotationComparator implements Comparator
{
	public int compare( Object inDotNotation1, Object inDotNotation2 )
	{
	    if( inDotNotation1 == null )
		return -1;
	    else if( inDotNotation2 == null )
		return 1;
	    else
		return ((DotNotation)inDotNotation1).compareTo( (DotNotation)inDotNotation2 );
	}
};
