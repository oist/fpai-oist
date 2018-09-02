package jp.oist.unit.ios.solarsystemlib.pvsystem.cell.temperature;

public class CellTemperature {
    public static class Variable {
        public final double module, cell;

        public Variable(double module, double cell) {
            this.module = module;
            this.cell = cell;
        }
    }
}
