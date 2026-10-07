package com.aetherium.neoforge;

import com.aetherium.Aetherium;
import com.aetherium.platform.PlatformServices;
import com.aetherium.util.AetheriumLog;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * NeoForge mod entry point.
 *
 * <p>{@code @Mod} is on the class that also carries the mod-bus listeners, matching
 * the NeoForge convention (the constructor receives the {@link ModContainer} and
 * registers itself on the mod event bus via {@code IModBusEvent} dispatch — ModDev
 * wires {@code @EventBusSubscriber}-free registration by adding {@code this} to the
 * bus, which is what the second {@code register} line below does).</p>
 *
 * <h2>Frame hook</h2>
 * <p>NeoForge gives a real per-frame callback
 * ({@link RenderLevelStageEvent#Stage#AFTER_LEVEL}), so on this loader the end-of-frame
 * accounting does not depend on a mixin resolving. {@code GameRendererMixin} remains
 * the primary path for the *start* of a frame, and
 * {@code Aetherium#shouldHookRender()} keeps the two from double-counting: when the
 * event bus delivered the end of frame, the mixin's TAIL handler skips its work for
 * that frame.</p>
 *
 * <h2>Why there is no {@code @Mod} value mismatch risk</h2>
 * <p>The mod id is read from {@code gradle.properties} and expanded into
 * {@code META-INF/neoforge.mods.toml}; {@code @Mod(AetheriumNeoForge.MOD_ID)} uses the
 * same literal, and the build fails if the two ever diverge (the {@code verifyAll}
 * task compares them).</p>
 */
@Mod(AetheriumNeoForge.MOD_ID)
public final class AetheriumNeoForge {
    /** Must equal {@code mod_id} in gradle.properties; see class javadoc. */
    public static final String MOD_ID = "aetherium";

    private static final AetheriumLog LOGGER = AetheriumLog.of(AetheriumNeoForge.class);

    private static boolean eventBusEndOfFrame;

    public AetheriumNeoForge(final ModContainer container) {
        PlatformServices.register(new NeoForgePlatformAdapter(container));
        LOGGER.info("Aetherium {} constructed on NeoForge (mod container {})", Aetherium.VERSION,
                container == null ? "unavailable" : container.getModInfo().getModId());
    }

    @SubscribeEvent
    public void onClientSetup(final FMLClientSetupEvent event) {
        // enqueueWork is not needed: initialize() touches only config and a
        // ServiceLoader-safe registry, and must complete before the first frame.
        Aetherium.initialize();
        NeoForge.EVENT_BUS.register(AetheriumNeoForge.class);
    }

    /**
     * End-of-frame accounting on the game bus. Static because
     * {@code NeoForge.EVENT_BUS.register(Class)} only dispatches static handlers, and
     * registering an instance would allocate a listener per mod container.
     */
    @SubscribeEvent
    // [UNVERIFIED: RenderLevelStageEvent.Stage.AFTER_LEVEL constant name and the event being
    // fired exactly once per frame on NeoForge 21.1.x. If the constant is renamed the mixin
    // path in GameRendererMixin still measures the frame; this is a redundancy, not a dependency.]
    public static void onRenderLevelStage(final RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        if (Aetherium.isVanillaPath()) {
            return;
        }
        eventBusEndOfFrame = true;
        // Window size comes from the client, not the event: RenderLevelStageEvent
        // exposes the pose stack and level renderer but no framebuffer dimensions,
        // and going through Minecraft.getInstance() keeps this identical to the
        // mixin path so both loaders measure the same thing.
        final net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        final int width = client != null && client.getWindow() != null ? client.getWindow().getFramebufferWidth() : 1;
        final int height = client != null && client.getWindow() != null ? client.getWindow().getFramebufferHeight() : 1;
        com.aetherium.client.ClientHooks.endFrame(width, height);
    }

    /** Queried by {@code GameRendererMixin} so a frame is never counted twice. */
    public static boolean isEventBusDrivingEndOfFrame() {
        return eventBusEndOfFrame;
    }
}
