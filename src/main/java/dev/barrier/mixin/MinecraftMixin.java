package dev.barrier.mixin;

import dev.barrier.BarrierClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {
  @Inject(method = "tick", at = @At("TAIL"))
  private void barrier$tick(CallbackInfo ci) {
    BarrierClient client = BarrierClient.get();
    if (client != null) client.tick();
  }
}
