package jp.oist.unit.ios.dcoes.houseagent;

import java.util.Hashtable;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;

import javax.measure.Measure;
import javax.measure.unit.SI;

import org.flexiblepower.context.FlexiblePowerContext;
import org.flexiblepower.ui.Widget;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.powermatcher.api.AgentEndpoint;
import net.powermatcher.api.monitoring.ObservableAgent;

@Component
@Designate(ocd = HouseModel.Config.class, factory = true)
public class HouseModel {

	@ObjectClassDefinition
	public @interface Config {
		@AttributeDefinition(description = "Agent ID for the house")
		String agentId() default "house";

		@AttributeDefinition(description = "Agent ID for the desired parent")
		String desiredParentId() default "auctioneer";

		@AttributeDefinition(type = AttributeType.INTEGER,
                             description = "Initial state of charge for the battery (between 20 and 95)")
		int initialSoC() default 62;

		@AttributeDefinition(type = AttributeType.DOUBLE,
                             description = "The power when electricity is exchanged with neighbours")
		double exchangeRateWatt() default 700d;
	}

	private final static Logger LOG = LoggerFactory.getLogger(HouseModel.class);

	private Config config;
	private ServiceRegistration<?> agentServiceRegistration;
	private FlexiblePowerContext context;
	private double soc;
	private Random random;
	private HouseAgent agent;
	private ScheduledFuture<?> socTask;

	private HouseCommand command;

	private ServiceRegistration<Widget> widgetServiceRegistration;

	@Activate
	public void activate(BundleContext bundleContext, final Config config) {
		this.config = config;

		agent = new HouseAgent(this, config.exchangeRateWatt());
		agent.init(config.agentId(), config.desiredParentId());

		agentServiceRegistration = bundleContext.registerService(
				new String[] { AgentEndpoint.class.getName(), ObservableAgent.class.getName() }, agent,
				new Hashtable<>());
		random = new Random();

		soc = config.initialSoC();

		socTask = context.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				updateSoC();

			}
		}, Measure.zero(SI.SECOND), Measure.valueOf(10, SI.SECOND));

		// Register the widget
		widgetServiceRegistration = bundleContext.registerService(Widget.class, new HouseWidget(this),
				new Hashtable<>());
	}

	@Deactivate
	public void deactivate() {
		socTask.cancel(false);
		agentServiceRegistration.unregister();
		widgetServiceRegistration.unregister();
	}

	@Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
	public void setContext(FlexiblePowerContext context) {
		this.context = context;
	}

	private void updateSoC() {
		// TODO use real SoC
		double newSoc = soc + (random.nextDouble() * 2) - 1;
		soc = Math.max(20, Math.min(95, newSoc));
		agent.notifyNewSoc(soc);
		LOG.info("The new SoC for house " + config.agentId() + " is " + soc + "%");
	}

	public void setCommand(HouseCommand command) {
		this.command = command;
		LOG.info("The command for house " + config.agentId() + " is " + command.toString());
		// TODO actually do something!
	}

	public String getAgentId() {
		return config.agentId();
	}

	public double getSoC() {
		return soc;
	}

	public HouseCommand getCommand() {
		return command;
	}

}
