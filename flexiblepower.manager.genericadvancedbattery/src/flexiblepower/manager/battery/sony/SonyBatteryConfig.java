package flexiblepower.manager.battery.sony;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
public @interface SonyBatteryConfig {
    @AttributeDefinition(description = "Unique resourceID")
    String resourceId() default "SonyBatteryManager";

    @AttributeDefinition(type = AttributeType.INTEGER,
                         description = "Number of 1.2 kWh IJ1001M Modules")
    int nrOfmodules() default 4;

    @AttributeDefinition(type = AttributeType.DOUBLE,
                         description = "initial State of Charge (0-1)")
    double initialSocRatio() default 0.5d;

    @AttributeDefinition(type = AttributeType.DOUBLE,
                         description = "minimum desired fill level (percent)")
    double minimumFillLevelPercent() default 20d;

    @AttributeDefinition(type = AttributeType.DOUBLE,
                         description = "maximum desired fill level (percent)")
    double maximumFillLevelPercent() default 90d;

    @AttributeDefinition(type = AttributeType.LONG,
                         description = "The simulation time step for a recalculation of the state")
    long updateIntervalSeconds() default 5L;
}
