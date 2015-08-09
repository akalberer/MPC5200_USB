package ch.ntb.inf.deep.runtime.mpc5200.driver.usb;

import ch.ntb.inf.deep.runtime.mpc5200.demo.USB_Demo;
import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.exceptions.UsbException;
import ch.ntb.inf.deep.runtime.ppc32.Task;

public class Device extends Task{
	
	public void open() throws UsbException{
		//TODO
	}
	
	public void open(int cfgValue, int ifaceNum, int altSetting){
		OhciDevice.open(cfgValue, ifaceNum, altSetting);
	}
	
	public void init(){
		OhciDevice.init();
	}
	
	
}
