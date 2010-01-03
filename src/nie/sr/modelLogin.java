package nie.sr;

public class modelLogin extends SRModel
{
    public modelLogin() throws SRException
    {
    }

    /***************************************************************
     * Check if the data actually loaded in from the form - we might
     * be missing some data...
     **************************************************************/
    
    public boolean isLoaded()
    {
		return (fUserPassword != null) && (fUserName != null);
    };
    
    /***************************************************************
     * Convenience routine so that I don't have to keep casting the
     * getView() call.
     **************************************************************/
    
    public viewLogin getLoginView()
    {
    	return (viewLogin)getView();
    }
    
    /***************************************************************
     * Setters and Getters
     **************************************************************/
    
    public void setUserName( String inUserName )
    {
    	fUserName = inUserName;
    }
    public void setUserPassword( String inUserPassword )
    {
    	fUserPassword = inUserPassword;
    }
    
    public String getUserName()
    {
    	return fUserName;
    }
    public String getUserPassword()
    {
    	return fUserPassword;
    }
    
    /***************************************************************
     * Instance variables
     **************************************************************/
    
    String fUserPassword;
    String fUserName;
}
