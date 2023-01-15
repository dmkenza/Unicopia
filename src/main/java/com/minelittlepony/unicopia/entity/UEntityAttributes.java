package com.minelittlepony.unicopia.entity;

import java.util.ArrayList;
import java.util.List;

import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import com.minelittlepony.unicopia.Unicopia;

import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.util.registry.Registry;

public interface UEntityAttributes {
    List<EntityAttribute> REGISTRY = new ArrayList<>();

    EntityAttribute EXTENDED_REACH_DISTANCE = ReachEntityAttributes.REACH;
    EntityAttribute EXTENDED_ATTACK_DISTANCE = ReachEntityAttributes.ATTACK_RANGE;
    EntityAttribute EXTRA_MINING_SPEED = register("earth.mining_speed", new ClampedEntityAttribute("player.miningSpeed", 1, 0, 5).setTracked(true));
    EntityAttribute ENTITY_GRAVTY_MODIFIER = register("player.gravity", (new EntityAttribute("player.gravityModifier", 1) {}).setTracked(true));

    private static EntityAttribute register(String name, EntityAttribute attribute) {
        REGISTRY.add(attribute);
        return Registry.register(Registry.ATTRIBUTE, Unicopia.id(name), attribute);
    }

    static void bootstrap() {}
}
