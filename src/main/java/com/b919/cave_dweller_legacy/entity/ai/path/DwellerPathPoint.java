package com.b919.cave_dweller_legacy.entity.ai.path;

import net.minecraft.pathfinding.PathPoint;

public class DwellerPathPoint extends PathPoint {
    private final DwellerNodeType nodeType;
    private final boolean climbing;
    private final boolean leap;

    public DwellerPathPoint(int x, int y, int z, DwellerNodeType nodeType, boolean climbing) {
        this(x, y, z, nodeType, climbing, false);
    }

    public DwellerPathPoint(int x, int y, int z, DwellerNodeType nodeType, boolean climbing, boolean leap) {
        super(x, y, z);
        this.nodeType = nodeType;
        this.climbing = climbing;
        this.leap = leap;
    }

    public DwellerNodeType getNodeType() {
        return nodeType;
    }
    public boolean isClimbing() {
        return climbing;
    }
    public boolean isLeap() {
        return leap;
    }
}
