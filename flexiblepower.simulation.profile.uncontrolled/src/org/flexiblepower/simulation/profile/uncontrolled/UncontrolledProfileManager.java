package org.flexiblepower.simulation.profile.uncontrolled;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.measure.Measure;
import javax.measure.quantity.Power;
import javax.measure.quantity.VolumetricFlowRate;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.flexiblepower.efi.uncontrolled.UncontrolledAllocation;
import org.flexiblepower.efi.uncontrolled.UncontrolledForecast;
import org.flexiblepower.efi.uncontrolled.UncontrolledMeasurement;
import org.flexiblepower.efi.uncontrolled.UncontrolledRegistration;
import org.flexiblepower.efi.uncontrolled.UncontrolledUpdate;
import org.flexiblepower.messaging.Cardinality;
import org.flexiblepower.messaging.Connection;
import org.flexiblepower.messaging.Endpoint;
import org.flexiblepower.messaging.MessageHandler;
import org.flexiblepower.messaging.Port;
import org.flexiblepower.ral.messages.AllocationRevoke;
import org.flexiblepower.ral.messages.AllocationStatusUpdate;
import org.flexiblepower.ral.messages.ControlSpaceRevoke;
import org.flexiblepower.ral.values.CommodityForecast;
import org.flexiblepower.ral.values.CommodityForecast.Builder;
import org.flexiblepower.ral.values.CommodityMeasurables;
import org.flexiblepower.ral.values.CommoditySet;
import org.flexiblepower.ral.values.ConstraintListMap;
import org.flexiblepower.ral.values.UncertainMeasure;
import org.flexiblepower.time.TimeService;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Port(name = "controller",
      accepts = { UncontrolledAllocation.class, AllocationRevoke.class },
      sends = { UncontrolledRegistration.class,
               UncontrolledUpdate.class,
               AllocationStatusUpdate.class,
               ControlSpaceRevoke.class },
      cardinality = Cardinality.SINGLE)
@Component(service = Endpoint.class, immediate = true)
@Designate(ocd = UncontrolledProfileManager.Config.class, factory = true)
public class UncontrolledProfileManager implements Runnable, MessageHandler, Endpoint, UncontrolledProfileForecaster {

    @ObjectClassDefinition
    public @interface Config {
        @AttributeDefinition(description = "CSV file with power data profile")
        String filename() default "pv.csv";

        @AttributeDefinition(type = AttributeType.BOOLEAN,
                             description = "Generates power [true] or consumes power [false]")
        boolean generatesPower() default true;

        @AttributeDefinition(type = AttributeType.INTEGER,
                             description = "Duration of each forecast element in minutes")
        int forecastDurationPerElement() default 15;

        @AttributeDefinition(type = AttributeType.INTEGER,
                             description = "Number of elements to use in each forecast")
        int forecastNumberOfElements() default 4;

        @AttributeDefinition(type = AttributeType.INTEGER,
                             description = "Randomness percentage [up and down] applied to forecast values")
        int forecastRandomnessPercentage() default 10;

        @AttributeDefinition(description = "Resource identifier")
        String resourceId() default "uncontrolledprofilemanager";

        @AttributeDefinition(type = AttributeType.INTEGER,
                             description = "Delay between updates will be send out in seconds")
        int updateDelay() default 5;

        @AttributeDefinition(description = "Commodity for this profile",
                             options = {
                            		 @Option(label=commodityElecricityOption, value=commodityElecricityOption),
                            		 @Option(label=commodityHeatOption, value=commodityHeatOption),
                            		 @Option(label=commodityGasOption, value=commodityGasOption)                            		 
                             })
        String profileCommodity() default commodityElecricityOption;
    }

    static final String commodityElecricityOption = "Electricity";
    static final String commodityHeatOption = "Heat";
    static final String commodityGasOption = "Gas";

    private CommoditySet commoditySet;

    private static final int YEAR = 2012;
    private static final int DAYS_IN_YEAR = 366;
    private static final int HOURS_IN_DAY = 24;
    private static final int MINUTES_IN_HOUR = 60;

    private static final Logger logger = LoggerFactory.getLogger(UncontrolledProfileManager.class);

    private Config config;
    private Connection connection;
    private ScheduledFuture<?> scheduledFuture;
    private float[] powerAtMinutesSinceJan1;
    private double randomFactor;
    private TimeService timeService;
    private ScheduledExecutorService scheduledExecutorService;

    @Activate
    public void activate(BundleContext bundleContext, final Config config) throws IOException {
        try {
            this.config = config;

            if (config.profileCommodity().equals(commodityElecricityOption)) {
                commoditySet = CommoditySet.onlyElectricity;
                logger.info("Electricity only");
            } else if (config.profileCommodity().equals(commodityHeatOption)) {
                commoditySet = CommoditySet.onlyHeat;
                logger.info("Heat only");
            } else {
                commoditySet = CommoditySet.onlyGas;
                logger.info("Gas only");
            }

            calcRandomFactor(config.forecastRandomnessPercentage());

            try {
                File file = new File(config.filename()); // For running from current directory
                if (file.exists() && file.isFile()) {
                    loadData(new FileInputStream(file));
                } else {
                    file = new File("res/" + config.filename()); // For running in Eclipse
                    if (file.exists() && file.isFile()) {
                        loadData(new FileInputStream(file));
                    } else {
                        URL url = bundleContext.getBundle().getResource(config.filename());
                        if (url != null) {
                            loadData(url.openStream());
                        } else {
                            throw new IllegalArgumentException("Could not load power profile data");
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("Could not load power profile data", e);
                throw (e);
            }

            scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(this,
                                                                           0, config.updateDelay(), TimeUnit.SECONDS);
        } catch (RuntimeException ex) {
            logger.error("Error during initialization of the uncontrolled profile manager: " + ex.getMessage(), ex);
            deactivate();
            throw ex;
        }
    }

    private void calcRandomFactor(int randomnessPercentage) {
        while (randomFactor == 0) {
            randomFactor = (2 * new Random().nextDouble() - 1) * config.forecastRandomnessPercentage() / 100 + 1;
        }
    }

    private CommodityForecast createForecast(Date startTime, int forecastNumberOfElements,
                                             int forecastDurationPerElement) {
        Builder forecastBuilder = CommodityForecast.create()
                                                   .duration(Measure.valueOf(60 * forecastDurationPerElement,
                                                                             SI.SECOND));
        for (int element = 0; element < forecastNumberOfElements; element++) {
            double powerValue = getPowerValue(startTime);

            if (config.profileCommodity().equals(commodityElecricityOption)) {
                forecastBuilder.electricity(new UncertainMeasure<Power>(powerValue * randomFactor, SI.WATT)).next();
            } else if (config.profileCommodity().equals(commodityHeatOption)) {
                forecastBuilder.heat(new UncertainMeasure<Power>(powerValue * randomFactor, SI.WATT)).next();
            } else {
                forecastBuilder.gas(new UncertainMeasure<VolumetricFlowRate>(powerValue * randomFactor,
                                                                             NonSI.CUBIC_METRE_PER_SECOND)).next();
            }

            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(startTime);
            calendar.add(Calendar.MINUTE, config.forecastDurationPerElement());
            startTime = calendar.getTime();
        }
        return forecastBuilder.build();
    }

    @Deactivate
    public void deactivate() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }
    }

    @Override
    public void disconnected() {
        connection = null;
    }

    @Override
    public CommodityForecast getForecast(Date startTime,
                                         int forecastNumberOfElements,
                                         int forecastDurationPerElement,
                                         int forecastRandomnessPercentage) {
        if (randomFactor == 0) {
            calcRandomFactor(forecastRandomnessPercentage);
        }
        return createForecast(startTime,
                              forecastNumberOfElements,
                              forecastDurationPerElement);
    }

    private double getPowerValue(Date date) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);

        int minutesSinceJan1 = minutesSinceJan1(month, day, hour, minute);
        float powerValue1 = powerAtMinutesSinceJan1[minutesSinceJan1];
        float powerValue2 = powerAtMinutesSinceJan1[(minutesSinceJan1 + 1) % powerAtMinutesSinceJan1.length];
        double interpolatedPowerValue = interpolate(powerValue1, powerValue2, second / 60.0);
        if (config.generatesPower()) {
            interpolatedPowerValue = -interpolatedPowerValue;
        }
        return interpolatedPowerValue;
    }

    @Override
    public void handleMessage(Object message) {
        // We do not expect messages
    }

    private double interpolate(float value1, float value2, double fraction) {
        return (1 - fraction) * value1 + fraction * value2;
    }

    private void loadData(InputStream is) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));

        powerAtMinutesSinceJan1 = new float[DAYS_IN_YEAR * HOURS_IN_DAY * MINUTES_IN_HOUR];
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            if (!line.startsWith("#")) { // Line does not contain comment
                String[] split = line.split(",");
                if (split.length == 5) {
                    int month = Integer.parseInt(split[0].trim());
                    int day = Integer.parseInt(split[1].trim());
                    int hour = Integer.parseInt(split[2].trim());
                    int minute = Integer.parseInt(split[3].trim());
                    float powerValue = Float.parseFloat(split[4].trim());

                    int index = minutesSinceJan1(month, day, hour, minute);
                    powerAtMinutesSinceJan1[index] = powerValue;
                }
            }
        }
        bufferedReader.close();
    }

    private int minutesSinceJan1(int month, int day, int hour, int minute) {
        long millisecondsAtJan1 = 0;
        try {
            millisecondsAtJan1 = new SimpleDateFormat("yyyy-dd-MM HH:mm:ss").parse(YEAR + "-01-01 00:00:00").getTime();
        } catch (ParseException e) {
        }
        Calendar calendar = new GregorianCalendar();
        calendar.set(YEAR, month - 1, day, hour, minute);
        long milliseconds = calendar.getTime().getTime();
        int minutesSinceJan1 = (int) ((milliseconds - millisecondsAtJan1) / 60000);
        return minutesSinceJan1;
    }

    @Override
    public MessageHandler onConnect(Connection connection) {
        this.connection = connection;

        connection.sendMessage(new UncontrolledRegistration(config.resourceId(),
                                                            timeService.getTime(),
                                                            Measure.zero(SI.SECOND),
                                                            commoditySet,
                                                            ConstraintListMap.create().build()));
        return this;
    }

    @Override
    public synchronized void run() {
        try {
            if (connection != null) {
                Date currentTime = timeService.getTime();

                double powerValue = getPowerValue(currentTime);

                CommodityMeasurables measurable;

                if (config.profileCommodity().equals(commodityElecricityOption)) {
                    measurable = CommodityMeasurables.create()
                                                     .electricity(Measure.valueOf(powerValue, SI.WATT))
                                                     .build();
                } else if (config.profileCommodity().equals(commodityHeatOption)) {
                    measurable = CommodityMeasurables.create().heat(Measure.valueOf(powerValue, SI.WATT)).build();
                } else {
                    measurable = CommodityMeasurables.create()
                                                     .gas(Measure.valueOf(powerValue, NonSI.CUBIC_METRE_PER_SECOND))
                                                     .build();
                }

                connection.sendMessage(new UncontrolledMeasurement(config.resourceId(),
                                                                   currentTime,
                                                                   currentTime,
                                                                   measurable));

                CommodityForecast forecast = createForecast(currentTime,
                                                            config.forecastNumberOfElements(),
                                                            config.forecastDurationPerElement());
                connection.sendMessage(new UncontrolledForecast(config.resourceId(), currentTime, currentTime, forecast));
            }
        } catch (Exception e) {
            logger.error("Error while running uncontrolled profile manager", e);
        }
    }

    @Reference
    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
    }

    @Reference
    public void setTimeService(TimeService timeService) {
        this.timeService = timeService;
    }
}
