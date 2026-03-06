package com.arknightsendfieldpingandserverpacketratedisplay.mixin.client;  // 修正为 client 子包

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicLong;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {
    @Unique
    private static final AtomicLong totalPackets = new AtomicLong(0);

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelHandlerContext;ZLio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"))
    private void onSendPacket(Packet<?> packet, ChannelHandlerContext context, boolean flush, io.netty.channel.ChannelFutureListener listener, CallbackInfo ci) {
        totalPackets.incrementAndGet();
    }

    @Inject(method = "channelRead(Lio/netty/channel/ChannelHandlerContext;Ljava/lang/Object;)V", at = @At("HEAD"))
    private void onChannelRead(ChannelHandlerContext context, Object msg, CallbackInfo ci) {
        if (msg instanceof Packet) {
            totalPackets.incrementAndGet();
        }
    }

    public static long getTotalPackets() {
        return totalPackets.get();
    }
}