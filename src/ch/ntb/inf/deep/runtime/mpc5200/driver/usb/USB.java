package ch.ntb.inf.deep.runtime.mpc5200.driver.usb;

import ch.ntb.inf.deep.runtime.mpc5200.IphyCoreMpc5200io;
import ch.ntb.inf.deep.unsafe.US;

public class USB implements IphyCoreMpc5200io{

	public void init(){
		//init CDM Fractional Divider Config Register (internal USB Clock 48 MHz)
		US.PUT4(MemBaseAddr+CDMFDCR, 0x05015555);
		
	}
}
