/* vim: set ts=4 sw=4 et fenc=utf-8 ff=unix cc=100 : */
package jp.oist.unit.ios.solarsystemlib.pvsystem;

import jp.oist.unit.ios.solarsystemlib.irradiance.Irradiance;
import jp.oist.unit.ios.solarsystemlib.solarposition.SolarPosition;

public class PvSystem {

    public enum RackingModel {
        OPEN_RACK_CELL_GLASSBACK,
        ROOF_MOUNT_CELL_GLASSBACK,
        OPEN_RACK_CELL_POLYMERBACK,
        INSULATED_RACK_CELL_POLYMERBACK,
        OPEN_RACK_POLYMER_THINFILM_STEEL,
        X22_CONCENTRATOR_TRACKER;
    }

    public final double surfaceTilt;
    public final double surfaceAzimuth;
    public final int modulesPerString;
    public final Irradiance.SurfaceType surfaceType;
    public final Double albedo;
    public final RackingModel rackingModel;

    public PvSystem(double surfaceTilt, double surfaceAzimuth, int modulesPerString) {
         this(surfaceTilt, surfaceAzimuth, modulesPerString,
             null, null, null);
    }

    public PvSystem(double surfaceTilt, double surfaceAzimuth, int modulesPerString, RackingModel rackingModel) {
         this(surfaceTilt, surfaceAzimuth, modulesPerString,
              null, null, rackingModel);
    }

    public PvSystem(double surfaceTilt, double surfaceAzimuth, int modulesPerString,
                    Irradiance.SurfaceType surfaceType) {
         this(surfaceTilt, surfaceAzimuth, modulesPerString,
              surfaceType, null, null);
    }

    public PvSystem(double surfaceTilt, double surfaceAzimuth, int modulesPerString,
                    Irradiance.SurfaceType surfaceType, RackingModel rackingModel) {
         this(surfaceTilt, surfaceAzimuth, modulesPerString,
              surfaceType, null, rackingModel);
    }

    public PvSystem(double surfaceTilt, double surfaceAzimuth, int modulesPerString,
                    Irradiance.SurfaceType surfaceType, Double albedo, RackingModel rackingModel) {
        this.surfaceTilt = surfaceTilt;
        this.surfaceAzimuth = surfaceAzimuth;
        this.modulesPerString = modulesPerString;
        this.surfaceType = surfaceType;
        this.albedo = (albedo == null)? ((surfaceType == null) ? 0.25 : surfaceType.getAlbedo()) : albedo;
        this.rackingModel = (rackingModel == null)? RackingModel.OPEN_RACK_CELL_GLASSBACK : rackingModel;
    }

    public double getAoi(SolarPosition.Variable sp) {
        return Irradiance.aoi(surfaceTilt, surfaceAzimuth, sp.getApparentZenith(), sp.getAzimuth());
    }
}