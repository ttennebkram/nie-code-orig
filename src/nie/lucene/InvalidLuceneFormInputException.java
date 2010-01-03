package nie.lucene;

import nie.core.NIEUtil;

public class InvalidLuceneFormInputException extends Exception
{
	public InvalidLuceneFormInputException( String inMessage )
	{
		// super( inMessage );
		// System.err.println( "ERROR: UIException: " + inMessage );
		this( null, inMessage, null );
	}

	public InvalidLuceneFormInputException(
		String inCGIFieldName,
		String inMessage
		)
	{
		this( inCGIFieldName, inMessage, null );
	}
	public InvalidLuceneFormInputException(
		String inCGIFieldName,
		String inMessage,
		String optXMLFieldPath
		)
	{
		super( (null==inCGIFieldName ? "(general)" : inCGIFieldName ) + ": " + inMessage );
		fMessage = inMessage;
		fCGIFieldName = inCGIFieldName;
		fXMLFieldPath = optXMLFieldPath;

		/***
		System.err.println(
			"New field error:" + NIEUtil.NL
			+ "fMessage=\"" + fMessage + "\"" + NIEUtil.NL
			+ "fFieldLabel=\"" + fFieldLabel + "\"" + NIEUtil.NL
			+ "fCGIFieldName=\"" + fCGIFieldName + "\"" + NIEUtil.NL
			+ "fXMLFieldPath=\"" + fXMLFieldPath + "\""
			);
		***/
	}


	private void _InvalidFormInputException(
		String inMessage,
		String inFieldLabel,
		String inXMLFieldPath,
		String inCGIFieldName
		)
	{
		// super( inFieldLabel + "(" + inCGIFieldName + "): " + inMessage );
		fMessage = inMessage;
		//fFieldLabel = inFieldLabel;
		//fXMLFieldPath = inXMLFieldPath;
		fCGIFieldName = inCGIFieldName;

		/***
		System.err.println(
			"New field error:" + NIEUtil.NL
			+ "fMessage=\"" + fMessage + "\"" + NIEUtil.NL
			+ "fFieldLabel=\"" + fFieldLabel + "\"" + NIEUtil.NL
			+ "fCGIFieldName=\"" + fCGIFieldName + "\"" + NIEUtil.NL
			+ "fXMLFieldPath=\"" + fXMLFieldPath + "\""
			);
		***/
	}

	public String getMessage() {
		return
			// getFieldLabel()
			// + "(" + getCGIFieldName() + "): "
			// getCGIFieldName() + ": "
			(null==getCGIFieldName() ? "(general)" : getCGIFieldName() ) + ": "
			+ getShortMessage()
			;
	}

	public String getShortMessage() {
		return fMessage;
	}

	public String _getFieldLabel() {
		return null;//fFieldLabel;
	}
	public String getCGIFieldName() {
		return fCGIFieldName;
	}
	public String getOptXMLFormPath() {
		return null;// fXMLFieldPath;
	}

	String fMessage;
	String _fFieldLabel;
	String fXMLFieldPath;
	String fCGIFieldName;
}
