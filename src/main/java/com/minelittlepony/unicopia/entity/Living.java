package com.minelittlepony.unicopia.entity;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.minelittlepony.unicopia.USounds;
import com.minelittlepony.unicopia.Unicopia;
import com.minelittlepony.unicopia.ability.magic.Caster;
import com.minelittlepony.unicopia.ability.magic.SpellContainer;
import com.minelittlepony.unicopia.ability.magic.SpellPredicate;
import com.minelittlepony.unicopia.ability.magic.SpellContainer.Operation;
import com.minelittlepony.unicopia.ability.magic.spell.Situation;
import com.minelittlepony.unicopia.advancement.UCriteria;
import com.minelittlepony.unicopia.block.data.DragonBreathStore;
import com.minelittlepony.unicopia.entity.effect.UEffects;
import com.minelittlepony.unicopia.item.GlassesItem;
import com.minelittlepony.unicopia.item.UItems;
import com.minelittlepony.unicopia.network.datasync.EffectSync;
import com.minelittlepony.unicopia.network.datasync.Transmittable;
import com.minelittlepony.unicopia.particle.ParticleUtils;
import com.minelittlepony.unicopia.projectile.ProjectileImpactListener;
import com.minelittlepony.unicopia.trinkets.TrinketsDelegate;
import com.minelittlepony.unicopia.util.MagicalDamageSource;
import com.minelittlepony.unicopia.util.VecHelper;

import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public abstract class Living<T extends LivingEntity> implements Equine<T>, Caster<T>, Transmittable {

    protected final T entity;

    private final EffectSync effectDelegate;

    private boolean prevSneaking;
    private boolean prevLanded;

    @Nullable
    private Runnable landEvent;

    @Nullable
    private Entity attacker;

    private int invinsibilityTicks;

    private final Enchantments enchants = new Enchantments(this);

    private final ItemTracker armour = new ItemTracker();

    protected Living(T entity, TrackedData<NbtCompound> effect) {
        this.entity = entity;
        this.effectDelegate = new EffectSync(this, effect);

        entity.getDataTracker().startTracking(effect, new NbtCompound());
    }

    public void waitForFall(Runnable action) {
        if (entity.isOnGround()) {
            action.run();
        } else {
            landEvent = action;
        }
    }

    public boolean sneakingChanged() {
        return entity.isSneaking() != prevSneaking;
    }

    public boolean landedChanged() {
        return entity.isOnGround() != prevLanded;
    }

    @Override
    public SpellContainer getSpellSlot() {
        return effectDelegate;
    }

    public Enchantments getEnchants() {
        return enchants;
    }

    public ItemTracker getArmour() {
        return armour;
    }

    @Override
    public void setMaster(T owner) {
    }

    @Override
    @NotNull
    public T getMaster() {
        return entity;
    }

    @Override
    public void tick() {
        armour.update(this, getArmourStacks());

        try {
            getSpellSlot().forEach(spell -> Operation.ofBoolean(spell.tick(this, Situation.BODY)), entity.world.isClient);
        } catch (Exception e) {
            Unicopia.LOGGER.error("Error whilst ticking spell on entity {}", getEntity(), e);
        }

        if (invinsibilityTicks > 0) {
            invinsibilityTicks--;
        }

        if (landEvent != null && entity.isOnGround() && landedChanged()) {
            landEvent.run();
            landEvent = null;
        }

        if (entity.hasStatusEffect(UEffects.PARALYSIS) && entity.getVelocity().horizontalLengthSquared() > 0) {
            entity.setVelocity(entity.getVelocity().multiply(0, 1, 0));
            updateVelocity();
        }

        enchants.tick();

        prevSneaking = entity.isSneaking();
        prevLanded = entity.isOnGround();

        if (!entity.world.isClient && (entity instanceof PlayerEntity || entity.hasCustomName())) {

            Vec3d targetPos = entity.getRotationVector().multiply(2).add(entity.getEyePos());

            if (entity.getWorld().isAir(new BlockPos(targetPos))) {
                DragonBreathStore store = DragonBreathStore.get(entity.world);
                String name = entity.getDisplayName().getString();
                store.popEntries(name).forEach(stack -> {
                    Vec3d randomPos = targetPos.add(VecHelper.supply(() -> entity.getRandom().nextTriangular(0.1, 0.5)));

                    if (!entity.getWorld().isAir(new BlockPos(randomPos))) {
                        store.put(name, stack.payload());
                    }

                    for (int i = 0; i < 10; i++) {
                        ParticleUtils.spawnParticle(entity.world, ParticleTypes.FLAME, randomPos.add(
                                VecHelper.supply(() -> entity.getRandom().nextTriangular(0.1, 0.5))
                        ), Vec3d.ZERO);
                    }

                    ItemEntity item = EntityType.ITEM.create(entity.world);
                    item.setStack(stack.payload());
                    item.setPosition(randomPos);
                    item.world.spawnEntity(item);
                    entity.world.playSoundFromEntity(null, entity, USounds.ITEM_DRAGON_BREATH_ARRIVE, entity.getSoundCategory(), 1, 1);

                    if (stack.payload().getItem() == UItems.OATS && entity instanceof PlayerEntity player) {
                        UCriteria.RECEIVE_OATS.trigger(player);
                    }
                });
            }
        }
    }

    @Override
    public void onJump() {
        if (getPhysics().isGravityNegative()) {
            entity.setVelocity(entity.getVelocity().multiply(1, -1, 1));
        }
    }

    @Nullable
    @Override
    public final Entity getAttacker() {
        return attacker;
    }

    @Override
    public Optional<Boolean> onDamage(DamageSource source, float amount) {

        if (source == DamageSource.LIGHTNING_BOLT) {
            if (invinsibilityTicks > 0 || tryCaptureLightning()) {
                return Optional.of(false);
            }
        }

        if (source instanceof MagicalDamageSource magical) {
            Entity attacker = ((MagicalDamageSource)source).getSpell();
            if (attacker != null) {
                this.attacker = attacker;
            }

            if (magical.breaksSunglasses()) {
                ItemStack glasses = GlassesItem.getForEntity(entity);
                if (glasses.getItem() == UItems.SUNGLASSES) {
                    ItemStack broken = UItems.BROKEN_SUNGLASSES.getDefaultStack();
                    broken.setNbt(glasses.getNbt());
                    TrinketsDelegate.getInstance().setEquippedStack(entity, TrinketsDelegate.FACE, broken);
                    playSound(SoundEvents.BLOCK_GLASS_BREAK, 1, 1);
                }
            }
        }

        return Optional.empty();
    }

    private boolean tryCaptureLightning() {
        return getInventoryStacks().filter(stack -> !stack.isEmpty() && stack.getItem() == UItems.EMPTY_JAR).findFirst().map(stack -> {
            invinsibilityTicks = 20;
            stack.split(1);
            giveBackItem(UItems.LIGHTNING_JAR.getDefaultStack());
            return stack;
        }).isPresent();
    }

    protected Stream<ItemStack> getInventoryStacks() {
        return Stream.of(entity.getStackInHand(Hand.MAIN_HAND), entity.getStackInHand(Hand.OFF_HAND));
    }

    protected Stream<ItemStack> getArmourStacks() {
        if (!TrinketsDelegate.hasTrinkets()) {
            return StreamSupport.stream(entity.getArmorItems().spliterator(), false);
        }
        return Stream.concat(
                TrinketsDelegate.getInstance().getEquipped(entity, TrinketsDelegate.NECKLACE),
                StreamSupport.stream(entity.getArmorItems().spliterator(), false)
        );
    }

    protected void giveBackItem(ItemStack stack) {
        entity.dropStack(stack);
    }

    @Override
    public boolean onProjectileImpact(ProjectileEntity projectile) {
        return getSpellSlot().get(true)
                .filter(effect -> !effect.isDead()
                        && effect instanceof ProjectileImpactListener
                        && ((ProjectileImpactListener)effect).onProjectileImpact(projectile))
                .isPresent();
    }

    protected void handleFall(float distance, float damageMultiplier, DamageSource cause) {
        getSpellSlot().get(SpellPredicate.IS_DISGUISE, false).ifPresent(spell -> {
            spell.getDisguise().onImpact(this, distance, damageMultiplier, cause);
        });
    }

    @Override
    public void setDirty() {}

    @Override
    public void toNBT(NbtCompound compound) {
        enchants.toNBT(compound);
        effectDelegate.toNBT(compound);
        toSyncronisedNbt(compound);
    }

    @Override
    public void fromNBT(NbtCompound compound) {
        enchants.fromNBT(compound);
        effectDelegate.fromNBT(compound);
        fromSynchronizedNbt(compound);
    }

    @Override
    public void toSyncronisedNbt(NbtCompound compound) {
        compound.put("armour", armour.toNBT());
    }

    @Override
    public void fromSynchronizedNbt(NbtCompound compound) {
        armour.fromNBT(compound.getCompound("armour"));
    }

    public void updateVelocity() {
        updateVelocity(entity);
    }

    public static Optional<Living<?>> getOrEmpty(Entity entity) {
        return PonyContainer.of(entity).map(PonyContainer::get).map(a -> a instanceof Living ? (Living<?>)a : null);
    }

    public static Living<?> living(Entity entity) {
        return getOrEmpty(entity).orElse(null);
    }

    public static void updateVelocity(Entity entity) {
        entity.velocityModified = true;
        //if (entity instanceof ServerPlayerEntity ply) {
        //    ply.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(ply));
        //}
    }

    public static void transmitPassengers(Entity entity) {
        if (entity.world instanceof ServerWorld sw) {
            sw.getChunkManager().sendToNearbyPlayers(entity, new EntityPassengersSetS2CPacket(entity));
        }
    }
}
