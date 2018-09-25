package org.flexiblepower.simulation.generator;

import java.util.concurrent.ScheduledFuture;

import javax.measure.Measure;
import javax.measure.unit.SI;

import org.flexiblepower.context.FlexiblePowerContext;
import org.flexiblepower.driver.generator.GeneratorControlParameters;
import org.flexiblepower.driver.generator.GeneratorLevel;
import org.flexiblepower.driver.generator.GeneratorState;
import org.flexiblepower.messaging.Endpoint;
import org.flexiblepower.messaging.Port;
import org.flexiblepower.ral.ext.AbstractResourceDriver;
import org.osgi.framework.BundleContext;
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

@Port(name = "manager", accepts = GeneratorControlParameters.class, sends = GeneratorState.class)
@Component(service = Endpoint.class, immediate = true)
@Designate(ocd = GeneratorSimulation.Config.class, factory = true)
public class GeneratorSimulation extends AbstractResourceDriver<GeneratorState, GeneratorControlParameters> implements
                                                                                                           Runnable {
    private static final Logger logger = LoggerFactory.getLogger(GeneratorSimulation.class);

    @ObjectClassDefinition
    public @interface Config {
        @AttributeDefinition(description = "Resource identifier")
        String resourceId() default "generator";

        @AttributeDefinition(type = AttributeType.INTEGER,
                             description = "Frequency with which updates will be sent out in seconds")
        int updateFrequency() default 5;
    }

    private ScheduledFuture<?> scheduledFuture;
    private Config config;
    private GeneratorLevel generatorLevel = new GeneratorLevel();

    class State implements GeneratorState {
        private final GeneratorLevel generatorLevel;

        public State(GeneratorLevel generatorLevel) {
            this.generatorLevel = generatorLevel;
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public GeneratorLevel getGeneratorLevel() {
            return generatorLevel;
        }

    }

    @Activate
    public void activate(BundleContext bundleContext, final Config config) {
        try {
            this.config = config;

            scheduledFuture = fpContext.scheduleAtFixedRate(this,
                                                            Measure.valueOf(0, SI.SECOND),
                                                            Measure.valueOf(config.updateFrequency(), SI.SECOND));
            generatorLevel.setLevel(0);

        } catch (RuntimeException ex) {
            logger.error("Error during initialization of the generator simulation: " + ex.getMessage(), ex);
            deactivate();
            throw ex;
        }
    }

    @Modified
    public void modify(BundleContext bundleContext, final Config config) {
        try {
            this.config = config;

        } catch (RuntimeException ex) {
            logger.error("Error during modification of the generator simulation: " + ex.getMessage(), ex);
            deactivate();
            throw ex;
        }
    }

    @Deactivate
    public void deactivate() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }
    }

    @Override
    public void run() {
        GeneratorState state = new State(generatorLevel);
        logger.debug("Publishing state {}", state);
        // System.out.println("ping");
        publishState(state);
    }

    private FlexiblePowerContext fpContext;

    @Reference
    public void setContext(FlexiblePowerContext fpContext) {
        this.fpContext = fpContext;
    }

    @Override
    protected void handleControlParameters(GeneratorControlParameters controlParameters) {
        generatorLevel = controlParameters.getLevel();
    }

    public GeneratorLevel getGeneratorLevel() {
        return generatorLevel;
    }
}
