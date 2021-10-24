package com.minelittlepony.unicopia.ability.magic.spell;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.minelittlepony.unicopia.Affinity;
import com.minelittlepony.unicopia.ability.magic.Affine;
import com.minelittlepony.unicopia.ability.magic.Caster;
import com.minelittlepony.unicopia.ability.magic.Spell;
import com.minelittlepony.unicopia.util.Registries;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.registry.Registry;

public final class SpellType<T extends Spell> implements Affine, SpellPredicate<T> {

    public static final Identifier EMPTY_ID = new Identifier("unicopia", "null");
    public static final SpellType<?> EMPTY_KEY = new SpellType<>(EMPTY_ID, Affinity.NEUTRAL, 0xFFFFFF, false, t -> null);

    private static final Registry<SpellType<?>> REGISTRY = Registries.createSimple(new Identifier("unicopia", "spells"));
    private static final Map<Affinity, Set<SpellType<?>>> BY_AFFINITY = new EnumMap<>(Affinity.class);

    public static final SpellType<IceSpell> FROST = register("frost", Affinity.GOOD, 0xBDBDF9, true, IceSpell::new);
    public static final SpellType<ScorchSpell> SCORCH = register("scorch", Affinity.BAD, 0, true, ScorchSpell::new);
    public static final SpellType<FireSpell> FLAME = register("flame", Affinity.GOOD, 0xFF5D00, true, FireSpell::new);
    public static final SpellType<InfernoSpell> INFERNAL = register("infernal", Affinity.BAD, 0xF00F00, true, InfernoSpell::new);
    public static final SpellType<ShieldSpell> SHIELD = register("shield", Affinity.GOOD, 0x66CDAA, true, ShieldSpell::new);
    public static final SpellType<ShieldSpell> REPULSE = register("repulse", Affinity.BAD, 0x66CDAA, true, ShieldSpell::new);
    public static final SpellType<AttractiveSpell> VORTEX = register("vortex", Affinity.GOOD, 0x4CDEE7, true, AttractiveSpell::new);
    public static final SpellType<AttractiveSpell> SUFFER = register("suffer", Affinity.BAD, 0x4CDEE7, true, AttractiveSpell::new);
    public static final SpellType<NecromancySpell> NECROMANCY = register("necromancy", Affinity.BAD, 0x8A3A3A, true, NecromancySpell::new);
    public static final SpellType<SiphoningSpell> SIPHONING = register("siphoning", Affinity.GOOD, 0xe308ab, true, SiphoningSpell::new);
    public static final SpellType<SiphoningSpell> DRAINING = register("draining", Affinity.BAD, 0xe308ab, true, SiphoningSpell::new);
    public static final SpellType<DisguiseSpell> DISGUISE = register("disguise", Affinity.BAD, 0x19E48E, false, DisguiseSpell::new);
    public static final SpellType<RevealingSpell> REVEALING = register("reveal", Affinity.GOOD, 0x5CE81F, true, RevealingSpell::new);
    public static final SpellType<JoustingSpell> JOUSTING = register("joust", Affinity.GOOD, 0xBDBDF9, false, JoustingSpell::new);
    public static final SpellType<AwkwardSpell> AWKWARD = register("awkward", Affinity.GOOD, 0xE1239C, true, AwkwardSpell::new);
    public static final SpellType<TransformationSpell> TRANSFORMATION = register("transformation", Affinity.NEUTRAL, 0x3A59AA, true, TransformationSpell::new);

    private final Identifier id;
    private final Affinity affinity;
    private int color;
    private final boolean obtainable;

    private final Factory<T> factory;

    private final boolean thrown;
    private final boolean attached;

    @Nullable
    private String translationKey;

    private SpellType(Identifier id, Affinity affinity, int color, boolean obtainable, Factory<T> factory) {
        this.id = id;
        this.affinity = affinity;
        this.color = color;
        this.obtainable = obtainable;
        this.factory = factory;

        Spell inst = create();
        thrown = SpellPredicate.IS_THROWN.test(inst);
        attached = SpellPredicate.IS_ATTACHED.test(inst);
    }

    public boolean isObtainable() {
        return obtainable;
    }

    public boolean mayThrow() {
        return thrown;
    }

    public boolean mayAttach() {
        return attached;
    }

    public Identifier getId() {
        return id;
    }

    /**
     * Gets the tint for this spell when applied to a gem.
     */
    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    @Override
    public Affinity getAffinity() {
        return affinity;
    }

    public String getTranslationKey() {
        if (translationKey == null) {
            translationKey = Util.createTranslationKey(getAffinity().getTranslationKey(), getId());
        }
        return translationKey;
    }

    public Text getName() {
        return new TranslatableText(getTranslationKey());
    }

    @Nullable
    public T create() {
        try {
            return factory.create(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Nullable
    public T apply(Caster<?> caster) {
        if (isEmpty()) {
            caster.setSpell(null);
            return null;
        }

        T spell = create();
        if (spell.apply(caster)) {
            return spell;
        }

        return null;
    }

    @Override
    public boolean test(@Nullable Spell spell) {
        return spell != null && spell.getType() == this;
    }

    public boolean isEmpty() {
        return this == EMPTY_KEY;
    }

    public static <T extends Spell> SpellType<T> register(Identifier id, Affinity affinity, int color, boolean obtainable, Factory<T> factory) {
        SpellType<T> type = new SpellType<>(id, affinity, color, obtainable, factory);
        byAffinity(affinity).add(type);
        Registry.register(REGISTRY, id, type);
        return type;
    }

    public static <T extends Spell> SpellType<T> register(String name, Affinity affinity, int color, boolean obtainable, Factory<T> factory) {
        return register(new Identifier("unicopia", name), affinity, color, obtainable, factory);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Spell> SpellType<T> empty() {
        return (SpellType<T>)EMPTY_KEY;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Spell> SpellType<T> getKey(Identifier id) {
        return (SpellType<T>)(EMPTY_ID.equals(id) ? EMPTY_KEY : REGISTRY.getOrEmpty(id).orElse(EMPTY_KEY));
    }

    public static SpellType<?> random(Random random) {
        return REGISTRY.getRandom(random);
    }

    public static Set<SpellType<?>> byAffinity(Affinity affinity) {
        return BY_AFFINITY.computeIfAbsent(affinity, a -> new HashSet<>());
    }

    @Nullable
    public static Spell fromNBT(@Nullable NbtCompound compound) {
        if (compound != null && compound.contains("effect_id")) {
            Spell effect = getKey(new Identifier(compound.getString("effect_id"))).create();

            if (effect != null) {
                effect.fromNBT(compound);
            }

            return effect;
        }

        return null;
    }

    public static NbtCompound toNBT(Spell effect) {
        NbtCompound compound = effect.toNBT();

        compound.putString("effect_id", effect.getType().getId().toString());

        return compound;
    }

    public interface Factory<T extends Spell> {
        T create(SpellType<T> type);
    }
}
