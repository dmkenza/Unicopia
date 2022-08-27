package com.minelittlepony.unicopia.client.particle;

import java.util.Optional;

import com.minelittlepony.common.util.Color;
import com.minelittlepony.unicopia.Unicopia;
import com.minelittlepony.unicopia.ability.magic.Caster;
import com.minelittlepony.unicopia.particle.OrientedBillboardParticleEffect;
import com.minelittlepony.unicopia.particle.ParticleHandle.Attachment;
import com.minelittlepony.unicopia.particle.ParticleHandle.Link;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.util.math.Vec3f;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;

public class RunesParticle extends OrientedBillboardParticle implements Attachment {

    private static final Identifier[] TEXTURES = new Identifier[] {
            Unicopia.id("textures/particles/runes_0.png"),
            Unicopia.id("textures/particles/runes_1.png"),
            Unicopia.id("textures/particles/runes_2.png"),
            Unicopia.id("textures/particles/runes_3.png"),
            Unicopia.id("textures/particles/runes_4.png"),
            Unicopia.id("textures/particles/runes_5.png")
    };

    protected float prevBaseSize = 0;
    protected float baseSize = 0;

    private float prevRotationAngle;
    private float rotationAngle;

    private Optional<Link> link = Optional.empty();

    private int stasisAge = -1;

    public RunesParticle(OrientedBillboardParticleEffect effect, ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
        super(effect, world, x, y, z, velocityX, velocityY, velocityZ);
        setMaxAge(70);

        red = world.random.nextFloat();
        green = world.random.nextFloat();
        blue = world.random.nextFloat();
    }

    @Override
    public boolean isStillAlive() {
        return age < (maxAge - 1);
    }

    @Override
    public void attach(Link link) {
        this.link = Optional.of(link);
        velocityX = 0;
        velocityY = 0;
        velocityZ = 0;
        Vec3d pos = link.get().map(Caster::getOriginVector).orElse(Vec3d.ZERO);
        setPos(pos.x, pos.y, pos.z);
    }

    @Override
    public void detach() {
        link = Optional.empty();
    }

    @Override
    public void setAttribute(int key, Object value) {
        if (key == 1) {
            int tint = (int)value;
            red = Color.r(tint);
            green = Color.g(tint);
            blue = Color.b(tint);
        }
    }
    @Override
    public float getScale(float tickDelta) {
       return MathHelper.lerp(tickDelta, prevBaseSize, baseSize) * super.getScale(tickDelta);
    }

    @Override
    protected Identifier getTexture() {
        return TEXTURES[0];
    }

    private float getAlphaScale() {
        float transitionScale = age < maxAge / 2 ? 5 : 3;
        return (float)Math.min(1, Math.sin(Math.PI * age / maxAge) * transitionScale);
    }

    @Override
    protected int getBrightness(float tint) {
        return 0xF000F0;
    }

    @Override
    protected void renderQuads(Tessellator te, BufferBuilder buffer, float x, float y, float z, float tickDelta) {

        float alpha = this.alpha * getAlphaScale();

        float angle = MathHelper.lerp(tickDelta, prevRotationAngle, rotationAngle);

        for (int i = 0; i < TEXTURES.length; i++) {
            RenderSystem.setShaderTexture(0, TEXTURES[i]);
            RenderSystem.setShaderColor(red, green, blue, alpha);

            Vec3f[] corners = new Vec3f[]{
                    new Vec3f(-1, -1, 0),
                    new Vec3f(-1,  1, 0),
                    new Vec3f( 1,  1, 0),
                    new Vec3f( 1, -1, 0)
            };
            float scale = getScale(tickDelta);

            float ringSpeed = (i % 2 == 0 ? i : -1) * i;

            Quaternion ringAngle = Vec3f.POSITIVE_Z.getDegreesQuaternion(angle * ringSpeed);

            for(int k = 0; k < 4; ++k) {
               Vec3f corner = corners[k];
               corner.rotate(ringAngle);
               corner.rotate(rotation);
               corner.scale(scale);
               corner.add(x, y + 0.001F, z);
            }

            renderQuad(te, buffer, corners, alpha, tickDelta);
        }
    }

    @Override
    public void tick() {
        super.tick();

        link.flatMap(Link::get).map(Caster::getEntity).ifPresentOrElse(e -> {
            if (getAlphaScale() >= 0.9F) {
                if (stasisAge < 0) {
                    stasisAge = age;
                }
                age = stasisAge;
            }
        }, this::detach);

        prevBaseSize = baseSize;
        if (baseSize < 3) {
            baseSize++;
        }

        prevRotationAngle = rotationAngle;
        rotationAngle = MathHelper.wrapDegrees(rotationAngle + 0.3F);
    }
}
