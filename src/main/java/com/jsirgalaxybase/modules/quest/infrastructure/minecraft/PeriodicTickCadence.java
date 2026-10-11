package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

/** Small pure helper so the periodic scheduling boundary can be tested without Minecraft. */
final class PeriodicTickCadence {

    private PeriodicTickCadence() {
    }

    static boolean isDue(int ticksExisted, int intervalTicks) {
        return intervalTicks > 0 && ticksExisted % intervalTicks == 0;
    }
}
