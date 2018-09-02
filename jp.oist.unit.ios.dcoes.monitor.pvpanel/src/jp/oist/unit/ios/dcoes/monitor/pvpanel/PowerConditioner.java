package jp.oist.unit.ios.dcoes.monitor.pvpanel;

import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.measure.Measure;
import javax.measure.unit.SI;

import org.flexiblepower.messaging.Endpoint;
import org.flexiblepower.observation.Observation;
import org.flexiblepower.observation.ObservationConsumer;
import org.flexiblepower.observation.ObservationProvider;
import org.flexiblepower.ral.ResourceControlParameters;
import org.flexiblepower.ral.drivers.uncontrolled.PowerState;
import org.flexiblepower.ral.drivers.uncontrolled.UncontrollableDriver;
import org.flexiblepower.ral.ext.AbstractResourceDriver;
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
import jp.oist.unit.ios.solarsystemlib.Location;
import jp.oist.unit.ios.solarsystemlib.ModelChain;
import jp.oist.unit.ios.solarsystemlib.collection.DefaultModelCollection;
import jp.oist.unit.ios.solarsystemlib.collection.ModelCollection;
import jp.oist.unit.ios.solarsystemlib.pvsystem.PvSystem;
import jp.oist.unit.ios.solarsystemlib.solarposition.SolarPosition;
import jp.oist.unit.ios.solarsystemlib.irradiance.Irradiance;

@Component(designateFactory=PowerConditioner.Config.class, provide=Endpoint.class, immediate=true)
public class PowerConditioner
	extends AbstractResourceDriver<PowerState, ResourceControlParameters>
	implements ObservationConsumer<IDcoesMessage>, UncontrollableDriver, Runnable {

    private final static Logger log = LoggerFactory.getLogger(PowerConditioner.class);

    private ObservationProvider<IDcoesMessage> provider = null;
    private final Object pbLock = new Object();
    private final Object wsLock = new Object();

    private final Location loc = new Location(26.462, 127.831, 42.982);
    // typo: grass -> glass
    private final PvSystem pvsys1 = new PvSystem(27.5, 225.0, 6, Irradiance.SurfaceType.GRASS);
    private final PvSystem pvsys2 = new PvSystem(27.5, 225.0, 6, Irradiance.SurfaceType.GRASS);
    public final ModelCollection models = new DefaultModelCollection();
    
    private ModelChain modelChain1 = null;
    private ModelChain modelChain2 = null;
    
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
                                                          ZoneId.of("Asia/Tokyo"));
        double dPower = emu.getDouble("pvc_charge_power");
        double dVoltage = emu.getDouble("pvc_charge_voltage");
        double dCurrent = emu.getDouble("pvc_charge_current");
        boolean hasError = message.getJSONArray("tags").toList().contains("pvc_error");

        latestPvcState = new PvcState(timestamp, Measure.valueOf(dPower*-1.0, SI.WATT));

        double rsoc = emu.getDouble("rsoc");

        boolean canMessagePublished = true;

        if (hasError || rsoc >= 95) {
            /* TODO: simulate pv-power if rsoc is full */
			canMessagePublished = false;
			if (latestWeather == null)
				return;
			
			Irradiance irradiance = models.irradiance();
			
			long ts = latestWeather.getLong("timestamp");
			Instant instant = Instant.ofEpochSecond(ts);
			ZonedDateTime dt = ZonedDateTime.ofInstant(instant, ZoneId.of("Asia/Tokyo"));
			
			double ghi = latestWeather.getDouble("solar_radiation");
			double pressure = latestWeather.getDouble("barometer");
			double tempAir = latestWeather.getDouble("outside_temperature");
			double windSpeed = latestWeather.getDouble("wind_speed");
			
			SolarPosition.Variable solarpos = loc.getSolarPosition(dt, pressure, tempAir);
			Irradiance.Variable irradvals = irradiance.getIrradiance(solarpos, ghi);
			
			ModelChain.Result rslt1 = modelChain1.pvWatts(dt, irradvals,  pressure, tempAir, windSpeed);
			ModelChain.Result rslt2 = modelChain1.pvWatts(dt, irradvals,  pressure, tempAir, windSpeed);

			double totalPower = rslt1.dcPower + rslt2.dcPower;
			
			totalPower *= 0.97;
			totalPower *= -1.0;

			latestPvcState = new PvcState(timestamp, Measure.valueOf(totalPower, SI.WATT));
			
			canMessagePublished = true;
        }

        if (canMessagePublished) {
            //synchronized(pbLock) {
            //}
			publishState(latestPvcState);
        }
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
        Map<String, Object> opts = new HashMap<>();
        opts.put("pvWattsDc", new Object[] {233.0 * 6.0, -0.003});
        
        modelChain1 = new ModelChain(pvsys1, loc, opts);
        modelChain1.setModelCollection(models);

        modelChain2 = new ModelChain(pvsys2, loc, opts);
        modelChain2.setModelCollection(models);
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

	@Override
	public void run() {
		log.info("RUN");
	}

	@Override
	protected void handleControlParameters(ResourceControlParameters controlParameters) {
		log.info("handleControlParameters");
	}
}