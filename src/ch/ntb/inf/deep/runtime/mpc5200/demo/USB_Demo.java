package ch.ntb.inf.deep.runtime.mpc5200.demo;

import java.io.PrintStream;

import ch.ntb.inf.deep.runtime.mpc5200.driver.UART3;
import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.Device;
import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.exceptions.UsbException;
import ch.ntb.inf.deep.runtime.ppc32.Task;

public class USB_Demo extends Task{
	
	private Device usbDev;
	private boolean initDone = false;
	
	public void action(){
		if(!initDone){
			usbDev = new Device();
			usbDev.init();
			initDone = true;
		}
	}
	
	static{
		
		// Initialize UART (9600 8N1)
		UART3.start(9600, UART3.NO_PARITY, (short)8);

		// Use the UART3 for stdout and stderr
		System.out = new PrintStream(UART3.out);
		System.err = System.out;

		// Print a string to the stdout
		System.out.println("USB Demo:");
		
		Task t = new USB_Demo();
		t.period = 1;
		Task.install(t);
	}
}
