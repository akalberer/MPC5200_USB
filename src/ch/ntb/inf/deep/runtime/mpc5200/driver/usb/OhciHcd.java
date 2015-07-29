package ch.ntb.inf.deep.runtime.mpc5200.driver.usb;

import ch.ntb.inf.deep.runtime.Kernel;
import ch.ntb.inf.deep.runtime.mpc5200.InterruptMpc5200io;
import ch.ntb.inf.deep.runtime.mpc5200.IphyCoreMpc5200io;
import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.exceptions.UsbException;
import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.OhciState;
import ch.ntb.inf.deep.unsafe.US;

public class OhciHcd extends InterruptMpc5200io implements IphyCoreMpc5200io{

	private static final int FI = 0x2edf;		//fminterval: 12000 bits per frame (-1)
	private static final int FSMPS = (0x7FFF & (6 * (FI - 210)/7));
	private static final int OCR = 3;			
	
	private static byte[] hcca;
	private static final int HCCA_SIZE = 256;
	private static int[] testED;		//TODO has to be built in another way (possible to create more than one), just for test
	private static int[] testED_empty;	//TODO -> for init -> need empty list?
	private static int[] testED_TD;		//TODO
	private static int[] testED_TD_data; //TODO
	private static int[] bulkEDQueue;
	private static int[] bulkEDQueue_empty; //TODO -> for init -> need empty list?
	private static int[] doneList;
	
	private static OhciState state;
	private static boolean intHalted = false;		// flag to detect if HC was halted due to interrupt
		
	public static final int PORT_RESET_HW_DELAY = 10000;	// 10ms delay -> is hardware specific
	public static final int PORT_RESET_DELAY = 50000;		// 50ms reset signaling
	
	/**
	 * HcControl (control) register masks
	 */
	public static final int OHCI_CTRL_PLE = 0x00000004;		// periodic list enable
	public static final int OHCI_CTRL_IE = 0x00000008;		// isochronous enable
	public static final int OHCI_CTRL_CLE = 0x00000010;		// control list enable
	public static final int OHCI_CTRL_BLE = 0x00000020;		// bulk list enable
	public static final int OHCI_CTRL_IR = 0x00000100;		// interrupt routing
	public static final int OHCI_CTRL_RWE = 0x00000400;		// remote wakeup enable
	public static final int OHCI_CTRL_HCFS = 0x000000C0;	// host controller functional state
	public static final int OHCI_CTRL_RWC = 0x00000200;		// remote wakeup connected
	public static final int OHCI_CTRL_CBSR = 0x00000003;	// control/bulk service ratio
	
	public static final int OHCI_SCHED_ENABLES = (OHCI_CTRL_CLE); //| OHCI_CTRL_BLE); //only bulk and control | OHCI_CTRL_PLE | OHCI_CTRL_IE);
	public static final int OHCI_CONTROL_INIT = OHCI_CTRL_CBSR;
	
	/**
	 * HcCommandStatus (cmdstatus) register masks
	 */
	public static final int OHCI_HCR = 0x00000001;		// host controller reset
	public static final int OHCI_CLF = 0x00000002;		// control list filled
	public static final int OHCI_BLF = 0x00000004;		// bulk list filled
	public static final int OHCI_OCR = 0x00000008;		// ownership change request
	public static final int OHCI_SOC = 0x00030000;		// scheduling overrun count
	
	public static final int OHCI_USB_RESET = 0x00000000;
	public static final int OHCI_USB_RESUME = 0x00000040;
	public static final int OHCI_USB_OPER = 0x00000080;
	public static final int OHCI_USB_SUSPEND = 0x000000C0;
			
	/** roothub a masks 
	*/
	public static final int	RH_A_NDP = 2;				// number of downstream ports
	public static final int	RH_A_PSM = 0x00000100;		// power switching mode
	public static final int	RH_A_NPS = 0x00000200;		// no power switching
	public static final int	RH_A_DT	= 0x00000400;		// device type (mbz)
	public static final int	RH_A_OCPM = 0x00000800;		// over current protection mode
	public static final int RH_A_NOCP =	0x00001000;		// no over current protection	
	public static final int RH_A_POTPGT = (0xFF << 24);	// power on to power good time
	
	/** roothub.portstatus [i] bits 
	*/
	public static final int RH_PS_CCS = 0x00000001;		// current connect status
	public static final int RH_PS_PES = 0x00000002;		// port enable status
	public static final int RH_PS_PSS = 0x00000004;		// port suspend status
	public static final int RH_PS_POCI = 0x00000008;	// port over current indicator
	public static final int RH_PS_PRS = 0x00000010;		// port reset status
	public static final int RH_PS_PPS = 0x00000100;		// port power status
	public static final int RH_PS_LSDA = 0x00000200;	// low speed device attached
	public static final int RH_PS_CSC = 0x00010000;		// connect status change
	public static final int RH_PS_PESC = 0x00020000;	// port enable status change
	public static final int RH_PS_PSSC = 0x00040000;	// port suspend status change
	public static final int RH_PS_OCIC = 0x00080000;	// over current indicator change
	public static final int RH_PS_PRSC = 0x00100000;	// port reset status change
	
	/**
	 * roothub status bits
	 */
	public static final int RH_HS_LPS = 0x00000001;		// local power status
	public static final int RH_HS_OCI = 0x00000002;		// over current indicator
	public static final int RH_HS_DRWE = 0x00008000;	// device remote wakeup enable
	public static final int RH_HS_LPSC = 0x00010000;	// local power status change
	public static final int RH_HS_OCIC = 0x00020000;	// over current indicator change
	public static final int RH_HS_CRWE = 0x80000000;	// clear remote wakeup enable
	
	/**
	 * masks used with interrupt registers:
	 * HcInterruptStatus (USBHCISR)
	 * HcInterruptEnable (USBHCIER)
	 * HcInterruptDisable (USBHCIDR)
	 */
	public static final int OHCI_INTR_SO = 0x00000001;		// scheduling overrun
	public static final int OHCI_INTR_WDH = 0x00000002;		// writeback of done_head
	public static final int OHCI_INTR_SF = 0x00000004;		// start frame
	public static final int OHCI_INTR_RD = 0x00000008;		// resume detect
	public static final int OHCI_INTR_UE = 0x00000010;		// unrecoverable error
	public static final int OHCI_INTR_FNO = 0x00000020;		// frame number overflow
	public static final int OHCI_INTR_RHSC = 0x00000040;	// root hub status change
	public static final int OHCI_INTR_OC = 0x40000000;		// ownership change
	public static final int OHCI_INTR_MIE = 0x80000000;		// master interrupt enable
	public static final int OHCI_INTR_INIT = (OHCI_INTR_MIE | OHCI_INTR_RHSC | OHCI_INTR_UE | OHCI_INTR_RD | OHCI_INTR_WDH);
	
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
			// device died, disconnected
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
		if((intState & OHCI_INTR_UE) != 0 ){		// unrecoverable error
			state = OhciState.OHCI_RH_HALTED;
			intHalted = true;
			ohci_hcd_reset();
			return;
		}
		
		//TODO RHSC, RD
		
		if((intState & OHCI_INTR_WDH) != 0){		// write done head
			updateDoneList();
		}
		// leaving INT_SF enabled when there's still unlinking to be done (next frame).
		if((intState & OHCI_INTR_SF) != 0 ){		// start of frame
			//TODO deactivate interrupt if no unlinking has to be done anymore (next frame) Z.948 ohci-hcd.c
		}
		
		if(state == OhciState.OHCI_RH_RUNNING){
			US.PUT4(USBHCISR, intState);		// clears ISR Flags
			US.PUT4(USBHCIER, 0x80000000);		// enable USB Interrupts again
		}		
	}
	
	public static void run() throws UsbException{
		//TODO used to check for exceptions occured in ISR, reinit HCD, etc.
		if(intHalted){
			throw new UsbException("HC died, detected by ISR");	//TODO improve error handling
		}
		//TODO thats not absolutely correct, just for testing:
		System.out.println("testED: ");
		System.out.println(US.REF(testED));
		US.PUT4(USBHCCHEDR, US.REF(testED));				//set ED for control transfer with a 0 setup packet
//		US.PUT4(USBHCBHEDR, US.REF(bulkEDQueue));			//set ED for bulk transfer

		US.PUT4(USBHCIER, (US.GET4(USBHCIER)|OHCI_INTR_SF));
		US.PUT4(USBHCCTRLR, (US.GET4(USBHCCTRLR)| OHCI_SCHED_ENABLES));	// activate list processing -> crashes processor!?
		US.PUT4(USBHCCMDSR, (US.GET4(USBHCCMDSR) | OHCI_CLF));	// set control list filled
//		US.GET4(USBHCCTRLR);		//flush writes?
	}
	
	/**
	 * reset root hub
	 * each reset needs around 10ms, need at least 5, better 6 resets to get the full 50ms reset signaling for the connected device
	 * @throws UsbException
	 */
	public static void resetRootHub() throws UsbException{
		int portResetFrameNumDone = US.GET4(USBHCFNR) + PORT_RESET_DELAY;	// check with actual frame number
		int	nofResets = 6;
		
		do{
			long currentTime = Kernel.time();
			long lastTime = 0;
			while(Kernel.time() - currentTime < 2*PORT_RESET_HW_DELAY){		// wait
				if( (US.GET4(USBHCRHP1SR) & RH_PS_PRS) == 0){	// if no reset pending
					lastTime = Kernel.time();
					break;
				}
			}
			
			if(lastTime == 0 || !(lastTime < (currentTime + 2*PORT_RESET_HW_DELAY)) ){	//to be sure, if it should happen, that on coincidence kernel.time is 0
				throw new UsbException("OhciHcd: hardware timeout");
			}
			
			int tmp = US.GET4(USBHCRHP1SR);
			if( (tmp & RH_PS_CCS) == 0 ){
				throw new UsbException("OhciHcd: no device connected");
			}
			if( (tmp & RH_PS_PRSC) == 1 ){		// port reset completed bit still 1 -> reset it by writing 1
				US.PUT4(USBHCRHP1SR, (US.GET4(USBHCRHP1SR) | RH_PS_PRSC ));
			}
			
			// start next reset, wait till it is probably done
			US.PUT4(USBHCRHP1SR, (US.GET4(USBHCRHP1SR) | RH_PS_PRS) );
			currentTime = Kernel.time();
			while(Kernel.time() - currentTime < PORT_RESET_HW_DELAY);		// wait 10ms
			
			nofResets--;	// max 6 resets with 10ms -> decrement			
		}while(	(US.GET4(USBHCFNR) < portResetFrameNumDone) && nofResets > 0);
	}
	
	private static void ohci_hcd_reset(){
		US.PUT4(USBHCCTRLR, (US.GET4(USBHCCTRLR) & OHCI_CTRL_RWC));		// reset HC, except RWC
		state = OhciState.OHCI_RH_HALTED;
	}
	
	private static void updateDoneList(){
		//TODO -> ohci-q.c Z.932ff
	}
	
	public static void periodicReInit(){
		// read and set frame interval
		int val = (US.GET4(USBHCFIR) & 0x3FFF);
		if(val != FI){
			val = FI;
		}
		val |= (FSMPS << 16);						// set interval to 12000-1, set FSMP
		US.PUT4(USBHCFIR, (val|FI) );	
		US.PUT4(USBHCPSR, (int)(FI*9/10));			// config periodic start (0.9*FrameInterval)
	}
	
	public static void init() throws UsbException{
		state = OhciState.OHCI_RH_HALTED;
		
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
		
		//init structures
		hcca = new byte[HCCA_SIZE];
		testED = new int[4];
		testED_empty = new int[4];
		testED_TD = new int[4];
		testED_TD_data = new int[64];
		bulkEDQueue = new int[4];
		bulkEDQueue_empty = new int[4];
		doneList = new int[512];
		
		//init first endpoints for testing !?
		// 2) allocate and init any Host Controller structures, including HCCA block
		testED[0] = 0x04000000;		// max 1024, Control Format -> F=0, speed full, direction from TD, endpoint 0, function address 0
		testED[1] = US.REF(testED_TD);	// TD Queue Tail pointer
		testED[2] = US.REF(testED_TD);	// TD Queue Head pointer
		testED[3] = 0x00000000;		// no next endpoint
		testED_TD[0] = 0x01E40000;	// buffer can be smaller (R=1), SETUP Packet, DelayInterrupt -> no Interrupt, DATA0, ErrorCount = 0 
		testED_TD[1] = US.REF(testED_TD_data);
		testED_TD[2] = 0x00000000;
		testED_TD[3] = US.REF((testED_TD[3] + 0x3)); 		//last byte so + 3
		
		testED_empty[0] = 0;
		testED_empty[1] = 0;
		testED_empty[2] = 0;
		testED_empty[3] = 0;
		bulkEDQueue_empty[0] = 0;
		bulkEDQueue_empty[1] = 0;
		bulkEDQueue_empty[2] = 0;
		bulkEDQueue_empty[3] = 0;
		
		// 3) load driver, take control of host controller
		US.PUT4(USBHCCMDSR, (US.GET4(USBHCCMDSR) |(1 << OCR)) ); // set OwnershipChangeRequest
		
		// -> routed to interrupt controller in SIU -> route from there to SMI, NORMAL interrupt
		InterruptMpc5200io ohciInt = new OhciHcd();
		InterruptMpc5200io.install(ohciInt, 6); 				// USB is peripheral number 6
		US.PUT4(ICTLPIMR, US.GET4(ICTLPIMR) & ~0x02000000);		// accept interrupts from USB
		if( (US.GET4(0xf0000530) & 0x00008000) != 0){			//already usb interrupt pending
			US.PUT4(0xf0000530, 0x00000000);
			System.out.println("already 1");
		}
//		US.PUT4(USBHCCTRLR, (US.GET4(USBHCCTRLR) | 0x00000200) ); 	// #!activate Interrupt routing of HC (with activated: 0x0300)(ignored by 5200), 
																	// and RemoteWakeupConnected -> when commented out not active -> only needed by hid-devices

		// read and set frame interval
		val = (US.GET4(USBHCFIR) & 0x3FFF);
		val = FI;
		val |= (FSMPS << 16);						// set interval to 12000-1, set FSMP
		US.PUT4(USBHCFIR, (val|FI) );	
		US.PUT4(USBHCPSR, (int)(FI*9/10));			// config periodic start (0.9*FrameInterval)
		
		long delay;
		switch(US.GET4(USBHCCTRLR) & OHCI_CTRL_HCFS){
			case OHCI_USB_OPER:
				delay = 0;
				break;
			case OHCI_USB_SUSPEND:
			case OHCI_USB_RESUME:
				US.PUT4(USBHCCTRLR, (US.GET4(USBHCCTRLR) & OHCI_CTRL_RWC));
				US.PUT4(USBHCCTRLR, (US.GET4(USBHCCTRLR) | OHCI_USB_RESUME));
				delay = 10000;		//10ms
				break;
			// case OHCI_USB_RESET
			default:
				US.PUT4(USBHCCTRLR, (US.GET4(USBHCCTRLR) & OHCI_CTRL_RWC));
				US.PUT4(USBHCCTRLR, (US.GET4(USBHCCTRLR) | OHCI_USB_RESET));
				delay = 50000;		//50ms
				break;
		}
		
		long currentTime = Kernel.time();
		while(Kernel.time() - currentTime < delay);			// wait
		
		for(int i = 0; i < hcca.length; i++){				//flush hcca
			hcca[i] = 0;
		}
		
		int saveFmInterval = US.GET4(USBHCFIR);
		US.PUT4(USBHCCMDSR, (US.GET4(USBHCCMDSR)| OHCI_HCR) );	//issue a software reset
		currentTime = Kernel.time();
		while( Kernel.time() - currentTime < 10);	//wait for 10us
		if ((US.GET4(USBHCCMDSR) & OHCI_HCR) != 0){
			//reset failed
			throw new UsbException("HC reset failed.");
		}
		US.PUT4(USBHCFIR, saveFmInterval);			//restore frame interval register
		
		//check USB State -> should be USBSuspend
		if( (US.GET4(USBHCCTRLR) & OHCI_CTRL_HCFS ) != OHCI_USB_SUSPEND){
			System.out.println("not in suspend");
			throw new UsbException(Integer.toHexString((US.GET4(USBHCCTRLR) & 0x000000C0 )));
		}
		state = OhciState.OHCI_RH_SUSPENDED;
		
		// now we're in the SUSPEND state ... must go OPERATIONAL within 2msec else HC enters RESUME

		US.PUT4(USBHCCHEDR, US.REF(testED_empty));				//set ED for control transfer
		US.PUT4(USBHCBHEDR, US.REF(bulkEDQueue_empty));			//set ED for bulk transfer
		
		// 4) Set up HC registers and HC Communications Area
		System.out.println("Addr HCCA: ");
		System.out.println(US.REF(hcca));
		US.PUT4(USBHCHCCAR, US.REF(hcca));			// set pointer to HCCA
		System.out.println("Addr Done: ");
		System.out.println(US.REF(doneList));
		US.PUT4(USBHCDHR, US.REF(doneList));		// memory for done head
				
		periodicReInit();
		
		if( (US.GET4(USBHCFIR) & 0x7fff0000) == 0){
			throw new UsbException("init error: Frame Interval Register is 0.");
			//probably jump to software reset to try again
		}
		
		//start controller operations
		US.PUT4(USBHCCTRLR, (US.GET4(USBHCCTRLR) & OHCI_CTRL_RWC));
		US.PUT4(USBHCCTRLR, (US.GET4(USBHCCTRLR) | OHCI_CONTROL_INIT | OHCI_USB_OPER));		
		state = OhciState.OHCI_RH_RUNNING;
		
		// wake on ConnectStatusChange, matching external hubs
		US.PUT4(USBHCRHSR, (US.GET4(USBHCRHSR) | RH_HS_DRWE ));
		
		// choose the interrupts we care about now, others later on demand
		US.PUT4(USBHCISR, 0xFFFFFFFF);				// clear interrupt status register
		US.PUT4(USBHCIER, OHCI_INTR_INIT);			// enable desired interrupts
		
		val = US.GET4(USBHCRHDRA);
		val &= ~(RH_A_PSM | RH_A_OCPM | RH_A_NPS);	// Power switching supported, all ports powered at the same time
		val |= (RH_A_NOCP | RH_A_NDP);				// config PowerSwitchingMode, OverCurrentProtection, NofDownstreamPorts
		val |= 0x32000000;							// 100ms PowerOnToPowerGoodTime
		US.PUT4(USBHCRHDRA, val ); 	
		US.PUT4(USBHCRHDRB, 0x00000000);							// power switching global, devices removable
//		US.PUT4(USBHCRHSR, 0x00000001);								// turn off power
		US.PUT4(USBHCRHSR, (US.GET4(USBHCRHSR) | RH_HS_LPSC)); 		//enable power on all ports

		delay = ((US.GET4(USBHCRHDRA) & 0xFF000000) >> 23) * 1000;	// wait -> POTPGT * 2 (in 2ms unit) delay after powering hub
		while(Kernel.time() - currentTime < delay);					// give USB device 100ms time to set up
				
//		US.PUT4(USBHCIER, (US.GET4(USBHCIER)|0xC000007B));		// enable interrupts except SOF
//		US.PUT4(USBHCCTRLR, (US.GET4(USBHCCTRLR) | 0x010));		// CLE > Control List Enable
//		// set ControlListFilled
//		US.PUT4(USBHCCMDSR, (US.GET4(USBHCCMDSR) | 0x02));
//		
//
//		
//		// reload HC Frame Remaining register before going USB Operational
//		US.PUT4(USBHCFRR, (US.GET4(USBHCFRR) | FI) );
////		// enable SOF interrupts to test Interrupts with deep
////		US.PUT4(USBHCIER, (US.GET4(USBHCIER) | 0xC000007F));
//		// 5) Begin sending SOF tokens on the USB -> set HcControl to USB Operational
//		val = US.GET4(USBHCCTRLR);
//		val &= ~0x00000040;
//		val |= 0x00000080;
//		US.PUT4(USBHCCTRLR, val);
//		// HC begins sending SOF 1ms later
//		state = OhciState.OHCI_RH_RUNNING;	
	}
	
	static{
						
		US.PUT4(GPWER, US.GET4(GPWER) | 0x80000000);	// enable GPIO use
		US.PUT4(GPWDDR, US.GET4(GPWDDR) | 0x80000000);	// make output
		US.PUT4(GPWOUT, US.GET4(GPWOUT) | 0x80000000);
	}
	
}
