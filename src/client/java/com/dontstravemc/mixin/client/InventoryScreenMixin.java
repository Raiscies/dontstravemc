package com.dontstravemc.mixin.client;

import com.dontstravemc.crafting.TechCraftingScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends Screen {

    protected InventoryScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addTechCraftingButton(CallbackInfo ci) {
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) this;
        int x = accessor.getLeftPos() + 77;
        int y = accessor.getTopPos() + 30;

        this.addRenderableWidget(Button.builder(Component.literal("T"), (button) -> {
            Minecraft.getInstance().setScreen(new TechCraftingScreen());
        }).bounds(x, y, 20, 20).build());
    }
}
