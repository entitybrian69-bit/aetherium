package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-chunk-save-begin c21|c214
import com.aetherium.perf.Tuning;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.util.ArrayDeque;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ChunkHolder;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:t-chunk-save-else none
// @era:t-chunk-save-else v16
//~ import com.aetherium.perf.Tuning;
//~ import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
//~ import java.util.ArrayDeque;
//~ import java.util.function.BooleanSupplier;
//~ import net.minecraft.server.level.ChunkHolder;
//~ import net.minecraft.world.level.chunk.ChunkAccess;
//~ import net.minecraft.world.level.chunk.ImposterProtoChunk;
//~ import net.minecraft.world.level.chunk.LevelChunk;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:t-chunk-save-else c26
//~ import com.aetherium.perf.Tuning;
//~ import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
//~ import java.util.ArrayDeque;
//~ import java.util.function.BooleanSupplier;
//~ import net.minecraft.server.level.ChunkHolder;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:t-chunk-save-end

/**
 * Smooth autosave (1.10.0, Minecraft 1.16.5, single player): the periodic autosave serializes
 * every changed chunk in one tick (hundreds of chunks, a freeze of up to a second on a phone).
 * During that autosave the chunks are queued instead and saved up to 16 per tick, exactly the way
 * vanilla would have saved them. Any other save (pause menu, quitting, /save-all) is untouched
 * and also writes whatever is still queued, so nothing is ever lost.
 */
@Mixin(targets = "net.minecraft.server.level.ChunkMap")
public abstract class TuningChunkSaveMixin {
    // @era:t-chunk-save-begin c21
    @Unique
    private static final int AETHERIUM$PER_TICK = 16;
    @Unique
    private static final int AETHERIUM$MIN_PER_TICK = 2;

    @Shadow
    private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> visibleChunkMap;

    @Unique
    private ArrayDeque<ChunkHolder> aetherium$saveQueue;

    @Shadow
    private boolean saveChunkIfNeeded(final ChunkHolder holder) {
        throw new AssertionError();
    }

    @SuppressWarnings("unchecked")
    @Inject(method = "saveAllChunks(Z)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$queueAutosave(final boolean flush, final CallbackInfo ci) {
        final ArrayDeque<ChunkHolder> queue = this.aetherium$saveQueue;
        if (flush || !Tuning.autosaving || !Tuning.smoothAutosave) {
            if (queue != null) {
                queue.clear(); // vanilla saves everything that changed, the queued chunks included
            }
            return;
        }
        ArrayDeque<ChunkHolder> q = queue;
        if (q == null) {
            q = new ArrayDeque<ChunkHolder>();
            this.aetherium$saveQueue = q;
        }
        q.clear();
        // values() is a fastutil ObjectCollection (Iterable); cast for the signature stubs.
        for (final ChunkHolder holder : (Iterable<ChunkHolder>) (Object) this.visibleChunkMap.values()) {
            if (holder.wasAccessibleSinceLastSave()) {
                q.add(holder);
            }
        }
        ci.cancel();
    }

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At("TAIL"), require = 0)
    private void aetherium$drain(final BooleanSupplier hasTime, final CallbackInfo ci) {
        final ArrayDeque<ChunkHolder> queue = this.aetherium$saveQueue;
        if (queue == null || queue.isEmpty()) {
            return;
        }
        int saved = 0;
        while (saved < AETHERIUM$PER_TICK && !queue.isEmpty()
                && (saved < AETHERIUM$MIN_PER_TICK || hasTime.getAsBoolean())) {
            final ChunkHolder holder = queue.poll();
            if (this.visibleChunkMap.get(holder.getPos().toLong()) != holder) {
                continue; // unloaded meanwhile: the unload already saved it
            }
            if (this.saveChunkIfNeeded(holder)) { // vanilla's own per-chunk autosave step
                saved++;
            }
        }
    }
    // @era:t-chunk-save-else none
    // @era:t-chunk-save-else v16
    //~ @Unique
    //~ private static final int AETHERIUM$PER_TICK = 16;
    //~ @Unique
    //~ private static final int AETHERIUM$MIN_PER_TICK = 2;

    //~ @Shadow
    //~ private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> visibleChunkMap;

    //~ @Unique
    //~ private ArrayDeque<ChunkHolder> aetherium$saveQueue;

    //~ @Shadow
    //~ private boolean save(final ChunkAccess chunk) {
        //~ throw new AssertionError();
    //~ }

    //~ @SuppressWarnings("unchecked")
    //~ @Inject(method = "saveAllChunks(Z)V", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$queueAutosave(final boolean flush, final CallbackInfo ci) {
        //~ final ArrayDeque<ChunkHolder> queue = this.aetherium$saveQueue;
        //~ if (flush || !Tuning.autosaving || !Tuning.smoothAutosave) {
            //~ if (queue != null) {
                //~ queue.clear(); // vanilla saves everything that changed, the queued chunks included
            //~ }
            //~ return;
        //~ }
        //~ ArrayDeque<ChunkHolder> q = queue;
        //~ if (q == null) {
            //~ q = new ArrayDeque<ChunkHolder>();
            //~ this.aetherium$saveQueue = q;
        //~ }
        //~ q.clear();
        //~ // values() is a fastutil ObjectCollection (Iterable); cast for the signature stubs.
        //~ for (final ChunkHolder holder : (Iterable<ChunkHolder>) (Object) this.visibleChunkMap.values()) {
            //~ if (holder.wasAccessibleSinceLastSave()) {
                //~ q.add(holder);
            //~ }
        //~ }
        //~ ci.cancel();
    //~ }

    //~ @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At("TAIL"), require = 0)
    //~ private void aetherium$drain(final BooleanSupplier hasTime, final CallbackInfo ci) {
        //~ final ArrayDeque<ChunkHolder> queue = this.aetherium$saveQueue;
        //~ if (queue == null || queue.isEmpty()) {
            //~ return;
        //~ }
        //~ int saved = 0;
        //~ while (saved < AETHERIUM$PER_TICK && !queue.isEmpty()
                //~ && (saved < AETHERIUM$MIN_PER_TICK || hasTime.getAsBoolean())) {
            //~ final ChunkHolder holder = queue.poll();
            //~ if (this.visibleChunkMap.get(holder.getPos().toLong()) != holder) {
                //~ continue; // unloaded meanwhile: the unload already saved it
            //~ }
            //~ final ChunkAccess chunk = holder.getChunkToSave().getNow(null);
            //~ if (chunk instanceof ImposterProtoChunk || chunk instanceof LevelChunk) {
                //~ this.save(chunk);
                //~ holder.refreshAccessibility();
                //~ saved++;
            //~ }
        //~ }
    //~ }
    // @era:t-chunk-save-else c214
    //~ @Unique
    //~ private static final int AETHERIUM$PER_TICK = 16;
    //~ @Unique
    //~ private static final int AETHERIUM$MIN_PER_TICK = 2;

    //~ @Shadow
    //~ private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> visibleChunkMap;

    //~ @Unique
    //~ private ArrayDeque<ChunkHolder> aetherium$saveQueue;

    //~ @Shadow
    //~ private boolean saveChunkIfNeeded(final ChunkHolder holder, final long now) {
        //~ throw new AssertionError();
    //~ }

    //~ @SuppressWarnings("unchecked")
    //~ @Inject(method = "saveAllChunks(Z)V", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$queueAutosave(final boolean flush, final CallbackInfo ci) {
        //~ final ArrayDeque<ChunkHolder> queue = this.aetherium$saveQueue;
        //~ if (flush || !Tuning.autosaving || !Tuning.smoothAutosave) {
            //~ if (queue != null) {
                //~ queue.clear(); // vanilla saves everything that changed, the queued chunks included
            //~ }
            //~ return;
        //~ }
        //~ ArrayDeque<ChunkHolder> q = queue;
        //~ if (q == null) {
            //~ q = new ArrayDeque<ChunkHolder>();
            //~ this.aetherium$saveQueue = q;
        //~ }
        //~ q.clear();
        //~ // values() is a fastutil ObjectCollection (Iterable); cast for the signature stubs.
        //~ for (final ChunkHolder holder : (Iterable<ChunkHolder>) (Object) this.visibleChunkMap.values()) {
            //~ if (holder.wasAccessibleSinceLastSave()) {
                //~ q.add(holder);
            //~ }
        //~ }
        //~ ci.cancel();
    //~ }

    //~ @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At("TAIL"), require = 0)
    //~ private void aetherium$drain(final BooleanSupplier hasTime, final CallbackInfo ci) {
        //~ final ArrayDeque<ChunkHolder> queue = this.aetherium$saveQueue;
        //~ if (queue == null || queue.isEmpty()) {
            //~ return;
        //~ }
        //~ int saved = 0;
        //~ while (saved < AETHERIUM$PER_TICK && !queue.isEmpty()
                //~ && (saved < AETHERIUM$MIN_PER_TICK || hasTime.getAsBoolean())) {
            //~ final ChunkHolder holder = queue.poll();
            //~ if (this.visibleChunkMap.get(holder.getPos().toLong()) != holder) {
                //~ continue; // unloaded meanwhile: the unload already saved it
            //~ }
            //~ if (this.saveChunkIfNeeded(holder, System.nanoTime() / 1_000_000L)) { // Util.getMillis()
                //~ saved++;
            //~ }
        //~ }
    //~ }
    // @era:t-chunk-save-else c26
    //~ @Unique
    //~ private static final int AETHERIUM$PER_TICK = 16;
    //~ @Unique
    //~ private static final int AETHERIUM$MIN_PER_TICK = 2;

    //~ @Shadow
    //~ private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> visibleChunkMap;

    //~ @Unique
    //~ private ArrayDeque<ChunkHolder> aetherium$saveQueue;

    //~ @Shadow
    //~ private boolean saveChunkIfNeeded(final ChunkHolder holder, final long now) {
        //~ throw new AssertionError();
    //~ }

    //~ @SuppressWarnings("unchecked")
    //~ @Inject(method = "saveAllChunks(Z)V", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$queueAutosave(final boolean flush, final CallbackInfo ci) {
        //~ final ArrayDeque<ChunkHolder> queue = this.aetherium$saveQueue;
        //~ if (flush || !Tuning.autosaving || !Tuning.smoothAutosave) {
            //~ if (queue != null) {
                //~ queue.clear(); // vanilla saves everything that changed, the queued chunks included
            //~ }
            //~ return;
        //~ }
        //~ ArrayDeque<ChunkHolder> q = queue;
        //~ if (q == null) {
            //~ q = new ArrayDeque<ChunkHolder>();
            //~ this.aetherium$saveQueue = q;
        //~ }
        //~ q.clear();
        //~ // values() is a fastutil ObjectCollection (Iterable); cast for the signature stubs.
        //~ for (final ChunkHolder holder : (Iterable<ChunkHolder>) (Object) this.visibleChunkMap.values()) {
            //~ if (holder.wasAccessibleSinceLastSave()) {
                //~ q.add(holder);
            //~ }
        //~ }
        //~ ci.cancel();
    //~ }

    //~ @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At("TAIL"), require = 0)
    //~ private void aetherium$drain(final BooleanSupplier hasTime, final CallbackInfo ci) {
        //~ final ArrayDeque<ChunkHolder> queue = this.aetherium$saveQueue;
        //~ if (queue == null || queue.isEmpty()) {
            //~ return;
        //~ }
        //~ int saved = 0;
        //~ while (saved < AETHERIUM$PER_TICK && !queue.isEmpty()
                //~ && (saved < AETHERIUM$MIN_PER_TICK || hasTime.getAsBoolean())) {
            //~ final ChunkHolder holder = queue.poll();
            //~ if (this.visibleChunkMap.get(holder.getPos().pack()) != holder) {
                //~ continue; // unloaded meanwhile: the unload already saved it
            //~ }
            //~ if (this.saveChunkIfNeeded(holder, System.nanoTime() / 1_000_000L)) { // Util.getMillis()
                //~ saved++;
            //~ }
        //~ }
    //~ }
    // @era:t-chunk-save-end
}
