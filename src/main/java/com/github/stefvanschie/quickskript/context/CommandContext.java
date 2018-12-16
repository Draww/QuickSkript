package com.github.stefvanschie.quickskript.context;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A command context to indicate this code is being ran from a command and to provide information about said command.
 *
 * @since 0.1.0
 */
public class CommandContext implements Context {

    /**
     * The executor of the command
     */
    @NotNull
    private final CommandSender sender;

    /**
     * Constructs a new command context.
     *
     * @param sender the executor of the command
     * @since 0.1.0
     */
    public CommandContext(@NotNull CommandSender sender) {
        this.sender = sender;
    }

    /**
     * Gets the {@link #sender}
     *
     * @return the sender
     * @since 0.1.0
     */
    @NotNull
    public CommandSender getSender() {
        return sender;
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Contract(pure = true)
    @Override
    public CommandSender getCommandSender() {
        return getSender();
    }
}
