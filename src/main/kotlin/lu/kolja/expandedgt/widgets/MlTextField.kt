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
import org.lwjgl.glfw.GLFW
import kotlin.math.max

class MlTextField(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private val textSupplier: () -> String,
    private val textConsumer: (String) -> Unit,
    private val placeholder: Component = Component.empty()
) : WidgetGroup(x, y, width, height) {

    companion object {
        const val DEFAULT_MAX_LENGTH: Int = Int.MAX_VALUE

        private const val ACTION_SET_TEXT = 1
        private const val UPDATE_SET_TEXT = 1

        private var focusedField: MlTextField? = null
    }

    private var textField: CachedTextField? = null

    private var currentString = textSupplier()
    private var lastSent = currentString

    private var scrollAmount = 0.0
    private var dragging = false
    private var hasFocus = false

    init {
        if (isRemote) {
            val font = Minecraft.getInstance().font

            textField = CachedTextField(font, width - 4).apply {
                setCharacterLimit(DEFAULT_MAX_LENGTH)
                setCursorListener {
                    clampScroll()
                    ensureCursorVisible()
                }
                setValueListener {
                    clampScroll()
                    ensureCursorVisible()
                }
                setValue(currentString)
            }
        }
    }

    fun getValue(): String {
        return textField?.value() ?: currentString
    }

    fun setValue(v: String) {
        currentString = v

        if (!isRemote) return

        textField?.setValue(v)
        clampScroll()
        ensureCursorVisible()
    }

    fun setDirectly(newText: String) {
        setValue(newText)
        sendToServer(true)
    }

    /*
     * Initial server -> client value.
     */
    override fun writeInitialData(buffer: FriendlyByteBuf) {
        super.writeInitialData(buffer)

        currentString = textSupplier()
        buffer.writeUtf(currentString)
    }

    override fun readInitialData(buffer: FriendlyByteBuf) {
        super.readInitialData(buffer)

        applySyncedText(buffer.readUtf(DEFAULT_MAX_LENGTH))
    }

    /*
     * Detect external changes on the server, e.g. another player/config action.
     */
    override fun detectAndSendChanges() {
        super.detectAndSendChanges()

        if (isRemote) return

        val newText = textSupplier()

        if (newText != currentString) {
            currentString = newText

            writeUpdateInfo(UPDATE_SET_TEXT) {
                it.writeUtf(newText)
            }
        }
    }

    /*
     * Server -> client updates.
     */
    override fun readUpdateInfo(id: Int, buffer: FriendlyByteBuf) {
        if (id == UPDATE_SET_TEXT) {
            applySyncedText(buffer.readUtf(DEFAULT_MAX_LENGTH))
            return
        }

        super.readUpdateInfo(id, buffer)
    }

    /*
     * Client -> server updates.
     */
    override fun handleClientAction(id: Int, buffer: FriendlyByteBuf) {
        if (id == ACTION_SET_TEXT) {
            val requested = buffer.readUtf(DEFAULT_MAX_LENGTH)

            textConsumer(requested)

            // Read it back in case the consumer ever sanitizes/transforms it.
            currentString = textSupplier()

            if (currentString != requested) {
                writeUpdateInfo(UPDATE_SET_TEXT) {
                    it.writeUtf(currentString)
                }
            }

            return
        }

        super.handleClientAction(id, buffer)
    }

    private fun applySyncedText(newText: String) {
        currentString = newText
        lastSent = newText

        if (!isRemote) return

        val field = textField ?: return

        if (field.value() != newText) {
            field.setValue(newText)
            clampScroll()
            ensureCursorVisible()
        }
    }

    private fun sendToServer(force: Boolean = false) {
        if (!isRemote) return

        val value = getValue()

        currentString = value

        if (force || value != lastSent) {
            writeClientAction(ACTION_SET_TEXT) {
                it.writeUtf(value)
            }

            lastSent = value
        }
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

        if (focusedField === this) {
            focusedField = null
        }
    }

    private fun getMaxScroll(): Double {
        val field = textField ?: return 0.0
        val font = Minecraft.getInstance().font

        val textHeight = field.lineCount() * font.lineHeight

        return max(
            textHeight - (sizeHeight - 4),
            0
        ).toDouble()
    }

    private fun setScrollAmount(amount: Double) {
        scrollAmount = Mth.clamp(
            amount,
            0.0,
            getMaxScroll()
        )
    }

    private fun clampScroll() {
        setScrollAmount(scrollAmount)
    }

    private fun ensureCursorVisible() {
        val field = textField ?: return
        val font = Minecraft.getInstance().font

        val viewHeight = sizeHeight - 4
        val caretLine = field.lineAtCursor()
        val caretY = caretLine * font.lineHeight

        val top = scrollAmount
        val bottom = scrollAmount + viewHeight - font.lineHeight

        if (caretY.toDouble() < top) {
            setScrollAmount(caretY.toDouble())
        } else if (caretY.toDouble() > bottom) {
            setScrollAmount(
                caretY - (viewHeight - font.lineHeight).toDouble()
            )
        }
    }

    override fun updateScreen() {
        super.updateScreen()

        val field = textField ?: return

        /*
         * currentString is authoritative here.
         *
         * Don't read the machine's client copy directly while editing;
         * the server widget is responsible for synchronizing changes.
         */
        if (!hasFocus && field.value() != currentString) {
            field.setValue(currentString)

            clampScroll()
            ensureCursorVisible()
        }
    }

    private fun moveCursorToMouse(
        mouseX: Double,
        mouseY: Double
    ) {
        val field = textField ?: return

        val relX = mouseX - (positionX + 2)
        val relY = mouseY - (positionY + 2) + scrollAmount

        field.seekCursorToPoint(relX, relY)

        ensureCursorVisible()
    }

    private fun blink(): Boolean {
        return (Util.getMillis() / 500L) % 2L == 0L
    }

    override fun mouseClicked(
        mouseX: Double,
        mouseY: Double,
        button: Int
    ): Boolean {
        if (!isActive || button != 0) return false

        if (!isMouseOverElement(mouseX, mouseY)) {
            if (hasFocus) {
                loseFocus()
            }

            return false
        }

        val field = textField ?: return false

        takeFocus()
        dragging = true

        if (!net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            field.setSelecting(false)
        }

        moveCursorToMouse(mouseX, mouseY)

        return true
    }

    override fun mouseReleased(
        mouseX: Double,
        mouseY: Double,
        button: Int
    ): Boolean {
        dragging = false

        return super.mouseReleased(
            mouseX,
            mouseY,
            button
        )
    }

    override fun mouseDragged(
        mouseX: Double,
        mouseY: Double,
        button: Int,
        dragX: Double,
        dragY: Double
    ): Boolean {
        if (dragging && hasFocus) {
            moveCursorToMouse(mouseX, mouseY)
            return true
        }

        return super.mouseDragged(
            mouseX,
            mouseY,
            button,
            dragX,
            dragY
        )
    }

    override fun mouseWheelMove(
        mouseX: Double,
        mouseY: Double,
        wheelDelta: Double
    ): Boolean {
        if (!isMouseOverElement(mouseX, mouseY)) {
            return false
        }

        val font = Minecraft.getInstance().font

        setScrollAmount(
            scrollAmount - wheelDelta * font.lineHeight
        )

        return true
    }

    override fun keyPressed(
        keyCode: Int,
        scanCode: Int,
        modifiers: Int
    ): Boolean {
        if (!hasFocus) return false

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return false
        }

        val field = textField ?: return false

        val handled = field.keyPressed(keyCode)

        if (handled) {
            clampScroll()
            ensureCursorVisible()
            sendToServer()
        }

        return true
    }

    override fun charTyped(
        codePoint: Char,
        modifiers: Int
    ): Boolean {
        if (!hasFocus) return false

        val field = textField ?: return false

        field.insertText(codePoint.toString())

        clampScroll()
        ensureCursorVisible()
        sendToServer()

        return true
    }

    override fun drawInBackground(
        graphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTicks: Float
    ) {
        val field = textField ?: return
        val font = Minecraft.getInstance().font

        RenderSystem.enableDepthTest()

        val x0 = positionX
        val y0 = positionY
        val width = sizeWidth
        val height = sizeHeight

        val background = 0xFF202020.toInt()
        val border =
            if (hasFocus)
                0xFFFFFFFF.toInt()
            else
                0xFF808080.toInt()

        graphics.fill(
            x0,
            y0,
            x0 + width,
            y0 + height,
            background
        )

        graphics.fill(
            x0,
            y0,
            x0 + width,
            y0 + 1,
            border
        )

        graphics.fill(
            x0,
            y0 + height - 1,
            x0 + width,
            y0 + height,
            border
        )

        graphics.fill(
            x0,
            y0,
            x0 + 1,
            y0 + height,
            border
        )

        graphics.fill(
            x0 + width - 1,
            y0,
            x0 + width,
            y0 + height,
            border
        )

        val clipLeft = x0 + 2
        val clipTop = y0 + 2
        val clipRight = x0 + width - 2
        val clipBottom = y0 + height - 2

        graphics.enableScissor(
            clipLeft,
            clipTop,
            clipRight,
            clipBottom
        )

        val firstLine =
            (scrollAmount / font.lineHeight).toInt()

        var y =
            clipTop -
                    scrollAmount.toInt() +
                    firstLine * font.lineHeight

        val selectionBegin =
            if (field.hasSelection())
                field.selection().begin
            else
                -1

        val selectionEnd =
            if (field.hasSelection())
                field.selection().end
            else
                -1

        val selectionColor =
            0x80007FFF.toInt()

        for (index in firstLine until field.lineCount()) {
            if (y > clipBottom) break

            val line = field.line(index)

            val string =
                field.value().substring(
                    line.begin,
                    line.end
                )

            if (field.hasSelection()) {
                val lineStart = line.begin
                val lineEnd = line.end

                if (
                    !(
                            selectionEnd <= lineStart ||
                                    selectionBegin >= lineEnd
                            )
                ) {
                    val selectionStartInLine =
                        max(
                            0,
                            selectionBegin - lineStart
                        )

                    val selectionEndInLine =
                        minOf(
                            string.length,
                            selectionEnd - lineStart
                        )

                    if (
                        selectionStartInLine <
                        selectionEndInLine
                    ) {
                        val beforeSelection =
                            string.substring(
                                0,
                                selectionStartInLine
                            )

                        val selectedText =
                            string.substring(
                                selectionStartInLine,
                                selectionEndInLine
                            )

                        val selectionX =
                            clipLeft +
                                    font.width(beforeSelection)

                        val selectionWidth =
                            font.width(selectedText)

                        graphics.fill(
                            selectionX,
                            y,
                            selectionX + selectionWidth,
                            y + font.lineHeight,
                            selectionColor
                        )
                    }
                }
            }

            graphics.drawString(
                font,
                string,
                clipLeft,
                y,
                0xFFFFFFFF.toInt()
            )

            y += font.lineHeight
        }

        if (hasFocus && blink()) {
            val currentLine =
                field.lineAtCursor()

            val line =
                field.line(currentLine)

            val cursorX =
                clipLeft +
                        font.width(
                            field.value().substring(
                                line.begin,
                                field.cursor()
                            )
                        )

            val cursorY =
                clipTop +
                        currentLine * font.lineHeight -
                        scrollAmount.toInt()

            if (cursorY in clipTop until clipBottom) {
                graphics.fill(
                    cursorX,
                    cursorY,
                    cursorX + 1,
                    cursorY + font.lineHeight,
                    0xFFFFFFFF.toInt()
                )
            }
        }

        graphics.disableScissor()

        if (
            field.value().isEmpty() &&
            !hasFocus &&
            placeholder.string.isNotEmpty()
        ) {
            graphics.drawString(
                font,
                placeholder,
                clipLeft,
                clipTop,
                0xFF808080.toInt()
            )
        }

        super.drawInBackground(
            graphics,
            mouseX,
            mouseY,
            partialTicks
        )
    }

    private data class Line(
        val begin: Int,
        val end: Int
    )

    private class CachedTextField(
        font: Font,
        width: Int
    ) : MultilineTextField(font, width) {

        private var cache:
                MutableList<Line>? = null

        data class Selection(
            val begin: Int,
            val end: Int
        )

        init {
            rebuild()
        }

        private fun cacheList():
                MutableList<Line> {

            var result = cache

            if (result == null) {
                result = ArrayList()
                cache = result
            }

            return result
        }

        fun lineCount(): Int =
            cacheList().size

        fun line(index: Int): Line {
            val lines = cacheList()

            if (lines.isEmpty()) {
                return Line(0, 0)
            }

            return lines[
                Mth.clamp(
                    index,
                    0,
                    lines.size - 1
                )
            ]
        }

        fun lineAtCursor(): Int =
            super.getLineAtCursor()

        fun selection(): Selection {
            val selection =
                super.getSelected()

            return Selection(
                selection.beginIndex(),
                selection.endIndex()
            )
        }

        override fun setValue(value: String) {
            super.setValue(value)
            rebuild()
        }

        override fun insertText(text: String) {
            super.insertText(text)
            rebuild()
        }

        private fun rebuild() {
            val lines = cacheList()

            lines.clear()

            super.iterateLines().forEach {
                lines.add(
                    Line(
                        it.beginIndex(),
                        it.endIndex()
                    )
                )
            }
        }
    }
}