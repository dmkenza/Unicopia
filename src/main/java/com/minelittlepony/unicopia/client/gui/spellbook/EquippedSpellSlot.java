package com.minelittlepony.unicopia.client.gui.spellbook;

import java.util.List;

import com.minelittlepony.common.client.gui.element.Button;
import com.minelittlepony.unicopia.ability.magic.spell.effect.CustomisedSpellType;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vector4f;

public class EquippedSpellSlot extends Button {

    protected final ItemRenderer itemRenderer = MinecraftClient.getInstance().getItemRenderer();

    private final CustomisedSpellType<?> spell;

    public EquippedSpellSlot(int x, int y, CustomisedSpellType<?> spell) {
        super(x, y, 16, 16);
        this.spell = spell;
        getStyle().setTooltip(() -> {
            if (spell.isEmpty()) {
                return List.of();
            }
            return spell.getDefaultStack().getTooltip(MinecraftClient.getInstance().player, TooltipContext.Default.NORMAL);
        });
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float tickDelta) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, SpellbookScreen.SLOT);
        RenderSystem.enableBlend();

        drawTexture(matrices, x - 8, y - 8, 0, 0, 32, 32, 32, 32);

        Vector4f pos = new Vector4f(x, y, 0, 1);
        pos.transform(matrices.peek().getPositionMatrix());

        if (spell.isEmpty()) {
            RenderSystem.setShaderColor(1, 1, 1, 0.3F);
            RenderSystem.setShaderTexture(0, SpellbookScreen.GEM);
            drawTexture(matrices, x, y, 0, 0, 16, 16, 16, 16);
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1, 1, 1, 1);
        } else {
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            drawItem((int)pos.getX(), (int)pos.getY());
        }
        if (isHovered()) {
            HandledScreen.drawSlotHighlight(matrices, x, y, 0);
            this.onPress();
        }
    }

    protected void drawItem(int x, int y) {
        itemRenderer.renderInGui(spell.getDefaultStack(), x, y);
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }
}
