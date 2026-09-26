package org.solidhax.shiro.mixin;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(PlayerTabOverlay.class)
public interface PlayerTabOverlayAccessor {

    @Invoker("getPlayerInfos")
    List<PlayerInfo> shiroPlayerInfos();

    @Accessor("header")
    Component shiroHeader();

    @Accessor("footer")
    Component shiroFooter();
}
