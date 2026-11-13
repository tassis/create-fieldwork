package dev.tassis.createfieldwork.schematic.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.*;

/** Injected metrics test fitting policy; production supplies native component-aware Font splitting. */
class ScribingTextFitTest {
    @Test
    void labelsRemainWholeWhenTheyFitAndTooSmallBoxesStayEmpty() {
        ToIntFunction<FormattedText> width = text -> text.getString().codePointCount(0, text.getString().length()) * 6;
        var text = FormattedText.of("Settings");
        assertSame(text, fit(text, 80, width));
        assertEquals("", fit(text, 0, width).getString());
        assertEquals("", fit(text, 17, width).getString());
        assertEquals("...", fit(text, 18, width).getString());
    }

    @Test
    void multilingualAndWideFontMetricsNeverOverflowOrSplitCodePoints() {
        for (int scale : new int[]{1, 2, 3}) {
            ToIntFunction<FormattedText> width = text -> text.getString().codePoints().map(point ->
                scale * (point == 'i' ? 2 : point < 128 ? 6 : 9)).sum();
            for (String label : List.of("Schematic Scribing Table", "藍圖材料抄寫臺", "Build 🛠 藍圖", "iiiiiiii Settings")) {
                for (int limit = 0; limit <= 220; limit++) {
                    var fitted = fit(FormattedText.of(label), limit, width);
                    assertTrue(width.applyAsInt(fitted) <= limit);
                    assertFalse(fitted.getString().endsWith("\ud83d"));
                    if (width.applyAsInt(FormattedText.of(label)) <= limit) assertEquals(label, fitted.getString());
                }
            }
        }
    }

    @Test
    void fittedPrefixRetainsComponentStyleInsteadOfFlattening() {
        var prefix = Component.literal("Cut").withStyle(net.minecraft.ChatFormatting.BOLD);
        var result = ScribingTextFit.fit(Component.literal("Long styled title"), 40,
            text -> text.getString().length() * 6, (text, budget) -> prefix);
        assertEquals("Cut...", result.getString());
        var boldParts = new java.util.ArrayList<String>();
        result.visit((style, content) -> {
            if (style.isBold()) boldParts.add(content);
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        assertEquals(List.of("Cut"), boldParts);
    }

    private static FormattedText fit(FormattedText text, int limit, ToIntFunction<FormattedText> width) {
        return ScribingTextFit.fit(text, limit, width, (value, budget) -> {
            String content = value.getString();
            int end = content.length();
            while (end > 0 && width.applyAsInt(FormattedText.of(content.substring(0, end))) > budget) {
                end = content.offsetByCodePoints(end, -1);
            }
            return FormattedText.of(content.substring(0, end));
        });
    }
}
