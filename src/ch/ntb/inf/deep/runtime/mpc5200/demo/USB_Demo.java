package ch.ntb.inf.deep.runtime.mpc5200.demo;

import java.io.PrintStream;

import ch.ntb.inf.deep.runtime.mpc5200.driver.UART3;
import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.Device;
import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.OhciHcd;
import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.TransferDirection;
import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.exceptions.UsbException;
import ch.ntb.inf.deep.runtime.ppc32.Task;

public class USB_Demo extends Task{
	
	private Device usbDev;
	private boolean initDone = false;
	private boolean writeDone = false;
	private static byte[] testData = new byte[]{(byte)0x41, (byte)0x42, (byte)0x43, (byte)0x30};
	private static byte[] testData2 = new byte[]{(byte)0x30, (byte)0x31, (byte)0x32, (byte)0x0D, (byte)0x0A};
	private static byte[] readData;
	
	public void action(){
		if(!initDone){
			usbDev = new Device();
			if(OhciHcd.initDone()){
				initDone = true;
			}
		}
		if(initDone && !usbDev.isOpen() ){
			try {
				usbDev.open(1, 0, 1);
			} catch (UsbException e) {
				System.out.println("USB dev open failed.");
				e.printStackTrace();
			}
		}
		if(initDone && usbDev.isOpen() && !writeDone){
//			try{
//				usbDev.bulkTransfer(2, TransferDirection.OUT, testData2, testData2.length);
				writeDone = true;
//			}
//			catch (UsbException e){
//				System.out.println("Bulk transfer failed.");
//				e.printStackTrace();
//			}
		}
		if(initDone && usbDev.isOpen() && writeDone && (nofActivations % 2000 == 0)){
			try{
//				usbDev.bulkTransfer(1, TransferDirection.IN, readData, readData.length);
				usbDev.bulkTransfer(2, TransferDirection.OUT, testData2, testData2.length);
			}
			catch(UsbException e){
				System.out.println("Bulk in failed");
			}
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
		
		readData = new byte[8];
	}
}
