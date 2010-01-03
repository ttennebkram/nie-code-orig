package nie.sr;

public class SRException extends Exception
{
    public SRException()
    {
	super();
    }

    public SRException( Throwable inThrowable )
    {
	// super( inThrowable );
	super( "" + inThrowable );
    }

    public SRException( String inMessage )
    {
	super( inMessage );
    }
}
