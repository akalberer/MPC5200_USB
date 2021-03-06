package ch.ntb.inf.deep.runtime.mpc5200.driver.usb;

import ch.ntb.inf.deep.runtime.Kernel;
import ch.ntb.inf.deep.runtime.mpc5200.InterruptMpc5200io;
import ch.ntb.inf.deep.runtime.mpc5200.IphyCoreMpc5200io;
import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.exceptions.UsbException;
import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.OhciState;
import ch.ntb.inf.deep.unsafe.US;

/**
 * OHCI (Open Host Controller Interface) Host Controller Driver for the MPC5200
 * 
 * @author Andreas Kalberer
 */

public class OhciHcd extends InterruptMpc5200io implements IphyCoreMpc5200io{

	private static final boolean verbose_dbg = false;
	
	/**
	 * memory structures for host controller and enumeration
	 */
	private static byte[] hcca;
	private static final int HCCA_SIZE = 263;	//256 + 7 
	private static OhciEndpointDescriptor controlEndpointDesc;
	private static OhciEndpointDescriptor bulkEndpointOutDesc;
	private static OhciEndpointDescriptor bulkEndpointInDesc;
	private static UsbRequest getDevDescEnum;
	private static byte[] dataEnumDevDesc;
	private static byte[] devDesc;
	private static UsbRequest getDevDesc;
	private static TransferDescriptor emptyControlTD;
	private static TransferDescriptor emptyBulkOutTD;
	private static TransferDescriptor emptyBulkInTD;
	private static int[] doneList;
	private static UsbRequest setUsbAddress;
	private static final int usbDevAddress = 1;
	
	private static boolean initDone = false;
	
	/**
	 * data toggle states of different transfers, false: DATA0; true: DATA1
	 */
	private static boolean dataToggleControl = true; 		// false: DATA0; true: DATA1
	private static boolean dataToggleBulkOut = true;		// false: DATA0; true: DATA1
	private static boolean dataToggleBulkIn = true;			// false: DATA0; true: DATA1
	
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
	
	public static final int OHCI_SCHED_ENABLES = (OHCI_CTRL_CLE | OHCI_CTRL_BLE); //| OHCI_CTRL_PLE | OHCI_CTRL_IE);
	public static final int OHCI_CONTROL_INIT = 0;//OHCI_CTRL_CBSR;
	
	/**
	 * HcCommandStatus (cmdstatus) register masks
	 */
	public static final int OHCI_HCR = 0x00000001;		// host controller reset
	public static final int OHCI_CLF = 0x00000002;		// control list filled
	public static final int OHCI_BLF = 0x00000004;		// bulk list filled
	public static final int OHCI_OCR = 0x00000008;		// ownership change request
	public static final int OHCI_SOC = 0x00030000;		// scheduling overrun count
	
	/**
	 * Host controller functional states (HCFS)
	 */
	public static final int OHCI_USB_RESET = 0x00000000;	// reset
	public static final int OHCI_USB_RESUME = 0x00000040;	// resume
	public static final int OHCI_USB_OPER = 0x00000080;		// operational
	public static final int OHCI_USB_SUSPEND = 0x000000C0;	// suspend
			
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
	 * frame interval values
	 */
	public static final int FI = 0x2edf;				//frame interval: 12000 bits per frame (-1)
	public static final int FSMPS = (0x7FFF & (6 * (FI - 210)/7));	// FSLargestDataPacket	
	
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
	
	/**
	 * check if exceptions on USB communication occured and later reinit HC
	 * @throws UsbException
	 */
	public static void run() throws UsbException{
		//TODO used to check for exceptions occured in ISR, reinit HCD, etc.
		if(intHalted){
			throw new UsbException("HC died, detected by ISR");	//TODO improve error handling, add reinit etc.
		}
	}
	
	/**
	 * reset root hub
	 * each reset needs around 10ms, need at least 5, better 6 resets to get the full 50ms reset signaling for the connected device
	 * @throws UsbException on failure
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
	
	/**
	 * check if init of host controller is done
	 * @return true: if finished, else false
	 */
	public static boolean initDone(){
		return initDone;
	}
	
	/**
	 * enumerate the connected device on the USB port
	 * @throws UsbException
	 */
	public static void enumerateDevice() throws UsbException{
		long currentTime = Kernel.time();
		while(Kernel.time() - currentTime < 10000); // wait 10ms after reset
		
		US.PUT4(USBHCIER, (US.GET4(USBHCIER)|OHCI_INTR_SF));
		US.PUT4(USBHCCTRLR, (US.GET4(USBHCCTRLR)| OHCI_SCHED_ENABLES));	// activate list processing
		
		controlEndpointDesc.setSkipBit();		// set skip bit
		controlEndpointDesc.setTdTailPointer(emptyControlTD.getTdAddress());		// TD Queue Tail pointer
		controlEndpointDesc.setTdHeadPointer(emptyControlTD.getTdAddress());		// TD Queue Head pointer
		controlEndpointDesc.setNextEndpointDescriptor(controlEndpointDesc.getEndpointDescriptorAddress());		// no next endpoint
		controlEndpointDesc.clearSkipBit();			//clear skip bit
		
		dataEnumDevDesc = new byte[8];
		getDevDescEnum = new UsbRequest();
		getDevDescEnum.getDeviceDescriptorEnumeration(dataEnumDevDesc);
		
		while( !getDevDescEnum.controlDone() );		// wait for dev descriptor read
		
		setUsbAddress = new UsbRequest();
		setUsbAddress.setAddress(1);
		while( !setUsbAddress.controlDone() );		// wait for finish of set address
		
		//then switch address of usb dev in endpoint
		controlEndpointDesc.setUsbDevAddress(usbDevAddress);
		bulkEndpointOutDesc.setUsbDevAddress(usbDevAddress);
		bulkEndpointInDesc.setUsbDevAddress(usbDevAddress);
		
		controlEndpointDesc.setMaxPacketSize(dataEnumDevDesc[7]);		//set skip bit and mps 64byte
		devDesc = new byte[18];
		getDevDesc = new UsbRequest();
		getDevDesc.getDeviceDescriptor(devDesc);
	}
	
	/**
	 * set control list filled flag in command status register (USBHCCMDSR -> CLF)
	 */
	public static void setControlListFilled(){
		US.PUT4(USBHCCMDSR, (US.GET4(USBHCCMDSR) | OHCI_CLF));
	}
	
	/**
	 * set bulk list filled flag in command status register (USBHCCMDSR -> BLF)
	 */
	public static void setBulkListFilled(){
		US.PUT4(USBHCCMDSR, (US.GET4(USBHCCMDSR) | OHCI_BLF));
	}
	
	/**
	 * reset ohci host controller
	 */
	private static void ohci_hcd_reset(){
		US.PUT4(USBHCCTRLR, (US.GET4(USBHCCTRLR) & OHCI_CTRL_RWC));		// reset HC, except RWC
		state = OhciState.OHCI_RH_HALTED;
	}
	
	/**
	 * set skip bit of control endpoint
	 */
	public static void skipControlEndpoint(){
		controlEndpointDesc.setSkipBit();
	}
	
	/**
	 * clear skip bit of control endpoint and set control list filled flag
	 */
	public static void resumeControlEndpoint(){
		controlEndpointDesc.clearSkipBit();
		setControlListFilled();		// set control list filled
	}
	
	/**
	 * set skip bit of OUT endpoint
	 */
	public static void skipBulkEndpointOut(){
		bulkEndpointOutDesc.setSkipBit();
		US.PUT4(USBHCCTRLR, (US.GET4(USBHCCTRLR)& ~(OHCI_CTRL_CLE)));	// deactivate control list processing
	}
	
	/**
	 * clar skip bit of OUT endpoint and set bulk list filled flag
	 */
	public static void resumeBulkEndpointOut(){
		bulkEndpointOutDesc.clearSkipBit();		
		setBulkListFilled();
	}
	
	/**
	 * set skip bit of IN endpoint
	 */
	public static void skipBulkEndpointIn(){
		bulkEndpointInDesc.setSkipBit();
		US.PUT4(USBHCCTRLR, (US.GET4(USBHCCTRLR)& ~(OHCI_CTRL_CLE)));	// deactivate control list processing
	}
	
	/**
	 * clear skip bit of IN endpoint and set bulk list filled flag
	 */
	public static void resumeBulkEndpointIn(){
		bulkEndpointInDesc.clearSkipBit();
		setBulkListFilled();
	}
	
	/**
	 * enqueue TransferDescriptor to the corresponding list
	 * @param epType	type of endpoint needed for transfer
	 * @param td		TransferDescriptor that should be enqueued
	 */
	public static void enqueueTd(EndpointType epType, TransferDescriptor td) throws UsbException{
		
		switch(epType){
			case CONTROL:
	
				if( controlEndpointDesc.getTdHeadPointer() == controlEndpointDesc.getTdTailPointer() ){		// head == tail pointer, no td in the list
					if(verbose_dbg){
						System.out.println("control list is empty.");
					}
					if( US.GET4(controlEndpointDesc.getTdHeadPointer()) == 0xF0000000 ){			// head is empty dummy descriptor
						if(verbose_dbg){
							System.out.println("head is empty descriptor");
							System.out.println("add as next for new td: ");
							System.out.println(emptyControlTD.getTdAddress() );
							System.out.println("add addr of new to testED[4]: ");
							System.out.println(td.getTdAddress());
						}				
						td.setNextTD(emptyControlTD.getTdAddress());								// set nextTd in td
						controlEndpointDesc.setTdHeadPointer(td.getTdAddress());					// set td as new head pointer
						US.PUT4(USBHCCHEDR, controlEndpointDesc.getEndpointDescriptorAddress());	//set Head ED for control transfer
					}
				}
				else{					// already TD's in list
					if(verbose_dbg){
						System.out.println("already TD in list");
						if( US.GET4(controlEndpointDesc.getTdHeadPointer()) == 0xF0000000 ){
							System.out.println("head is empty desc");
						}
					}
					// find last td before empty dummy descriptor, start at head pointer
					int nextTd = ((controlEndpointDesc.getTdHeadPointer()) + 8);					// NextTD
					int nofSearches = 0;
					if(US.GET4(nextTd) != 0xF0000000){					// check if not just one element in list
						while ((US.GET4(US.GET4(nextTd)) != 0xF0000000) && (nofSearches < 2000) ){
							nextTd = (US.GET4(nextTd) + 8);				// NextTD field in TransferDescriptor
							nofSearches++;
						}
					}
					if(nofSearches >= 2000){
						throw new UsbException("enqueue: search of tail TD failed!");
					}
					else{
						if( (td.getType() == TdType.IN) ||  (td.getType()== TdType.OUT)){	
							if(!dataToggleControl){
								td.setDataToggle(false);
								dataToggleControl = false;
							}
							else{
								td.setDataToggle(true);
								dataToggleControl = true;
							}
						}
	
						// add before empty TD
						if(verbose_dbg){
							System.out.println("next for new td: ");
							System.out.println(US.GET4(nextTd));
						}
						td.setNextTD(US.GET4(nextTd));					// set old end td pointer as next td for new enqueued td
						if(verbose_dbg){
							System.out.println("add addr of new to Td:");
							System.out.println("nextTd:");
							System.out.println(nextTd);
						}
						US.PUT4(nextTd, td.getTdAddress());				// set td next pointer
					}
				}
				break;
			case BULK_OUT:
				if( bulkEndpointOutDesc.getTdHeadPointer() == bulkEndpointOutDesc.getTdTailPointer() ){		// head == tail pointer, no td in the list
					if(verbose_dbg){
						System.out.println("bulk OUT list is empty.");
					}
					if( US.GET4(bulkEndpointOutDesc.getTdHeadPointer()) == 0xF0000000 ){		// head is empty dummy descriptor				
						td.setNextTD(emptyBulkOutTD.getTdAddress());							// set nextTd in td
						bulkEndpointOutDesc.setTdHeadPointer(td.getTdAddress());				// set td as new head pointer
						if(dataToggleBulkOut){
							td.setDataToggle(false);
							dataToggleBulkOut = false;
						}
						else{
							td.setDataToggle(true);
							dataToggleBulkOut = true;
						}
						US.PUT4(USBHCBHEDR, bulkEndpointOutDesc.getEndpointDescriptorAddress());	//set Head ED for bulk transfer
					}
				}
				else{				// already TD's in list
					if(verbose_dbg){
						System.out.println("already TD in list");
						if( US.GET4(bulkEndpointOutDesc.getTdHeadPointer()) == 0xF0000000 ){
							System.out.println("head is empty desc");
						}
					}
					// find last td before empty dummy descriptor, start at head pointer
					int nextTd = ((bulkEndpointOutDesc.getTdHeadPointer()) + 8);		// NextTD
					int nofSearches = 0;
					if(US.GET4(nextTd) != 0xF0000000){				// check if not just one element in list
						while ((US.GET4(US.GET4(nextTd)) != 0xF0000000) && (nofSearches < 2000) ){
							nextTd = (US.GET4(nextTd) + 8);			// NextTD field in TransferDescriptor
							nofSearches++;
						}
					}
	
					if(nofSearches >= 2000){
						throw new UsbException("enqueue: search of tail TD failed!");
					}
					else{
						if(dataToggleBulkOut){
							td.setDataToggle(false);
							dataToggleBulkOut = false;
						}
						else{
							td.setDataToggle(true);
							dataToggleBulkOut = true;
						}
						if(verbose_dbg){
							System.out.println("next for new td: ");
							System.out.println(US.GET4(nextTd));
						}
						td.setNextTD(US.GET4(nextTd));				// set old end td pointer as next td for new enqueued td
						if(verbose_dbg){
							System.out.println(nextTd);
						}
						US.PUT4(nextTd, td.getTdAddress());			// set td next pointer
					}
				}
				
				break;
			case BULK_IN:
				if( bulkEndpointInDesc.getTdHeadPointer() == bulkEndpointInDesc.getTdTailPointer() ){		// head == tail pointer, no td in the list
					if(verbose_dbg){
						System.out.println("bulk IN list is empty.");
					}
					if( US.GET4(bulkEndpointInDesc.getTdHeadPointer()) == 0xF0000000 ){			// head is empty dummy descriptor				
						td.setNextTD(emptyBulkInTD.getTdAddress());								// set nextTd in td
						bulkEndpointInDesc.setTdHeadPointer(td.getTdAddress());					// set td as new head pointer
						if(dataToggleBulkIn){
							td.setDataToggle(false);
							dataToggleBulkIn = false;
						}
						else{
							td.setDataToggle(true);
							dataToggleBulkIn = true;
						}
						bulkEndpointOutDesc.setNextEndpointDescriptor(bulkEndpointInDesc.getEndpointDescriptorAddress());	//set Head ED for bulk transfer
					}
				}
				else{				// already TD's in list
					if(verbose_dbg){
						System.out.println("already TD in list");
						if( US.GET4(bulkEndpointInDesc.getTdHeadPointer()) == 0xF0000000 ){
							System.out.println("head is empty desc");
						}
					}
					// find last td before empty dummy descriptor, start at head pointer
					int nextTd = ((bulkEndpointInDesc.getTdHeadPointer()) + 8);		// NextTD
					int nofSearches = 0;
					if(US.GET4(nextTd) != 0xF0000000){		// check if not just one element in list
						while ((US.GET4(US.GET4(nextTd)) != 0xF0000000) && (nofSearches < 2000) ){
							nextTd = (US.GET4(nextTd) + 8);			// NextTD field in TransferDescriptor
							nofSearches++;
						}
					}
	
					if(nofSearches >= 2000){
						throw new UsbException("enqueue: search of tail TD failed!");
					}
					else{
						if(dataToggleBulkIn){
							td.setDataToggle(false);
							dataToggleBulkIn = false;
						}
						else{
							td.setDataToggle(true);
							dataToggleBulkIn = true;
						}
						if(verbose_dbg){
							System.out.println("IN: next for new td: ");
							System.out.println(US.GET4(nextTd));
						}
						td.setNextTD(US.GET4(nextTd));					// set old end td pointer as next td for new enqueued td
						if(verbose_dbg){
							System.out.println(nextTd);
						}
						US.PUT4(nextTd, td.getTdAddress());				// set td next pointer
					}
				}
				break;
			case ISOCHRONOUS:
				throw new UsbException("ISOCHRONOUS not supported!");
				//break;
			case INTERRUPT:
				throw new UsbException("INTERRUPT not supported!");
				//break;
			default:
				break;
		}
	}
	
	/**
	 * set bulk endpoint number
	 * @param endpointNumber	endpoint number that OUT or IN transfers are needed for this device
	 * @param dir				IN or OUT direction
	 * @throws UsbException		on failure
	 */
	public static void setBulkEndpointNumber(int endpointNumber, TransferDirection dir) throws UsbException{
		if(dir == TransferDirection.IN){
			bulkEndpointInDesc.setEndpoint(endpointNumber);
		}
		else{
			bulkEndpointOutDesc.setEndpoint(endpointNumber);
		}
	}
	
	/**
	 * update the done list of the hcd
	 */
	private static void updateDoneList(){
		//TODO -> ohci-q.c Z.932ff
	}
	
	/**
	 * re-init of frame interfal and FSLargestDataPacket
	 */
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
	
	/**
	 * init of host controller and host controller driver
	 * @throws UsbException on failure
	 */
	public static void init() throws UsbException{
		state = OhciState.OHCI_RH_HALTED;
		
		// 1) init CDM Fractional Divider Config Register (internal USB Clock 48 MHz)
		int val = US.GET4(CDMFDCR);
		if( (US.GET4(CDMPORCR) & 0x40) == 0x40 ){		// assumes 33Mhz clock
			val |= 0x00010001;							// checkout 5200lite.c
		}
		else{
			val |= 0x00015555;								
		}
		US.PUT4(CDMFDCR,  val);		// config ext_48mhz_en, fraction divider enable, divider counter
		
		// 2) init GPS port config register for USB support
		val = US.GET4(GPSPCR);
		val &= ~0x00800000;			// internal 48MHz USB Clock, pin is GPIO
		val &= ~0x00007000;			// USB Differential mode
		val |= 0x00001000;			// USB 1
		US.PUT4(GPSPCR, val);
		
		if( (US.GET4(USBHCREVR)& 0xFF) != 0x10 ){		// 3) check HC revision (compliant to USB 1.1)
			//wrong HC revision
			throw new UsbException("Wrong HC revision");
		}
		
		// 4) init structures
		hcca = new byte[HCCA_SIZE];
		controlEndpointDesc = new OhciEndpointDescriptor(EndpointType.CONTROL, 8);
		bulkEndpointOutDesc = new OhciEndpointDescriptor(EndpointType.BULK_OUT, 64);
		bulkEndpointInDesc = new OhciEndpointDescriptor(EndpointType.BULK_IN, 64);
		emptyControlTD = new TransferDescriptor(TdType.EMPTY);
		emptyBulkOutTD = new TransferDescriptor(TdType.EMPTY);
		emptyBulkInTD = new TransferDescriptor(TdType.EMPTY);
		doneList = new int[512];
		
		// allocate and init any Host Controller structures, including HCCA block
		controlEndpointDesc.setMaxPacketSize(8);		// max 8 Bytes first, Control Format -> F=0, speed full, direction from TD, endpoint 0, function address 0
		controlEndpointDesc.setTdTailPointer(emptyControlTD.getTdAddress());		// TD Queue Tail pointer
		controlEndpointDesc.setTdHeadPointer(emptyControlTD.getTdAddress());		// TD Queue Head pointer
		controlEndpointDesc.setNextEndpointDescriptor(controlEndpointDesc.getEndpointDescriptorAddress());		// no next endpoint
		if(verbose_dbg){
			System.out.println("controlEP address: ");
			System.out.println(controlEndpointDesc.getEndpointDescriptorAddress());
		}
		bulkEndpointOutDesc.setTdTailPointer(emptyBulkOutTD.getTdAddress());
		bulkEndpointOutDesc.setTdHeadPointer(emptyBulkOutTD.getTdAddress());
		bulkEndpointOutDesc.setNextEndpointDescriptor(bulkEndpointInDesc.getEndpointDescriptorAddress());
		bulkEndpointInDesc.setTdTailPointer(emptyBulkInTD.getTdAddress());
		bulkEndpointInDesc.setTdHeadPointer(emptyBulkInTD.getTdAddress());
		bulkEndpointInDesc.setNextEndpointDescriptor(bulkEndpointOutDesc.getEndpointDescriptorAddress());
		
		// 5) load driver, take control of host controller
		US.PUT4(USBHCCMDSR, (US.GET4(USBHCCMDSR) | OHCI_OCR) ); // set OwnershipChangeRequest
		
		// 6) -> routed to interrupt controller in SIU -> route from there to SMI, NORMAL interrupt
		InterruptMpc5200io ohciInt = new OhciHcd();
		InterruptMpc5200io.install(ohciInt, 6); 				// USB is peripheral number 6
		US.PUT4(ICTLPIMR, US.GET4(ICTLPIMR) & ~0x02000000);		// accept interrupts from USB
		if( (US.GET4(0xf0000530) & 0x00008000) != 0){			//already usb interrupt pending
			US.PUT4(0xf0000530, 0x00000000);
			System.out.println("already 1");
		}

		// 7) read and set frame interval
		val = (US.GET4(USBHCFIR) & 0x3FFF);
		val = FI;
		val |= (FSMPS << 16);									// set interval to 12000-1, set FSMP
		US.PUT4(USBHCFIR, (val|FI) );	
		US.PUT4(USBHCPSR, (int)(FI*9/10));						// config periodic start (0.9*FrameInterval)
		
		// 8) wait depending on current functional state
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
		
		// 9) save frame interval register
		int saveFmInterval = US.GET4(USBHCFIR);
		// 10) issue a software reset
		US.PUT4(USBHCCMDSR, (US.GET4(USBHCCMDSR)| OHCI_HCR) );	
		currentTime = Kernel.time();
		while( Kernel.time() - currentTime < 10);				// 11) wait for 10us
		if ((US.GET4(USBHCCMDSR) & OHCI_HCR) != 0){
			//reset failed
			throw new UsbException("HC reset failed.");
		}
		US.PUT4(USBHCFIR, saveFmInterval);						// 12) restore frame interval register
		
		// 13) check USB State -> should be USBSuspend
		if( (US.GET4(USBHCCTRLR) & OHCI_CTRL_HCFS ) != OHCI_USB_SUSPEND){
			System.out.println("not in suspend");
			throw new UsbException(Integer.toHexString((US.GET4(USBHCCTRLR) & 0x000000C0 )));
		}
		state = OhciState.OHCI_RH_SUSPENDED;
		
		// now we're in the SUSPEND state ... must go OPERATIONAL within 2msec else HC enters RESUME

		US.PUT4(USBHCCHEDR, controlEndpointDesc.getEndpointDescriptorAddress());		// 14) set ED for control transfer
		US.PUT4(USBHCBHEDR, bulkEndpointOutDesc.getEndpointDescriptorAddress());		//	   set ED for bulk transfer
		
		// 15) Set up HC registers and HC Communications Area
		if(verbose_dbg){
			System.out.println("Addr HCCA: ");
			System.out.println(US.REF(hcca));
		}
		US.PUT4(USBHCHCCAR, (US.REF(hcca) + 28));			// set pointer to HCCA
		if(verbose_dbg){
			System.out.println("Addr Done: ");
			System.out.println(US.REF(doneList));
		}
		US.PUT4(USBHCDHR, (US.REF(doneList) + 8));			// memory for done head
		
		// 16) set frame interval
		periodicReInit();
		
		if( (US.GET4(USBHCFIR) & 0x7fff0000) == 0){
			throw new UsbException("init error: Frame Interval Register is 0.");
			//TODO think about: probably jump to software reset to try again
		}
		
		// 17) start controller operations
		US.PUT4(USBHCCTRLR, (US.GET4(USBHCCTRLR) & OHCI_CTRL_RWC));
		US.PUT4(USBHCCTRLR, (US.GET4(USBHCCTRLR) | OHCI_CONTROL_INIT | OHCI_USB_OPER));		
		state = OhciState.OHCI_RH_RUNNING;
		
		// wake on ConnectStatusChange, matching external hubs
		US.PUT4(USBHCRHSR, (US.GET4(USBHCRHSR) | RH_HS_DRWE ));
		
		// 18) choose the interrupts we care about now, others later on demand
		US.PUT4(USBHCISR, 0xFFFFFFFF);				// clear interrupt status register
		US.PUT4(USBHCIER, OHCI_INTR_INIT);			// enable desired interrupts
		
		// 19) configure root hub
		val = US.GET4(USBHCRHDRA);
		val &= ~(RH_A_PSM | RH_A_OCPM | RH_A_NPS);	// Power switching supported, all ports powered at the same time
		val |= (RH_A_NOCP | RH_A_NDP);				// config PowerSwitchingMode, OverCurrentProtection, NofDownstreamPorts
		val |= 0x32000000;							// 100ms PowerOnToPowerGoodTime
		US.PUT4(USBHCRHDRA, val ); 	
		US.PUT4(USBHCRHDRB, 0x00000000);			// power switching global, devices removable
		// 20) enable power on all ports
		US.PUT4(USBHCRHSR, (US.GET4(USBHCRHSR) | RH_HS_LPSC)); 		

		// 21) wait -> POTPGT * 2 (in 2ms unit) delay after powering hub
		delay = ((US.GET4(USBHCRHDRA) & 0xFF000000) >> 23) * 1000;	
		while(Kernel.time() - currentTime < delay);					// give USB device 100ms time to set up
		
		if( (US.GET4(USBHCRHP1SR) & RH_PS_CCS) != 0){ 				// device connected
			// reset signaling on port
			resetRootHub();
			
			// enumerate device
			enumerateDevice();
		}
		
		initDone = true;
	}

	static{
						
		US.PUT4(GPWER, US.GET4(GPWER) | 0x80000000);		// enable GPIO use
		US.PUT4(GPWDDR, US.GET4(GPWDDR) | 0x80000000);		// make output
		US.PUT4(GPWOUT, US.GET4(GPWOUT) | 0x80000000);
	}
	
}
