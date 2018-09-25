package net.powermatcher.fpai.controller;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import net.powermatcher.fpai.agents.BufferAgent;
import net.powermatcher.fpai.agents.FpaiAgent;
import net.powermatcher.fpai.agents.TimeshifterAgent;
import net.powermatcher.fpai.agents.UnconstrainedAgent;
import net.powermatcher.fpai.agents.UncontrolledAgent;

import org.flexiblepower.efi.EfiControllerManager;
import org.flexiblepower.messaging.Connection;
import org.flexiblepower.messaging.Endpoint;
import org.flexiblepower.messaging.MessageHandler;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(immediate = true, service = { Endpoint.class })
@Designate(ocd = PowerMatcherController.Config.class, factory = true)
public class PowerMatcherController implements EfiControllerManager {

	@ObjectClassDefinition
    public @interface Config {
        @AttributeDefinition(required = false)
        String desiredParent() default "auctioneer";

        @AttributeDefinition()
        String agentIdPrefix() default "fpai-agent-";
    }

    private BundleContext bundleContext;

    private final Set<AgentMessageHandler> activeHandlers = new HashSet<AgentMessageHandler>();

    private final AtomicInteger agentId = new AtomicInteger(1);

    private String agentIdPrefix;

    private String desiredParent;

    @Activate
    public void activate(BundleContext context, final Config config) throws Exception {
        bundleContext = context;
        agentIdPrefix = config.agentIdPrefix();
        desiredParent = config.desiredParent();
    }

    @Deactivate
    public void deactivate() {
        synchronized (activeHandlers) {
            for (AgentMessageSender handler : activeHandlers.toArray(new AgentMessageHandler[activeHandlers.size()])) {
                removeHandler(handler);
            }
        }
    }

    @Override
    public MessageHandler onConnect(Connection connection) {
        String agentId = agentIdPrefix + this.agentId.getAndIncrement() + "-";

        Class<? extends FpaiAgent> clazz = null;
        if ("buffer".equals(connection.getPort().name())) {
            clazz = BufferAgent.class;
        } else if ("timeshifter".equals(connection.getPort().name())) {
            clazz = TimeshifterAgent.class;
        } else if ("unconstrained".equals(connection.getPort().name())) {
            clazz = UnconstrainedAgent.class;
        } else if ("uncontrolled".equals(connection.getPort().name())) {
            clazz = UncontrolledAgent.class;
        } else {
            // Wut?
            throw new IllegalArgumentException("Unknown type of connection");
        }

        AgentMessageHandler newHandler = new AgentMessageHandler(bundleContext,
                                                                 this,
                                                                 connection,
                                                                 agentId,
                                                                 desiredParent,
                                                                 clazz);
        synchronized (activeHandlers) {
            activeHandlers.add(newHandler);
        }
        return newHandler;
    }

    public void removeHandler(AgentMessageSender handler) {
        synchronized (activeHandlers) {
            activeHandlers.remove(handler);
            handler.destroyAgent();
        }
    }
}
