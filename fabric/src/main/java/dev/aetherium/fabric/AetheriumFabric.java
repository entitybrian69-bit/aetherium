package dev.aetherium.fabric;

import dev.aetherium.Aetherium;
import net.fabricmc.api.ClientModInitializer;

public final class AetheriumFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Aetherium.init();
    }
}
