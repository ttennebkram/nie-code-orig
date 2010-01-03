package nie.lucene;

public class LuceneConfigException extends Exception
{
	public LuceneConfigException( String inMessage )
	{
		super( inMessage );
		System.err.println( "ERROR: LuceneConfigException: " + inMessage );
	}
}
