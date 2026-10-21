package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:boost16-begin none
// @era:boost16-else v16
//~ import com.aetherium.perf.NoLock;
//~ import com.aetherium.perf.Tuning;
//~ import java.util.Map;
//~ import java.util.concurrent.locks.ReadWriteLock;
//~ import net.minecraft.network.syncher.EntityDataAccessor;
//~ import net.minecraft.network.syncher.SynchedEntityData;
//~ import net.minecraft.world.entity.Entity;
//~ import org.spongepowered.asm.mixin.Final;
//~ import org.spongepowered.asm.mixin.Mutable;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:boost16-end

/**
 * Lock-free entity data (1.17.2, Minecraft 1.16.5): every read of an entity's synced values
 * (health, pose, shared flags; dozens per entity per tick, on the client and the built-in server)
 * takes a read lock and looks the value up in a hash map with a boxed key. Only the thread that
 * ticks the entity touches its data, so entities created while the option is on get a lock that
 * does nothing, and the values are also kept in a plain array indexed by their id, which the read
 * path uses (the map stays complete and is still used by everything else).
 */
@Mixin(targets = "net.minecraft.network.syncher.SynchedEntityData")
public abstract class TuningEntityDataMixin {
    // @era:boost16-begin none
    // @era:boost16-else v16
    //~ @Shadow
    //~ @Final
    //~ @Mutable
    //~ private ReadWriteLock lock;

    //~ @Shadow
    //~ @Final
    //~ private Map<Integer, SynchedEntityData.DataItem<?>> itemsById;

    //~ /** Items by id; replaced (never changed in place) when an item is defined. */
    //~ @Unique
    //~ private SynchedEntityData.DataItem<?>[] aetherium$byId;

    //~ @Inject(method = "<init>", at = @At("TAIL"), require = 0)
    //~ private void aetherium$noLock(final Entity entity, final CallbackInfo ci) {
        //~ if (Tuning.fastEntityData) {
            //~ this.lock = NoLock.INSTANCE;
        //~ }
    //~ }

    //~ @Inject(method = "createDataItem", at = @At("TAIL"), require = 0)
    //~ private void aetherium$index(final EntityDataAccessor<?> accessor, final Object value, final CallbackInfo ci) {
        //~ final int id = accessor.getId();
        //~ final SynchedEntityData.DataItem<?> item = this.itemsById.get(Integer.valueOf(id));
        //~ if (id < 0 || id > 255 || item == null) {
            //~ return;
        //~ }
        //~ final SynchedEntityData.DataItem<?>[] old = this.aetherium$byId;
        //~ final SynchedEntityData.DataItem<?>[] grown = new SynchedEntityData.DataItem<?>[Math.max(id + 1, old == null ? 0 : old.length)];
        //~ if (old != null) {
            //~ System.arraycopy(old, 0, grown, 0, old.length);
        //~ }
        //~ grown[id] = item;
        //~ this.aetherium$byId = grown;
    //~ }

    //~ @Redirect(method = "getItem", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"),
            //~ require = 0)
    //~ private Object aetherium$lookup(final Map<Integer, ?> map, final Object key) {
        //~ final SynchedEntityData.DataItem<?>[] byId = this.aetherium$byId;
        //~ if (Tuning.fastEntityData && byId != null && key instanceof Integer) {
            //~ final int id = ((Integer) key).intValue();
            //~ if (id >= 0 && id < byId.length) {
                //~ final Object item = byId[id];
                //~ if (item != null) {
                    //~ return item;
                //~ }
            //~ }
        //~ }
        //~ return map.get(key);
    //~ }
    // @era:boost16-end
}
