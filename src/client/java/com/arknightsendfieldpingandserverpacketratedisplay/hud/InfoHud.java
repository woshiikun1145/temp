package com.arknightsendfieldpingandserverpacketratedisplay.hud;

import com.arknightsendfieldpingandserverpacketratedisplay.PacketRateManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.text.DecimalFormat;

/**
 * HUD 渲染类，支持两种显示模式（简单/详细），通过 Alt+F6 切换。
 * 快捷键 Ctrl+O 复制 Ping 信息，Ctrl+T 复制全部信息。
 */
public class InfoHud implements HudRenderCallback {
    private static InfoHud instance;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.#");

    // 显示模式：false = 简单模式（仅Ping），true = 详细模式（完整信息）
    private boolean detailedMode = false;

    // 按键绑定
    private static final KeyBinding TOGGLE_KEY = new KeyBinding(
            "key.arknights-endfield-ping.toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F6,
            "category.arknights-endfield-ping"
    );
    private static final KeyBinding COPY_PING_KEY = new KeyBinding(
            "key.arknights-endfield-ping.copy_ping",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "category.arknights-endfield-ping"
    );
    private static final KeyBinding COPY_ALL_KEY = new KeyBinding(
            "key.arknights-endfield-ping.copy_all",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_T,
            "category.arknights-endfield-ping"
    );

    public static void initialize() {
        instance = new InfoHud();
        HudRenderCallback.EVENT.register(instance);

        // 注册按键
        KeyBindingHelper.registerKeyBinding(TOGGLE_KEY);
        KeyBindingHelper.registerKeyBinding(COPY_PING_KEY);
        KeyBindingHelper.registerKeyBinding(COPY_ALL_KEY);

        // 监听按键
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_KEY.wasPressed()) {
                instance.detailedMode = !instance.detailedMode;
            }
            while (COPY_PING_KEY.wasPressed()) {
                if (GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
                        GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS) {
                    instance.copyPingInfo();
                }
            }
            while (COPY_ALL_KEY.wasPressed()) {
                if (GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
                        GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS) {
                    instance.copyAllInfo();
                }
            }
        });
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        PacketRateManager stats = PacketRateManager.getInstance();

        if (detailedMode) {
            renderDetailed(context, client, stats);
        } else {
            renderSimple(context, client, stats);
        }
    }

    /** 简单模式：只显示 Ping */
    private void renderSimple(DrawContext context, MinecraftClient client, PacketRateManager stats) {
        int ping = stats.getCurrentPing();
        String text = "Ping: " + ping + "ms";
        int color = getPingColor(ping);
        drawText(context, client, text, color);
    }

    /** 详细模式：显示完整信息 */
    private void renderDetailed(DrawContext context, MinecraftClient client, PacketRateManager stats) {
        String[] lines = {
                "Ping: Now " + formatPing(stats.getCurrentPing()),
                "         In the last minute Max " + formatPing(stats.getMaxPingMinute()) + " Min " + formatPing(stats.getMinPingMinute()) + " Avg " + formatPing((int) stats.getAvgPingMinute()),
                "         Since I joined the server Max " + formatPing(stats.getMaxPingTotal()) + " Min " + formatPing(stats.getMinPingTotal()) + " Avg " + formatPing((int) stats.getAvgPingTotal()),
                "Server packet rate: Now " + formatRate(stats.getCurrentPacketsPerSec()) + " Packet per sec",
                "         In the last minute: Max " + formatRate(stats.getMaxPacketsMinute()) + " Packet per sec Min " + formatRate(stats.getMinPacketsMinute()) + " Packet per sec Avg " + formatRate(stats.getAvgPacketsMinute()) + " Packet per sec",
                "         Since I joined the server: Max " + formatRate(stats.getMaxPacketsTotal()) + " Packet per sec Min " + formatRate(stats.getMinPacketsTotal()) + " Packet per sec Avg " + formatRate(stats.getAvgPacketsTotal()) + " Packet per sec"
        };

        int y = 10;
        for (String line : lines) {
            drawText(context, client, line, 0xFFFFFF);
            y += 10;
        }
    }

    /** 辅助绘制方法：始终左下角，带阴影 */
    private void drawText(DrawContext context, MinecraftClient client, String text, int color) {
        int x = 10; // 左边距
        int y = client.getWindow().getScaledHeight() - 10 - client.textRenderer.fontHeight; // 底部
        context.drawTextWithShadow(client.textRenderer, Text.literal(text), x, y, color);
    }

    /** 格式化 Ping 并应用颜色（用于详细模式中的数字部分，但这里只返回带颜色的字符串？简化处理：整个行用白色，数字颜色在 format 中通过添加颜色代码实现？但 drawText 不支持颜色代码。为了保持简单，我们在详细模式中所有文本统一用白色，数字的颜色在 format 中无法单独控制。若需数字变色，需分段渲染，这会使代码复杂。按需求优先级，先实现基本功能。 */
    private String formatPing(int ping) {
        return ping + "ms";
    }

    private String formatRate(double rate) {
        return DECIMAL_FORMAT.format(rate);
    }

    /** 获取 Ping 的颜色（用于简单模式） */
    private int getPingColor(int ping) {
        if (ping >= 1000) return 0x8B0000; // 深红
        if (ping >= 300) return 0xFF0000;  // 红
        if (ping >= 100) return 0xFFFF00;  // 黄
        return 0xFFFFFF;                    // 白
    }

    /** 复制 Ping 信息（Ctrl+O） */
    private void copyPingInfo() {
        PacketRateManager stats = PacketRateManager.getInstance();
        String content = String.format(
                "Ping: Now %dms\n         In the last minute Max %dms Min %dms Avg %.1fms\n         Since I joined the server Max %dms Min %dms Avg %.1fms",
                stats.getCurrentPing(),
                stats.getMaxPingMinute(), stats.getMinPingMinute(), stats.getAvgPingMinute(),
                stats.getMaxPingTotal(), stats.getMinPingTotal(), stats.getAvgPingTotal()
        );
        MinecraftClient.getInstance().keyboard.setClipboard(content);
    }

    /** 复制全部信息（Ctrl+T） */
    private void copyAllInfo() {
        PacketRateManager stats = PacketRateManager.getInstance();
        String content = String.format(
                "Ping: Now %dms\n         In the last minute Max %dms Min %dms Avg %.1fms\n         Since I joined the server Max %dms Min %dms Avg %.1fms\nServer packet rate: Now %.1f Packet per sec\n         In the last minute: Max %.1f Packet per sec Min %.1f Packet per sec Avg %.1f Packet per sec\n         Since I joined the server: Max %.1f Packet per sec Min %.1f Packet per sec Avg %.1f Packet per sec",
                stats.getCurrentPing(),
                stats.getMaxPingMinute(), stats.getMinPingMinute(), stats.getAvgPingMinute(),
                stats.getMaxPingTotal(), stats.getMinPingTotal(), stats.getAvgPingTotal(),
                stats.getCurrentPacketsPerSec(),
                stats.getMaxPacketsMinute(), stats.getMinPacketsMinute(), stats.getAvgPacketsMinute(),
                stats.getMaxPacketsTotal(), stats.getMinPacketsTotal(), stats.getAvgPacketsTotal()
        );
        MinecraftClient.getInstance().keyboard.setClipboard(content);
    }
}