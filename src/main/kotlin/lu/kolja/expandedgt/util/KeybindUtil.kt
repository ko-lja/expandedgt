package lu.kolja.expandedgt.util

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft

object KeybindUtil {
    val isShiftDown: Boolean
        get() = isKeyDown(InputConstants.KEY_LSHIFT)

    fun isKeyDown(key: Int) = InputConstants.isKeyDown(Minecraft.getInstance().window.window, key)
}