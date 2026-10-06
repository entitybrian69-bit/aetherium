package dev.aetherium.neoforge;

import dev.aetherium.Aetherium;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(value = Aetherium.MOD_ID, dist = Dist.CLIENT)
public final class AetheriumNeoForge {
    public AetheriumNeoForge() {
        if (FMLEnvironment.dist == Dist.CLIENT) Aetherium.init();
    }
}
