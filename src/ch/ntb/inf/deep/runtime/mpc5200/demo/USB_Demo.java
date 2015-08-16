package ch.ntb.inf.deep.runtime.mpc5200.demo;

import java.io.PrintStream;
import ch.ntb.inf.deep.runtime.mpc5200.driver.UART3;
import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.Device;
import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.OhciHcd;
import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.TransferDirection;
import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.exceptions.UsbException;
import ch.ntb.inf.deep.runtime.ppc32.Task;

/**
 * USB demo application with AT90USB1287 and CombiExp Board of NTB,
 * <br>
 * AVR has to be loaded with UsbTempMeasure Demo software.<br>
 *
 * Overview:<br>
 * The AVR measures the temperature of the on-board NTC and sends the actual
 * temperature over USB to the MPC5200. If the temperature is over 30°C, the MPC5200
 * issues a warning string that temperature is now over 30°C.
 * This demo is created to test the OHCI-Host Controller Driver for the MPC5200.
 * 
 * @author Andreas Kalberer
 *
 */

public class USB_Demo extends Task{
	
	private Device usbDev;
	private boolean initDone = false;
	private static byte[] demoData;
	private static byte[] tempWarning;
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
				usbDev.bulkTransfer(2, TransferDirection.OUT, demoData, demoData.length);
			} catch (UsbException e) {
				System.out.println("USB dev open failed.");
				e.printStackTrace();
			}
		}
		if(initDone && usbDev.isOpen() && (nofActivations % 100 == 0)){		// read every 100ms
			try{

				usbDev.bulkTransfer(1, TransferDirection.IN, readData, readData.length);
			}
			catch(UsbException e){
				System.out.println("Bulk IN transfer failed.");
				e.printStackTrace();
			}
		}
		if(readData[0] != 0){
			int i;
			if(readData[0] == 0x01 && readData[1] == 0x60){		// if FTDI protocol: cut the first two bytes
				i = 2;
				readData[0] = 0;		//"read" header bytes of FTDI protocol
				readData[1] = 0;
			}
			else{
				i = 0;
			}
			for(; (i < readData.length) && (readData[i] != 0); i++){
				System.out.print((char)(readData[i]));
				if(i == 15 && readData[i] >= 0x33){		// temperature >= 30°C?
					try{
						usbDev.bulkTransfer(2, TransferDirection.OUT, tempWarning, tempWarning.length);
					}
					catch(UsbException e){
						System.out.println("Bulk OUT transfer failed.");
						e.printStackTrace();
					}
				}
				readData[i] = 0;
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
		
		// init demo task
		Task t = new USB_Demo();
		t.period = 1;
		Task.install(t);
		
		// init strings -> byte array
		readData = new byte[64];
		demoData = "USB Demo:\r\n".getBytes();
		tempWarning = "Temperature > 30°C!\r\n".getBytes();
	}
}
