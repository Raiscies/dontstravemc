package com.mixin.client;

import com.dontstravemc.crafting.client.TechCraftingScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractRecipeBookScreen<InventoryMenu> {
    
    @Shadow protected int leftPos;
    @Shadow protected int topPos;

    public InventoryScreenMixin(InventoryMenu menu, Component title) {
        super(menu, null, null, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addTechCraftingButton(CallbackInfo ci) {
        int x = this.leftPos + 77;
        int y = this.topPos + 30;

        this.addRenderableWidget(Button.builder(Component.literal("T"), (button) -> {
            Minecraft.getInstance().setScreen(new TechCraftingScreen());
        }).bounds(x, y, 20, 20).build());
    }
}
