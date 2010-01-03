package nie.config_ui;

import nie.core.*;

public class ConfiguratorException extends Exception
{
	private static final String kClassName = "DBConfigException";

	public ConfiguratorException( String inMessage )
	{
		super( inMessage );

		// We need to do this because we don't get to see the exception
		// Useful for any class exception partner for classes that
		// will be instantiated by JDOMHelper.makeObjectFromConfigPath()
		final String kFName = "constructor";
		errorMsg( kFName, "Called with message \"" + inMessage + "\"" );
	}
	public ConfiguratorException( String inLocationTag, String inMessage )
	{
		this( inLocationTag + inMessage );
	}



	// This gets us to the logging object
	private static RunLogInterface getRunLogObject()
	{
		return RunLogBasicImpl.getRunLogObject();
	}
	private static boolean errorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg( kClassName, inFromRoutine,
			inMessage
			);
	}



}
