package com.b919.cave_dweller_legacy.entity.ai.path;

public enum DwellerNodeType {
    WALK(2.7F, 1.0F),
    CROUCH(1.8F, 1.2F),
    CRAWL(0.5F, 1.5F);

    private final float height;
    private final float movementCost;

    DwellerNodeType(float height, float movementCost) {
        this.height = height;
        this.movementCost = movementCost;
    }

    public float getHeight() {
        return height;
    }

    public float getMovementCost() {
        return movementCost;
    }
}