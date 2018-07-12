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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;
import jp.oist.unit.ios.dcoes.monitor.message.EssMessage;
import jp.oist.unit.ios.dcoes.monitor.message.IDcoesMessage;
import jp.oist.unit.ios.dcoes.monitor.message.WeatherMessage;

@Component(designateFactory=PowerBoard.Config.class, provide = Endpoint.class, immediate=true)
public class PowerBoard
	extends AbstractResourceDriver<PowerState, ResourceControlParameters>
    implements UncontrollableDriver, ObservationConsumer<IDcoesMessage>, Runnable {

    private final static Logger log = LoggerFactory.getLogger(PowerBoard.class);

    @Meta.OCD
    interface Config {
        @Meta.AD(deflt="", description="name")
        String name();
    }

    protected PowerBoardState latestPbState;
    
    private FlexiblePowerContext flexiblePowerContext;
    
    private ObservationProvider<IDcoesMessage> provider = null;
    
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
    public void onFoundProvider(ObservationProvider<IDcoesMessage> provider) {
        if (this.provider != null)
            this.provider.unsubscribe(this);
        log.info(String.format("register to %s", provider.toString()));
        provider.subscribe(this);
        this.provider = provider;
    }
    
    @Activate
    public void activate(BundleContext context, Map<String, ?> properties) {
        log.info("Activate");
    }
    
    @Deactivate
    public void deactivate() {
        log.info("Deactivate");
        if (this.provider != null)
            this.provider.unsubscribe(this);
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