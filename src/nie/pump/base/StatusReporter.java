//package nie.core;
package nie.pump.base;
import org.jdom.Element;

/**
 * Title:        DPump
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:      New Idea Engineering
 * @author Kevin-Neil Klop and Mark Benett
 * @version 1.0
 */

import nie.core.JDOMHelper;
import nie.core.JDOMHelperException;

public interface StatusReporter
{
	public JDOMHelper getStatusXML();
	//public String getStatusXML();
	public boolean canExit();
}
