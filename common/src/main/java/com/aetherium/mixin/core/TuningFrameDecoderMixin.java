package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:boost16-begin none
// @era:boost16-else v16
//~ import com.aetherium.perf.Tuning;
//~ import io.netty.buffer.ByteBuf;
//~ import io.netty.channel.ChannelHandlerContext;
//~ import io.netty.handler.codec.CorruptedFrameException;
//~ import java.util.List;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:boost16-end

/**
 * Faster packet decoding, part 1 (1.17.2, Minecraft 1.16.5): vanilla splits the incoming stream
 * into packets by copying each length prefix into a new 3-byte array, wrapping that in a new
 * buffer to read it, and then copying the whole packet into another new buffer. This reads the
 * length straight from the stream and hands the packet on as a reference-counted view of the
 * received bytes (the standard Netty way to cut frames; Netty copies the receive buffer itself
 * when a view is still in use). Same packets, same "wait for more bytes" behaviour, same error
 * for an over-long length prefix. Only used for server connections; singleplayer does not go
 * through this decoder.
 */
@Mixin(targets = "net.minecraft.network.Varint21FrameDecoder")
public abstract class TuningFrameDecoderMixin {
    // @era:boost16-begin none
    // @era:boost16-else v16
    //~ @Inject(method = "decode", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out,
                                  //~ final CallbackInfo ci) {
        //~ if (!Tuning.fastNetwork) {
            //~ return;
        //~ }
        //~ ci.cancel();
        //~ in.markReaderIndex();
        //~ int length = 0;
        //~ for (int i = 0; i < 3; i++) {
            //~ if (!in.isReadable()) {
                //~ in.resetReaderIndex();
                //~ return;
            //~ }
            //~ final byte b = in.readByte();
            //~ length |= (b & 0x7F) << (7 * i);
            //~ if (b >= 0) {
                //~ if (in.readableBytes() < length) {
                    //~ in.resetReaderIndex();
                    //~ return;
                //~ }
                //~ out.add(in.readRetainedSlice(length));
                //~ return;
            //~ }
        //~ }
        //~ throw new CorruptedFrameException("length wider than 21-bit");
    //~ }
    // @era:boost16-end
}
