package ventilation;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.RaspiPin;

public class VentilationWriter
{
	private final GpioPinDigitalOutput pin1;
	private final GpioPinDigitalOutput pin2;

	private int				vent		= 1;

	public VentilationWriter()
	{
        final GpioController gpio = GpioFactory.getInstance();
		pin1 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "DaySpeed");
		pin2 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, "HighSpeed");

		if (pin2.getState().getValue() == 0)
		{
			if (pin1.getState().getValue() == 0)
			{
				vent = 0;
			}
			else
			{
				vent = 1;
			}
		}
		else
		{
			vent = 2;
		}
		setVent(vent);

		// pin1.setShutdownOptions(true, PinState.LOW);
		// pin2.setShutdownOptions(true, PinState.LOW);
		System.out.println("VentilationWriter initialized to level" + vent + ": " + pin1 + "/" + pin2);
	}

	public void setVent(final int val)
	{
		vent = val;
		switch (val)
		{
			case 0:
				pin1.low();
				pin2.low();
				break;
			case 1:
				pin2.low();
				pin1.high();
				break;
			case 2:
				pin1.low();
				pin2.high();
				break;

			default:
				break;
		}
	}

	public int getVent()
	{
		return vent;
	}

}
