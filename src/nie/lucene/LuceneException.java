package nie.lucene;

public class LuceneException extends Exception
{
	public LuceneException( String inMessage )
	{
		super( inMessage );
		System.err.println( "ERROR: LuceneException: " + inMessage );
		// this.printStackTrace( System.err );
	}
}
