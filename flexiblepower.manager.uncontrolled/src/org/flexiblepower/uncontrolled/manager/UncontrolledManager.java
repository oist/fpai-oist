package org.flexiblepower.uncontrolled.manager;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.measure.Measurable;
import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Power;
import javax.measure.unit.SI;

import org.flexiblepower.context.FlexiblePowerContext;
import org.flexiblepower.efi.UncontrolledResourceManager;
import org.flexiblepower.efi.uncontrolled.UncontrolledMeasurement;
import org.flexiblepower.efi.uncontrolled.UncontrolledRegistration;
import org.flexiblepower.efi.uncontrolled.UncontrolledUpdate;
import org.flexiblepower.messaging.Endpoint;
import org.flexiblepower.messaging.Port;
import org.flexiblepower.ral.ResourceControlParameters;
import org.flexiblepower.ral.drivers.uncontrolled.PowerState;
import org.flexiblepower.ral.ext.AbstractResourceManager;
import org.flexiblepower.ral.messages.ControlSpaceRevoke;
import org.flexiblepower.ral.messages.ResourceMessage;
import org.flexiblepower.ral.values.CommodityMeasurables;
import org.flexiblepower.ral.values.CommoditySet;
import org.flexiblepower.ral.values.ConstraintListMap;
import org.flexiblepower.ui.Widget;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = Endpoint.class, immediate = true)
@Designate(ocd = UncontrolledManager.Config.class, factory = true)
@Port(name = "driver", accepts = PowerState.class)
public class UncontrolledManager extends
                                AbstractResourceManager<PowerState, ResourceControlParameters> implements
                                                                                              UncontrolledResourceManager {

    private static final Logger logger = LoggerFactory.getLogger(UncontrolledManager.class);

    @ObjectClassDefinition
    public @interface Config {
        @AttributeDefinition(description = "Resource identifier")
        String resourceId() default "uncontrolled";

        @AttributeDefinition(type = AttributeType.INTEGER,
                             description = "Expiration of the ControlSpaces [s]",
                             required = false)
        int expirationTime() default 20;

        @AttributeDefinition(type = AttributeType.BOOLEAN,
                             description = "Show simple widget")
        boolean showWidget() default false;
    }

    private Config config;

    private FlexiblePowerContext context;
    private UncontrolledManagerWidget widget;
    private ServiceRegistration<Widget> widgetRegistration;
    private Measurable<Power> lastDemand;
    private Date changedState;
    private Measure<Integer, Duration> allocationDelay;

    @Reference
    public void setContext(FlexiblePowerContext context) {
        this.context = context;
    }

    @Activate
    public void activate(BundleContext bundleContext, final Config config) {
        this.config = config;
        if (config.showWidget()) {
            widget = new UncontrolledManagerWidget(this);
            widgetRegistration = bundleContext.registerService(Widget.class, widget, null);
        }

        logger.debug("Activated");
    };

    @Deactivate
    public void deactivate() {
        if (widgetRegistration != null) {
            widgetRegistration.unregister();
            widgetRegistration = null;
        }

        logger.debug("Deactivated");
    }

    public Measurable<Power> getLastDemand() {
        return lastDemand;
    }

    public String getResourceId() {
        return config.resourceId();
    }

    @Override
    protected List<? extends ResourceMessage> startRegistration(PowerState state) {
        changedState = context.currentTime();
        allocationDelay = Measure.valueOf(5, SI.SECOND);
        ConstraintListMap constraintList = ConstraintListMap.electricity(null); // this version of the uncontrolled
                                                                                // manager does not support
                                                                                // curtailments...
        UncontrolledRegistration reg = new UncontrolledRegistration(getResourceId(),
                                                                    changedState,
                                                                    allocationDelay,
                                                                    CommoditySet.onlyElectricity, constraintList);
        UncontrolledUpdate update = createUncontrolledUpdate(state);
        return Arrays.asList(reg, update);
    }

    private UncontrolledUpdate createUncontrolledUpdate(PowerState state) {
        Measurable<Power> currentUsage = state.getCurrentUsage();
        CommodityMeasurables measurables = CommodityMeasurables.electricity(currentUsage);
        UncontrolledUpdate update = new UncontrolledMeasurement(getResourceId(),
                                                                changedState,
                                                                context.currentTime(),
                                                                measurables);
        return update;
    }

    @Override
    protected List<? extends ResourceMessage> updatedState(PowerState state) {
        return Arrays.asList(createUncontrolledUpdate(state));
    }

    @Override
    protected ResourceControlParameters receivedAllocation(ResourceMessage message) {
        throw new AssertionError();
    }

    @Override
    protected ControlSpaceRevoke createRevokeMessage() {
        return new ControlSpaceRevoke(config.resourceId(), context.currentTime());
    }
}
