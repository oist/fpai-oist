/* vim: set ts=4 sw=4 et fenc=utf-8 ff=unix : */
package jp.oist.unit.ios.dcoes.monitor;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.flexiblepower.observation.Observation;
import org.flexiblepower.observation.ObservationConsumer;
import org.flexiblepower.observation.ext.SimpleObservationProvider;
import org.json.JSONObject;
import org.json.JSONTokener;import org.osgi.framework.BundleContext;
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

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component(designate = Monitor.Config.class, provide=IDcoesMessage.class, immediate=true)
public class Monitor {
    private final static Logger log = LoggerFactory.getLogger(Monitor.class);
    
    private final static String ESS_SCHEMA_PATH = "jp/oist/unit/ios/dcoes/monitor/message/ess.schema.json";
    private final static String WS_SCHEMA_PATH = "jp/oist/unit/ios/dcoes/monitor/message/ws.schema.json";
    
    @Meta.OCD
    interface Config {
        @Meta.AD(deflt="tcp://127.0.0.1:1883", required=true, description="brokerUri")
        String brokerUri();
        
        @Meta.AD(deflt="", required=true, description="target ess_topic of message")
        String essTopic();
        
        @Meta.AD(deflt="", required=true, description="target weather_topic of message")
        String weatherTopic();

        @Meta.AD(deflt="MQTTv31", optionValues={"MQTTv31", "MQTTv311"}, description="MQTT Protocol version")
        String protocol();
        
        @Meta.AD(deflt="", description="login username")
        String username();
        
        @Meta.AD(deflt="", description="login password")
        String password();
    }
    
    private Schema essSchema;
    private Schema wsSchema;
    
    private JSONObject latestEss = null;
    private JSONObject latestWs = null;

    private SimpleObservationProvider<IDcoesMessage> provider;
    
    private ExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    private Monitor.Config config = null;

    private MqttDefaultFilePersistence persistence = null;

    private MqttAsyncClient mqttCli = null;

    private final MqttConnectOptions mqttOpts = new MqttConnectOptions();
    
    private final MqttCallbackExtended mqttCallback = new MqttCallbackExtended() {
        @Override
        public void connectionLost(Throwable ex) {
            log.error("Connection lost", ex);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
        }

        @Override
        public void connectComplete(boolean reconnect, String brokerUri) {
            try {
                log.info(String.format("Connection established with \"%s\"", brokerUri));
                
                /* Handle Ess Message */
                IMqttToken essToken = mqttCli.subscribe(config.essTopic(), 1, new IMqttMessageListener() { 
                    @Override
                    public void messageArrived(String topic, MqttMessage message)throws Exception {
                        JSONObject payload = new JSONObject(new String(message.getPayload()));
                        try {
                            essSchema.validate(payload);
                            log.info(String.format("\"%s\" topic has arrived.", topic));

                            latestEss = payload;
                            Date timestamp = new Date(payload.getLong("timestamp"));
                            provider.publish(Observation.create(timestamp, new EssMessage(payload)));
                        } catch (ValidationException ex) {
                            log.error(String.format("\"%s\" topic has arrived, but payload of that is malformed.", topic), ex);
                            for (String msg : ex.getAllMessages()) { log.warn(msg); }
                        }
                    }
                });
                essToken.waitForCompletion(60);

                /* Handle WeatherStation Message */
                IMqttToken wsToken = mqttCli.subscribe(config.weatherTopic(), 1, new IMqttMessageListener() {
                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                        JSONObject payload = new JSONObject(new String(message.getPayload()));
                        try {
                            wsSchema.validate(payload);
                            log.info(String.format("\"%s\" topic has arrived.", topic));

                            latestWs = payload;
                            Date timestamp = new Date(payload.getLong("timestamp"));
                            provider.publish(Observation.create(timestamp, new WeatherMessage(payload)));
                        } catch (ValidationException ex) {
                            log.error(String.format("\"%s\" topic has arrived, but payload of that is malformed.", topic), ex);
                            for (String msg : ex.getAllMessages()) { log.warn(msg); }
                        }
                    }
                });
                wsToken.waitForCompletion(60);
            } catch (MqttException ex) {
                log.error("", ex);
            }
        }
    };
    
    private Schema loadSchema(String schemaPath) {
        JSONObject rawSchema = new JSONObject(new JSONTokener(getClass().getResourceAsStream(schemaPath)));
        SchemaLoader schemaLoader = SchemaLoader.builder().schemaJson(rawSchema).build();
        return schemaLoader.load().build();
    }

    @Activate
    public void activate(BundleContext context, Map<String, ?> properties) {
        log.debug("Activate");
        config = Configurable.createConfigurable(Monitor.Config.class, properties);

        provider = SimpleObservationProvider.create(this, IDcoesMessage.class)
                                            .observationOf("dcoes monitor")
                                            .build();

        mqttOpts.setUserName(config.username());
        mqttOpts.setPassword(config.password().toCharArray());
        mqttOpts.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        mqttOpts.setCleanSession(true);
        mqttOpts.setAutomaticReconnect(true);
        mqttOpts.setConnectionTimeout(30);
        mqttOpts.setKeepAliveInterval(60);
        
        String clientId = MqttAsyncClient.generateClientId();
        try {
        		essSchema = loadSchema(context.getBundle().getEntry(ESS_SCHEMA_PATH).getPath());
        		wsSchema = loadSchema(context.getBundle().getEntry(WS_SCHEMA_PATH).getPath());

        		persistence = new MqttDefaultFilePersistence();

            mqttCli = new MqttAsyncClient(config.brokerUri(), clientId, persistence);
            mqttCli.setCallback(mqttCallback);
            
            IMqttActionListener listener = new IMqttActionListener() {
                @Override
                public void onFailure(IMqttToken token, Throwable ex) {
                    log.error("Connection failure to server", ex);
                    scheduler.submit(new Runnable() {
                        public void run () {
                            IMqttActionListener listener;
                            try {
                                log.debug("Try to connect a server");
                                listener = (IMqttActionListener)token.getUserContext();
                                Thread.sleep(2000);
                                token.getClient().connect(mqttOpts, listener, listener);
                            } catch (MqttException ex) {
                                log.error("", ex);
                            } catch (InterruptedException ex) {
                                // nothing to do
                            }
                        }
                    });
                }

                @Override
                public void onSuccess(IMqttToken token) {
                }
            };

            mqttCli.connect(mqttOpts, listener, listener);
        } catch (MqttException ex) {
            log.error("", ex);
        }
    }
    
    @Deactivate
    public void deactivate() {
        log.debug("Deactivate");
        if (mqttCli != null && mqttCli.isConnected()) {
            try {
                mqttCli.disconnect();
            } catch(MqttException ex) {
                 // nothing to do                    
            }
                 
            try {
                mqttCli.close(true);
            } catch (MqttException ex) {
                // nothing to do
            }
         }
        provider.close();

        try {
            scheduler.awaitTermination(0, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            log.error("", ex);
        }
    }
    
    @Reference(optional=true, dynamic=true, multiple=true)
    public void subscribe(ObservationConsumer<IDcoesMessage> consumer) {
        log.info(String.format("%s subscribe", consumer.toString()));
        provider.subscribe(consumer);
        
        if (latestEss != null) {
            Date timestamp = new Date(latestEss.getLong("timestamp"));
            consumer.consume(provider, Observation.create(timestamp, new EssMessage(latestEss)));
        }

        if (latestWs != null) {
            Date timestamp = new Date(latestWs.getLong("timestamp"));
            consumer.consume(provider, Observation.create(timestamp, new WeatherMessage(latestWs)));
        }
    }

    public void unsubscribe(ObservationConsumer<IDcoesMessage> consumer) {
        provider.unsubscribe(consumer);
    }
}
