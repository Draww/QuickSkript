package com.github.stefvanschie.quickskript.bukkit.context;

import com.github.stefvanschie.quickskript.core.context.EventContext;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import org.bukkit.event.Event;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * An event context to indicate the code is being ran from an event.
 *
 * @since 0.1.0
 */
public class EventContextImpl implements ContextImpl, EventContext {

    /**
     * The event that was being executed
     */
    @NotNull
    private final Event event;

    /**
     * Constructs a new event context
     *
     * @param event the event that was executed
     * @since 0.1.0
     */
    public EventContextImpl(@NotNull Event event) {
        this.event = event;
    }

    /**
     * Gets the {@link #event}
     *
     * @return the event
     * @since 0.1.0
     */
    @NotNull
    public Event getEvent() {
        return event;
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Contract(pure = true)
    @Override
    public CommandSender getCommandSender() {
        if (getEvent() instanceof PlayerEvent) {
            return ((PlayerEvent) getEvent()).getPlayer();
        }

        if (getEvent() instanceof EntityEvent) {
            return ((EntityEvent) getEvent()).getEntity();
        }

        if (getEvent() instanceof BlockDamageEvent) {
            return ((BlockDamageEvent) getEvent()).getPlayer();
        }

        if (getEvent() instanceof BlockCanBuildEvent) {
            return ((BlockCanBuildEvent) getEvent()).getPlayer();
        }

        return null;
    }
}
