package nie.sr;

abstract public class SRModel
{
	/***************************************************
	 * Constructor
	 ***************************************************/

	public SRModel() throws SRException
	{
	};
	
	/***************************************************
	 * Setters and getters
	 **************************************************/
	 
	public void setView( SRView inView )
	{
		fView = inView;
	}
	public void setController( SRController inController )
	{
		fController = inController;
	}
	public void setUserInfo( SRUserInfo inUserInfo )
	{
		fUserInfo = inUserInfo;
	}
	
	public SRView getView()
	{
		return fView;
	}
	public SRController getController()
	{
		return fController;
	}
	public SRUserInfo	getUserInfo()
	{
		return fUserInfo;
	}
	
	/***************************************************
	 * The following must be implemented by a sub class
	 **************************************************/
	 
	abstract boolean isLoaded();
	
	/***************************************************
	 * Instance fields
	 **************************************************/
	 
	SRView fView;
	SRController fController;
	SRUserInfo	fUserInfo;
}
