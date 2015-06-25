package ch.ntb.inf.deep.runtime.mpc5200.driver.usb;

import ch.ntb.inf.deep.runtime.mpc5200.IphyCoreMpc5200io;
import ch.ntb.inf.deep.unsafe.US;

public class USB implements IphyCoreMpc5200io{

	public void init(){
		//init CDM Fractional Divider Config Register (internal USB Clock 48 MHz)
		US.PUT4(CDMFDCR, 0x04015555);
		//init GPS port config register for USB support
		int tmp = US.GET4(GPSPCR);
		tmp |= 0x00001000;
		tmp &= ~0x00002000;
		US.PUT4(GPSPCR, tmp);
		
		US.PUT4(USBHCRHSR, 0x00010000);
		
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
