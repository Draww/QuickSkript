package com.github.stefvanschie.quickskript.skript;

import com.github.stefvanschie.quickskript.QuickSkript;
import com.github.stefvanschie.quickskript.event.ComplexEventProxyFactory;
import com.github.stefvanschie.quickskript.event.EventProxyFactory;
import com.github.stefvanschie.quickskript.event.SimpleEventProxyFactory;
import com.github.stefvanschie.quickskript.psi.*;
import com.github.stefvanschie.quickskript.psi.condition.PsiHasPermissionCondition;
import com.github.stefvanschie.quickskript.psi.condition.PsiIsCondition;
import com.github.stefvanschie.quickskript.psi.effect.PsiCancelEventEffect;
import com.github.stefvanschie.quickskript.psi.effect.PsiExplosionEffect;
import com.github.stefvanschie.quickskript.psi.effect.PsiMessageEffect;
import com.github.stefvanschie.quickskript.psi.exception.ParseException;
import com.github.stefvanschie.quickskript.psi.expression.PsiConsoleSenderExpression;
import com.github.stefvanschie.quickskript.psi.expression.PsiParseExpression;
import com.github.stefvanschie.quickskript.psi.expression.PsiRandomNumberExpression;
import com.github.stefvanschie.quickskript.psi.function.*;
import com.github.stefvanschie.quickskript.psi.literal.PsiBooleanLiteral;
import com.github.stefvanschie.quickskript.psi.literal.PsiNumberLiteral;
import com.github.stefvanschie.quickskript.psi.literal.PsiPlayerLiteral;
import com.github.stefvanschie.quickskript.psi.literal.PsiStringLiteral;
import com.github.stefvanschie.quickskript.psi.section.PsiIf;
import com.github.stefvanschie.quickskript.psi.section.PsiWhile;
import com.github.stefvanschie.quickskript.util.Platform;
import com.github.stefvanschie.quickskript.util.event.ExperienceOrbSpawnEvent;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Silverfish;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.SimplePluginManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Instances of this class contain everything necessary for loading Skript files.
 * This allows addons to be easily loaded, as well as dropping all data after
 * all Skripts have been loaded (in order to save memory).
 * This means that the static modifier should only be used when the data is required
 * to also be present when the Skripts are being ran, not only when they are being loaded.
 *
 * @since 0.1.0
 */
public class SkriptLoader implements AutoCloseable {

    /**
     * The current loader instance or null if there is none present.
     */
    @Nullable
    private static SkriptLoader instance;

    /**
     * Gets the current loader instance. Returns null if there is none present.
     *
     * @return the current instance
     * @since 0.1.0
     */
    @Contract(pure = true)
    public static SkriptLoader get() {
        return instance;
    }


    /**
     * A list of all psi element factories.
     */
    @NotNull
    private final List<PsiElementFactory<?>> elements = new ArrayList<>();

    /**
     * A list of all psi section factories.
     */
    @NotNull
    private final List<PsiSectionFactory<?>> sections = new ArrayList<>();

    /**
     * A map indexing converters by their name.
     */
    @NotNull
    private final Map<String, PsiConverter<?>> converters = new HashMap<>();

    /**
     * A list of all event proxy factories.
     */
    @NotNull
    private final List<EventProxyFactory> events = new ArrayList<>();

    /**
     * The constructor of {@link PluginCommand} wrapped into a {@link Function}.
     */
    @NotNull
    private final Function<String, PluginCommand> commandConstructor;

    /**
     * The internally used instance of the {@link CommandMap}.
     */
    @NotNull
    private final CommandMap commandMap;

    /**
     * Create a new instance, initializing it with all default (non-addon) data.
     *
     * @since 0.1.0
     */
    public SkriptLoader() {
        Validate.isTrue(instance == null, "A SkriptLoader is already present, can't create another one.");

        registerDefaultElements();
        registerDefaultSections();
        registerDefaultConverters();
        registerDefaultEvents();

        try {
            Constructor<PluginCommand> rawConstructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            rawConstructor.setAccessible(true);

            commandConstructor = name -> {
                try {
                    return rawConstructor.newInstance(name, QuickSkript.getInstance());
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new AssertionError("Error while constructing a PluginCommand instance:", e);
                }
            };

            Field commandMapField = SimplePluginManager.class.getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            commandMap = (CommandMap) commandMapField.get(Bukkit.getPluginManager());

        } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError("Error while getting the CommandMap:", e);
        }

        instance = this;
    }


    /**
     * Parses text into psi elements.
     * Returns null if no element was found.
     *
     * @param input the text to be parsed
     * @param lineNumber the line number of the element which will potentially be parsed
     * @return the parsed psi element, or null if none were found
     * @since 0.1.0
     */
    @Nullable
    @Contract(pure = true)
    public PsiElement<?> tryParseElement(@NotNull String input, int lineNumber) {
        input = input.trim();

        for (PsiElementFactory<?> factory : elements) {
            PsiElement<?> element = factory.tryParse(input, lineNumber);

            if (element != null)
                return element;
        }

        return null;
    }

    /**
     * Parses text into psi elements.
     * Throws a {@link ParseException} if no element was found.
     *
     * @param input the text to be parsed
     * @param lineNumber the line number of the element that will be parsed
     * @return the parsed psi element
     * @since 0.1.0
     */
    @NotNull
    @Contract(pure = true)
    public PsiElement<?> forceParseElement(@NotNull String input, int lineNumber) {
        PsiElement<?> result = tryParseElement(input, lineNumber);
        if (result != null)
            return result;

        throw new ParseException("Unable to find an expression named: " + input, lineNumber);
    }


    /**
     * Parses a file section into a psi section.
     * Throws a {@link ParseException} if the parsing wasn't successful.
     *
     * @param input the text to be parsed
     * @param elementsSupplier the action which parses the contained elements on demand
     * @param lineNumber the line number of the section that will be parsed
     * @return the parsed psi section, or null if none were found
     * @since 0.1.0
     */
    @NotNull
    @Contract(pure = true)
    public PsiSection forceParseSection(@NotNull String input,
            @NotNull Supplier<PsiElement<?>[]> elementsSupplier, int lineNumber) {
        for (PsiSectionFactory<?> factory : sections) {
            PsiSection result = factory.tryParse(input, elementsSupplier, lineNumber);

            if (result != null)
                return result;
        }

        //fall back to an 'if' statement which doesn't start with 'if'
        PsiElement<?> condition = forceParseElement(input, lineNumber);
        //try to parse the condition before all elements inside the section
        return new PsiIf(elementsSupplier.get(), condition, lineNumber);
    }


    /**
     * Returns a converter based on the specified name.
     * Returns null if no converter was found.
     *
     * @param name the name of the converter
     * @return the converter
     * @since 0.1.0
     */
    @Nullable
    @Contract(pure = true)
    public PsiConverter<?> tryGetConverter(@NotNull String name) {
        return converters.get(name);
    }

    /**
     * Returns a converter based on the specified name.
     * Throws a {@link ParseException} if no converter was found.
     *
     * @param name the name of the converter
     * @param lineNumber the line number of the element that tried to retrieve a converter
     * @return the converter
     * @since 0.1.0
     */
    @NotNull
    @Contract(pure = true)
    public PsiConverter<?> forceGetConverter(@NotNull String name, int lineNumber) {
        PsiConverter<?> result = tryGetConverter(name);
        if (result != null)
            return result;

        throw new ParseException("Unable to find a converter named: " + name, lineNumber);
    }


    /**
     * Parses the inputted text and returns whether an event executor was registered.
     *
     * @param input the input to parse
     * @param toRegisterSupplier a {@link Supplier} which can create one {@link SkriptEventExecutor} instance to register
     * @return whether a registration took place
     * @since 0.1.0
     */
    public boolean tryRegisterEventExecutor(@NotNull String input,
            @NotNull Supplier<SkriptEventExecutor> toRegisterSupplier) {
        input = input.trim();

        for (EventProxyFactory factory : events) {
            if (factory.tryRegister(input, toRegisterSupplier))
                return true;
        }

        return false;
    }


    /**
     * Registers the specified factory.
     *
     * @param factory the element factory to register
     * @since 0.1.0
     */
    public void registerElement(@NotNull PsiElementFactory<?> factory) {
        elements.add(factory);
    }

    /**
     * Registers the specified factory.
     *
     * @param factory the section factory to register
     * @since 0.1.0
     */
    public void registerSection(@NotNull PsiSectionFactory<?> factory) {
        sections.add(factory);
    }

    /**
     * Registers the specified converter.
     *
     * @param name the name of the converter
     * @param converter the converter to register
     * @since 0.1.0
     */
    public void registerConverter(@NotNull String name, @NotNull PsiConverter<?> converter) {
        Validate.isTrue(converters.put(name, converter) == null, "A PsiConverter with the same name has already been registered.");
    }

    /**
     * Registers the specified factory.
     *
     * @param factory the event factory to register
     * @since 0.1.0
     */
    public void registerEvent(@NotNull EventProxyFactory factory) {
        events.add(factory);
    }


    /**
     * Creates a command and registers it in the internal {@link CommandMap}
     * after running the specified initializer.
     *
     * @param name the name of the command
     * @param initializer the initializer of the new command instance
     * @since 0.1.0
     */
    public void registerCommand(@NotNull String name, @NotNull Consumer<PluginCommand> initializer) {
        PluginCommand command = commandConstructor.apply(name);
        initializer.accept(command);
        commandMap.register("quickskript", command);
    }


    /**
     * Deletes the current loader instance.
     * Normally should only be called if you are the one who created it.
     *
     * @since 0.1.0
     */
    @Override
    public void close() {
        Validate.notNull(instance, "No SkriptLoader is present, can't close it.");
        instance = null;
    }


    private void registerDefaultElements() {
        //effects
        //these are at the top, cause they are always the outermost element
        registerElement(new PsiCancelEventEffect.Factory());
        registerElement(new PsiExplosionEffect.Factory());
        registerElement(new PsiMessageEffect.Factory());

        //this one is here, because it has special identifiers around it
        registerElement(new PsiStringLiteral.Factory());

        //conditions
        registerElement(new PsiHasPermissionCondition.Factory());
        registerElement(new PsiIsCondition.Factory());

        //expressions
        registerElement(new PsiConsoleSenderExpression.Factory());
        registerElement(new PsiParseExpression.Factory());
        registerElement(new PsiRandomNumberExpression.Factory());

        //functions
        registerElement(new PsiAbsoluteValueFunction.Factory());
        registerElement(new PsiAtan2Function.Factory());
        registerElement(new PsiCalculateExperienceFunction.Factory());
        registerElement(new PsiCeilFunction.Factory());
        registerElement(new PsiCosineFunction.Factory());
        registerElement(new PsiDateFunction.Factory());
        registerElement(new PsiExponentialFunction.Factory());
        registerElement(new PsiFloorFunction.Factory());
        registerElement(new PsiInverseCosineFunction.Factory());
        registerElement(new PsiInverseSineFunction.Factory());
        registerElement(new PsiInverseTangentFunction.Factory());
        registerElement(new PsiLocationFunction.Factory());
        registerElement(new PsiLogarithmFunction.Factory());
        registerElement(new PsiMaximumFunction.Factory());
        registerElement(new PsiMinimumFunction.Factory());
        registerElement(new PsiModuloFunction.Factory());
        registerElement(new PsiNaturalLogarithmFunction.Factory());
        registerElement(new PsiProductFunction.Factory());
        registerElement(new PsiRoundFunction.Factory());
        registerElement(new PsiSineFunction.Factory());
        registerElement(new PsiSquareRootFunction.Factory());
        registerElement(new PsiSumFunction.Factory());
        registerElement(new PsiTangentFunction.Factory());
        registerElement(new PsiVectorFunction.Factory());
        registerElement(new PsiWorldFunction.Factory());

        //literals
        registerElement(new PsiBooleanLiteral.Factory());
        registerElement(new PsiNumberLiteral.Factory());
        registerElement(new PsiPlayerLiteral.Factory());
    }

    private void registerDefaultSections() {
        registerSection(new PsiIf.Factory());
        registerSection(new PsiWhile.Factory());
    }

    private void registerDefaultConverters() {
        registerConverter("number", new PsiNumberLiteral.Converter());
        registerConverter("text", new PsiStringLiteral.Converter());
    }

    @SuppressWarnings("HardcodedFileSeparator")
    private void registerDefaultEvents() {
        registerEvent(new SimpleEventProxyFactory()
                .registerEvent(AreaEffectCloudApplyEvent.class, "on (?:(?:area)|(?:AoE)) (?:cloud )?effect")
                .registerEvent(BlockCanBuildEvent.class, "on(?: block)? can build check")
                .registerEvent(BlockDamageEvent.class, "on block damag(?:ing|e)")
                .registerEvent(BlockFromToEvent.class, "on(?: block)? (?:flow(?:ing)?|mov(?:e|ing))")
                .registerEvent(BlockIgniteEvent.class, "on(?: block)? ignit(?:e|ion)?")
                .registerEvent(ChunkLoadEvent.class, "on chunk load(?:ing)?")
                .registerEvent(ChunkPopulateEvent.class, "on chunk (?:generat|populat)(?:e|ing)")
                .registerEvent(ChunkUnloadEvent.class, "on chunk unload(?:ing)?")
                .registerEvent(CreeperPowerEvent.class, "on creeper power")
                .registerEvent(EntityCombustEvent.class, "on combust(?:ing)?")
                .registerEvent("org.spigotmc.event.entity.EntityDismountEvent", "on dismount(?:ing)?", Platform.SPIGOT)
                .registerEvent(EntityExplodeEvent.class, "on explo(?:(?:d(?:e|ing))|(?:sion))")
                .registerEvent("org.spigotmc.event.entity.EntityMountEvent", "on mount(?:ing)?", Platform.SPIGOT)
                .registerEvent(EntityRegainHealthEvent.class, "on heal(?:ing)?")
                .registerEvent(EntityToggleGlideEvent.class, "on(?: toggle)? glid(?:e|ing)(?: state change)?")
                .registerEvent(ExperienceOrbSpawnEvent.class, "on (?:(?:e)?xp(?:erience)?(?: orb)? spawn|spawn of(?: a(?:n)?)? (?:e)?xp(?:erience)?(?: orb)?)")
                .registerEvent(ExplosionPrimeEvent.class, "on explosion prime")
                .registerEvent(FoodLevelChangeEvent.class, "on (?:food|hunger) (?:level|met(?:er|re)|bar) chang(?:e|ing)")
                .registerEvent(FurnaceBurnEvent.class, "on fuel burn(?:ing)?")
                .registerEvent(PlayerBedEnterEvent.class, "on (?:(?:bed enter(?:ing)?)|(?:(?:player )?enter(?:ing)? (?:a )?bed))")
                .registerEvent(PlayerBedLeaveEvent.class, "on (?:(?:bed leav(?:e|ing))|(?:(player )?leav(?:e|ing) (a )?bed))")
                .registerEvent(PlayerBucketEmptyEvent.class, "on(?: player)?(?: empty(?:ing)?)?(?: a)? bucket(?: empty(?:ing)?)?")
                .registerEvent(PlayerBucketFillEvent.class, "on(?: player)?(?: fill(?:ing)?)?(?: a)? bucket(?: fill(?:ing)?)?")
                .registerEvent(PlayerCommandPreprocessEvent.class, "on command")
                .registerEvent(PlayerFishEvent.class, "on(?: player)? fish(?:ing)?")
                .registerEvent(PlayerLoginEvent.class, "on(?: player)? connect(?:ing)?")
                .registerEvent(PlayerSwapHandItemsEvent.class, "on swap(ping of)?(?: (?:hand|held))? items?")
                .registerEvent(PlayerToggleFlightEvent.class, "on(?: player)? (?:flight toggl(?:e|ing)|toggl(?:e|ing) flight)")
        );

        registerEvent(new ComplexEventProxyFactory()
                .registerEvent(EntityChangeBlockEvent.class, "on enderman place", matcher -> event ->
                        event.getEntity() instanceof Enderman && event.getTo() != Material.AIR)
                .registerEvent(EntityChangeBlockEvent.class, "on enderman pickup", matcher -> event ->
                        event.getEntity() instanceof Enderman && event.getTo() == Material.AIR)
                .registerEvent(EntityChangeBlockEvent.class, "on sheep eat", matcher -> event ->
                        event.getEntity() instanceof Sheep)
                .registerEvent(EntityChangeBlockEvent.class, "on silverfish enter", matcher -> event ->
                        event.getEntity() instanceof Silverfish && EnumSet.of(
                                Material.INFESTED_COBBLESTONE,
                                Material.INFESTED_STONE,
                                Material.INFESTED_CHISELED_STONE_BRICKS,
                                Material.INFESTED_CRACKED_STONE_BRICKS,
                                Material.INFESTED_MOSSY_STONE_BRICKS,
                                Material.INFESTED_STONE_BRICKS
                        ).contains(event.getTo()))
                .registerEvent(EntityChangeBlockEvent.class, "on silverfish exit", matcher -> event ->
                        event.getEntity() instanceof Silverfish && event.getTo() == Material.AIR)
                .registerEvent(PlayerCommandPreprocessEvent.class, "on command \"([\\s\\S]+)\"", matcher -> {
                    String command = matcher.group(1); //TODO the regex of this group is probably incorrect
                    String finalCommand = command.startsWith("/") ? command.substring(1) : command;

                    return event -> {
                        String message = event.getMessage();
                        return message.startsWith(finalCommand, message.startsWith("/") ? 1 : 0);
                    };
                })
                .registerEvent(PlayerEditBookEvent.class, "on book (edit|change|write|sign|signing)", matcher -> {
                    String group = matcher.group(1).toLowerCase();
                    return group.equals("sign") || group.equals("signing")
                            ? PlayerEditBookEvent::isSigning : event -> !event.isSigning();
                })
                .registerEvent(PlayerInteractEvent.class, "on (?:(right|left)(?: |-)?)?(?:mouse(?: |-)?)?click(?:ing)?", matcher -> {
                    //TODO: This expression needs to be completed in the future, since it's missing optional additional parts
                    String clickType = matcher.group(1);

                    if (clickType == null)
                        return event -> true;
                    else if (clickType.equals("left"))
                        return event -> {
                            Action action = event.getAction();
                            return action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
                        };
                    else if (clickType.equals("right"))
                        return event -> {
                            Action action = event.getAction();
                            return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
                        };

                    throw new AssertionError("Unknown click type detected for event registration");
                })
                .registerEvent(PlayerJoinEvent.class, "on first (?:join|login)", matcher -> event ->
                        !event.getPlayer().hasPlayedBefore())
        );
    }
}