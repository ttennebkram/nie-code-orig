package nie.sr.util;

import java.io.*;
import java.sql.*;

public class sqlPlus
{
    static public void main( String[] inArgs )
    {
	boolean lContinue = true;
	BufferedReader lReader = new BufferedReader( new InputStreamReader( System.in ) );

	String inputBuffer = "";

	while( lContinue )
	{
	    try {
		System.out.print( ">" );
		String lReadLine = lReader.readLine();

		if( lReadLine == null )
		    lContinue = false;
		else
		    lReadLine = lReadLine.trim();

		if( inputBuffer.length() > 0 )
		    inputBuffer += " ";

		if( lReadLine != null )
		    inputBuffer += lReadLine;

		if( inputBuffer.endsWith( ";" ) || ((lReadLine == null) && (inputBuffer.length() != 0)) )
		    {
			if( inputBuffer.trim().endsWith( ";" ) )
			    inputBuffer = doReplaceAll( inputBuffer, ";", " " ).trim();

			System.out.println( "executing: '" + inputBuffer + "'" );
			executeSQL( inputBuffer );
			inputBuffer = "";
		    }

	    } catch( IOException ioe ) {
		lContinue = false;
	    }
	}
    }

    static public String doReplaceAll( String inString, String inSearchTerm, String inReplacementString )
    {
	String lRetString = inString;
	
	for( int lIndex = lRetString.indexOf( inSearchTerm );
	     lIndex >= 0;
	     lIndex = lRetString.indexOf( inSearchTerm ) )
	    {
		lRetString = lRetString.substring( 0, lIndex ) +
		    inReplacementString +
		    lRetString.substring( lIndex + inSearchTerm.length() );
	    }
	
	return lRetString;
    }
    
    static public void executeSQL( String inString )
    {
	if( gConnection == null )
	    gConnection = getConnection();

	if( gConnection == null )
	    {
		System.out.println( "Connection was null.  exiting." );
		System.exit( -1 );
	    }

	try
	    {
		Statement lStatement = gConnection.createStatement();

		if( lStatement != null )
		    {
			System.out.println( "Executing:\n" + inString );
			ResultSet lResultSet = lStatement.executeQuery( inString );
			if( lResultSet != null )
			    displayResultSet( lResultSet );
		    }
	    }
	catch( SQLException se ) {
	    System.out.flush();
	    System.err.println( se );
	    se.printStackTrace();
	}
	return;
    }

    public static Connection getConnection()
    {
	try
	    {
		DriverManager.registerDriver( new oracle.jdbc.OracleDriver());
		//		Connection oracleConnection = DriverManager.getConnection("jdbc:oracle:thin:@172.16.0.2:1521:devl", "nie", "verity7" );
		Connection oracleConnection = DriverManager.getConnection("jdbc:oracle:thin:@192.168.0.4:1521:stack", "kklop", "kklop" );
		gConnection = oracleConnection;
		return gConnection;
	    }
	catch( SQLException se )
	    {
		System.out.println( se );
		System.out.flush();
		se.printStackTrace();
		System.exit( -1 );
	    }
	return null;
    }

    public static void displayResultSet( ResultSet inResultSet )
    {
	try
	    {
		ResultSetMetaData lMetaData = inResultSet.getMetaData();
		int lColumnCount = lMetaData.getColumnCount();
		int lColumnWidths[] = new int[lColumnCount];
		int lTotalWidth = 0;
		for( int i = 1; i <= lColumnCount; i ++ )
		    {
			lColumnWidths[i-1] = lMetaData.getColumnDisplaySize( i );
			lTotalWidth += lColumnWidths[i-1];
		    }
		
		for( int i = 0; i < lColumnCount; i++ )
		    {
			lColumnWidths[i] = (lColumnWidths[i] * 80) / lTotalWidth;
			if( lColumnWidths[i] == 0 )
			    lColumnWidths[i] = 1;
		    }

		for( int i = 1; i <= lColumnCount; i++ )
		    {
			String lHeaderName = lMetaData.getColumnLabel(i);
			if( lHeaderName.length() > lColumnWidths[i-1] )
			    lHeaderName = lHeaderName.substring( 0, lColumnWidths[i-1] );
			System.out.print( lHeaderName );
			for( int j = lHeaderName.length(); j < lColumnWidths[i-1]; j++ )
			    System.out.print( ' ' );
		    }
		System.out.println( "" );

		for( int i = 0; i < lTotalWidth; i++ )
		    System.out.print( '-' );
		System.out.println();

		// Now it's time to output the values
		while( inResultSet.next() )
		    {
			for( int i = 1; i <= lColumnCount; i++ )
			    {
				String lFieldContents = inResultSet.getString(i);
				if( lFieldContents == null )
				    lFieldContents = "";

				if( lFieldContents.length() > lColumnWidths[i-1] )
				    lFieldContents = lFieldContents.substring( 0, lColumnWidths[i-1] );
				System.out.print( lFieldContents );
				for( int j = lFieldContents.length(); j < lColumnWidths[i-1]; j++ )
				    System.out.print( ' ' );
			    }
			System.out.println( "" );
		    }
	    }
	catch( SQLException se )
	    {
		System.out.println( se );
		System.out.flush();
		se.printStackTrace();
	    }

	System.out.println( "" );

	return;
    }

    static Connection gConnection;
}
