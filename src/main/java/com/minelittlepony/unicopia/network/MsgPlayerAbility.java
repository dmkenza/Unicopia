package com.minelittlepony.unicopia.network;

import com.minelittlepony.unicopia.ability.Ability;
import com.minelittlepony.unicopia.ability.data.Hit;
import com.minelittlepony.unicopia.entity.player.Pony;
import com.minelittlepony.unicopia.util.network.Packet;
import com.minelittlepony.unicopia.ability.Abilities;

import net.minecraft.util.Identifier;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Sent to the server when a player activates an ability.
 */
public class MsgPlayerAbility<T extends Hit> implements Packet<ServerPlayerEntity> {

    private final Ability<T> power;

    private final T data;

    @SuppressWarnings("unchecked")
    MsgPlayerAbility(PacketByteBuf buffer) {
        power = (Ability<T>) Abilities.REGISTRY.get(new Identifier(buffer.readString(32767)));
        data = power.getSerializer().fromBuffer(buffer);
    }

    public MsgPlayerAbility(Ability<T> power, T data) {
        this.power = power;
        this.data = data;
    }

    @Override
    public void toBuffer(PacketByteBuf buffer) {
        buffer.writeString(Abilities.REGISTRY.getId(power).toString());
        data.toBuffer(buffer);
    }

    @Override
    public void handle(ServerPlayerEntity sender) {
        Pony player = Pony.of(sender);
        if (player == null) {
            return;
        }

        if (!power.canApply(player, data)) {
            Channel.CANCEL_PLAYER_ABILITY.send(sender, new MsgCancelPlayerAbility());
        } else {
            power.apply(player, data);
        }
    }
}
