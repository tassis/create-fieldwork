package dev.tassis.createfieldwork.schematic.client;

import net.minecraft.network.chat.FormattedText;

import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

/** Fits short workstation labels using the actual caller's font measurements. */
public final class ScribingTextFit {
    public static FormattedText fit(FormattedText text, int maxWidth, ToIntFunction<FormattedText> width,
                                    BiFunction<FormattedText, Integer, FormattedText> prefix) {
        if (maxWidth <= 0) {
            return FormattedText.EMPTY;
        }
        if (width.applyAsInt(text) <= maxWidth) {
            return text;
        }
        FormattedText suffix = FormattedText.of("...");
        if (width.applyAsInt(suffix) > maxWidth) {
            return FormattedText.EMPTY;
        }
        // Production delegates this prefix operation to Font.substrByWidth,
        // preserving styled components rather than flattening their contents.
        return FormattedText.composite(prefix.apply(text, maxWidth - width.applyAsInt(suffix)), suffix);
    }

    private ScribingTextFit() {}
}
