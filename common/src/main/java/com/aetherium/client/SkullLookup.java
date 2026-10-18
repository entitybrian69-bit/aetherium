package com.aetherium.client;

// @era:t-skull-lookup-begin none
// @era:t-skull-lookup-else v16
//~ import com.aetherium.util.AetheriumLog;
//~ import com.mojang.authlib.GameProfile;
//~ import com.mojang.authlib.minecraft.MinecraftSessionService;
//~ import java.util.concurrent.ExecutorService;
//~ import java.util.concurrent.Executors;
//~ import java.util.concurrent.ThreadFactory;
//~ import net.minecraft.client.Minecraft;
// @era:t-skull-lookup-end

/**
 * Async player-head skins (1.10.0, Minecraft 1.16.5). Vanilla fetches the skin of a player head
 * from Mojang's session server synchronously, on whatever thread loads the head, which on the
 * client is the render thread: every new head in view froze the game for a network round trip
 * (often a second or more on mobile data). Here the request runs on one background thread and the
 * head picks the skin up when it arrives (it shows the default skin until then).
 *
 * <p>Only the session-server request moves: the name lookup through the profile cache (not thread
 * safe) still runs in vanilla code, so heads that only carry a name stay on the vanilla path.</p>
 */
public final class SkullLookup {
    private SkullLookup() {
    }

    // @era:t-skull-lookup-begin none
    // @era:t-skull-lookup-else v16
    //~ private static final AetheriumLog LOG = AetheriumLog.of(SkullLookup.class);
    //~ private static ExecutorService executor;

    //~ private static synchronized ExecutorService executor() {
        //~ if (executor == null) {
            //~ executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
                //~ @Override
                //~ public Thread newThread(final Runnable task) {
                    //~ final Thread thread = new Thread(task, "Aetherium skull skins");
                    //~ thread.setDaemon(true);
                    //~ thread.setPriority(Thread.MIN_PRIORITY);
                    //~ return thread;
                //~ }
            //~ });
        //~ }
        //~ return executor;
    //~ }

    //~ /** True on the client render thread (the only place the async path is used). */
    //~ public static boolean onClientThread() {
        //~ final Minecraft mc = Minecraft.getInstance();
        //~ return mc != null && mc.isSameThread();
    //~ }

    //~ /** Starts the skin request; the result is delivered to {@code head} on the client thread. */
    //~ public static void fill(final SkullOwnerAccess head, final GameProfile profile,
                            //~ final MinecraftSessionService service) {
        //~ executor().execute(new Runnable() {
            //~ @Override
            //~ public void run() {
                //~ final GameProfile filled;
                //~ try {
                    //~ filled = service.fillProfileProperties(profile, true);
                //~ } catch (RuntimeException e) {
                    //~ LOG.dev("Skull skin lookup failed: {}", e.toString());
                    //~ return;
                //~ }
                //~ if (filled == null) {
                    //~ return;
                //~ }
                //~ Minecraft.getInstance().execute(new Runnable() {
                    //~ @Override
                    //~ public void run() {
                        //~ head.aetherium$profileFilled(profile, filled);
                    //~ }
                //~ });
            //~ }
        //~ });
    //~ }
    // @era:t-skull-lookup-end
}
