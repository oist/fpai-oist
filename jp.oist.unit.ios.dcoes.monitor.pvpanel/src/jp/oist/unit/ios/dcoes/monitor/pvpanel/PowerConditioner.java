package jp.oist.unit.ios.dcoes.monitor.pvpanel;

import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import javax.measure.Measure;
import javax.measure.unit.SI;

import org.flexiblepower.messaging.Endpoint;
import org.flexiblepower.observation.Observation;
import org.flexiblepower.observation.ObservationConsumer;
import org.flexiblepower.observation.ObservationProvider;
import org.jruby.embed.ScriptingContainer;
import org.json.JSONObject;
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

@Component(designateFactory=PowerConditioner.Config.class, provide=Endpoint.class, immediate=true)
public class PowerConditioner 
    implements ObservationConsumer<IDcoesMessage> {

    private final static Logger log = LoggerFactory.getLogger(PowerConditioner.class);

    private ObservationProvider<IDcoesMessage> provider = null;
    private final Object pbLock = new Object();
    private final Object wsLock = new Object();
    
    protected PvcState latestPvcState = null;
    protected JSONObject latestWeather = null;
    
    @Meta.OCD
	interface Config {
        @Meta.AD(deflt="", description="name")
        String name();
	}
    
    private void consume(EssMessage ess) {
        JSONObject message = ess.getMessage();

        JSONObject ws = null;
        synchronized(wsLock) { ws = latestWeather; }

        JSONObject emu = message.getJSONObject("emu");

        ZonedDateTime timestamp = ZonedDateTime.ofInstant(Instant.ofEpochSecond(message.getLong("timestamp")),
                                                          ZoneOffset.systemDefault());
        double dPower = emu.getDouble("pvc_charge_power");
        double dVoltage = emu.getDouble("pvc_charge_voltage");
        double dCurrent = emu.getDouble("pvc_charge_current");
        boolean hasError = message.getJSONArray("tags").toList().contains("pvc_error");

        latestPvcState = new PvcState(timestamp,
                                      Measure.valueOf(dPower, SI.WATT),
        								 Measure.valueOf(dVoltage, SI.VOLT),
        								 Measure.valueOf(dCurrent, SI.AMPERE));

        double rsoc = emu.getDouble("rsoc");

        boolean canMessagePublished = true;

        if (hasError || rsoc >= 90) {
            /* TODO: simulate pv-power if rsoc is full */
        		canMessagePublished = true;
        }

        if (canMessagePublished) {
            synchronized(pbLock) {
                //latestPvcState = new PvcState(timestamp, power, voltage, current, hasError);
            }
        }
        //publishState(latestPvcState);
    }
    
    private void consume(WeatherMessage weather) {
        JSONObject message = weather.getMessage();
        synchronized(wsLock) {
        		latestWeather = message;
        }
    }

    @Activate
    public void activate() {
        log.info("Activate");
        try {
            ScriptingContainer container = new ScriptingContainer();
		} catch (Exception ex) {
			log.error("", ex);
		}
    }

    @Deactivate
    public void deactivate() {
        log.info("Deactivate");
        if (this.provider != null)
            this.provider.unsubscribe(this);
    }

	@Override
	public void consume(ObservationProvider<? extends IDcoesMessage> source,
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

    @Reference
    public void onFoundProvider(ObservationProvider<IDcoesMessage> provider) {
        if (this.provider != null)
            this.provider.unsubscribe(this);
        log.info(String.format("register to %s", provider.toString()));
        provider.subscribe(this);
        this.provider = provider;
    }
}