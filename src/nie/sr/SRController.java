package nie.sr;

abstract public class SRController
{
	public SRController( SRModel inModel ) throws SRException
	{
		setModel( inModel );
	}

	public void setModel( SRModel inModel )
	{
		fModel = inModel;
	}

	public SRModel getModel()
	{
		return fModel;
	}
	
	abstract public void process() throws SRException, SRException, SRException, SRException;

	SRModel fModel;
}
