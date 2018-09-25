package flexiblepower.manager.battery.powerwall;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
public @interface PowerwallBatteryConfig {
    @AttributeDefinition(description = "Unique resourceID")
    String resourceId() default "PowerwallBatteryManager";

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
