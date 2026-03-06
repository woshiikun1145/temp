package com.arknightsendfieldpingandserverpacketratedisplay;

import com.arknightsendfieldpingandserverpacketratedisplay.hud.InfoHud;
import net.fabricmc.api.ClientModInitializer;

public class ArknightsEndfieldPingAndServerPacketRateDisplay implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 初始化统计管理器
        PacketRateManager.initialize();
        // 初始化 HUD（含快捷键）
        InfoHud.initialize();
    }
}