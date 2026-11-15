package dev.barrier.mixin;

import dev.barrier.BarrierClient;
import net.minecraft.client.entity.particle.BarrierParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BarrierParticle.class)
abstract class BarrierParticleMixin {
  @Inject(method = "render", at = @At("HEAD"), cancellable = true)
  private void barrier$hideExistingVanillaMarker(CallbackInfo ci) {
    BarrierClient client = BarrierClient.get();
    if (client != null && client.replacesVanillaBarriers()) ci.cancel();
  }
}
