package com.dontstravemc.crafting;

public enum TechCategory {
    TOOLS("tools"),
    LIGHT("light"),
    SURVIVAL("survival"),
    FOOD("food"),
    SCIENCE("science"),
    FIGHT("fight"),
    STRUCTURES("structures"),
    REFINE("refine"),
    MAGIC("magic"),
    DRESS("dress");

    private final String id;

    TechCategory(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
