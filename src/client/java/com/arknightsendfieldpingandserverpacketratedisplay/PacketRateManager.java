package com.arknightsendfieldpingandserverpacketratedisplay;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 管理 Ping 和包速率的统计，使用基于时间采样（不依赖 tick）。
 */
public class PacketRateManager {
    private static PacketRateManager instance;

    // 采样间隔（毫秒）
    private static final long SAMPLE_INTERVAL_MS = 500;
    // 最近一分钟样本数
    private static final int MAX_SAMPLES = 120; // 60秒 / 0.5秒 = 120

    private static class Sample {
        long time;
        int ping;
        double packetsPerSec; // 瞬时速率
    }

    private final Deque<Sample> samples = new ArrayDeque<>(MAX_SAMPLES + 1);
    private long lastSampleTime = 0;
    private long lastPackets = 0; // 上次采样时的总包数（由 Mixin 更新）

    // 累计统计
    private long totalPingSum = 0;
    private int totalPingCount = 0;
    private int maxPingTotal = 0;
    private int minPingTotal = Integer.MAX_VALUE;

    private double totalPacketsSum = 0;
    private int totalPacketsCount = 0;
    private double maxPacketsTotal = 0;
    private double minPacketsTotal = Double.MAX_VALUE;

    private int currentPing = 0;
    private double currentPacketsPerSec = 0;
    private boolean connected = false;

    private PacketRateManager() {}

    public static void initialize() {
        instance = new PacketRateManager();
        instance.registerEvents();
    }

    public static PacketRateManager getInstance() {
        return instance;
    }

    private void registerEvents() {
        // 加入服务器时重置
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            reset();
            connected = true;
        });

        // 断开连接时清除
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            connected = false;
            samples.clear();
        });

        // 每 tick 检查时间，按固定间隔采样（不使用 tick 计数，使用真实时间）
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!connected || client.world == null || client.player == null) return;

            long now = System.currentTimeMillis();
            if (now - lastSampleTime >= SAMPLE_INTERVAL_MS) {
                sample(client, now);
                lastSampleTime = now;
            }
        });
    }

    private void sample(MinecraftClient client, long now) {
        // 获取当前 Ping
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        currentPing = (entry != null) ? entry.getLatency() : 0;

        // 获取当前总包数（由 Mixin 更新）
        long packetsNow = ClientConnectionMixin.getTotalPackets();
        if (packetsNow < 0) return; // 可能尚未初始化

        // 计算包速率 (包/秒)
        double packetsPerSec = 0;
        if (lastPackets != 0) {
            long timeDiff = now - lastSampleTime; // 实际可能略小于 SAMPLE_INTERVAL_MS，但差值很小，直接使用 SAMPLE_INTERVAL_MS 也可
            packetsPerSec = (packetsNow - lastPackets) * 1000.0 / timeDiff;
        }
        lastPackets = packetsNow;
        currentPacketsPerSec = packetsPerSec;

        // 创建样本
        Sample sample = new Sample();
        sample.time = now;
        sample.ping = currentPing;
        sample.packetsPerSec = packetsPerSec;
        samples.add(sample);

        // 移除超出1分钟的样本
        while (samples.size() > MAX_SAMPLES) {
            samples.poll();
        }

        // 更新累计统计
        totalPingSum += currentPing;
        totalPingCount++;
        maxPingTotal = Math.max(maxPingTotal, currentPing);
        if (currentPing > 0) minPingTotal = Math.min(minPingTotal, currentPing);

        totalPacketsSum += packetsPerSec;
        totalPacketsCount++;
        maxPacketsTotal = Math.max(maxPacketsTotal, packetsPerSec);
        minPacketsTotal = Math.min(minPacketsTotal, packetsPerSec);
    }

    private void reset() {
        samples.clear();
        lastPackets = 0;
        lastSampleTime = 0;
        totalPingSum = 0;
        totalPingCount = 0;
        maxPingTotal = 0;
        minPingTotal = Integer.MAX_VALUE;
        totalPacketsSum = 0;
        totalPacketsCount = 0;
        maxPacketsTotal = 0;
        minPacketsTotal = Double.MAX_VALUE;
        currentPing = 0;
        currentPacketsPerSec = 0;
    }

    // ---------- 公共获取方法 ----------
    public int getCurrentPing() { return currentPing; }
    public double getCurrentPacketsPerSec() { return currentPacketsPerSec; }

    // 最近一分钟统计
    public int getMinPingMinute() {
        return samples.stream().mapToInt(s -> s.ping).min().orElse(0);
    }
    public int getMaxPingMinute() {
        return samples.stream().mapToInt(s -> s.ping).max().orElse(0);
    }
    public double getAvgPingMinute() {
        return samples.stream().mapToInt(s -> s.ping).average().orElse(0);
    }
    public double getMinPacketsMinute() {
        return samples.stream().mapToDouble(s -> s.packetsPerSec).min().orElse(0);
    }
    public double getMaxPacketsMinute() {
        return samples.stream().mapToDouble(s -> s.packetsPerSec).max().orElse(0);
    }
    public double getAvgPacketsMinute() {
        return samples.stream().mapToDouble(s -> s.packetsPerSec).average().orElse(0);
    }

    // 累计统计
    public int getMinPingTotal() { return minPingTotal == Integer.MAX_VALUE ? 0 : minPingTotal; }
    public int getMaxPingTotal() { return maxPingTotal; }
    public double getAvgPingTotal() {
        return totalPingCount == 0 ? 0 : (double) totalPingSum / totalPingCount;
    }
    public double getMinPacketsTotal() { return minPacketsTotal == Double.MAX_VALUE ? 0 : minPacketsTotal; }
    public double getMaxPacketsTotal() { return maxPacketsTotal; }
    public double getAvgPacketsTotal() {
        return totalPacketsCount == 0 ? 0 : totalPacketsSum / totalPacketsCount;
    }
}