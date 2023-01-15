package com.minelittlepony.unicopia.ability.magic.spell;

import com.minelittlepony.unicopia.UTags;
import com.minelittlepony.unicopia.ability.magic.Caster;
import com.minelittlepony.unicopia.ability.magic.spell.effect.*;
import com.minelittlepony.unicopia.block.data.ModificationType;
import com.minelittlepony.unicopia.entity.player.Pony;
import com.minelittlepony.unicopia.particle.ParticleHandle;
import com.minelittlepony.unicopia.particle.UParticles;
import com.minelittlepony.unicopia.util.MagicalDamageSource;
import com.minelittlepony.unicopia.util.shape.Shape;
import com.minelittlepony.unicopia.util.shape.Sphere;

import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;

/**
 * Internal.
 * <p>
 * Used by the Rainboom ability.
 */
public class RainboomAbilitySpell extends AbstractSpell {

    private static final int RADIUS = 5;
    private static final Shape EFFECT_RANGE = new Sphere(false, RADIUS);

    private final ParticleHandle particlEffect = new ParticleHandle();

    private int age;

    public RainboomAbilitySpell(CustomisedSpellType<?> type) {
        super(type);
    }

    @Override
    public void setDead() {
        super.setDead();
        particlEffect.destroy();
    }

    @Override
    public boolean tick(Caster<?> source, Situation situation) {

        if (situation != Situation.BODY) {
            return false;
        }

        if (source.isClient()) {
            particlEffect.update(getUuid(), source, spawner -> {
                spawner.addParticle(UParticles.RAINBOOM_TRAIL, source.getOriginVector(), Vec3d.ZERO);
            });

           // source.addParticle(new OrientedBillboardParticleEffect(UParticles.RAINBOOM_RING, source.getPhysics().getMotionAngle()), source.getOriginVector(), Vec3d.ZERO);
        }

        LivingEntity owner = source.getMaster();

        if (owner == null) {
            return false;
        }

        source.findAllEntitiesInRange(RADIUS).forEach(e -> {
            e.damage(MagicalDamageSource.create("rainboom", source).setBreakSunglasses(), 6);
        });
        EFFECT_RANGE.translate(source.getOrigin()).getBlockPositions().forEach(pos -> {
            BlockState state = source.getReferenceWorld().getBlockState(pos);
            if (state.isIn(UTags.FRAGILE) && source.canModifyAt(pos, ModificationType.PHYSICAL)) {
                owner.world.breakBlock(pos, true);
            }
        });

        Vec3d motion = source.getEntity().getRotationVec(1).multiply(1.5);
        Vec3d velocity = source.getEntity().getVelocity().add(motion);

        while (velocity.length() > 3) {
            velocity = velocity.multiply(0.6);
        }

        source.getEntity().setVelocity(velocity);
        if (source instanceof Pony pony) {
            pony.getMagicalReserves().getExhaustion().multiply(0.2F);
        }

        return !source.getEntity().isRemoved() && age++ < 90 + 7 * source.getLevel().getScaled(9);
    }

    @Override
    public void toNBT(NbtCompound compound) {
        super.toNBT(compound);
        compound.putInt("age", age);
    }

    @Override
    public void fromNBT(NbtCompound compound) {
        super.fromNBT(compound);
        age = compound.getInt("age");
    }
}
