package com.github.stefvanschie.quickskript.psi.literal;

import com.github.stefvanschie.quickskript.context.CommandContext;
import com.github.stefvanschie.quickskript.context.Context;
import com.github.stefvanschie.quickskript.context.EventContext;
import com.github.stefvanschie.quickskript.psi.PsiElement;
import com.github.stefvanschie.quickskript.psi.PsiElementFactory;
import com.github.stefvanschie.quickskript.psi.exception.ExecutionException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Returns the player from the given context. Can never be pre computed since it relies completely on context.
 *
 * @since 0.1.0
 */
public class PsiPlayerLiteral extends PsiElement<Player> {

    /**
     * {@inheritDoc}
     */
    private PsiPlayerLiteral(int lineNumber) {
        super(lineNumber);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    protected Player executeImpl(@Nullable Context context) {
        if (context == null) {
            throw new ExecutionException("Cannot find a player without a context", lineNumber);
        }

        CommandSender sender = context.getCommandSender();

        if (!(sender instanceof Player)) {
            throw new ExecutionException("Command wasn't executed by a player", lineNumber);
        }

        return (Player) sender;
    }

    /**
     * A factory for creating player literals
     *
     * @since 0.1.0
     */
    public static class Factory implements PsiElementFactory<PsiPlayerLiteral> {

        /**
         * The pattern to parse player literals with
         */
        private final Pattern pattern = Pattern.compile("(?:the )?player");

        /**
         * {@inheritDoc}
         */
        @Nullable
        @Override
        public PsiPlayerLiteral tryParse(@NotNull String text, int lineNumber) {
            Matcher matcher = pattern.matcher(text);

            if (!matcher.matches())
                return null;

            return new PsiPlayerLiteral(lineNumber);
        }
    }
}