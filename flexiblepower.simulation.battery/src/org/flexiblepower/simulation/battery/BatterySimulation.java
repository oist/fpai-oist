package org.flexiblepower.simulation.battery;

import static javax.measure.unit.NonSI.KWH;
import static javax.measure.unit.SI.WATT;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;

import javax.measure.Measurable;
import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Power;
import javax.measure.unit.SI;

import org.flexiblepower.context.FlexiblePowerContext;
import org.flexiblepower.messaging.Endpoint;
import org.flexiblepower.ral.drivers.battery.BatteryControlParameters;
import org.flexiblepower.ral.drivers.battery.BatteryDriver;
import org.flexiblepower.ral.drivers.battery.BatteryMode;
import org.flexiblepower.ral.drivers.battery.BatteryState;
import org.flexiblepower.ral.ext.AbstractResourceDriver;
import org.flexiblepower.ui.Widget;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO Uit BatteryState weghalen van de charge en discharge efficiency
 *
 * @author waaijbdvd
 *
 */
@Component(service = Endpoint.class, immediate = true)
@Designate(ocd = BatterySimulation.Config.class, factory = true)
public class BatterySimulation
                              extends AbstractResourceDriver<BatteryState, BatteryControlParameters>
                                                                                                    implements
                                                                                                    BatteryDriver,
                                                                                                    Runnable {

	@ObjectClassDefinition
    public @interface Config {
        @AttributeDefinition(type = AttributeType.LONG,
                             description = "Interval between state updates [s]")
        long updateInterval() default 5L;

        @AttributeDefinition(type = AttributeType.DOUBLE,
                             description = "Total capacity [kWh]")
        double totalCapacity() default 1d;

        @AttributeDefinition(type = AttributeType.DOUBLE,
                             description = "Initial state of charge (from 0 to 1)")
        double initialStateOfCharge() default 0.5d;

        @AttributeDefinition(type = AttributeType.LONG,
                             description = "Charge power [W]")
        long chargePower() default 1500L;

        @AttributeDefinition(type = AttributeType.LONG,
                             description = "Discharge power [W]")
        long dischargePower() default 1500L;

        @AttributeDefinition(type = AttributeType.DOUBLE,
                             description = "Charge efficiency (from 0 to 1)")
        double chargeEfficiency() default 0.9d;

        @AttributeDefinition(type = AttributeType.DOUBLE,
                             description = "Discharge efficiency (from 0 to 1)")
        double dischargeEfficiency() default 0.9d;

        @AttributeDefinition(type = AttributeType.LONG,
                             description = "Self discharge power [W]")
        long selfDischargePower() default 50L;
    }

    class State implements BatteryState {
        private final double stateOfCharge; // State of Charge is always within [0, 1] range.
        private final BatteryMode mode;

        public State(double stateOfCharge, BatteryMode mode) {
            // This is a quick fix. It would be better to throw an exception. This should be done later.
            if (stateOfCharge < 0.0) {
                stateOfCharge = 0.0;
            } else if (stateOfCharge > 1.0) {
                stateOfCharge = 1.0;
            }

            this.stateOfCharge = stateOfCharge;
            this.mode = mode;
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public Measurable<Energy> getTotalCapacity() {
            return totalCapacityInKWh;
        }

        @Override
        public Measurable<Power> getChargeSpeed() {
            return chargeSpeedInWatt;
        }

        @Override
        public Measurable<Power> getDischargeSpeed() {
            return dischargeSpeedInWatt;
        }

        @Override
        public Measurable<Power> getSelfDischargeSpeed() {
            return selfDischargeSpeedInWatt;
        }

        @Override
        public double getChargeEfficiency() {
            return configuration.chargeEfficiency();
        }

        @Override
        public double getDischargeEfficiency() {
            return configuration.dischargeEfficiency();
        }

        @Override
        public Measurable<Duration> getMinimumOnTime() {
            return minTimeOn;
        }

        @Override
        public Measurable<Duration> getMinimumOffTime() {
            return minTimeOff;
        }

        @Override
        public double getStateOfCharge() {
            return stateOfCharge;
        }

        @Override
        public BatteryMode getCurrentMode() {
            return mode;
        }

        @Override
        public String toString() {
            return "State [stateOfCharge=" + stateOfCharge + ", mode=" + mode + "]";
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(BatterySimulation.class);

    private Measurable<Power> dischargeSpeedInWatt;
    private Measurable<Power> chargeSpeedInWatt;
    private Measurable<Power> selfDischargeSpeedInWatt;
    private Measurable<Energy> totalCapacityInKWh;
    private Measurable<Duration> minTimeOn;
    private Measurable<Duration> minTimeOff;

    private BatteryMode mode;
    private Date lastUpdatedTime;
    private Config configuration;
    private double stateOfCharge;

    private ScheduledFuture<?> scheduledFuture;

    private ServiceRegistration<Widget> widgetRegistration;

    private BatteryWidget widget;

    private FlexiblePowerContext context;

    @Reference
    public void setContext(FlexiblePowerContext context) {
        this.context = context;
        lastUpdatedTime = context.currentTime();
    }

    @Activate
    public void activate(BundleContext context, final Config config) throws Exception {
        try {
            configuration = config;

            totalCapacityInKWh = Measure.valueOf(configuration.totalCapacity(), KWH);
            chargeSpeedInWatt = Measure.valueOf(configuration.chargePower(), WATT);
            dischargeSpeedInWatt = Measure.valueOf(configuration.dischargePower(), WATT);
            selfDischargeSpeedInWatt = Measure.valueOf(configuration.selfDischargePower(), WATT);
            stateOfCharge = configuration.initialStateOfCharge();
            minTimeOn = Measure.valueOf(0, SI.SECOND);
            minTimeOff = Measure.valueOf(0, SI.SECOND);
            mode = BatteryMode.IDLE;

            publishState(new State(stateOfCharge, mode));

            scheduledFuture = this.context.scheduleAtFixedRate(this,
                                                               Measure.valueOf(0, SI.SECOND),
                                                               Measure.valueOf(configuration.updateInterval(),
                                                                               SI.SECOND));

            widget = new BatteryWidget(this);
            widgetRegistration = context.registerService(Widget.class, widget, null);
        } catch (Exception ex) {
            logger.error("Error during initialization of the battery simulation: " + ex.getMessage(), ex);
            deactivate();
            throw ex;
        }
    }

    @Deactivate
    public void deactivate() {
        if (widgetRegistration != null) {
            widgetRegistration.unregister();
            widgetRegistration = null;
        }
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }
    }

    @Modified
    public void modify(BundleContext context, final Config config) {
        try {
            configuration = config;

            totalCapacityInKWh = Measure.valueOf(configuration.totalCapacity(), KWH);
            chargeSpeedInWatt = Measure.valueOf(configuration.chargePower(), WATT);
            dischargeSpeedInWatt = Measure.valueOf(configuration.dischargePower(), WATT);
            selfDischargeSpeedInWatt = Measure.valueOf(configuration.selfDischargePower(), WATT);
            stateOfCharge = configuration.initialStateOfCharge();
            minTimeOn = Measure.valueOf(2, SI.SECOND);
            minTimeOff = Measure.valueOf(2, SI.SECOND);
            mode = BatteryMode.IDLE;
        } catch (RuntimeException ex) {
            logger.error("Error during initialization of the battery simulation: " + ex.getMessage(), ex);
            deactivate();
            throw ex;
        }
    }

    @Override
    public synchronized void run() {
        Date currentTime = context.currentTime();
        double durationSinceLastUpdate = (currentTime.getTime() - lastUpdatedTime.getTime()) / 1000.0; // in seconds
        lastUpdatedTime = currentTime;
        double amountOfChargeInWatt = 0;

        logger.debug("Battery simulation step. Mode={} Timestep={}s", mode, durationSinceLastUpdate);
        if (durationSinceLastUpdate > 0) {
            switch (mode) {
            case IDLE:
                amountOfChargeInWatt = 0;
                break;
            case CHARGE:
                amountOfChargeInWatt = chargeSpeedInWatt.doubleValue(WATT);
                break;
            case DISCHARGE:
                amountOfChargeInWatt = -dischargeSpeedInWatt.doubleValue(WATT);
                break;
            default:
                throw new AssertionError();
            }
            // always also self discharge
            double changeInW = amountOfChargeInWatt - selfDischargeSpeedInWatt.doubleValue(WATT);
            double changeInWS = changeInW * durationSinceLastUpdate;
            double changeinKWH = changeInWS / (1000.0 * 3600.0);

            double newStateOfCharge = stateOfCharge + (changeinKWH / totalCapacityInKWh.doubleValue(KWH));

            // check if the stateOfCharge is not outside the limits of the battery
            if (newStateOfCharge < 0.0) {
                newStateOfCharge = 0.0;
                // indicate that battery has stopped discharging
                mode = BatteryMode.IDLE;
            } else {
                if (newStateOfCharge > 1.0) {
                    newStateOfCharge = 1.0;
                    // indicate that battery has stopped charging
                    mode = BatteryMode.IDLE;
                }
            }

            State state = new State(newStateOfCharge, mode);
            logger.debug("Publishing state {}", state);
            publishState(state);

            stateOfCharge = newStateOfCharge;
        }
    }

    @Override
    protected void handleControlParameters(BatteryControlParameters controlParameters) {
        mode = controlParameters.getMode();
    }

    protected State getCurrentState() {
        return new State(stateOfCharge, mode);
    }
}
