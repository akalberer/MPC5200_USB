package ch.ntb.inf.deep.runtime.mpc5200.driver.usb;

import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.exceptions.UsbException;
import ch.ntb.inf.deep.runtime.ppc32.Task;

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
			System.out.println("init done");
			init = false;
			initDone = true;
		}
		if (nofActivations % 2000 == 0) {
			System.out.println("t");
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
	
	static{
		Task t = new OhciDevice();
		t.period = 1;
		Task.install(t);		
	}
}
