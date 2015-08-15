package ch.ntb.inf.deep.runtime.mpc5200.driver.usb;

import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.exceptions.UsbException;
import ch.ntb.inf.deep.runtime.ppc32.Task;

/**
 * USB Device class for the MPC5200
 *
 * @author Andreas Kalberer
 */

public class Device extends Task{
	
	private boolean open = false;
	private UsbRequest usbReq;
	
	/**
	 * create an USB-Device
	 */
	public Device(){
		
	}
	
	public void open() throws UsbException{
		//TODO
	}
	
	/**
	 * open a USB device with the following parameters
	 * @param cfgValue		index of desired configuration
	 * @param ifaceNum		interface number
	 * @param altSetting	alternate setting
	 * @throws UsbException		on failure
	 */
	public void open(int cfgValue, int ifaceNum, int altSetting) throws UsbException{
		OhciDevice.open(cfgValue, ifaceNum, altSetting);
		open = true;
	}
	
	/**
	 * check if device is open and configuration set
	 * @return
	 */
	public boolean isOpen(){
		return open;
	}
	
	/**
	 * initiate a Bulk transfer
	 * @param endpointNumber	desired endpoint
	 * @param dir				transfer direction, see enum, TransferDirection.IN or .OUT is possible
	 * @param data				data to transfer, or with IN transfer, byte array for read data
	 * @param dataLength		length of transfer
	 * @throws UsbException		on failure
	 */
	public void bulkTransfer(int endpointNumber, TransferDirection dir, byte[] data, int dataLength) throws UsbException{
		if(open){
			usbReq = new UsbRequest();
			OhciDevice.bulkTransfer(usbReq, endpointNumber, dir, data, dataLength);
		}
		else{
			throw new UsbException("USB Device is not open!");
		}
	}
}
