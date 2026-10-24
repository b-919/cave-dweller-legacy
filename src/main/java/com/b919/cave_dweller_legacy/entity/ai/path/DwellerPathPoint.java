package com.b919.cave_dweller_legacy.entity.ai.path;

import net.minecraft.pathfinding.PathPoint;

public class DwellerPathPoint extends PathPoint {
    private final DwellerNodeType nodeType;
    private final boolean climbing;

    public DwellerPathPoint(int x, int y, int z, DwellerNodeType nodeType, boolean climbing) {
        super(x, y, z);
        this.nodeType = nodeType;
        this.climbing = climbing;
    }

    public DwellerNodeType getNodeType() {
        return nodeType;
    }

    public boolean isClimbing() {
        return climbing;
    }
}