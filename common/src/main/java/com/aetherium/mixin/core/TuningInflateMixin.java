package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:boost16-begin none
// @era:boost16-else v16
//~ import com.aetherium.perf.Tuning;
//~ import io.netty.buffer.ByteBuf;
//~ import io.netty.channel.ChannelHandlerContext;
//~ import io.netty.handler.codec.DecoderException;
//~ import java.util.List;
//~ import java.util.zip.DataFormatException;
//~ import java.util.zip.Inflater;
//~ import org.spongepowered.asm.mixin.Final;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:boost16-end

/**
 * Faster packet decoding, part 2 (1.17.2, Minecraft 1.16.5): for every compressed packet vanilla
 * copies the compressed bytes into a new array, unpacks them into another new array and wraps
 * that; uncompressed packets are copied into a new buffer. With the option on, the compressed
 * bytes go through one reused array per connection, the packet is unpacked straight into a buffer
 * from Netty's pool (returned to it once the packet has been read), and uncompressed packets are
 * passed on as a view of the received bytes. Same checks, same error messages; server connections
 * only (singleplayer does not compress).
 */
@Mixin(targets = "net.minecraft.network.CompressionDecoder")
public abstract class TuningInflateMixin {
    // @era:boost16-begin none
    // @era:boost16-else v16
    //~ @Shadow
    //~ @Final
    //~ private Inflater inflater;

    //~ @Shadow
    //~ private int threshold;

    //~ /** Compressed bytes of the current packet; kept between packets up to 256 KiB. */
    //~ @Unique
    //~ private byte[] aetherium$input;

    //~ @Inject(method = "decode", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out,
                                  //~ final CallbackInfo ci) throws DataFormatException {
        //~ if (!Tuning.fastNetwork) {
            //~ return;
        //~ }
        //~ ci.cancel();
        //~ if (in.readableBytes() == 0) {
            //~ return;
        //~ }
        //~ final int size = aetherium$readVarInt(in);
        //~ if (size == 0) {
            //~ out.add(in.readRetainedSlice(in.readableBytes()));
            //~ return;
        //~ }
        //~ if (size < this.threshold) {
            //~ throw new DecoderException("Badly compressed packet - size of " + size + " is below server threshold of " + this.threshold);
        //~ }
        //~ if (size > 2097152) {
            //~ throw new DecoderException("Badly compressed packet - size of " + size + " is larger than protocol maximum of " + 2097152);
        //~ }
        //~ final int length = in.readableBytes();
        //~ byte[] input = this.aetherium$input;
        //~ if (input == null || input.length < length) {
            //~ input = new byte[Math.max(length, 8192)];
            //~ if (input.length <= 262144) {
                //~ this.aetherium$input = input;
            //~ }
        //~ }
        //~ in.readBytes(input, 0, length);
        //~ final Inflater inflate = this.inflater;
        //~ inflate.setInput(input, 0, length);
        //~ final ByteBuf output = ctx.alloc().heapBuffer(size, size);
        //~ boolean handedOn = false;
        //~ try {
            //~ final int written;
            //~ if (output.hasArray()) {
                //~ written = inflate.inflate(output.array(), output.arrayOffset(), size);
                //~ output.writerIndex(size);
            //~ } else {
                //~ final byte[] unpacked = new byte[size];
                //~ written = inflate.inflate(unpacked);
                //~ output.writeBytes(unpacked);
            //~ }
            //~ if (written < size) {
                //~ output.setZero(written, size - written); // vanilla's array is zero beyond what was unpacked
            //~ }
            //~ out.add(output);
            //~ handedOn = true;
        //~ } finally {
            //~ if (!handedOn) {
                //~ output.release();
            //~ }
            //~ inflate.reset();
        //~ }
    //~ }

    //~ /** {@code FriendlyByteBuf.readVarInt}, without the wrapper object. */
    //~ @Unique
    //~ private static int aetherium$readVarInt(final ByteBuf in) {
        //~ int value = 0;
        //~ int bytes = 0;
        //~ byte b;
        //~ do {
            //~ b = in.readByte();
            //~ value |= (b & 127) << bytes++ * 7;
            //~ if (bytes > 5) {
                //~ throw new RuntimeException("VarInt too big");
            //~ }
        //~ } while ((b & 128) == 128);
        //~ return value;
    //~ }
    // @era:boost16-end
}
