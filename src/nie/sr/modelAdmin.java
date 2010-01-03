package nie.sr;

public class modelAdmin extends SRModel
{
    public modelAdmin() throws SRException
    {
    }

    public boolean isLoaded()
    {
	return false;
    }

    public String getFunctionCode()
    {
	return null;
    }

    public String getUserName() { return fUserName; };
    public String getPassword() { return fPassword; };
    public String getFullName() { return fFullName; };
    public String getEMail() { return fEMail; };
    public String getPhone1() { return fPhone1; };
    public String getPhone2() { return fPhone2; };
    public String getPhone3() { return fPhone3; };
    public int getSecurityLevel() { return fSecurity; };
    public int getUserID() { return fUserID; };

    public void setUserName( String inUserName ) { fUserName = inUserName; };
    public void setPassword( String inPassword ) { fPassword = inPassword; };
    public void setFullName( String inFullName ) { fFullName = inFullName; };
    public void setEMail( String inEMail ) { fEMail = inEMail; };
    public void setPhone1( String inPhone ) { fPhone1 = inPhone; };
    public void setPhone2( String inPhone ) { fPhone2 = inPhone; };
    public void setPhone3( String inPhone ) { fPhone3 = inPhone; };
    public void setSecurityLevel( int inSecurityLevel ) { fSecurity = inSecurityLevel; };
    public void setUserID( int inUserID ) { fUserID = inUserID; };

    String fUserName;
    String fPassword;
    String fFullName;
    String fEMail;
    String fPhone1;
    String fPhone2;
    String fPhone3;
    int fSecurity;
    int fUserID;
}
