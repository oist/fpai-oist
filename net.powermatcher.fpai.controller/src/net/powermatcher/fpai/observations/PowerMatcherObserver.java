package net.powermatcher.fpai.observations;

import net.powermatcher.api.monitoring.AgentObserver;
import net.powermatcher.api.monitoring.ObservableAgent;
import net.powermatcher.api.monitoring.events.AgentEvent;
import net.powermatcher.api.monitoring.events.AggregatedBidEvent;
import net.powermatcher.api.monitoring.events.OutgoingBidUpdateEvent;
import net.powermatcher.api.monitoring.events.OutgoingPriceUpdateEvent;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(immediate = true)
@Designate(ocd = PowerMatcherObserver.Config.class, factory = true)
public class PowerMatcherObserver implements AgentObserver {
	@ObjectClassDefinition
    public @interface Config {
        @AttributeDefinition(type = AttributeType.BOOLEAN,
                             description = "Whether outgoing price events should be published")
        boolean publishPriceEvents() default true;

        @AttributeDefinition(type = AttributeType.BOOLEAN,
                             description = "Whether outgoing bid events should be published")
        boolean publishBidEvents() default true;

        @AttributeDefinition(type = AttributeType.BOOLEAN,
                             description = "Whether aggregation events should be published")
        boolean publishAggregationEvents() default true;

        @AttributeDefinition(name = "agent.target")
        String agentTarget() default "";
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addAgent(ObservableAgent agent) {
        agent.addObserver(this);
    }

    public void removeAgent(ObservableAgent agent) {
        agent.removeObserver(this);
    }

    private PriceObservationProvider pricePublisher = null;
    private BidObservationProvider bidPublisher = null;
    private AggregationObservationProvider aggregationPublisher;

    @Activate
    public void activate(BundleContext context, final Config config) {
        if (config.publishPriceEvents()) {
            pricePublisher = new PriceObservationProvider(context);
        }
        if (config.publishBidEvents()) {
            bidPublisher = new BidObservationProvider(context);
        }
        if (config.publishAggregationEvents()) {
            aggregationPublisher = new AggregationObservationProvider(context);
        }
    }

    @Deactivate
    public void deactivate() {
        if (pricePublisher != null) {
            pricePublisher.close();
            pricePublisher = null;
        }
        if (bidPublisher != null) {
            bidPublisher.close();
            bidPublisher = null;
        }
        if (aggregationPublisher != null) {
            aggregationPublisher.close();
            aggregationPublisher = null;
        }
    }

    @Override
    public void handleAgentEvent(AgentEvent event) {
        if (pricePublisher != null && event instanceof OutgoingPriceUpdateEvent) {
            pricePublisher.publish((OutgoingPriceUpdateEvent) event);
        } else if (bidPublisher != null && event instanceof OutgoingBidUpdateEvent) {
            bidPublisher.publish((OutgoingBidUpdateEvent) event);
        } else if (aggregationPublisher != null && event instanceof AggregatedBidEvent) {
            aggregationPublisher.publish((AggregatedBidEvent) event);
        }
    }
}
