package io.mitallast.kernel;

public class Unit {
    private static final Unit unit = new Unit();

    public static Unit unit() {
        return unit;
    }

    private Unit() {
    }

    @Override
    public String toString() {
        return "Unit";
    }
}
