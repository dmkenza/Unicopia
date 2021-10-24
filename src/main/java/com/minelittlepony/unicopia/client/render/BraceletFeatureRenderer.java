package com.minelittlepony.unicopia.client.render;

import com.minelittlepony.common.util.Color;
import com.minelittlepony.unicopia.client.render.BraceletFeatureRenderer.BraceletModel;
import com.minelittlepony.unicopia.item.FriendshipBraceletItem;
import com.minelittlepony.unicopia.item.GlowableItem;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;

public class BraceletFeatureRenderer<E extends LivingEntity> implements AccessoryFeatureRenderer.Feature<E> {

    private static final Identifier TEXTURE = new Identifier("unicopia", "textures/models/armor/bracelet.png");

    private final BraceletModel steveModel;
    private final BraceletModel alexModel;

    private final FeatureRendererContext<E, ? extends BipedEntityModel<E>> context;

    public BraceletFeatureRenderer(FeatureRendererContext<E, ? extends BipedEntityModel<E>> context) {
        this.context = context;
        Dilation dilation = new Dilation(0.3F);
        steveModel = new BraceletModel(BraceletModel.getData(dilation, false).createModel(), false);
        alexModel = new BraceletModel(BraceletModel.getData(dilation, true).createModel(), true);
    }

    @Override
    public void render(MatrixStack stack, VertexConsumerProvider renderContext, int lightUv, E entity, float limbDistance, float limbAngle, float tickDelta, float age, float headYaw, float headPitch) {

        ItemStack item = entity.getEquippedStack(EquipmentSlot.CHEST);

        if (item.getItem() instanceof FriendshipBraceletItem) {


            int j = ((DyeableItem)item.getItem()).getColor(item);

            boolean alex = entity instanceof ClientPlayerEntity && ((ClientPlayerEntity)entity).getModel().startsWith("slim");

            BraceletModel model = alex ? alexModel : steveModel;

            if (entity instanceof ArmorStandEntity) {
                ModelPart arm = entity.getMainArm() == Arm.LEFT ? context.getModel().leftArm : context.getModel().rightArm;
                arm.visible = true;
                VertexConsumer consumer = renderContext.getBuffer(context.getModel().getLayer(context.getTexture(entity)));
                arm.render(stack, consumer, lightUv, OverlayTexture.DEFAULT_UV, 1, 1, 1, 1);
            }

            boolean glowing = ((GlowableItem)item.getItem()).isGlowing(item);

            VertexConsumer consumer = ItemRenderer.getArmorGlintConsumer(renderContext, RenderLayer.getArmorCutoutNoCull(TEXTURE), false, false);

            model.setAngles(context.getModel());
            model.setVisible(entity.getMainArm());
            model.render(stack, consumer, glowing ? 0x0F00F0 : lightUv, OverlayTexture.DEFAULT_UV, Color.r(j), Color.g(j), Color.b(j), 1);
        }
    }

    static class BraceletModel extends Model {

        private final ModelPart leftArm;
        private final ModelPart rightArm;

        private final boolean alex;

        public BraceletModel(ModelPart tree, boolean alex) {
            super(RenderLayer::getEntityTranslucent);
            this.alex = alex;
            this.leftArm = tree.getChild(EntityModelPartNames.LEFT_ARM);
            this.rightArm = tree.getChild(EntityModelPartNames.RIGHT_ARM);
        }

        public static TexturedModelData getData(Dilation dilation, boolean alex) {
            ModelData data = new ModelData();
            ModelPartData root = data.getRoot();

            root.addChild(EntityModelPartNames.RIGHT_ARM, ModelPartBuilder.create().uv(0, alex ? 6 : 0).cuboid(-3, 7, -2, alex ? 3 : 4, 2, 4, dilation), ModelTransform.NONE);
            root.addChild(EntityModelPartNames.LEFT_ARM,  ModelPartBuilder.create().mirrored().uv(0, alex ? 6 : 0).cuboid(-1, 7, -2, alex ? 3 : 4, 2, 4, dilation), ModelTransform.NONE);

            return TexturedModelData.of(data, 64, 64);
        }

        public void setAngles(BipedEntityModel<?> biped) {
            leftArm.copyTransform(biped.leftArm);
            rightArm.copyTransform(biped.rightArm);
            if (alex) {
                rightArm.pivotX++;
            }
        }

        public void setVisible(Arm arm) {
            leftArm.visible = arm == Arm.LEFT;
            rightArm.visible = arm == Arm.RIGHT;
        }

        @Override
        public void render(MatrixStack matrixStack, VertexConsumer vertexConsumer, int i, int j, float f, float g, float h, float k) {
            leftArm.render(matrixStack, vertexConsumer, i, j, f, g, h, k);
            rightArm.render(matrixStack, vertexConsumer, i, j, f, g, h, k);
        }
    }

}
