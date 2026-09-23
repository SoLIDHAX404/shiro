package org.solidhax.shiro.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.solidhax.shiro.cosmetics.NameReplacer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Font.class)
public abstract class FontMixin {

    @ModifyVariable(method = {"prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;", "width(Lnet/minecraft/util/FormattedCharSequence;)I"}, at = @At("HEAD"), argsOnly = true)
    private FormattedCharSequence shiro$replaceSequence(FormattedCharSequence text) {
        return NameReplacer.INSTANCE.replace(text);
    }

    @ModifyVariable(method = {"prepareText(Ljava/lang/String;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;", "width(Ljava/lang/String;)I"}, at = @At("HEAD"), argsOnly = true)
    private String shiro$replaceString(String text) {
        return NameReplacer.INSTANCE.replace(text);
    }

    @ModifyVariable(method = "width(Lnet/minecraft/network/chat/FormattedText;)I", at = @At("HEAD"), argsOnly = true)
    private FormattedText shiro$replaceText(FormattedText text) {
        return NameReplacer.INSTANCE.replace(text);
    }
}
