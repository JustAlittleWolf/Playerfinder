package me.wolfii.playerfinder.mixin;

import me.wolfii.playerfinder.PlayerFinder;
import me.wolfii.playerfinder.Config;
import me.wolfii.playerfinder.render.Rendermode;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(method = "renderHitbox", at = @At("HEAD"), cancellable = true)
    private static void checkCancelRenderDefaultHiboxes(MatrixStack matrices, VertexConsumer vertices, Entity entity, float tickDelta, CallbackInfo ci) {
        if (Config.hideVanillaHitboxes && PlayerFinder.rendermode != Rendermode.NONE) ci.cancel();
    }
}
