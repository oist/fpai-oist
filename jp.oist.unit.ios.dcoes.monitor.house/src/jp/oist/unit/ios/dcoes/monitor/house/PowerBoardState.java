/* vim: set ts=4 sw=4 et fenc=utf-8 ff=unix : */
package jp.oist.unit.ios.dcoes.monitor.house;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.measure.Measurable;
import javax.measure.quantity.ElectricCurrent;
import javax.measure.quantity.ElectricPotential;
import javax.measure.quantity.Power;
import javax.measure.unit.SI;

import org.flexiblepower.ral.drivers.uncontrolled.PowerState;

public class PowerBoardState implements PowerState {
	
    private final ZonedDateTime timestamp;
	
    private final Measurable<Power> power;
	
    private final Measurable<ElectricPotential> voltage;

    private final Measurable<ElectricCurrent> current;
	
	public PowerBoardState(ZonedDateTime timestamp, Measurable<Power> power,
                           Measurable<ElectricPotential> voltage,
                           Measurable<ElectricCurrent> current) {
        this.timestamp = timestamp;
        this.power = power;
        this.voltage = voltage;
        this.current = current;
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public Measurable<Power> getCurrentUsage() {
        return power;
    }
    
    public String toString() {
        return String.format("%s - P:%.2f V:%.1f C%.2f",
                             timestamp,
                             power.doubleValue(SI.WATT),
                             voltage.doubleValue(SI.VOLT),
                             current.doubleValue(SI.AMPERE));
    }
}