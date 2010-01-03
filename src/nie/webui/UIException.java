package nie.webui;

public class UIException extends Exception
{
	public UIException( String inMessage )
	{
		super( inMessage );
		System.err.println( "ERROR: UIException: " + inMessage );
		// this.printStackTrace( System.err );
	}
}
