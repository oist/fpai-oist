package flexiblepower.manager.genericadvancedbattery;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
public @interface GenericAdvancedBatteryConfig {
    @AttributeDefinition(description = "Unique resourceID")
    String resourceId() default "AdvancedBatteryManager";

    @AttributeDefinition(type = AttributeType.DOUBLE,
                         description = "Total Capacity in kWh")
    double totalCapacityKWh() default 5d;

    @AttributeDefinition(type = AttributeType.DOUBLE,
                         description = "Maximum absolute charging rate in Watts")
    double maximumChargingRateWatts() default 1500d;

    @AttributeDefinition(type = AttributeType.DOUBLE,
                         description = "Maximum absolute discharging rate in Watts (Should be a positive value)")
    double maximumDischargingRateWatts() default 1500d;

    @AttributeDefinition(type = AttributeType.DOUBLE,
                         description = "The rated capacity of the battery in Ah")
    double ratedCapacityAh() default 24d;

    @AttributeDefinition(type = AttributeType.INTEGER,
                         description = "Number of full discharge cycles until battery end of life (80% capacity)")
    int nrOfCyclesBeforeEndOfLife() default 6000;

    // TODO: Make this less confusing, what does 0.5 mean when min and max are
    // 20 and 90...
    @AttributeDefinition(type = AttributeType.DOUBLE,
                         description = "initial State of Charge (0-1).")
    double initialSocRatio() default 0.5d;

    @AttributeDefinition(type = AttributeType.DOUBLE,
                         description = "minimum desired fill level (percent)")
    double minimumFillLevelPercent() default 20d;

    @AttributeDefinition(type = AttributeType.DOUBLE,
                         description = "maximum desired fill level (percent)")
    double maximumFillLevelPercent() default 90d;

    @AttributeDefinition(type = AttributeType.INTEGER,
                         description = "The number of modulesation steps between idle and charging and between idle and discharging")
    int nrOfModulationSteps() default 9;

    @AttributeDefinition(type = AttributeType.INTEGER,
                         description = "The simulation time step for a recalculation of the state")
    int updateIntervalSeconds() default 5;

    // TODO We could make the rated voltage, and the constants K, A and B
    // configurable, but this is a really advanced feature that requires Matlab
    // Simulink, so it is not in the first implementation.
    @AttributeDefinition(type = AttributeType.DOUBLE,
                         description = "*ADVANCED SETTINGS* Rated Voltage of the battery")
    double ratedVoltage() default 52.6793d;

    @AttributeDefinition(type = AttributeType.DOUBLE,
                         description = "*ADVANCED SETTINGS* The constant K (unitless) of the battery batteryModel")
    double KValue() default 0.011d;

    @AttributeDefinition(type = AttributeType.DOUBLE, 
                         description = "*ADVANCED SETTINGS* Exponential Voltage constant used to calculate the Voltage in Volts")
    double constantA() default 3d;

    @AttributeDefinition(type = AttributeType.DOUBLE, 
                         description = "*ADVANCED SETTINGS* Exponential Capacity constant used to calculate the Voltage.(Ah^-1)")
    double constantB() default 2.8d;

    @AttributeDefinition(type = AttributeType.DOUBLE, description = "*ADVANCED SETTINGS* The internal resistance in Ohms")
    double internalResistanceOhms() default 0.036d;

    @AttributeDefinition(type = AttributeType.DOUBLE, 
                         description = "The battery's allowed maximum charging power in Watts when it approaches the minimum or maximum (which are fixed at 5% and 95%).")
    double batterySavingPowerWatts() default 500d;
}
