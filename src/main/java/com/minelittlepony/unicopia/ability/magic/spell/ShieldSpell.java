package com.minelittlepony.unicopia.ability.magic.spell;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import com.minelittlepony.unicopia.EquinePredicates;
import com.minelittlepony.unicopia.Unicopia;
import com.minelittlepony.unicopia.ability.magic.Attached;
import com.minelittlepony.unicopia.ability.magic.Caster;
import com.minelittlepony.unicopia.ability.magic.Thrown;
import com.minelittlepony.unicopia.ability.magic.spell.ShieldSpell.Target;
import com.minelittlepony.unicopia.entity.player.Pony;
import com.minelittlepony.unicopia.item.FriendshipBraceletItem;
import com.minelittlepony.unicopia.item.enchantment.UEnchantments;
import com.minelittlepony.unicopia.particle.MagicParticleEffect;
import com.minelittlepony.unicopia.particle.ParticleHandle;
import com.minelittlepony.unicopia.particle.SphereParticleEffect;
import com.minelittlepony.unicopia.projectile.MagicProjectileEntity;
import com.minelittlepony.unicopia.projectile.ProjectileUtil;
import com.minelittlepony.unicopia.util.shape.Sphere;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EyeOfEnderEntity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

public class ShieldSpell extends AbstractSpell implements Attached, Thrown {

    private final ParticleHandle particlEffect = new ParticleHandle();

    private final Map<UUID, Target> targets = new TreeMap<>();

    protected ShieldSpell(SpellType<?> type) {
        super(type);
    }

    @Override
    public void setDead() {
        super.setDead();
        particlEffect.destroy();
    }

    protected void generateParticles(Caster<?> source) {
        float radius = (float)getDrawDropOffRange(source);

        source.spawnParticles(new Sphere(true, radius), (int)(radius * 6), pos -> {
            source.addParticle(new MagicParticleEffect(getType().getColor()), pos, Vec3d.ZERO);
        });

        particlEffect.ifAbsent(source, spawner -> {
            spawner.addParticle(new SphereParticleEffect(getType().getColor(), 0.3F, radius), source.getOriginVector(), Vec3d.ZERO);
        }).ifPresent(p -> {
            p.attach(source);
            p.setAttribute(0, radius);
            p.setAttribute(1, getType().getColor());
        });
    }

    @Override
    public boolean onThrownTick(MagicProjectileEntity source) {
        if (source.isClient()) {
            generateParticles(source);
        }

        applyEntities(source);
        return true;
    }

    @Override
    public boolean onBodyTick(Caster<?> source) {
        if (source.isClient()) {
            generateParticles(source);
        }

        long costMultiplier = applyEntities(source);
        if (costMultiplier > 0) {
            double cost = 2 + source.getLevel().get();

            cost *= costMultiplier / ((1 + source.getLevel().get()) * 3F);
            cost /= 2.725D;

            if (!source.subtractEnergyCost(cost)) {
                setDead();
            }
        }

        return !isDead();
    }

    /**
     * Calculates the maximum radius of the shield. aka The area of effect.
     */
    public double getDrawDropOffRange(Caster<?> source) {
        float multiplier = source.getMaster().isSneaking() ? 1 : 2;
        return (4 + (source.getLevel().get() * 2)) / multiplier;
    }

    protected List<Entity> getTargets(Caster<?> source, double radius) {

        Entity owner = source.getMaster();

        boolean ownerIsValid = isFriendlyTogether(source) && (EquinePredicates.PLAYER_UNICORN.test(owner) && owner.isSneaking());

        return source.findAllEntitiesInRange(radius)
            .filter(entity -> {
                return
                        !FriendshipBraceletItem.isComrade(source, entity)
                        && (entity instanceof LivingEntity
                        || entity instanceof TntEntity
                        || entity instanceof FallingBlockEntity
                        || entity instanceof EyeOfEnderEntity
                        || entity instanceof BoatEntity
                        || ProjectileUtil.isFlyingProjectile(entity)
                        || entity instanceof AbstractMinecartEntity)
                        && !(entity instanceof ArmorStandEntity)
                        && !(ownerIsValid && (Pony.equal(entity, owner) || owner.isConnectedThroughVehicle(entity)));
            })
            .collect(Collectors.toList());
    }

    protected long applyEntities(Caster<?> source) {
        double radius = getDrawDropOffRange(source);

        Vec3d origin = source.getOriginVector();

        this.targets.values().removeIf(Target::tick);

        List<Entity> targets = getTargets(source, radius);
        targets.forEach(i -> {
            try {
                this.targets.computeIfAbsent(i.getUuid(), Target::new);
                double dist = i.getPos().distanceTo(origin);

                applyRadialEffect(source, i, dist, radius);
            } catch (Throwable e) {
                Unicopia.LOGGER.error("Error updating shield effect", e);
            }
        });

        return this.targets.values().stream().filter(Target::canHurt).count();
    }

    protected void applyRadialEffect(Caster<?> source, Entity target, double distance, double radius) {
        Vec3d pos = source.getOriginVector();

        if (ProjectileUtil.isFlyingProjectile(target)) {
            if (!ProjectileUtil.isProjectileThrownBy(target, source.getMaster())) {
                if (distance < 1) {
                    target.playSound(SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, 0.1F, 1);
                    target.remove(RemovalReason.DISCARDED);
                } else {
                    ProjectileUtil.ricochet(target, pos, 0.9F);
                }
            }
        } else if (target instanceof LivingEntity) {
            double force = Math.max(0.1, radius / 4);

            if (isFriendlyTogether(source) && target instanceof PlayerEntity) {
                force *= calculateAdjustedForce(Pony.of((PlayerEntity)target));
            } else {
                force *= 0.75;
            }

            applyForce(pos, target, force, distance);
        }
    }

    /**
     * Applies a force to the given entity based on distance from the source.
     */
    protected void applyForce(Vec3d pos, Entity target, double force, double distance) {
        pos = target.getPos().subtract(pos).normalize().multiply(force);

        if (target instanceof LivingEntity) {
            pos = pos.multiply(1 / (1 + EnchantmentHelper.getEquipmentLevel(UEnchantments.HEAVY, (LivingEntity)target)));
        }

        target.addVelocity(
                pos.x,
                pos.y + (distance < 1 ? distance : 0),
                pos.z
        );
    }

    /**
     * Returns a force to apply based on the given player's given race.
     */
    protected double calculateAdjustedForce(Pony player) {
        double force = 0.75;

        if (player.getSpecies().canUseEarth()) {
            force /= 2;

            if (player.getMaster().isSneaking()) {
                force /= 6;
            }
        } else if (player.getSpecies().canFly()) {
            force *= 2;
        }

        return force;
    }

    class Target {

        int cooldown = 20;

        Target(UUID id) {
        }

        boolean tick() {
            return --cooldown < 0;
        }

        boolean canHurt() {
            return cooldown == 20;
        }
    }
}
