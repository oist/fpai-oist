package jp.oist.unit.ios.dcoes.monitor.house;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import javax.measure.Measurable;
import javax.measure.Measure;
import javax.measure.quantity.ElectricCurrent;
import javax.measure.quantity.ElectricPotential;
import javax.measure.quantity.Power;
import javax.measure.unit.SI;

import org.flexiblepower.context.FlexiblePowerContext;
import org.flexiblepower.messaging.Endpoint;
import org.flexiblepower.observation.Observation;
import org.flexiblepower.observation.ObservationConsumer;
import org.flexiblepower.observation.ObservationProvider;
import org.flexiblepower.ral.ResourceControlParameters;
import org.flexiblepower.ral.drivers.uncontrolled.PowerState;
import org.flexiblepower.ral.drivers.uncontrolled.UncontrollableDriver;
import org.flexiblepower.ral.ext.AbstractResourceDriver;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.oist.unit.ios.dcoes.monitor.Monitor;
import jp.oist.unit.ios.dcoes.monitor.message.EssMessage;
import jp.oist.unit.ios.dcoes.monitor.message.IDcoesMessage;
import jp.oist.unit.ios.dcoes.monitor.message.WeatherMessage;

@Component(service = Endpoint.class)
@Designate(ocd = PowerBoard.Config.class, factory = true)
public class PowerBoard
	extends AbstractResourceDriver<PowerState, ResourceControlParameters>
    implements UncontrollableDriver, ObservationConsumer<IDcoesMessage>, Runnable {

    private final static Logger log = LoggerFactory.getLogger(PowerBoard.class);

    @ObjectClassDefinition
    public @interface Config {
        @AttributeDefinition(description = "name", required = false)
        String name() default "";
    }

    protected PowerBoardState latestPbState;
    
    private FlexiblePowerContext flexiblePowerContext;
    
    private Monitor monitor = null;
    
    private final Object pbLock = new Object();
    private final Object wsLock = new Object();
    
    private void consume(EssMessage ess) {
        JSONObject message = ess.getMessage();

        /* AC Output*/	
        JSONObject emu = message.getJSONObject("emu");

        ZonedDateTime timestamp = ZonedDateTime.ofInstant(Instant.ofEpochSecond(message.getLong("timestamp")),
                                                          ZoneOffset.systemDefault());

        Measurable<Power> power = Measure.valueOf(emu.getDouble("ups_output_power"), SI.WATT);
        Measurable<ElectricPotential> voltage = Measure.valueOf(emu.getDouble("ups_output_voltage"), SI.VOLT);
        Measurable<ElectricCurrent> current = Measure.valueOf(emu.getDouble("ups_output_current"), SI.AMPERE);

        synchronized(pbLock) {
            latestPbState = new PowerBoardState(timestamp, power, voltage, current);
            log.info(latestPbState.toString());
        }
        publishState(latestPbState);
    }
    
    private void consume(WeatherMessage weather) {
        JSONObject message = weather.getMessage();
        // nothing to do...
    }
    
    @Reference
	public void onFoundProvider(Monitor monitor) {
    	if (this.monitor != null)
    		this.monitor.unsubscribe(this);
    	log.info(String.format("register to %s", monitor.toString()));
    	monitor.subscribe(this);
    	this.monitor = monitor;
    }
    
    @Activate
    public void activate(BundleContext context, Map<String, ?> properties) {
        log.info("Activate");
    }
    
    @Deactivate
    public void deactivate() {
        log.info("Deactivate");
        if (this.monitor != null)
            this.monitor.unsubscribe(this);
    }

    @Override
    public void consume(ObservationProvider<? extends IDcoesMessage> provider,
            Observation<? extends IDcoesMessage> observation) {
        IDcoesMessage message = observation.getValue();
        switch(message.getType()) {
        case ESS:
            consume((EssMessage)message);
            break;
        case WEATHER:
            consume((WeatherMessage)message);
            break;
        }
    }

	@Override
	protected void handleControlParameters(ResourceControlParameters controlParameters) {
		log.info("handleControlParameters");
		// TODO Auto-generated method stub
	}

	@Override
	public void run() {
		log.info("RUN");
	}
}
