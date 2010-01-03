package nie.sr;

import java.util.*;
import java.io.File;
import javax.servlet.jsp.*;
import javax.servlet.http.*;

public class viewMain extends SRView
{
    /************************************************************************
     ************************************************************************
     **
     ** Constructor
     **
     ************************************************************************
     ***********************************************************************/
    
    public viewMain( modelMain inModel )
    	throws SRException
    {
		super( inModel );
    }
    
    /************************************************************************
     ************************************************************************
     **
     ** Setters and Getters
     **
     ************************************************************************
     ***********************************************************************/
    
    // This one is for convenience so that I don't have
    // to keep casting the base class' getModel() call
    
    public modelMain getModelMain()
    	throws SRException
    {
		modelMain lModel = (modelMain)getModel();
		if( null == lModel )
		    throw new SRException( "getModel() returned a null in viewMain" );
		else
			return lModel;
    }

    /************************************************************************
     ************************************************************************
     **
     ** Constants that we've used
     **
     ************************************************************************
     ***********************************************************************/
    
    // static final String kFileNamePrefix = "html" + File.separator;
    static final String kFileNamePrefix = "html/";
    static final String kFileNameSuffix =  ".html";
}
