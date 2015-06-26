package ch.ntb.inf.deep.runtime.mpc5200.demo;

import java.io.PrintStream;

import ch.ntb.inf.deep.runtime.mpc5200.driver.UART3;
import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.USB;
import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.exceptions.UsbException;

public class USB_Demo{
	
	static{
		
		// Initialize UART (9600 8N1)
		UART3.start(9600, UART3.NO_PARITY, (short)8);

		// Use the UART3 for stdout and stderr
		System.out = new PrintStream(UART3.out);
		System.err = System.out;

		// Print a string to the stdout
		System.out.println("USB Demo:");
		
		USB usb = new USB();
		try{
			usb.init();
		}
		catch(UsbException e){
			e.printStackTrace();
		}
	}
}
