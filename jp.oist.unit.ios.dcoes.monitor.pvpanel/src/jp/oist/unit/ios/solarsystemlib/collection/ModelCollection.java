/* vim: set ts=4 sw=4 et fenc=utf-8 ff=unix cc=100 : */
package jp.oist.unit.ios.solarsystemlib.collection;

import jp.oist.unit.ios.solarsystemlib.atomsphere.Atmosphere;
import jp.oist.unit.ios.solarsystemlib.solarposition.SolarPosition;
import jp.oist.unit.ios.solarsystemlib.atomsphere.airmass.RelativeAirmassModel;
import jp.oist.unit.ios.solarsystemlib.irradiance.Irradiance;
import jp.oist.unit.ios.solarsystemlib.irradiance.aoi.AoiLossModel;
import jp.oist.unit.ios.solarsystemlib.irradiance.dni.DniModel;
import jp.oist.unit.ios.solarsystemlib.irradiance.extraradiation.ExtraRadiationModel;
import jp.oist.unit.ios.solarsystemlib.irradiance.skydiffuse.SkyDiffuseModel;
import jp.oist.unit.ios.solarsystemlib.irradiance.spectrum.SpectralLossModel;
import jp.oist.unit.ios.solarsystemlib.pvsystem.cell.temperature.CellTemperatureModel;
import jp.oist.unit.ios.solarsystemlib.solarposition.SolarPositionModel;

public interface ModelCollection {

    Irradiance irradiance();

    Atmosphere atmosphere();

    SolarPosition solarposition();

    CellTemperatureModel getCellTemperatureModel();

    SpectralLossModel getSpectralLossModel();

    AoiLossModel getAoiLossModel();

    SkyDiffuseModel getSkyDiffuseModel();

    SolarPositionModel getSolarPositionModel();

    ExtraRadiationModel getExtraRadiationModel();

    DniModel getDniModel();

    RelativeAirmassModel getRelativeAirmassModel();
}