package net.powermatcher.fpai.peakshaving;

import net.powermatcher.core.concentrator.TransformingConcentrator;

import org.flexiblepower.observation.Observation;
import org.flexiblepower.observation.ObservationConsumer;
import org.flexiblepower.observation.ObservationProvider;
import org.flexiblepower.ral.drivers.uncontrolled.PowerState;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(immediate = true)
@Designate(ocd = TransformingConcentratorInformer.Config.class, factory = true)
public class TransformingConcentratorInformer implements ObservationConsumer<PowerState> {
    @ObjectClassDefinition
    public @interface Config {
        @AttributeDefinition(description = "The filter that is used to determine which transforming concentrator should get the power values")
        String concentrator_target() default "(agentId=peakshavingconcentrator)";

        @AttributeDefinition(description = "The filter that is used to determine which observation provider should be used to get the power values")
        String observationProvider_target() default "(org.flexiblepower.monitoring.observationOf=something)";
    }

    private TransformingConcentrator concentrator;

    @Reference
    public void setConcentrator(TransformingConcentrator concentrator) {
        this.concentrator = concentrator;
    }

    @Reference
    public void setObservationProvider(ObservationProvider<PowerState> provider) {
        provider.subscribe(this);
    }

    public void unsetObservationProvider(ObservationProvider<PowerState> provider) {
        provider.unsubscribe(this);
    }

    @Override
    public void consume(ObservationProvider<? extends PowerState> source,
                        Observation<? extends PowerState> observation) {
        concentrator.setMeasuredFlow(observation.getValue().getCurrentUsage());
    }
}
