/*
 * Created on Aug 24, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package nie.webui;

import org.jdom.Element;
import nie.core.*;

/**
 * @author mbennett
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public interface UIScreenInterface
{

	public Element processRequest(
			AuxIOInfo inRequestInfo, AuxIOInfo inResponseInfo,
			boolean inIsStandaloneHTMLPage
		)
			throws UIException
		;

	public boolean verifySecurityLevelAccess( AuxIOInfo inReuqestInfo );

	// public Element generateFancyLinksToThisReport( AuxIOInfo inRequestInfo );
}
