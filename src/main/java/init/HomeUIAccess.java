package init;

import shelly.ShellyUDP;
import ventilation.VentilationWriter;

public class HomeUIAccess
{
	private final VentilationWriter	ventWriter;
	private final ShellyUDP			shelly;

	public HomeUIAccess(final VentilationWriter ventWriter, final ShellyUDP shelly)
	{
		this.ventWriter = ventWriter;
		this.shelly = shelly;
	}

	public boolean hasShelly()
	{
		return shelly != null;
	}

	public void setAggressiveness(final int v)
	{
		shelly.setAggressiveness(v);
	}

	public int getAggressiveness()
	{
		return shelly.getAggressiveness();
	}

	public boolean isContinousUpdate()
	{
		return shelly.isContinousUpdate();
	}

	public void setContinousUpdate(final boolean continousUpdate)
	{
		shelly.setContinousUpdate(continousUpdate);
	}

	//

	public boolean hasVent()
	{
		return ventWriter != null;
	}

	public void setVent(final int val)
	{
		ventWriter.setVent(val);
	}


	public int getVent()
	{
		return ventWriter.getVent();
	}
}
