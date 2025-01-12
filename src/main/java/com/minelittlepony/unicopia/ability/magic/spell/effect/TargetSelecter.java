package com.minelittlepony.unicopia.ability.magic.spell.effect;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.minelittlepony.unicopia.EquinePredicates;
import com.minelittlepony.unicopia.ability.magic.Caster;
import com.minelittlepony.unicopia.ability.magic.SpellPredicate;
import com.minelittlepony.unicopia.ability.magic.spell.Spell;
import com.minelittlepony.unicopia.entity.player.Pony;
import com.minelittlepony.unicopia.item.FriendshipBraceletItem;
import net.minecraft.entity.Entity;

public class TargetSelecter {

    private final Map<UUID, Target> targets = new TreeMap<>();

    private final Spell spell;

    public TargetSelecter(Spell spell) {
        this.spell = spell;
    }

    public Stream<Entity> getEntities(Caster<?> source, double radius, Predicate<Entity> filter) {
        targets.values().removeIf(Target::tick);

        Entity owner = source.getMaster();

        boolean ownerIsValid = spell.isFriendlyTogether(source) && (EquinePredicates.PLAYER_UNICORN.test(owner) && owner.isSneaking());

        return source.findAllEntitiesInRange(radius)
            .filter(entity -> {
                return !FriendshipBraceletItem.isComrade(source, entity)
                        && !SpellPredicate.IS_SHIELD_LIKE.isOn(entity)
                        && !(ownerIsValid && (Pony.equal(entity, owner) || owner.isConnectedThroughVehicle(entity)));
            })
            .filter(filter)
            .map(i -> {
                targets.computeIfAbsent(i.getUuid(), Target::new);
                return i;
            });
    }

    public long getTotalDamaged() {
        return targets.values().stream().filter(Target::canHurt).count();
    }

    static final class Target {
        private int cooldown = 20;

        Target(UUID id) { }

        boolean tick() {
            return --cooldown < 0;
        }

        boolean canHurt() {
            return cooldown == 20;
        }
    }
}
