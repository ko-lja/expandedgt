package lu.kolja.expandedgt.widgets

import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.Util
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.MultilineTextField
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.lwjgl.glfw.GLFW
import java.util.ArrayList
import kotlin.math.max

@OnlyIn(Dist.CLIENT)
class MlTextField(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private val textSupplier: () -> String,
    private val textConsumer: (String) -> Unit,
    private val placeholder: Component = Component.empty()
): WidgetGroup(x, y, width, height) {

    companion object {
        const val DEFAULT_MAX_LENGTH: Int = Int.MAX_VALUE
        private const val ACTION_SET_TEXT = 1

        private var focusedField: MlTextField? = null
    }

    private val font: Font = Minecraft.getInstance().font

    private val textField = CachedTextField(font, width - 4).apply {
        setCharacterLimit(DEFAULT_MAX_LENGTH)
        setCursorListener { clampScroll(); ensureCursorVisible() }
        setValueListener { clampScroll(); ensureCursorVisible() }
    }

    private var scrollAmount = 0.0
    private var dragging = false
    private var hasFocus = false
    private var lastSent: String = ""

    init {
        val init = textSupplier()
        textField.setValue(init)
        lastSent = init
    }

    fun getValue(): String = textField.value()

    fun setValue(v: String) {
        textField.setValue(v)
        clampScroll()
        ensureCursorVisible()
    }

    fun setDirectly(newText: String) {
        setValue(newText)
        sendToServer(true)
    }

    private fun takeFocus() {
        if (focusedField !== this) {
            focusedField?.loseFocus()
            focusedField = this
        }
        hasFocus = true
    }

    private fun loseFocus() {
        hasFocus = false
        dragging = false
        if (focusedField === this) focusedField = null
    }

    private fun sendToServer(force: Boolean = false) {
        val v = getValue()
        textConsumer(v)
        if (force || v != lastSent) {
            writeClientAction(ACTION_SET_TEXT) { buf -> buf.writeUtf(v) }
            lastSent = v
        }
    }

    override fun handleClientAction(id: Int, buffer: FriendlyByteBuf) {
        if (id == ACTION_SET_TEXT) {
            val v = buffer.readUtf(DEFAULT_MAX_LENGTH)
            textConsumer(v)
            return
        }
        super.handleClientAction(id, buffer)
    }

    private fun getMaxScroll(): Double {
        val textH = textField.lineCount() * font.lineHeight
        return max(textH - (sizeHeight - 4), 0).toDouble()
    }

    private fun setScrollAmount(a: Double) {
        scrollAmount = Mth.clamp(a, 0.0, getMaxScroll())
    }

    private fun clampScroll() = setScrollAmount(scrollAmount)

    private fun ensureCursorVisible() {
        val viewH = sizeHeight - 4
        val caretLine = textField.lineAtCursor()
        val caretY = caretLine * font.lineHeight

        val top = scrollAmount
        val bottom = scrollAmount + viewH - font.lineHeight

        if (caretY.toDouble() < top) {
            setScrollAmount(caretY.toDouble())
        } else if (caretY.toDouble() > bottom) {
            setScrollAmount(caretY - (viewH - font.lineHeight).toDouble())
        }
    }

    override fun updateScreen() {
        super.updateScreen()
        if (!hasFocus) {
            val fromMachine = textSupplier()
            if (fromMachine != textField.value()) {
                textField.setValue(fromMachine)
                lastSent = fromMachine
                clampScroll()
                ensureCursorVisible()
            }
        }
    }

    private fun moveCursorToMouse(mx: Double, my: Double) {
        val relX = mx - (positionX + 2)
        val relY = my - (positionY + 2) + scrollAmount
        textField.seekCursorToPoint(relX, relY)
        ensureCursorVisible()
    }

    private fun blink(): Boolean = (Util.getMillis() / 500L) % 2L == 0L

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!isActive || button != 0) return false

        if (!isMouseOverElement(mouseX, mouseY)) {
            if (hasFocus) loseFocus()
            return false
        }

        takeFocus()
        dragging = true

        if (!net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            textField.setSelecting(false)
        }

        moveCursorToMouse(mouseX, mouseY)
        return true
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        dragging = false
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        if (dragging && hasFocus) {
            moveCursorToMouse(mouseX, mouseY)
            return true
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY)
    }

    override fun mouseWheelMove(mouseX: Double, mouseY: Double, wheelDelta: Double): Boolean {
        if (!isMouseOverElement(mouseX, mouseY)) return false
        setScrollAmount(scrollAmount - wheelDelta * font.lineHeight)
        return true
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!hasFocus) return false

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return false
        }

        val handled = textField.keyPressed(keyCode)
        if (handled) {
            clampScroll()
            ensureCursorVisible()
            sendToServer()
        }

        return true
    }

    override fun charTyped(codePoint: Char, modifiers: Int): Boolean {
        if (!hasFocus) return false
        textField.insertText(codePoint.toString())
        clampScroll()
        ensureCursorVisible()
        sendToServer()
        return true
    }

    override fun drawInBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        RenderSystem.enableDepthTest()

        val x0 = positionX
        val y0 = positionY
        val w = sizeWidth
        val h = sizeHeight

        val bg = 0xFF202020.toInt()
        val border = if (hasFocus) 0xFFFFFFFF.toInt() else 0xFF808080.toInt()

        graphics.fill(x0, y0, x0 + w, y0 + h, bg)
        graphics.fill(x0, y0, x0 + w, y0 + 1, border)                 // top
        graphics.fill(x0, y0 + h - 1, x0 + w, y0 + h, border)         // bottom
        graphics.fill(x0, y0, x0 + 1, y0 + h, border)                 // left
        graphics.fill(x0 + w - 1, y0, x0 + w, y0 + h, border)         // right

        val clipL = x0 + 2
        val clipT = y0 + 2
        val clipR = x0 + w - 2
        val clipB = y0 + h - 2
        graphics.enableScissor(clipL, clipT, clipR, clipB)

        val firstLine = (scrollAmount / font.lineHeight).toInt()
        var y = clipT - scrollAmount.toInt() + firstLine * font.lineHeight

        val selectionBegin = if (textField.hasSelection()) textField.selection().begin else -1
        val selectionEnd = if (textField.hasSelection()) textField.selection().end else -1
        val selectionColor = 0x80007FFF.toInt()

        for (idx in firstLine until textField.lineCount()) {
            if (y > clipB) break

            val ln = textField.line(idx)
            val str = textField.value().substring(ln.begin, ln.end)

            if (textField.hasSelection()) {
                val lineStartChar = ln.begin
                val lineEndChar = ln.end

                if (!(selectionEnd <= lineStartChar || selectionBegin >= lineEndChar)) {
                    val selStartInLine = max(0, selectionBegin - lineStartChar)
                    val selEndInLine = minOf(str.length, selectionEnd - lineStartChar)

                    if (selStartInLine < selEndInLine) {
                        val preSel = str.substring(0, selStartInLine)
                        val selectionText = str.substring(selStartInLine, selEndInLine)

                        val selX = clipL + font.width(preSel)
                        val selW = font.width(selectionText)
                        graphics.fill(selX, y, selX + selW, y + font.lineHeight, selectionColor)
                    }
                }
            }

            graphics.drawString(font, str, clipL, y, 0xFFFFFFFF.toInt())
            y += font.lineHeight
        }

        if (hasFocus && blink()) {
            val curLine = textField.lineAtCursor()
            val ln = textField.line(curLine)
            val cx = clipL + font.width(textField.value().substring(ln.begin, textField.cursor()))
            val cy = clipT + curLine * font.lineHeight - scrollAmount.toInt()
            if (cy in clipT until clipB) {
                graphics.fill(cx, cy, cx + 1, cy + font.lineHeight, 0xFFFFFFFF.toInt())
            }
        }

        graphics.disableScissor()

        if (textField.value().isEmpty() && !hasFocus && placeholder.string.isNotEmpty()) {
            graphics.drawString(font, placeholder, clipL, clipT, 0xFF808080.toInt())
        }

        super.drawInBackground(graphics, mouseX, mouseY, partialTicks)
    }

    private data class Line(val begin: Int, val end: Int)

    private class CachedTextField(font: Font, w: Int): MultilineTextField(font, w) {
        private var cache: MutableList<Line>? = null

        data class Selection(val begin: Int, val end: Int)

        init {
            rebuild()
        }

        private fun cacheList(): MutableList<Line> {
            var c = cache
            if (c == null) {
                c = ArrayList()
                cache = c
            }
            return c
        }

        fun lineCount() = cacheList().size

        fun line(idx: Int): Line {
            val c = cacheList()
            if (c.isEmpty()) return Line(0, 0)
            return c[Mth.clamp(idx, 0, c.size - 1)]
        }

        fun lineAtCursor() = super.getLineAtCursor()

        fun selection(): Selection {
            val sv = super.getSelected()
            return Selection(sv.beginIndex(), sv.endIndex())
        }

        override fun setValue(v: String) {
            super.setValue(v)
            rebuild()
        }

        override fun insertText(t: String) {
            super.insertText(t)
            rebuild()
        }

        private fun rebuild() {
            val c = cacheList()
            c.clear()
            super.iterateLines().forEach { sv ->
                c.add(Line(sv.beginIndex(), sv.endIndex()))
            }
        }
    }
}