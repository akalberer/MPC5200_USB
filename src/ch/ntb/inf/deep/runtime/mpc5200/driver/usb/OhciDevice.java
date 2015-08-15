package ch.ntb.inf.deep.runtime.mpc5200.driver.usb;

import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.exceptions.UsbException;
import ch.ntb.inf.deep.runtime.ppc32.Task;

public class OhciDevice extends Task{

	private static boolean init = true;
	private static boolean initDone = false;
	private static boolean done = false;
	
	private static UsbRequest setConfig;
	private static UsbRequest setInterface;
	
	private UsbRequest bulkTransfer;
	
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
	
	public static void open(int cfgValue, int ifaceNum, int altSetting) throws UsbException{
		setConfig = new UsbRequest();
		setInterface = new UsbRequest();
		
		setConfig.setConfiguration(cfgValue);
		//TODO leave set interface out
//		setInterface.setInterface(ifaceNum, altSetting);
	}
	
	public static void bulkTransfer(UsbRequest usbReq, int endpointNumber, TransferDirection dir, byte[] data, int dataLength) throws UsbException{
		OhciHcd.setBulkEndpointNumber(endpointNumber, dir);
		usbReq.bulkTransfer(endpointNumber, dir, data, dataLength);
	}
	
	static{
		Task t = new OhciDevice();
		t.period = 1;
		Task.install(t);		
	}
}
