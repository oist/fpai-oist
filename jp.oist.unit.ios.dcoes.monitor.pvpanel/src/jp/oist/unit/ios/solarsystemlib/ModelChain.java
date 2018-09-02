/* vim : set ts=4 sw=4 et fenc=utf-8 ff=unix cc=100 : */
package jp.oist.unit.ios.solarsystemlib;

import jp.oist.unit.ios.solarsystemlib.atomsphere.airmass.Airmass;
import jp.oist.unit.ios.solarsystemlib.collection.DefaultModelCollection;
import jp.oist.unit.ios.solarsystemlib.collection.ModelCollection;
import jp.oist.unit.ios.solarsystemlib.common.Consts;
import jp.oist.unit.ios.solarsystemlib.irradiance.Irradiance;
import jp.oist.unit.ios.solarsystemlib.irradiance.aoi.AoiLossModel;
import jp.oist.unit.ios.solarsystemlib.irradiance.spectrum.SpectralLossModel;
import jp.oist.unit.ios.solarsystemlib.pvsystem.PvSystem;
import jp.oist.unit.ios.solarsystemlib.pvsystem.ac.PvWattsAc;
import jp.oist.unit.ios.solarsystemlib.pvsystem.cell.temperature.CellTemperature;
import jp.oist.unit.ios.solarsystemlib.pvsystem.cell.temperature.CellTemperatureModel;
import jp.oist.unit.ios.solarsystemlib.pvsystem.dc.PvWattsDc;
import jp.oist.unit.ios.solarsystemlib.solarposition.SolarPosition;

import java.time.ZonedDateTime;
import java.util.Map;

public class ModelChain {

    public static class Result {
        public final double dcPower, acPower;
        public Result(double dcPower, double acPower) {
            this.dcPower = dcPower;
            this.acPower = acPower;
        }
    }

    private class PrepareInputs {
        public double pressure, tempAir, windSpeed;
        public SolarPosition.Variable solarPosition;
        public Airmass airmass;
        public Irradiance.PoaVariable poaIrradiance;
        public double aoi;
        public double aoiCoefficient, spectralCoefficient;
        public double effectiveIrradiance;
    }

    public final PvSystem system;
    public final Location location;
    public final Map<String, Object> options;

    private ModelCollection models = new DefaultModelCollection();

    public ModelChain(PvSystem system, Location location, Map<String, Object> options) {
        this.system = system;
        this.location = location;
        this.options = options;
    }

    public PrepareInputs prepareInputs(ZonedDateTime ts, Irradiance.Variable irrad,
                                       Double pressure, Double tempAir, Double windSpeed) {
        PrepareInputs ret = new PrepareInputs();
        ret.pressure = (pressure == null) ? Consts.DEFAULT_PRESSURE : pressure;
        ret.tempAir = (tempAir == null) ? Consts.DEFAULT_TEMPERATURE : tempAir;
        ret.windSpeed = (windSpeed == null) ? 0.0 : windSpeed;

        // Prepare Inputs
        SolarPosition solarposition = models.solarposition();
        Irradiance irradiance = models.irradiance();

        ret.solarPosition = solarposition.estimate(ts, location, ret.pressure, ret.tempAir);
        ret.airmass = location.getAirmass(ret.solarPosition);
        ret.aoi = Irradiance.aoi(system.surfaceTilt, system.surfaceAzimuth,
                                 ret.solarPosition.getApparentZenith(),
                                 ret.solarPosition.getAzimuth());
        ret.poaIrradiance = irradiance.getPoaIrradiance(system.surfaceTilt, system.surfaceAzimuth,
                                                        ret.solarPosition, irrad, ret.airmass,
                                                        system.surfaceType);

        Object[] aoiLossVars = (Object[]) options.getOrDefault("aoiLossModel", null);
        AoiLossModel aoiLossModel = models.getAoiLossModel();
        ret.aoiCoefficient = aoiLossModel.estimate(ret.aoi, aoiLossVars);

        SpectralLossModel spectralLossModel = models.getSpectralLossModel();
        Object[] spectralLossVars = (Object[]) options.getOrDefault("spectralLossModel", null);
        ret.spectralCoefficient = spectralLossModel.estimate(ret.airmass, spectralLossVars);

        ret.effectiveIrradiance = effectiveIrradiance(ret.poaIrradiance,
                                                      ret.aoiCoefficient, ret.spectralCoefficient,
                                                      null);
        return ret;
    }

    public ModelChain.Result pvWatts(ZonedDateTime ts, Irradiance.Variable irrad,
                                     Double pressure, Double tempAir, Double windSpeed) {
        Object[] pvWattsDcOpts = (Object[])options.getOrDefault("pvWattsDc", null);
        if (pvWattsDcOpts == null || pvWattsDcOpts.length < 1)
            throw new IllegalArgumentException("\"pvWattsDc\" key must be set as. Object[] {"
                                            + "0: [Double:pdc0 (required)], "
                                            + "1: [Double:gammaPdc (default: -0.03)], "
                                            + "2: [Double:reftemp (default: 25.0) ]}");

        // Nameplate DC rating
        double pdc0 = (Double)pvWattsDcOpts[0];
        // The temperature coefficient in units of 1/c. default -0.03
        double gammaPdc = (pvWattsDcOpts.length > 1)? (Double)pvWattsDcOpts[1] : -0.03;
        // Cell reference temperature in degC . default 25.0
        double refTemp = (pvWattsDcOpts.length > 2)? (Double)pvWattsDcOpts[2] : 25.0;

        PrepareInputs vars = prepareInputs(ts, irrad, pressure, tempAir, windSpeed);

        CellTemperatureModel cellTempModel = models.getCellTemperatureModel();
        CellTemperature.Variable cellTemp = cellTempModel.estimate(vars.poaIrradiance, vars.tempAir, vars.windSpeed,
                                                                   system.rackingModel);
        PvWattsDc pvWattsDc = new PvWattsDc(models);
        double pdc = pvWattsDc.estimate(vars.effectiveIrradiance, cellTemp, pdc0, gammaPdc, refTemp);

        Object[] pvWattsAcOpts = (Object[])options.getOrDefault("pvWattsAc", null);
        Double nominalEfficiency = null;
        Double referenceEfficiency = null;
        if (pvWattsAcOpts != null) {
            nominalEfficiency = (pvWattsAcOpts.length > 0)? (Double)pvWattsAcOpts[0] : null;
            referenceEfficiency = (pvWattsAcOpts.length > 1)? (Double)pvWattsAcOpts[1] : null;
        }

        PvWattsAc pvWattsAc = new PvWattsAc(models);
        double pac = pvWattsAc.estimate(pdc, pdc0, nominalEfficiency, referenceEfficiency);

        return new ModelChain.Result(pdc, pac);
    }

    /**
     * Estimates effective irradiance
     *
     * @param poa          POA Irradiance
     * @param aoiLoss      aoi loss
     * @param spectralLoss spectral loss
     * @param fd           fraction of diffuse irradiance
     * @return effective irradiance
     */
    public double effectiveIrradiance(Irradiance.PoaVariable poa, double aoiLoss, double spectralLoss, Double fd) {
        fd = (fd == null) ? 1.0 : fd;
        return spectralLoss * (poa.direct * aoiLoss + fd * poa.diffuse);
    }

    public void setModelCollection(ModelCollection models) {
        this.models = models;
    }

    public ModelCollection getModelCollection() {
        return models;
    }
}
