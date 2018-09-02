/* vim: set sw=4 ts=4 et fenc=utf-8 ff=unix cc=100 : */
package jp.oist.unit.ios.solarsystemlib.collection;

import jp.oist.unit.ios.solarsystemlib.atomsphere.Atmosphere;
import jp.oist.unit.ios.solarsystemlib.solarposition.SolarPosition;
import jp.oist.unit.ios.solarsystemlib.atomsphere.airmass.RelativeAirmassKastenYoung1989Model;
import jp.oist.unit.ios.solarsystemlib.atomsphere.airmass.RelativeAirmassModel;
import jp.oist.unit.ios.solarsystemlib.irradiance.Irradiance;
import jp.oist.unit.ios.solarsystemlib.irradiance.aoi.AoiLossModel;
import jp.oist.unit.ios.solarsystemlib.irradiance.aoi.AoiLossAshraeiamModel;
import jp.oist.unit.ios.solarsystemlib.irradiance.dni.DniModel;
import jp.oist.unit.ios.solarsystemlib.irradiance.dni.DniErbsModel;
import jp.oist.unit.ios.solarsystemlib.irradiance.extraradiation.ExtraRadiationModel;
import jp.oist.unit.ios.solarsystemlib.irradiance.extraradiation.ExtraRadiationSpencerModel;
import jp.oist.unit.ios.solarsystemlib.irradiance.skydiffuse.SkyDiffuseHaydaviesModel;
import jp.oist.unit.ios.solarsystemlib.irradiance.skydiffuse.SkyDiffuseModel;
import jp.oist.unit.ios.solarsystemlib.irradiance.spectrum.SpectralLossModel;
import jp.oist.unit.ios.solarsystemlib.pvsystem.cell.temperature.SapmCellTemperature;
import jp.oist.unit.ios.solarsystemlib.pvsystem.cell.temperature.CellTemperatureModel;
import jp.oist.unit.ios.solarsystemlib.solarposition.SolarPositionModel;
import jp.oist.unit.ios.solarsystemlib.solarposition.SolarPositionSpaModel;

public class DefaultModelCollection implements ModelCollection {

    protected SolarPositionModel solarPositionModel = new SolarPositionSpaModel(this);
    protected ExtraRadiationModel extraRadiationModel = new ExtraRadiationSpencerModel(this);
    protected DniModel dniModel = new DniErbsModel(this);
    protected RelativeAirmassModel relativeAmModel = new RelativeAirmassKastenYoung1989Model(this);
    protected SkyDiffuseModel skyDiffuseModel = new SkyDiffuseHaydaviesModel(this);
    protected AoiLossModel aoiLossModel = new AoiLossAshraeiamModel(this);
    protected SpectralLossModel spectralLossModel = new SpectralLossModel.NoLoss(this);
    protected CellTemperatureModel cellTemperatureModel = new SapmCellTemperature(this);

    private Irradiance irradiance = null;
    private Atmosphere atmosphere = null;
    private SolarPosition solarposition = null;

    public DefaultModelCollection() {
        irradiance = new Irradiance(this);
        atmosphere = new Atmosphere(this);
        solarposition = new SolarPosition(this);
    }

    @Override
    public Irradiance irradiance() {
        return irradiance;
    }

    @Override
    public Atmosphere atmosphere() {
        return atmosphere;
    }

    @Override
    public SolarPosition solarposition() {
        return solarposition;
    }

    @Override
    public CellTemperatureModel getCellTemperatureModel() {
        return cellTemperatureModel;
    }

    @Override
    public SpectralLossModel getSpectralLossModel() {
        return spectralLossModel;
    }

    @Override
    public AoiLossModel getAoiLossModel() {
        return aoiLossModel;
    }

    @Override
    public SkyDiffuseModel getSkyDiffuseModel() {
        return skyDiffuseModel;
    }

    @Override
    public SolarPositionModel getSolarPositionModel() {
        return solarPositionModel;
    }

    @Override
    public ExtraRadiationModel getExtraRadiationModel() {
        return extraRadiationModel;
    }

    @Override
    public DniModel getDniModel() {
        return dniModel;
    }

    @Override
    public RelativeAirmassModel getRelativeAirmassModel() {
        return relativeAmModel;
    }
}