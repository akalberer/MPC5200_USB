package ch.ntb.inf.deep.runtime.mpc5200.driver.usb;

import ch.ntb.inf.deep.runtime.Kernel;
import ch.ntb.inf.deep.runtime.mpc5200.InterruptMpc5200io;
import ch.ntb.inf.deep.runtime.mpc5200.IphyCoreMpc5200io;
import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.exceptions.UsbException;
import ch.ntb.inf.deep.unsafe.US;

public class USB extends InterruptMpc5200io implements IphyCoreMpc5200io{

	private static final int FI = 0x2edf;		//fminterval
	private static final int FSMPS = (0x7FFF & (6 * (FI - 210)/7));
	private static final int OCR = 3;			
	
	private byte[] HCCA;
	private static final int HCCA_SIZE = 256;
	private byte[] testED;		//TODO has to be built in another way (possible to create more than one), just for test
	
	public USB(){
		HCCA = new byte[HCCA_SIZE];
		testED = new byte[16];
	}
	
	public void action(){
		//USB ISR
		System.out.println("Interrupt received");
	}
	
	public void init() throws UsbException{
		//init CDM Fractional Divider Config Register (internal USB Clock 48 MHz)
		US.PUT4(CDMFDCR, 0x04015555);
		//init GPS port config register for USB support
		int val = US.GET4(GPSPCR);
		val |= 0x00001000;
		val &= ~0x00002000;
		US.PUT4(GPSPCR, val);
		
		if( (US.GET4(USBHCREVR)& 0xFF) != 0x10 ){		// 1) check HC revision (compliant to USB 1.1)
			//wrong HC revision
			throw new UsbException("Wrong HC revision");
		}
		
		// 2) allocate and init any Host Controller structures, including HCCA block
				
		// 3) load driver, take control of host controller
		val = US.GET4(USBHCCMDSR);					// set OwnershipChangeRequest
		US.PUT4(USBHCCMDSR, (val|(1 << OCR)) );
		
		// 4) Set up HC registers and HC Communications Area
		US.PUT4(USBHCHCCAR, US.REF(HCCA));			// set pointer to HCCA
		// interrupt Routing bit is ignored from HC Control Register 
		// -> routed to interrupt controller in SIU -> route from there to SMI, NORMAL interrupt
		InterruptMpc5200io usbInt = new USB();
		InterruptMpc5200io.install(usbInt, 6); // USB is peripheral number 6
		US.PUT4(ICTLPIMR, US.GET4(ICTLPIMR) & ~0x02000000);	// accept interrupts from USB
		US.PUT4(USBHCCTRLR, (US.GET4(USBHCCTRLR) | 0x0300) ); // activate Interrupt routing of HC (ignored by 5200), and RemoteWakeupConnected 
		// read and set frame interval
		val = (US.GET4(USBHCFIR) & 0x3FFF);
		if(val != FI){
			val = FI;
		}
		val |= (FSMPS << 16);						// set interval to 12000-1, set FSMP
		System.out.println("val:");
		System.out.println(val);
		US.PUT4(USBHCFIR, (val|FI) );	
		US.PUT4(USBHCRHDRA, (US.GET4(USBHCRHDRA)| 0x00001200) ); // config PowerSwitchingMode, OverCurrentProtection
		//USBHCRHDRB -> config removable device is reset value -> nothing to do
		
		//now wait minimum time specified in the USB Specification for assertion of reset -> how much!?
		for(int i = 0; i < 10000; i++);
		
		//setup host controller
		int saveFmInterval = US.GET4(USBHCFIR);
		
		US.PUT4(USBHCCMDSR, (US.GET4(USBHCCMDSR)| 0x01) );	//issue a software reset
		long currentTime = Kernel.time();
		while( Kernel.time() - currentTime < 10);	//wait for 10us
		US.PUT4(USBHCFIR, saveFmInterval);			//restore frame interval register
		
		//check USB State -> should be USBSuspend
		if( (US.GET4(USBHCCTRLR) & 0x000000C0 ) != 0x0C0){
			System.out.println("not in suspend");
			throw new UsbException(Integer.toHexString((US.GET4(USBHCCTRLR) & 0x000000C0 )));
		}
		
		US.PUT4(USBHCCHEDR, US.REF(testED));	//set ED for control
		US.PUT4(USBHCIER, (US.GET4(USBHCIER)|0xC000007B));		// enable interrupts except SOF
		US.PUT4(USBHCCTRLR, (US.GET4(USBHCCTRLR) | 0x014));		// CLE > Control List Enable
		US.PUT4(USBHCPSR, (int)(0.9f*FI));
		
		// 5) Begin sending SOF tokens on the USB		
		// check for port status changed, but that's old
		long cnt = 0;
		while(true){
			if( (US.GET4(USBHCCTRLR) & 0x000000C0 ) == 0x080){
				System.out.println("Op");
			}
			
			if(cnt > 10000000){
				System.out.println(".");
				cnt = 0;
			}
			cnt++;
		}
		
	}
	
}
