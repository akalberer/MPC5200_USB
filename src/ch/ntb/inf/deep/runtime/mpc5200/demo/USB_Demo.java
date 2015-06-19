package ch.ntb.inf.deep.runtime.mpc5200.demo;

import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.USB;

public class USB_Demo {

	static{
		USB usb = new USB();
		usb.init();
	}
}
