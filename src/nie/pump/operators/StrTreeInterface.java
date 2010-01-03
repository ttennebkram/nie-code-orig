//package nie.core.operators;
package nie.pump.operators;
import nie.core.*;

/**
 * Title:        Interface for recursive String style tree operators
 * Description:
	Various classes of nodes may want to participate in calculating
	string expressions.
 * Copyright:    Copyright (c) 2001
 * Company:      New Idea Engineering
 * @author Kevin-Neil Klop and Mark Benett
 * @version 1.0
 */

import nie.pump.base.WorkUnit;

public interface StrTreeInterface
{

	public String evaluate( WorkUnit wu )
		throws OpTreeException
		;

}
