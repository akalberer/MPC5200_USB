package ch.ntb.inf.deep.runtime.mpc5200.driver.usb;

import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.exceptions.UsbException;
import ch.ntb.inf.deep.runtime.ppc32.Task;

/**
 * OHCI Device for use with OhciHcd (Host Controller Driver) on MPC5200
 * 
 * @author Andreas Kalberer
 */

public class OhciDevice extends Task{

	private static boolean init = true;
	private static boolean initDone = false;
	private static boolean done = false;
	
	private static UsbRequest setConfig;
	private static UsbRequest setInterface;
	
	public void action(){
		if(init){
			try{
				OhciHcd.init();
			}
			catch(UsbException e){
				e.printStackTrace();
			}
			init = false;
			initDone = true;
		}
		if(OhciHcd.inISR == true){
//			System.out.println(".");
			OhciHcd.inISR = false;
		}
		if(!done && initDone){
			try {
				OhciHcd.run();
			} catch (UsbException e) {
				System.out.println("UsbException occured:");
				e.printStackTrace();
			}
			done = true;
		}
	}
	
	/**
	 * open the OhciDevice present on the MPC5200 USB
	 * @param cfgValue		index of desired configuration
	 * @param ifaceNum		interface number
	 * @param altSetting	alternate setting
	 * @throws UsbException		on failure
	 */
	static void open(int cfgValue, int ifaceNum, int altSetting) throws UsbException{
		setConfig = new UsbRequest();
		setInterface = new UsbRequest();
		
		setConfig.setConfiguration(cfgValue);
		//TODO AVR USB does not support it, so leave it out at the moment
//		setInterface.setInterface(ifaceNum, altSetting);
	}
	
	/**
	 * initiate a bulk transfer on the OhciHcd
	 * @param usbReq			USB request used to create the needed structs
	 * @param endpointNumber	desired endpoint
	 * @param dir				transfer direction, see enum, TransferDirection.IN or .OUT is possible
	 * @param data				data to transfer, or with IN transfer, byte array for read data
	 * @param dataLength		length of transfer
	 * @throws UsbException		on failure
	 */
	static void bulkTransfer(UsbRequest usbReq, int endpointNumber, TransferDirection dir, byte[] data, int dataLength) throws UsbException{
		OhciHcd.setBulkEndpointNumber(endpointNumber, dir);
		usbReq.bulkTransfer(endpointNumber, dir, data, dataLength);
	}
	
	static{
		Task t = new OhciDevice();
		t.period = 1;
		Task.install(t);		
	}
}
