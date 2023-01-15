package com.minelittlepony.unicopia.client.particle;

import com.minelittlepony.unicopia.particle.OrientedBillboardParticleEffect;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Vec3f;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Quaternion;

public abstract class OrientedBillboardParticle extends AbstractBillboardParticle {

    protected boolean fixed;
    protected Quaternion rotation = new Quaternion(0, 0, 0, 1);

    public OrientedBillboardParticle(OrientedBillboardParticleEffect effect, ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);

        fixed = effect.isAngleFixed();
        if (fixed) {
            rotation.hamiltonProduct(Vec3f.POSITIVE_Y.getDegreesQuaternion(effect.getPitch()));
            rotation.hamiltonProduct(Vec3f.POSITIVE_X.getDegreesQuaternion(180 - effect.getYaw()));
        }
    }

    @Override
    public void buildGeometry(VertexConsumer drawer, Camera camera, float tickDelta) {
        if (!fixed) {
            rotation = camera.getRotation();
        }
        super.buildGeometry(drawer, camera, tickDelta);
    }

    @Override
    protected void renderQuads(Tessellator te, BufferBuilder buffer, float x, float y, float z, float tickDelta) {
        Vec3f[] corners = new Vec3f[]{
                new Vec3f(-1, -1, 0),
                new Vec3f(-1,  1, 0),
                new Vec3f( 1,  1, 0),
                new Vec3f( 1, -1, 0)
        };
        float scale = getScale(tickDelta);

        for(int k = 0; k < 4; ++k) {
           Vec3f corner = corners[k];
           corner.rotate(rotation);
           corner.scale(scale);
           corner.add(x, y, z);
        }

        float alpha = this.alpha * (1 - ((float)age / maxAge));

        renderQuad(te, buffer, corners, alpha, tickDelta);
    }

}
