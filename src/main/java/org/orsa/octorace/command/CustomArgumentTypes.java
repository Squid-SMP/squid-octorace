package org.orsa.octorace.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import de.maxhenkel.admiral.argumenttype.ArgumentTypeRegistry;
import de.maxhenkel.admiral.argumenttype.ArgumentTypeSupplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.Arrays;

public class CustomArgumentTypes {

    public static void register(ArgumentTypeRegistry registry) {
//        registerEnum(registry, CheckpointCommand.CheckpointEditAction.class);
        registerPlayerName(registry);
    }

    private static void registerPlayerName(ArgumentTypeRegistry registry) {
        registry.register(
                PlayerNameArg.class,
                new ArgumentTypeSupplier<CommandSourceStack, PlayerNameArg, String>() {
                    @Override
                    public ArgumentType<String> get() {
                        return StringArgumentType.word();
                    }

                    @Override
                    public SuggestionProvider<CommandSourceStack> getSuggestionProvider() {
                        return (ctx, builder) -> {
                            var players = ctx.getSource().getServer().getPlayerList().getPlayers();
                            var names = players.stream().map(p -> p.getName().getString()).toList();
                            return SharedSuggestionProvider.suggest(names, builder);
                        };
                    }
                },
                (ctx, s) -> new PlayerNameArg(s)
        );
    }

    private static <T extends Enum<T>> void registerEnum(ArgumentTypeRegistry registry, Class<T> enumClass) {
        registry.register(
                enumClass,
                new ArgumentTypeSupplier<CommandSourceStack, Object, String>() {
                    @Override
                    public ArgumentType<String> get() {
                        return StringArgumentType.word();
                    }

                    @Override
                    public SuggestionProvider<CommandSourceStack> getSuggestionProvider() {
                        return (ctx, builder) -> SharedSuggestionProvider.suggest(
                                Arrays.stream(enumClass.getEnumConstants()).map(v -> v.name().toLowerCase()),
                                builder
                        );
                    }
                },
                (ctx, s) -> Enum.valueOf(enumClass, s.toUpperCase())
        );
    }
}
