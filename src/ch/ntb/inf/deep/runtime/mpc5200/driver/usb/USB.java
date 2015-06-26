package ch.ntb.inf.deep.runtime.mpc5200.driver.usb;

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
	
	public USB(){
		HCCA = new byte[HCCA_SIZE];
	}
	
	public void action(){
		//USB ISR
	}
	
	public void init() throws UsbException{
		//init CDM Fractional Divider Config Register (internal USB Clock 48 MHz)
		US.PUT4(CDMFDCR, 0x04015555);
		//init GPS port config register for USB support
		int val = US.GET4(GPSPCR);	//WTF?
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
		
		// 5) Begin sending SOF tokens on the USB
		//TODO
		
		
		//TODO ohci_run -> create ohci class and use it there, now just for testing
		//TODO state -> halted
		val = (US.GET4(USBHCFIR) & 0x3FFF);
		if(val != FI){
			val = FI;
		}
		val |= (FSMPS << 16);						// set interval to 12000-1, set FSMP
		System.out.println("val:");
		System.out.println(val);
		US.PUT4(USBHCFIR, (val|FI) );		
		
		long cnt = 0;
		while( (US.GET4(USBHCRHP1SR)&0x001F0010) == 0 ){
			if(cnt > 10000000){
				System.out.println(".");
				cnt = 0;
			}
			cnt++;
		}
		
		System.out.println("Port status changed.");
	}
	
}
