package nie.webui;

public class UIConfigException extends Exception
{
	public UIConfigException( String inMessage )
	{
		super( inMessage );
		System.err.println( "ERROR: UIConfigException: " + inMessage );
	}
}
