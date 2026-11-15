package dev.barrier.mixin;

import dev.barrier.BarrierClient;
import net.minecraft.client.render.Culler;
import net.minecraft.client.render.world.WorldRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
abstract class WorldRendererMixin {
  @Inject(
      method = "renderEntities",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;prepare(Lnet/minecraft/world/World;Lnet/minecraft/client/render/TextRenderer;Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity;Lnet/minecraft/client/options/GameOptions;F)V",
              shift = At.Shift.AFTER))
  private void barrier$render(Entity camera, Culler culler, float tickDelta, CallbackInfo ci) {
    BarrierClient client = BarrierClient.get();
    if (client != null) client.render(tickDelta);
  }
}
