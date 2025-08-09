package eu.pb4.stylednicknames.command;


import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.TextParserUtils;
import eu.pb4.placeholders.api.parsers.TextParserV1;
import eu.pb4.stylednicknames.MixinHooks;
import eu.pb4.stylednicknames.NickUtils;
import eu.pb4.stylednicknames.StyledNicknames;
import eu.pb4.stylednicknames.StyledNicknamesForge;
import eu.pb4.stylednicknames.config.ConfigManager;
import eu.pb4.stylednicknames.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;


public class StyledNicknameCommands {
    public static final boolean VANISH = Services.PLATFORM.isModLoaded("melius-vanish");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                    literal(StyledNicknames.MOD_ID)
                            .requires(require("stylednicknames.main", 0))
                            .executes(StyledNicknameCommands::about)

                            .then(literal("reload")
                                    .requires(require("stylednicknames.reload", 3))
                                    .executes(StyledNicknameCommands::reloadConfig)
                            )

                            .then(literal("set")
                                    .requires(require("stylednicknames.change_others", 3))
                                    .then(argument("player", EntityArgument.player())
                                            .then(argument("nickname", StringArgumentType.greedyString()).suggests(OTHER_PREVIOUS_NICKNAME_PROVIDER)
                                                    .executes(StyledNicknameCommands::changeOther)
                                            )
                                    )
                            )
                            .then(literal("clear")
                                    .requires(require("stylednicknames.change_others", 3))
                                    .then(argument("player", EntityArgument.player())
                                            .executes(StyledNicknameCommands::resetOther)
                                    )
                            )
            );

            var node = dispatcher.register(
                    literal("nickname")
                            .requires(require("stylednicknames.use", 3).or((s) -> ConfigManager.getConfig().configData.allowByDefault))

                            .then(literal("set")
                                    .then(argument("nickname", StringArgumentType.greedyString()).suggests(PREVIOUS_NICKNAME_PROVIDER)
                                            .executes(StyledNicknameCommands::change)
                                    )
                            )
                            .then(literal("clear").executes(StyledNicknameCommands::reset))
            );

            dispatcher.register(
                    literal("nick")
                            .requires(require("stylednicknames.use", 3).or((s) -> ConfigManager.getConfig().configData.allowByDefault))
                            .redirect(node)
            );

            dispatcher.register(
                    literal("realname")
                            .requires(require("stylednicknames.realname", 3).or((s) -> ConfigManager.getConfig().configData.allowByDefault))
                            .then(argument("nickname", StringArgumentType.greedyString()).suggests(NICKNAME_PROVIDER)
                                    .executes(StyledNicknameCommands::realname)
                            )
            );
    }

    static Predicate<CommandSourceStack> require(String s,int defaultPerm) {
        return sourceStack -> sourceStack.hasPermission(defaultPerm);//Permissions.require("stylednicknames.change_others", 3)
    }

    private static int change(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        var config = ConfigManager.getConfig();
        var nickname = context.getArgument("nickname", String.class);
        if (config.configData.maxLength > 0) {
            Map<String, TextParserV1.TagNodeBuilder> handlers = new HashMap<>();
            for (var entry : TextParserV1.SAFE.getTags()) {
                if ((config.defaultFormattingCodes.getBoolean(entry.name())
                        || context.getSource().hasPermission( 2))) {

                    handlers.put(entry.name(), entry.parser());

                    if (entry.aliases() != null) {
                        for (var a : entry.aliases()) {
                            handlers.put(a, entry.parser());
                        }
                    }
                }
            }

            if (config.configData.allowLegacyFormatting) {
                for (ChatFormatting formatting : ChatFormatting.values()) {
                    if (handlers.get(formatting.getName()) != null) {
                        nickname = nickname.replace(String.copyValueOf(new char[]{'&', formatting.getChar()}), "<" + formatting.getName() + ">");
                    }
                }
            }

            var output = TextParserUtils.formatText(nickname, handlers::get);

            if (output.getString().length() > config.configData.maxLength && !context.getSource().hasPermission(2)) {
                context.getSource().sendSuccess(() -> ConfigManager.getConfig().tooLongText, false);
                return 1;
            }
        }

        MixinHooks.styledNicknames$set(player,nickname, true);
        context.getSource().sendSuccess(() ->
                        Placeholders.parseText(ConfigManager.getConfig().changeText, Placeholders.PREDEFINED_PLACEHOLDER_PATTERN, NickUtils.placeholdersCommand(player)),
                false);
        return 0;
    }

    private static int reset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MixinHooks.styledNicknames$set(player,null, false);
        context.getSource().sendSuccess(() ->
                        Placeholders.parseText(ConfigManager.getConfig().resetText, Placeholders.PREDEFINED_PLACEHOLDER_PATTERN, Map.of(
                                "nickname", context.getSource().getPlayer().getName(),
                                "name", context.getSource().getPlayer().getName()
                        )),
                false);
        return 0;
    }

    private static int changeOther(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        String nickname = StringArgumentType.getString(context,"nickname");
        MixinHooks.styledNicknames$set(player,nickname, false);
        context.getSource().sendSuccess(() -> Component.translatable("Changed nickname of %s to %s", player.getName(), NickUtils.getOutputOrVanilla(player,true)), false);
        return 0;
    }

    private static int resetOther(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        MixinHooks.styledNicknames$set(player,null, false);
        context.getSource().sendSuccess(() -> Component.translatable("Cleared nickname of %s", player.getName()), false);
        return 0;
    }

    private static int realname(CommandContext<CommandSourceStack> context) {
        String nickname = StringArgumentType.getString(context, "nickname");
        List<ServerPlayer> players = context.getSource().getServer().getPlayerList().getPlayers();
        Map<ServerPlayer, MutableComponent> foundPlayers = new HashMap<>();
        for (ServerPlayer player : players) {
            MutableComponent output = NickUtils.getOutputOrVanilla(player,false);
            if (output == null) continue;
            if (output.getString().equals(nickname) && canSeePlayer(player, context.getSource())) {
                foundPlayers.put(player, output);
            }
        }
        if (foundPlayers.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("No player with that nickname is currently online."));
        } else {
            if (foundPlayers.size() > 1) {
                context.getSource().sendSuccess(() -> Component.translatable("Found %s players with that nickname:", foundPlayers.size()), false);
            }
            foundPlayers.forEach((serverPlayerEntity, mutableText) -> {
                        context.getSource().sendSuccess(() -> Component.translatable("The real name of %s is %s.",
                                serverPlayerEntity.getDisplayName(), serverPlayerEntity.getScoreboardName()), false);
                    }
            );
        }
        return 0;
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        if (ConfigManager.loadConfig()) {
            context.getSource().sendSuccess(() -> Component.literal("Reloaded config!"), false);
        } else {
            context.getSource().sendFailure(Component.literal("Error occurred while reloading config!").withStyle(ChatFormatting.RED));

        }
        return 1;
    }

    private static int about(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("Styled Nicknames")
                .withStyle(ChatFormatting.BLUE)
                .append(Component.literal(" - " + StyledNicknamesForge.VERSION)
                        .withStyle(ChatFormatting.WHITE)
                ), false);

        return 1;
    }

    private static final SuggestionProvider<CommandSourceStack> PREVIOUS_NICKNAME_PROVIDER = (source, builder) -> {
        ServerPlayer player = source.getSource().getPlayer();
        return SharedSuggestionProvider.suggest(getNicknameSuggestion(player), builder);
    };

    private static final SuggestionProvider<CommandSourceStack> OTHER_PREVIOUS_NICKNAME_PROVIDER = (source, builder) -> {
        ServerPlayer player = EntityArgument.getPlayer(source, "player");
        return SharedSuggestionProvider.suggest(getNicknameSuggestion(player), builder);
    };

    private static Collection<String> getNicknameSuggestion(ServerPlayer player) {
        if (player != null) {
            String nickname = NickUtils.getNickname(player);
            if (nickname != null) {
                return Collections.singletonList(nickname);
            }
        }
        return Collections.emptyList();
    }

    private static boolean canSeePlayer(ServerPlayer player, CommandSourceStack viewing) {
    //    if (VANISH) {
    //        return VanishAPI.canSeePlayer(player.server, player.getUUID(), viewing);
    //    }
        return true;
    }

    private static final SuggestionProvider<CommandSourceStack> NICKNAME_PROVIDER = (context, builder) -> {
        List<ServerPlayer> players = context.getSource().getServer().getPlayerList().getPlayers();
        Set<String> nicknames = players.stream()
                .filter(player -> canSeePlayer(player, context.getSource()))
                .map(player -> NickUtils.getOutputOrVanilla(player,false))
                .filter(Objects::nonNull)
                .map(Component::getString)
                .collect(Collectors.toSet());
        return SharedSuggestionProvider.suggest(nicknames, builder);
    };

}
