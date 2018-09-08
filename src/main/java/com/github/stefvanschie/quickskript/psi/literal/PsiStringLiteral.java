package com.github.stefvanschie.quickskript.psi.literal;

import com.github.stefvanschie.quickskript.psi.PsiConverter;
import com.github.stefvanschie.quickskript.psi.PsiElementFactory;
import com.github.stefvanschie.quickskript.psi.util.PsiPrecomputedHolder;
import com.github.stefvanschie.quickskript.util.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A psi element which holds text messages. This is always pre computed.
 *
 * @since 0.1.0
 */
public class PsiStringLiteral extends PsiPrecomputedHolder<Text> {

    /**
     * Creates a new string literal from the given message
     *
     * @param message the message this psi is wrapping
     * @since 0.1.0
     */
    private PsiStringLiteral(@NotNull Text message, int lineNumber) {
        super(message, lineNumber);
    }

    /**
     * A factory for creating {@link PsiStringLiteral}s
     *
     * @since 0.1.0
     */
    public static class Factory implements PsiElementFactory<PsiStringLiteral> {

        /**
         * A pattern for matching strings
         */
        private final Pattern pattern = Pattern.compile("\"([\\s\\S]+)\"");

        /**
         * {@inheritDoc}
         */
        @Nullable
        @Override
        public PsiStringLiteral tryParse(@NotNull String text, int lineNumber) {
            Matcher matcher = pattern.matcher(text);

            if (!matcher.matches())
                return null;

            return new PsiStringLiteral(Text.parse(matcher.group(1)), lineNumber);
        }
    }

    /**
     * A converter to convert types to a psi string literal
     *
     * @since 0.1.0
     */
    public static class Converter implements PsiConverter<PsiStringLiteral> {

        /**
         * {@inheritDoc}
         */
        @Nullable
        @Override
        public PsiStringLiteral convert(@NotNull Object object, int lineNumber) {
            return new PsiStringLiteral(Text.parse(object.toString()), lineNumber);
        }
    }
}
