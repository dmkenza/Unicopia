package com.minelittlepony.unicopia.command;

import java.util.function.Function;

import com.minelittlepony.unicopia.InteractionManager;
import com.minelittlepony.unicopia.ability.magic.Spell;
import com.minelittlepony.unicopia.ability.magic.spell.SpellType;
import com.minelittlepony.unicopia.command.DisguiseCommand.Arg;
import com.minelittlepony.unicopia.entity.player.Pony;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.EntitySummonArgumentType;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.GameRules;

public class DisguiseCommand {
    private static final SimpleCommandExceptionType FAILED_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("commands.disguise.notfound"));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager
            .literal("disguise")
            .requires(s -> s.hasPermissionLevel(2))
            .executes(context -> reveal(context.getSource(), context.getSource().getPlayer()))
            .then(
                CommandManager.argument("target", EntityArgumentType.players())
                .then(buildEntityDisguise(context -> EntityArgumentType.getPlayer(context, "target")))
                .then(buildPlayerDisguise(context -> EntityArgumentType.getPlayer(context, "target")))
            )
            .then(buildEntityDisguise(context -> context.getSource().getPlayer()))
            .then(buildPlayerDisguise(context -> context.getSource().getPlayer()))
        );
    }

    private static ArgumentBuilder<ServerCommandSource, ?> buildEntityDisguise(Arg<ServerPlayerEntity> targetOp) {
        return CommandManager.argument("entity", EntitySummonArgumentType.entitySummon())
                    .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                    .executes(context -> disguise(
                        context.getSource(),
                        targetOp.apply(context),
                        loadEntity(context.getSource(),
                            EntitySummonArgumentType.getEntitySummon(context, "entity"),
                            new NbtCompound())))
        .then(
                CommandManager.argument("nbt", NbtCompoundArgumentType.nbtCompound())
                    .executes(context -> disguise(
                        context.getSource(),
                        targetOp.apply(context),
                        loadEntity(context.getSource(),
                            EntitySummonArgumentType.getEntitySummon(context, "entity"),
                            NbtCompoundArgumentType.getNbtCompound(context, "nbt"))))
        );
    }

    private static ArgumentBuilder<ServerCommandSource, ?> buildPlayerDisguise(Arg<ServerPlayerEntity> targetOp) {
        return CommandManager.argument("playername", StringArgumentType.string())
                    .executes(context -> disguise(
                        context.getSource(),
                        targetOp.apply(context),
                        loadPlayer(context.getSource(), StringArgumentType.getString(context, "playername"))));
    }

    static int disguise(ServerCommandSource source, PlayerEntity player, Entity entity) throws CommandSyntaxException {
        if (entity == null) {
            throw FAILED_EXCEPTION.create();
        }

        Pony iplayer = Pony.of(player);
        iplayer.getSpellSlot().get(SpellType.DISGUISE, true)
            .orElseGet(() -> SpellType.DISGUISE.apply(iplayer))
            .setDisguise(entity);

        if (source.getEntity() == player) {
            source.sendFeedback(new TranslatableText("commands.disguise.success.self", entity.getName()), true);
        } else {
            if (player.getEntityWorld().getGameRules().getBoolean(GameRules.SEND_COMMAND_FEEDBACK)) {
                player.sendSystemMessage(new TranslatableText("commands.disguise.success", entity.getName()), Util.NIL_UUID);
            }

            source.sendFeedback(new TranslatableText("commands.disguise.success.other", player.getName(), entity.getName()), true);
        }

        return 0;
    }

    static Entity loadEntity(ServerCommandSource source, Identifier id, NbtCompound nbt) {
        nbt = nbt.copy();
        nbt.putString("id", id.toString());
        return EntityType.loadEntityWithPassengers(nbt, source.getWorld(), Function.identity());
    }

    static Entity loadPlayer(ServerCommandSource source, String username) {
        return InteractionManager.instance().createPlayer(source.getWorld(), new GameProfile(null, username));
    }

    static int reveal(ServerCommandSource source, PlayerEntity player) {
        Pony iplayer = Pony.of(player);
        iplayer.getSpellSlot().get(SpellType.DISGUISE, true).ifPresent(Spell::setDead);

        if (source.getEntity() == player) {
            source.sendFeedback(new TranslatableText("commands.disguise.removed.self"), true);
        } else {
            if (player.getEntityWorld().getGameRules().getBoolean(GameRules.SEND_COMMAND_FEEDBACK)) {
                player.sendSystemMessage(new TranslatableText("commands.disguise.removed"), Util.NIL_UUID);
            }

            source.sendFeedback(new TranslatableText("commands.disguise.removed.other", player.getName()), true);
        }

        return 0;
    }

    interface Arg<T> {
        T apply(CommandContext<ServerCommandSource> context) throws CommandSyntaxException;
    }
}
