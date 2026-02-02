package com.tibolatte.greeny.blocks;

import net.minecraft.util.StringRepresentable;

public enum HeartState implements StringRepresentable {
    ACTIVE("active"),
    ANGRY("angry"),
    DORMANT("dormant");

    private final String name;
    HeartState(String name) { this.name = name; }
    @Override public String getSerializedName() { return name; }
}