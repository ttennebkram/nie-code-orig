package nie.di;

public class DiConfigException extends Exception
{
	public DiConfigException( String inMessage )
	{
		super( inMessage );
	}
	public DiConfigException( String inLocationTag, String inMessage )
	{
		super( inLocationTag + inMessage );
	}
}
