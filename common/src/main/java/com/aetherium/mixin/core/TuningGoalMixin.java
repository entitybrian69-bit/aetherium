package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:boost16-begin none
// @era:boost16-else v16
//~ import com.aetherium.perf.Tuning;
//~ import java.util.EnumSet;
//~ import java.util.Iterator;
//~ import java.util.Map;
//~ import java.util.Set;
//~ import java.util.function.Supplier;
//~ import net.minecraft.util.profiling.ProfilerFiller;
//~ import net.minecraft.world.entity.ai.goal.Goal;
//~ import net.minecraft.world.entity.ai.goal.WrappedGoal;
//~ import org.spongepowered.asm.mixin.Final;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:boost16-end

/**
 * Faster mob AI (1.17.2, Minecraft 1.16.5): {@code GoalSelector.tick} runs for every mob twice per
 * server tick (goals and targets) and builds several streams, lambdas and iterators each time.
 * This is the same algorithm written as plain loops: the same goals are stopped, started and
 * ticked in the same order, the same flags are locked and released, and the profiler sections
 * keep their names. Only the temporary objects are gone.
 *
 * <p>Vanilla, step by step (each stream is evaluated goal by goal, so a goal sees the flag changes
 * of the goals before it, exactly as in these loops):</p>
 * <ol>
 *   <li>goalCleanup: every running goal that uses a disabled flag or cannot continue is stopped;
 *       then every flag held by a goal that is no longer running is released.</li>
 *   <li>goalUpdate: every goal that is not running, uses no disabled flag, can take over all its
 *       flags and can be used, takes its flags (the previous holder of each is stopped) and
 *       starts.</li>
 *   <li>goalTick: every running goal ticks.</li>
 * </ol>
 */
@Mixin(targets = "net.minecraft.world.entity.ai.goal.GoalSelector")
public abstract class TuningGoalMixin {
    // @era:boost16-begin none
    // @era:boost16-else v16
    //~ @Shadow
    //~ @Final
    //~ private static WrappedGoal NO_GOAL;

    //~ @Shadow
    //~ @Final
    //~ private Map<Goal.Flag, WrappedGoal> lockedFlags;

    //~ @Shadow
    //~ @Final
    //~ private Set<WrappedGoal> availableGoals;

    //~ @Shadow
    //~ @Final
    //~ private Supplier<ProfilerFiller> profiler;

    //~ @Shadow
    //~ @Final
    //~ private EnumSet<Goal.Flag> disabledFlags;

    //~ @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$tick(final CallbackInfo ci) {
        //~ if (!Tuning.fastMobAi) {
            //~ return;
        //~ }
        //~ ci.cancel();
        //~ final ProfilerFiller profilerFiller = this.profiler.get();
        //~ final Map<Goal.Flag, WrappedGoal> locked = this.lockedFlags;
        //~ final EnumSet<Goal.Flag> disabled = this.disabledFlags;
        //~ final WrappedGoal none = NO_GOAL;

        //~ profilerFiller.push("goalCleanup");
        //~ for (final WrappedGoal goal : this.availableGoals) {
            //~ if (goal.isRunning() && (aetherium$usesAny(goal.getFlags(), disabled) || !goal.canContinueToUse())) {
                //~ goal.stop();
            //~ }
        //~ }
        //~ final Iterator<Map.Entry<Goal.Flag, WrappedGoal>> held = locked.entrySet().iterator();
        //~ while (held.hasNext()) {
            //~ if (!held.next().getValue().isRunning()) {
                //~ held.remove();
            //~ }
        //~ }
        //~ profilerFiller.pop();

        //~ profilerFiller.push("goalUpdate");
        //~ for (final WrappedGoal goal : this.availableGoals) {
            //~ if (goal.isRunning()) {
                //~ continue;
            //~ }
            //~ final EnumSet<Goal.Flag> flags = goal.getFlags();
            //~ if (aetherium$usesAny(flags, disabled)) {
                //~ continue;
            //~ }
            //~ boolean replaceable = true;
            //~ for (final Goal.Flag flag : flags) {
                //~ if (!locked.getOrDefault(flag, none).canBeReplacedBy(goal)) {
                    //~ replaceable = false;
                    //~ break;
                //~ }
            //~ }
            //~ if (!replaceable || !goal.canUse()) {
                //~ continue;
            //~ }
            //~ for (final Goal.Flag flag : flags) {
                //~ locked.getOrDefault(flag, none).stop();
                //~ locked.put(flag, goal);
            //~ }
            //~ goal.start();
        //~ }
        //~ profilerFiller.pop();

        //~ profilerFiller.push("goalTick");
        //~ for (final WrappedGoal goal : this.availableGoals) {
            //~ if (goal.isRunning()) {
                //~ goal.tick();
            //~ }
        //~ }
        //~ profilerFiller.pop();
    //~ }

    //~ @org.spongepowered.asm.mixin.Unique
    //~ private static boolean aetherium$usesAny(final EnumSet<Goal.Flag> flags, final EnumSet<Goal.Flag> disabled) {
        //~ if (disabled.isEmpty() || flags.isEmpty()) {
            //~ return false;
        //~ }
        //~ for (final Goal.Flag flag : flags) {
            //~ if (disabled.contains(flag)) {
                //~ return true;
            //~ }
        //~ }
        //~ return false;
    //~ }
    // @era:boost16-end
}
