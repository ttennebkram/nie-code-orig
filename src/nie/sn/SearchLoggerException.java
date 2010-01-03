package nie.sn;

public class SearchLoggerException extends Exception
{
	public SearchLoggerException( String inMessage )
	{
		super( inMessage );
	}

	public SearchLoggerException( String inLocationTag, String inMessage )
	{
		this( inLocationTag + inMessage );
	}

}
