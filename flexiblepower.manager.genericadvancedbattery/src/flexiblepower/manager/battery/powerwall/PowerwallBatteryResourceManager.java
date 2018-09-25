package flexiblepower.manager.battery.powerwall;

import java.lang.annotation.Annotation;

import javax.measure.Measure;
import javax.measure.unit.SI;

import org.flexiblepower.context.FlexiblePowerContext;
import org.flexiblepower.messaging.Endpoint;
import org.flexiblepower.ui.Widget;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.Designate;

import flexiblepower.manager.genericadvancedbattery.GenericAdvancedBatteryConfig;
import flexiblepower.manager.genericadvancedbattery.GenericAdvancedBatteryDeviceModel;
import flexiblepower.manager.genericadvancedbattery.GenericAdvancedBatteryResourceManager;

@Component(service = Endpoint.class, immediate = true)
@Designate(ocd=PowerwallBatteryConfig.class, factory=true)
public class PowerwallBatteryResourceManager extends GenericAdvancedBatteryResourceManager {

    // TODO The real powerwall is less efficient (including inverter around 87% than the generic batteryModel).
    private static final double CAPACITY_KWH = 7;
    private static final double RATED_VOLTAGE = 433.3507;
    private PowerwallBatteryConfig powerwallConfiguration;

    @Activate
    public void activate(BundleContext bundleContext, final PowerwallBatteryConfig powerwallBatteryConfig) {
        try {
            powerwallConfiguration = powerwallBatteryConfig;

            final String resourceId = powerwallConfiguration.resourceId();
            final double totalCapacityKWh = CAPACITY_KWH;
            final double maximumChargingRateWatts = 2000.0;
            final double maximumDischargingRateWatts = 2000.0;
            final double ratedCapacityAh = CAPACITY_KWH * 1000.0 / RATED_VOLTAGE;
            final int nrOfCyclesBeforeEndOfLife = 4000;
            final double initialSocRatio = powerwallConfiguration.initialSocRatio();
            final int nrOfModulationSteps = 19;
            final double minimumFillLevelPercent = powerwallConfiguration.minimumFillLevelPercent();
            final double maximumFillLevelPercent = powerwallConfiguration.maximumFillLevelPercent();
            final long updateIntervalSeconds = powerwallConfiguration.updateIntervalSeconds();

            final double ratedVoltage = RATED_VOLTAGE;
            final double KValue = 0.12903;
            //final double QAmpereHours = 17.5;
            final double constantA = 60;
            final double constantB = 3.4893;
            final double internalResistanceOhms = 0.22857;
            final double batterySavingPowerWatts = 500.0;

            // Create a config
            config = new GenericAdvancedBatteryConfig() {
				@Override
				public Class<? extends Annotation> annotationType() {
					return GenericAdvancedBatteryConfig.class;
				}

				@Override
				public String resourceId() {
					return resourceId;
				}

				@Override
				public double totalCapacityKWh() {
					return totalCapacityKWh;
				}

				@Override
				public double maximumChargingRateWatts() {
					return maximumChargingRateWatts;
				}

				@Override
				public double maximumDischargingRateWatts() {
					return maximumDischargingRateWatts;
				}

				@Override
				public double ratedCapacityAh() {
					return ratedCapacityAh;
				}

				@Override
				public int nrOfCyclesBeforeEndOfLife() {
					return nrOfCyclesBeforeEndOfLife;
				}

				@Override
				public double initialSocRatio() {
					return initialSocRatio;
				}

				@Override
				public double minimumFillLevelPercent() {
					return minimumFillLevelPercent;
				}

				@Override
				public double maximumFillLevelPercent() {
					return maximumFillLevelPercent;
				}

				@Override
				public int nrOfModulationSteps() {
					return nrOfModulationSteps;
				}

				@Override
				public int updateIntervalSeconds() {
					return Long.valueOf(updateIntervalSeconds).intValue();
				}

				@Override
				public double ratedVoltage() {
					return ratedVoltage;
				}

				@Override
				public double KValue() {
					return KValue;
				}

				@Override
				public double constantA() {
					return constantA;
				}

				@Override
				public double constantB() {
					return constantB;
				}

				@Override
				public double internalResistanceOhms() {
					return internalResistanceOhms;
				}

				@Override
				public double batterySavingPowerWatts() {
					return batterySavingPowerWatts;
				}
            };

            // Initialize the batteryModel correctly to start the first time step.
            batteryModel = new GenericAdvancedBatteryDeviceModel(config, context);

            scheduledFuture = context.scheduleAtFixedRate(this,
                                                          Measure.valueOf(0, SI.SECOND),
                                                          Measure.valueOf(config.updateIntervalSeconds(),
                                                                          SI.SECOND));

            widget = new PowerwallBatteryWidget(batteryModel);
            widgetRegistration = bundleContext.registerService(Widget.class, widget, null);
            logger.debug("Advanced Battery Manager activated");
        } catch (Exception ex) {
            logger.error("Error during initialization of the battery simulation: " + ex.getMessage(), ex);
            deactivate();
        }
    }

    @Override
    @Deactivate
    public void deactivate() {
        logger.debug("Advanced Battery Manager deactivated");
        if (widgetRegistration != null) {
            widgetRegistration.unregister();
        }
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
    }

    @Override
    @Reference(cardinality=ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
    public void setContext(FlexiblePowerContext context) {
        this.context = context;
    }
}
