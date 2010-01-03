package nie.sr.util;

import java.io.*;
import nie.sr.*;
import java.util.*;
import java.sql.*;

public class DataRoller implements Runnable
{
	public static nie.sr.SRConfig gConfig;
	public static String gConfigFileName;
	
	public static final String kDefaultFileName = "/tmp/sr_config.xml";
	public static final String kGetMostRecentTransaction = "SELECT sysdate-max(start_time) AS offset FROM log";
	public static final String kUpdateQueryPreamble = "UPDATE log SET start_time = start_time + ";
	public static final String kUpdateQueryPostamble = "";
	public static final String kCleanupQuery = "UPDATE log SET end_time = start_time";

	public static void main( String inArgs[] )
	{
		gConfigFileName = kDefaultFileName;

		if( inArgs.length > 0 )
			gConfigFileName = inArgs[0];
		
		DataRoller lDataRoller = new DataRoller();
		lDataRoller.run();
	}
	
	public DataRoller()
	{
		gConfig = new nie.sr.SRConfig();
		try
			{
			gConfig.SRConfigInit( gConfigFileName );
			}
		catch( SRException sre ) {
			sre.printStackTrace();
			}
	}
	
	public void run()
	{
		Connection lConnection = gConfig.getConnection();
		try
			{
			Statement lStatement = lConnection.createStatement();
			ResultSet lResultSet = lStatement.executeQuery( kGetMostRecentTransaction );
			if( lResultSet.next() )
				{
				float lAdder = lResultSet.getFloat( "offset" );
				System.out.println( "Adjusting by " + lAdder );
				String lUpdateQuery = kUpdateQueryPreamble + lAdder + kUpdateQueryPostamble;
				lStatement.executeUpdate( lUpdateQuery );
				lStatement.executeUpdate( kCleanupQuery );
				}
			}
		catch( SQLException se ) {
			se.printStackTrace();
			}
	}
}
