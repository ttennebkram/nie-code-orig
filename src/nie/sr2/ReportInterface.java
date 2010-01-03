/*
 * Created on Aug 24, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package nie.sr2;

import org.jdom.Element;
import nie.core.*;

/**
 * @author mbennett
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public interface ReportInterface
{

	public Element runReport(
			AuxIOInfo inRequestInfo, AuxIOInfo inResponseInfo,
			boolean inIsStandaloneHTMLPage
		)
			throws ReportException
		;

	public Element generateMenuLinksToThisReport( AuxIOInfo inRequestInfo );
	public void generateMenuLinksToThisReportCompact(
		AuxIOInfo inRequestInfo,
		Element inTopRow, Element inBottomRow
		);

	public boolean verifyAccessLevel( AuxIOInfo inReuqestInfo );
	public int getRequiredAccessLevel();
}
