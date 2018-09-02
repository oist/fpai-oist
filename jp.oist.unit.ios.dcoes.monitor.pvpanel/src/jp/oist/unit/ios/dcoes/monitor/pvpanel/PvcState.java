package jp.oist.unit.ios.dcoes.monitor.pvpanel;

import java.time.ZonedDateTime;

import javax.measure.Measurable;
import javax.measure.quantity.ElectricCurrent;
import javax.measure.quantity.ElectricPotential;
import javax.measure.quantity.Power;

import org.flexiblepower.ral.drivers.uncontrolled.PowerState;

public class PvcState implements PowerState {
	
    private final ZonedDateTime timestamp;
	
    private final Measurable<Power> power;
    
	public PvcState(ZonedDateTime timestamp, Measurable<Power> power) {
        this.timestamp = timestamp;
        this.power = power;
    }

	@Override
	public boolean isConnected() {
		return true;
	}

	@Override
	public Measurable<Power> getCurrentUsage() {
		return power;
	}
}