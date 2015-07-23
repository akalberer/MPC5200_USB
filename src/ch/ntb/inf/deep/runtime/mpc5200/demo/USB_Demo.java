package ch.ntb.inf.deep.runtime.mpc5200.demo;

import java.io.PrintStream;

import ch.ntb.inf.deep.runtime.mpc5200.driver.UART3;
import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.OhciHcd;
import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.exceptions.UsbException;
import ch.ntb.inf.deep.runtime.ppc32.Task;

public class USB_Demo extends Task{
	
	public void action(){
		if (nofActivations % 2000 == 0) {
			System.out.println("t");
		}
		if(OhciHcd.inISR == true){
			System.out.println(".");
		}
		try {
			OhciHcd.run();
		} catch (UsbException e) {
			System.out.println("UsbException occured:");
			e.printStackTrace();
		}
	}
	
	static{
		
		// Initialize UART (9600 8N1)
		UART3.start(9600, UART3.NO_PARITY, (short)8);

		// Use the UART3 for stdout and stderr
		System.out = new PrintStream(UART3.out);
		System.err = System.out;

		Task t = new USB_Demo();
		t.period = 1;
		Task.install(t);
		// Print a string to the stdout
		System.out.println("USB Demo:");
		
		try{
			OhciHcd.init();
		}
		catch(UsbException e){
			e.printStackTrace();
		}
		System.out.println("init done");

	}
}
