package org.flexiblepower.monitoring.csv;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.flexiblepower.observation.ObservationProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Designate(ocd = Monitor.Config.class, factory = true)
public class Monitor {
    private static Logger logger = LoggerFactory.getLogger(Monitor.class);

    @ObjectClassDefinition
    public @interface Config {
        @AttributeDefinition(description = "The directory to which the CSV files will be written")
        String outputDirectory() default "/tmp/fpai/monitor";
    }

    private final Map<ObservationProvider<?>, MonitoredProvider<?>> monitoredProviders;

    public Monitor() {
        monitoredProviders = new ConcurrentHashMap<ObservationProvider<?>, MonitoredProvider<?>>();
    }

    private final Map<ObservationProvider<?>, Map<String, Object>> unhandledObservationProviders =
                                                                                                 new HashMap<ObservationProvider<?>, Map<String, Object>>();
    private File dataDir;

    @Activate
    public void activate(final Config config) {
        dataDir = new File(config.outputDirectory());
        if (!dataDir.exists()) {
            if (!dataDir.mkdirs()) {
                logger.error("Could not create the directory [{}] for monitor data", dataDir);
                dataDir = null;
            }
        } else if (!dataDir.isDirectory() || !dataDir.canWrite()) {
            logger.error("The directory [{}] can not be written to", dataDir);
            dataDir = null;
        }

        if (dataDir != null) {
            logger.info("Started monitoring, output in [{}]", dataDir.getAbsolutePath());

            synchronized (unhandledObservationProviders) {
                for (Entry<ObservationProvider<?>, Map<String, Object>> entry : unhandledObservationProviders.entrySet()) {
                    addProvider(entry.getKey(), entry.getValue());
                }
                unhandledObservationProviders.clear();
            }
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public <T> void addProvider(ObservationProvider<T> provider, Map<String, Object> properties) {
        if (dataDir != null) {
            logger.debug("Started monitoring of [{}]", provider);
            monitoredProviders.put(provider, new MonitoredProvider<T>(dataDir, provider, properties));
        } else {
            synchronized (unhandledObservationProviders) {
                if (dataDir == null) {
                    unhandledObservationProviders.put(provider, properties);
                } else {
                    addProvider(provider, properties);
                }
            }
        }
    }

    public void removeProvider(ObservationProvider<?> provider) {
        MonitoredProvider<?> storedProvider = monitoredProviders.get(provider);
        if (storedProvider != null) {
            storedProvider.close();
        }
        unhandledObservationProviders.remove(provider);
    }
}
