package ch.ntb.inf.deep.runtime.mpc5200.driver.usb;

import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.exceptions.UsbException;
import ch.ntb.inf.deep.runtime.ppc32.Task;

public class Device extends Task{
	
	private boolean open = false;
	
	public Device(){
		
	}
	
	public void open() throws UsbException{
		//TODO
	}
	
	public void open(int cfgValue, int ifaceNum, int altSetting) throws UsbException{
		OhciDevice.open(cfgValue, ifaceNum, altSetting);
		open = true;
	}
	
	public boolean isOpen(){
		return open;
	}
}
