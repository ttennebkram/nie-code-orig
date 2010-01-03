//package nie.core.operators;
package nie.pump.operators;
import nie.core.*;

/**
 * Title:        Interface for recursive boolean style tree operators
 * Description:
	We need this because we really need two different classes
		The and, or, not stuff, which does have children, etc
		The leaf nodes that compare work unit stuff
		They need to cooperate, but have very different constructors
		and methods, so they will both implement this interface.
 * Copyright:    Copyright (c) 2001
 * Company:      New Idea Engineering
 * @author Kevin-Neil Klop and Mark Benett
 * @version 1.0
 */

import nie.pump.base.WorkUnit;

public interface OpTreeInterface
{

	public boolean evaluate( WorkUnit wu )
		throws OpTreeException
		;

}
