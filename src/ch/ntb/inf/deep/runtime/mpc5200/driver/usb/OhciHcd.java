package ch.ntb.inf.deep.runtime.mpc5200.driver.usb;

import ch.ntb.inf.deep.runtime.Kernel;
import ch.ntb.inf.deep.runtime.mpc5200.InterruptMpc5200io;
import ch.ntb.inf.deep.runtime.mpc5200.IphyCoreMpc5200io;
import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.exceptions.UsbException;
import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.OhciState;
import ch.ntb.inf.deep.unsafe.US;

public class OhciHcd extends InterruptMpc5200io implements IphyCoreMpc5200io{

	private static final int FI = 0x2edf;		//fminterval
	private static final int FSMPS = (0x7FFF & (6 * (FI - 210)/7));
	private static final int OCR = 3;			
	
	private static byte[] HCCA;
	private static final int HCCA_SIZE = 256;
	private static int[] testED;		//TODO has to be built in another way (possible to create more than one), just for test
	private static int[] testED_TD;		//TODO
	private static int[] testED_TD_data; //TODO
	private static int[] bulkEDQueue;
	
	private static OhciState state;
	private static boolean intHalted = false;		// flag to detect if HC was halted due to interrupt
	
	public static final int OHCI_INT_RHSC = 0x00000040;
	public static final int OHCI_INT_FNO = 0x00000020;
	public static final int OHCI_INT_UE = 0x00000010;
	public static final int OHCI_INT_RD = 0x00000008;
	public static final int OHCI_INT_SF = 0x00000004;
	public static final int OHCI_INT_WDH = 0x00000002;
	public static final int OHCI_INT_SO = 0x00000001;
	
	public static boolean inISR = false;
//	public USB(){
//		
//	}
	
	public void action(){
		US.PUT4(GPWOUT, US.GET4(GPWOUT) & ~0x80000000);	//switch led on
		//USB ISR
		//prevent USB from generating Interrupt again before we are finished
		US.PUT4(USBHCIDR, 0x80000000);
		inISR = true;
		
		int  intState = US.GET4(USBHCISR);
		
		//check for an all 1's result -> typical consequence of dead, unclocked or unplugged devices
		if( intState == 0x4000007F ){
			// device died, disconneected
			state = OhciState.OHCI_RH_HALTED;
			intHalted = true;
			return;
		}
		
		// only check for enabled interrupts
		intState &= US.GET4(USBHCIER);
		
		if(intState == 0 || (state == OhciState.OHCI_RH_HALTED) ){
			// interrupt probably from other device, not from this USB
			//TODO throw Exception?
			return;
		}
		if((intState & OHCI_INT_UE) != 0 ){		// unrecoverable error
			state = OhciState.OHCI_RH_HALTED;
			intHalted = true;
			ohci_hcd_reset();
			return;
		}
		
		//TODO RHSC, RD
		
		if((intState & OHCI_INT_WDH) != 0){		// write done head
			updateDoneList();
		}
		// leaving INT_SF enabled when there's still unlinking to be done (next frame).
		if((intState & OHCI_INT_SF) != 0 ){		// start of frame
			//TODO deactivate interrupt if no unlinking has to be done anymore (next frame) Z.948 ohci-hcd.c
		}
		
		if(state == OhciState.OHCI_RH_RUNNING){
			US.PUT4(USBHCISR, intState);		// clears ISR Flags
			US.PUT4(USBHCIDR, 0x80000000);		// enable USB Interrupts again
		}
				
	}
	
	public static void run() throws UsbException{
		//TODO used to check for exceptions occured in ISR, reinit HCD, etc.
		if(intHalted){
			throw new UsbException("HC died, detected by ISR");	//TODO improve error handling
		}
	}
	
	private static void ohci_hcd_reset(){
		US.PUT4(USBHCCTRLR, (US.GET4(USBHCCTRLR) & 0x200));		// reset HC, except RWC
		state = OhciState.OHCI_RH_HALTED;
	}
	
	private static void updateDoneList(){
		//TODO -> ohci-q.c Z.932ff
	}
	
	public static void init() throws UsbException{
		//init CDM Fractional Divider Config Register (internal USB Clock 48 MHz)
		int val = US.GET4(CDMFDCR);
		if( (US.GET4(CDMPORCR) & 0x40) == 0x40 ){		// assumes 33Mhz clock
			val |= 0x00010001;							// checkout 5200lite.c
		}
		else{
			val |= 0x00015555;								
		}
		US.PUT4(CDMFDCR,  val);		// config ext_48mhz_en, fraction divider enable, divider counter
		
		//init GPS port config register for USB support
		val = US.GET4(GPSPCR);
		val &= ~0x00800000;			// internal 48MHz USB Clock, pin is GPIO
		val &= ~0x00007000;			// USB Differential mode
		val |= 0x00001000;			// USB 1
		US.PUT4(GPSPCR, val);
		
		if( (US.GET4(USBHCREVR)& 0xFF) != 0x10 ){		// 1) check HC revision (compliant to USB 1.1)
			//wrong HC revision
			throw new UsbException("Wrong HC revision");
		}
		
		// -> routed to interrupt controller in SIU -> route from there to SMI, NORMAL interrupt
		InterruptMpc5200io ohciInt = new OhciHcd();
		InterruptMpc5200io.install(ohciInt, 6); 				// USB is peripheral number 6
		US.PUT4(ICTLPIMR, US.GET4(ICTLPIMR) & ~0x02000000);		// accept interrupts from USB
		US.PUT4(USBHCCTRLR, (US.GET4(USBHCCTRLR) | 0x00000200) ); 	// #!activate Interrupt routing of HC (with activated: 0x0300)(ignored by 5200), and RemoteWakeupConnected
		
		US.PUT4(USBHCRHDRA, (US.GET4(USBHCRHDRA & ~0x00000300))); 	// Power switching supported, all ports powered at the same time
		US.PUT4(USBHCRHDRA, (US.GET4(USBHCRHDRA)| 0x00001002) ); 	// config PowerSwitchingMode, OverCurrentProtection
		US.PUT4(USBHCRHDRB, 0x00000000);							// power switching global, devices removable
		US.PUT4(USBHCRHSR, 0x00000001);								// turn off power
		
		US.PUT4(USBHCRHSR, (US.GET4(USBHCRHSR) | 0x00010000)); 		//enable power on all ports
		
		// 2) allocate and init any Host Controller structures, including HCCA block
		testED[0] = 0x04000000;		// max 1024, Control Format -> F=0, speed full, direction from TD, endpoint 0, functino address 0
		testED[1] = US.REF(testED_TD);	// TD Queue Tail pointer
		testED[2] = US.REF(testED_TD);	// TD Queue Head pointer
		testED[3] = 0x00000000;		// no next endpoint
	    testED_TD[0] = 0x01E40000;	// buffer can be smaller (R=1), SETUP Packet, DelayInterrupt -> no Interrupt, DATA0, ErrorCount = 0 
		testED_TD[1] = US.REF(testED_TD_data);
		testED_TD[2] = 0x00000000;
		testED_TD[3] = US.REF((testED_TD[3] + 0x3)); 		//last byte so + 3
		US.PUT4(USBHCCHEDR, US.REF(testED));				//set ED for control transfer
//		US.PUT4(USBHCBHEDR, US.REF(bulkEDQueue));			//set ED for bulk transfer
		
		// 3) load driver, take control of host controller
		US.PUT4(USBHCCMDSR, (US.GET4(USBHCCMDSR) |(1 << OCR)) ); // set OwnershipChangeRequest
		
		// 4) Set up HC registers and HC Communications Area
		System.out.println("Addr HCCA: ");
		System.out.println(US.REF(HCCA));
		US.PUT4(USBHCHCCAR, US.REF(HCCA));			// set pointer to HCCA
		
		// read and set frame interval
		val = (US.GET4(USBHCFIR) & 0x3FFF);
		if(val != FI){
			val = FI;
		}
		val |= (FSMPS << 16);						// set interval to 12000-1, set FSMP
		System.out.println("val:");
		System.out.println(val);
		US.PUT4(USBHCFIR, (val|FI) );	
		US.PUT4(USBHCPSR, (int)(FI*9/10));			// config periodic start (0.9*FrameInterval)
						
		//setup host controller
				
		US.PUT4(USBHCIER, (US.GET4(USBHCIER)|0xC000007B));		// enable interrupts except SOF
		US.PUT4(USBHCCTRLR, (US.GET4(USBHCCTRLR) | 0x010));		// CLE > Control List Enable
		// set ControlListFilled
		US.PUT4(USBHCCMDSR, (US.GET4(USBHCCMDSR) | 0x02));
		
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
		state = OhciState.OHCI_RH_SUSPENDED;
		
		// reload HC Frame Remaining register before going USB Operational
		US.PUT4(USBHCFRR, (US.GET4(USBHCFRR) | FI) );
//		// enable SOF interrupts to test Interrupts with deep
//		US.PUT4(USBHCIER, (US.GET4(USBHCIER) | 0xC000007F));
		// 5) Begin sending SOF tokens on the USB -> set HcControl to USB Operational
		val = US.GET4(USBHCCTRLR);
		val &= ~0x00000040;
		val |= 0x00000080;
		US.PUT4(USBHCCTRLR, val);
		// HC begins sending SOF 1ms later
		state = OhciState.OHCI_RH_RUNNING;	
	}
	
	static{
		HCCA = new byte[HCCA_SIZE];
		testED = new int[4];
		testED_TD = new int[4];
		testED_TD_data = new int[64];
		bulkEDQueue = new int[4];
		
		US.PUT4(GPWER, US.GET4(GPWER) | 0x80000000);	// enable GPIO use
		US.PUT4(GPWDDR, US.GET4(GPWDDR) | 0x80000000);	// make output
		US.PUT4(GPWOUT, US.GET4(GPWOUT) | 0x80000000);
	}
	
}
