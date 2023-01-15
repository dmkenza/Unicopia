package com.minelittlepony.unicopia.entity.behaviour;

import com.minelittlepony.unicopia.InteractionManager;
import com.minelittlepony.unicopia.ability.magic.Caster;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;

public class MinecartBehaviour extends EntityBehaviour<AbstractMinecartEntity> {

    @Override
    public AbstractMinecartEntity onCreate(AbstractMinecartEntity entity, EntityAppearance context, boolean replaceOld) {
        super.onCreate(entity, context, replaceOld);
        if (replaceOld && entity.world.isClient) {
            InteractionManager.instance().playLoopingSound(entity, InteractionManager.SOUND_MINECART, entity.getId());
        }
        return entity;
    }

    @Override
    public void update(Caster<?> source, AbstractMinecartEntity entity, Disguise spell) {
        entity.setYaw(entity.getYaw() - 90);
        entity.prevYaw -= 90;

        entity.setPitch(0);
        entity.prevPitch = 0;

        if (source.getEntity() instanceof LivingEntity living) {
            if (living.hurtTime > 0) {
                entity.setDamageWobbleTicks(living.hurtTime);
                entity.setDamageWobbleStrength(1);
                entity.setDamageWobbleSide(20 + (int)source.getEntity().fallDistance / 10);
            }
        }
    }
}
