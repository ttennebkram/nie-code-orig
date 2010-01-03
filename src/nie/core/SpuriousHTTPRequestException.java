// An exception to represent a very specific socket communications
// error the comes up frequently but seems harmless
// I think it has something to do with spurious IE reloads, Nick says
// not to worry, IE does lots of odd little things behind the scenes

package nie.core;

public class SpuriousHTTPRequestException extends Exception
{
	public SpuriousHTTPRequestException( String inMessage )
	{
		super( inMessage );
	}
}
