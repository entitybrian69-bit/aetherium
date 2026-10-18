package com.aetherium.client;

import com.aetherium.perf.FogCull;
import com.aetherium.qol.ParticleFilter;
import com.aetherium.qol.Scenery;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
// @era:fog-begin shader17|shader19
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.level.material.FogType;
// @era:fog-else buffer|data
//~ import net.minecraft.client.Minecraft;
//~ import net.minecraft.world.level.material.FogType;
// @era:fog-else params
//~ import net.minecraft.client.renderer.FogParameters;
//~ import net.minecraft.world.level.material.FogType;
// @era:fog-else gl
//~ import com.mojang.blaze3d.systems.RenderSystem;
//~ import net.minecraft.tags.FluidTags;
//~ import net.minecraft.tags.Tag;
//~ import net.minecraft.world.level.material.Fluid;
//~ import net.minecraft.world.level.material.FluidState;
// @era:fog-end
// @era:registry-begin builtin
import net.minecraft.core.registries.BuiltInRegistries;
// @era:registry-else static
//~ import net.minecraft.core.Registry;
// @era:registry-end

/**
 * Fog and particle hooks behind the scenery options (everything except the sky draws, which are
 * cancelled directly in {@code SkyMixin}). The decisions are pure functions in {@link Scenery}
 * and {@link ParticleFilter}; this class only reads and writes Minecraft's fog state, which
 * changed shape five times between 1.16.5 and 26.x:
 * <ul>
 *   <li>1.16.5: fixed-function {@code RenderSystem.fogStart/End/Density}</li>
 *   <li>1.17 - 1.21.1: shader fog, {@code RenderSystem.get/setShaderFogStart/End}</li>
 *   <li>1.21.2 - 1.21.5: an immutable {@code FogParameters} returned by {@code setupFog}</li>
 *   <li>1.21.6+: a fog uniform with separate environmental and render-distance ranges</li>
 * </ul>
 */
public final class SceneryHooks {
    private static final float[] FOG = new float[4];
    private static final Map<ParticleType<?>, Integer> CATEGORY = new IdentityHashMap<ParticleType<?>, Integer>();

    private SceneryHooks() {
    }

    // ------------------------------------------------------------------ fog

    /** Blindness (and from 1.19 Darkness) own the fog; the settings never lift them. */
    private static boolean blinded(final Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        }
        final LivingEntity living = (LivingEntity) entity;
        // @era:fog-begin shader19|params
        return living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS);
        // @era:fog-else gl|shader17|buffer|data
        //~ return living.hasEffect(MobEffects.BLINDNESS);
        // @era:fog-end
    }

    // @era:fog-begin shader17|shader19|params|buffer|data
    /** The medium at the camera; -1 for powder snow, whose fog is left alone. */
    private static int medium(final Camera camera) {
        final FogType type = camera.getFluidInCamera();
        if (type == FogType.WATER) {
            return Scenery.WATER;
        }
        if (type == FogType.LAVA) {
            return Scenery.LAVA;
        }
        if (type == FogType.POWDER_SNOW) {
            return -1;
        }
        return Scenery.AIR;
    }
    // @era:fog-else gl
    //~ /** {@code Tag.Named} extends {@code Tag}; widened here, as {@code FluidState.is} takes a {@code Tag}. */
    //~ @SuppressWarnings("unchecked")
    //~ private static Tag<Fluid> fluidTag(final Object named) {
        //~ return (Tag<Fluid>) named;
    //~ }

    //~ private static int medium(final Camera camera) {
        //~ final FluidState fluid = camera.getFluidInCamera();
        //~ return fluid.is(fluidTag(FluidTags.WATER)) ? Scenery.WATER
                //~ : fluid.is(fluidTag(FluidTags.LAVA)) ? Scenery.LAVA : Scenery.AIR;
    //~ }

    //~ /** FogRenderer.setupFog TAIL, terrain fog only. */
    //~ public static void adjustFog(final Camera camera, final float farPlane, final boolean thick) {
        //~ if (blinded(camera.getEntity())) {
            //~ return;
        //~ }
        //~ // Far terrain drawn this frame: the terrain fog reaches its edge instead of the render distance.
        //~ if (FarTerrain.drawnThisFrame() && !thick && Scenery.distanceFog && medium(camera) == Scenery.AIR
                //~ && FarTerrain.extendFog()) {
            //~ return;
        //~ }
        //~ if (Scenery.fogVanilla()) {
            //~ return;
        //~ }
        //~ final int medium = medium(camera);
        //~ if (!Scenery.adjustFog(medium, thick, farPlane, FogTracker.start(), FogTracker.end(), FogTracker.density(), FOG)) {
            //~ return;
        //~ }
        //~ if (medium == Scenery.WATER) {
            //~ RenderSystem.fogDensity(FOG[Scenery.DENSITY]);
        //~ } else {
            //~ RenderSystem.fogStart(FOG[Scenery.START]);
            //~ RenderSystem.fogEnd(FOG[Scenery.END]);
        //~ }
    //~ }
    // @era:fog-end

    // @era:fog-begin shader17|shader19
    /** FogRenderer.setupFog TAIL, terrain fog only (vanilla has just set the shader fog). */
    public static void adjustFog(final Camera camera, final float farPlane, final boolean thick) {
        adjustShaderFog(camera, farPlane, thick);
        // What the terrain is drawn with this frame, for the fog cull.
        FogCull.recordFogRange(RenderSystem.getShaderFogStart(), RenderSystem.getShaderFogEnd(),
                RenderSystem.getShaderFogColor()[3]);
    }

    private static void adjustShaderFog(final Camera camera, final float farPlane, final boolean thick) {
        if (Scenery.fogVanilla() || blinded(camera.getEntity())) {
            return;
        }
        final int medium = medium(camera);
        if (medium < 0 || !Scenery.adjustLinearFog(medium, thick, farPlane, RenderSystem.getShaderFogStart(),
                RenderSystem.getShaderFogEnd(), FOG)) {
            return;
        }
        RenderSystem.setShaderFogStart(FOG[Scenery.START]);
        RenderSystem.setShaderFogEnd(FOG[Scenery.END]);
    }
    // @era:fog-else buffer|data
    //~ /**
     //~ * Before FogRenderer writes the fog uniform: the four ranges to write instead, or null to
     //~ * keep vanilla's. Environmental effects (blindness, the Nether) live in their own range,
     //~ * which the distance settings never touch, so no effect check is needed here.
     //~ */
    //~ public static float[] adjustFogBuffer(final float envStart, final float envEnd, final float rdStart,
                                          //~ final float rdEnd) {
        //~ final float[] fog = adjustFogRanges(envStart, envEnd, rdStart, rdEnd);
        //~ if (fog == null) {
            //~ FogCull.recordFogRanges(envStart, envEnd, rdStart, rdEnd);
        //~ } else {
            //~ FogCull.recordFogRanges(fog[0], fog[1], fog[2], fog[3]);
        //~ }
        //~ return fog;
    //~ }

    //~ private static float[] adjustFogRanges(final float envStart, final float envEnd, final float rdStart,
                                           //~ final float rdEnd) {
        //~ if (Scenery.fogVanilla()) {
            //~ return null;
        //~ }
        //~ final Camera camera = ClientHooks.mainCamera(Minecraft.getInstance());
        //~ if (camera == null) {
            //~ return null;
        //~ }
        //~ final int medium = medium(camera);
        //~ if (medium < 0 || !Scenery.adjustFogData(medium, envStart, envEnd, rdStart, rdEnd, FOG)) {
            //~ return null;
        //~ }
        //~ return FOG;
    //~ }
    // @era:fog-else params
    //~ /** FogRenderer.setupFog RETURN, terrain fog only: the parameters to use instead. */
    //~ public static FogParameters adjustFog(final FogParameters fog, final Camera camera, final float renderDistance,
                                          //~ final boolean thick) {
        //~ final FogParameters used = adjustFogParameters(fog, camera, renderDistance, thick);
        //~ if (used != null) {
            //~ FogCull.recordFogRange(used.start(), used.end(), used.alpha());
        //~ }
        //~ return used;
    //~ }

    //~ private static FogParameters adjustFogParameters(final FogParameters fog, final Camera camera,
                                                     //~ final float renderDistance, final boolean thick) {
        //~ if (fog == null || fog == FogParameters.NO_FOG || Scenery.fogVanilla() || blinded(camera.getEntity())) {
            //~ return fog;
        //~ }
        //~ final int medium = medium(camera);
        //~ if (medium < 0 || !Scenery.adjustLinearFog(medium, thick, renderDistance, fog.start(), fog.end(), FOG)) {
            //~ return fog;
        //~ }
        //~ return new FogParameters(FOG[Scenery.START], FOG[Scenery.END], fog.shape(), fog.red(), fog.green(),
                //~ fog.blue(), fog.alpha());
    //~ }
    // @era:fog-else gl
    // @era:fog-end

    // ------------------------------------------------------------------ particles

    /** ParticleEngine.createParticle HEAD: true to drop this particle. */
    public static boolean dropParticle(final ParticleOptions options) {
        if (!ParticleFilter.anyDisabled() || options == null) {
            return false;
        }
        final ParticleType<?> type = options.getType();
        Integer category = CATEGORY.get(type);
        if (category == null) {
            // @era:registry-begin builtin
            final Object id = BuiltInRegistries.PARTICLE_TYPE.getKey(type);
            // @era:registry-else static
            //~ final Object id = Registry.PARTICLE_TYPE.getKey(type);
            // @era:registry-end
            category = Integer.valueOf(ParticleFilter.categoryOf(id == null ? null : id.toString()));
            CATEGORY.put(type, category);
        }
        return !ParticleFilter.allowed(category.intValue());
    }
}
