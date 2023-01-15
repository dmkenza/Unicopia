package com.minelittlepony.unicopia.network;

import java.util.Optional;

import com.minelittlepony.unicopia.ability.Abilities;
import com.minelittlepony.unicopia.ability.Ability;
import com.minelittlepony.unicopia.ability.ActivationType;
import com.minelittlepony.unicopia.ability.data.Hit;
import com.minelittlepony.unicopia.entity.player.Pony;
import com.minelittlepony.unicopia.util.network.Packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Sent to the server when a player activates an ability.
 */
public class MsgPlayerAbility<T extends Hit> implements Packet<ServerPlayerEntity> {
    private final Ability<T> power;
    private final Optional<T> data;
    private final ActivationType type;

    @SuppressWarnings("unchecked")
    MsgPlayerAbility(PacketByteBuf buffer) {
        power = (Ability<T>) Abilities.REGISTRY.get(buffer.readIdentifier());
        data = buffer.readOptional(power.getSerializer()::fromBuffer);
        type = ActivationType.of(buffer.readInt());
    }

    public MsgPlayerAbility(Ability<T> power, Optional<T> data, ActivationType type) {
        this.power = power;
        this.data = data;
        this.type = type;
    }

    @Override
    public void toBuffer(PacketByteBuf buffer) {
        buffer.writeIdentifier(Abilities.REGISTRY.getId(power));
        buffer.writeOptional(data, (buf, t) -> t.toBuffer(buf));
        buffer.writeInt(type.ordinal());
    }

    @Override
    public void handle(ServerPlayerEntity sender) {
        Pony player = Pony.of(sender);
        if (player == null) {
            return;
        }

        if (type != ActivationType.NONE) {
            power.onQuickAction(player, type, data);
        } else {
            data.filter(data -> power.canApply(player, data)).ifPresentOrElse(
                    data -> power.apply(player, data),
                    () -> Channel.CANCEL_PLAYER_ABILITY.send(sender, new MsgCancelPlayerAbility())
            );
        }
    }
}
