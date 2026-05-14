package org.orsa.octorace.commandNew;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import de.maxhenkel.admiral.argumenttype.ArgumentTypeRegistry;
import de.maxhenkel.admiral.argumenttype.ArgumentTypeSupplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import org.orsa.octorace.commandNew.octorace.CheckpointCommand;

import java.util.Arrays;

public class CustomArgumentTypes {

    public static void register(ArgumentTypeRegistry registry) {
//        registerEnum(registry, CheckpointCommand.CheckpointEditAction.class);
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
