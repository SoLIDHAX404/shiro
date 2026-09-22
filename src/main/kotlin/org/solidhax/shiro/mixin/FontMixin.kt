package org.solidhax.shiro.mixin

import net.minecraft.client.gui.Font
import net.minecraft.network.chat.FormattedText
import net.minecraft.util.FormattedCharSequence
import org.solidhax.shiro.cosmetics.NameReplacer
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.ModifyVariable

@Mixin(Font::class)
abstract class FontMixin {

    @ModifyVariable(
        method = ["prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font\$PreparedText;"],
        at = [At("HEAD")],
        argsOnly = true
    )
    private fun shiro_replacePreparedText(text: FormattedCharSequence): FormattedCharSequence = NameReplacer.replace(text)

    @ModifyVariable(
        method = ["prepareText(Ljava/lang/String;FFIZI)Lnet/minecraft/client/gui/Font\$PreparedText;"],
        at = [At("HEAD")],
        argsOnly = true
    )
    private fun shiro_replacePreparedString(text: String): String = NameReplacer.replace(text)

    @ModifyVariable(method = ["width(Lnet/minecraft/util/FormattedCharSequence;)I"], at = [At("HEAD")], argsOnly = true)
    private fun shiro_replaceSequenceWidth(text: FormattedCharSequence): FormattedCharSequence = NameReplacer.replace(text)

    @ModifyVariable(method = ["width(Lnet/minecraft/network/chat/FormattedText;)I"], at = [At("HEAD")], argsOnly = true)
    private fun shiro_replaceTextWidth(text: FormattedText): FormattedText = NameReplacer.replace(text)

    @ModifyVariable(method = ["width(Ljava/lang/String;)I"], at = [At("HEAD")], argsOnly = true)
    private fun shiro_replaceStringWidth(text: String): String = NameReplacer.replace(text)
}
