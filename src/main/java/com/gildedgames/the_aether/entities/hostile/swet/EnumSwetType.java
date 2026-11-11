package com.gildedgames.the_aether.entities.hostile.swet;

public enum EnumSwetType {

    BLUE(),
    GOLDEN();

    EnumSwetType() {

    }

    public int getId() {
        return this.ordinal();
    }

    public static EnumSwetType get(int id) {
        EnumSwetType[] values = values();

        return id >= 0 && id < values.length ? values[id] : BLUE;
    }
}
