package nie.pump.processors;

public class ExceptionPrematureEndOfData extends Exception
{
	public ExceptionPrematureEndOfData()
	{
		super();
	}
	public ExceptionPrematureEndOfData( String inMessage )
	{
		super( inMessage );
	}
}
