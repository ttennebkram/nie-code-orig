/*
 *  Copyright 2001 New Idea Engineering, Inc.
 *  Written by Kevin-Neil Klop
 *
 *  $Id: Application.java,v 1.1 2004/03/03 20:00:51 mbennett Exp $
 *
 *  $Log: Application.java,v $
 *  Revision 1.1  2004/03/03 20:00:51  mbennett
 *  Source tree from niesrv 226, from Feb 2004
 *
 *  Revision 1.2  2001/08/21 03:46:05  kevin
 *  Reindented to meet current NIE style guidelines.
 *  No code changes.
 *
 *  Revision 1.1  2001/07/31 21:44:20  kevin
 *  Initial revision
 *
 *
 */

//package nie.core;
package nie.pump.base;
import org.jdom.*;

public abstract class Application implements Runnable
{
	protected Element fElement = null;
	protected boolean hasMonitoringBeenSet;

	public Element getSection( String inSection )
	{
		return fElement.getChild( inSection );
	}

	public Element getSection( String inSection, Element inElement )
	{
		return inElement.getChild( inSection );
	}

	// Allow anybody to specifically tell us that monitoring is on
	// We do not allow for it to be turned off as it's not safe to
	// assume that all users will know; better safe than sorry.
	// This only affects performance, not functionality.
	public void setMonitoringOn()
	{
		hasMonitoringBeenSet = true;
	}

	public boolean getIsMonintoringOn()
	{
		return hasMonitoringBeenSet;
	}

	public abstract void run();

}
