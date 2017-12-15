import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

public class VentilationWriter
{
	private final GpioPinDigitalOutput pin1;
	private final GpioPinDigitalOutput pin2;
	
	private int				vent		= 1;

	public VentilationWriter()
	{
        final GpioController gpio = GpioFactory.getInstance();
		pin1 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "DaySpeed", PinState.HIGH);
		pin2 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, "HighSpeed", PinState.LOW);
		pin1.setShutdownOptions(true, PinState.LOW);
		pin2.setShutdownOptions(true, PinState.LOW);
		System.out.println("VentilationWriter initialized: "+pin1+"/"+pin2);
	}
	
	public void setVent(int val)
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
