package com.aether.client.gui.screen.inventory;

import com.aether.Aether;
import com.aether.inventory.container.IncubatorContainer;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class IncubatorScreen extends ContainerScreen<IncubatorContainer> {
	private static final ResourceLocation INCUBATOR_GUI_TEXTURES = new ResourceLocation(Aether.MODID, "textures/gui/container/incubator.png");
	
	public IncubatorScreen(IncubatorContainer container, PlayerInventory inventory, ITextComponent name) {
		super(container, inventory, name);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
	      this.minecraft.getTextureManager().bindTexture(INCUBATOR_GUI_TEXTURES);
	      int i = this.guiLeft;
	      int j = this.guiTop;
	      this.blit(i, j, 0, 0, this.xSize, this.ySize);
	      if (this.container.isIncubating()) {
	         int k = this.container.getFreezingTimeRemaining();
	         this.blit(i + 56, j + 36 + 12 - k, 176, 12 - k, 14, k + 2);
	      }

	      int l = this.container.getProgressionScaled();
	      this.blit(i + 79, j + 34, 176, 14, l + 1, 16);
	}
}
