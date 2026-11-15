package dev.barrier.mixin;

import dev.barrier.BarrierClient;
import net.minecraft.client.ParticleManager;
import net.minecraft.client.entity.particle.BarrierParticle;
import net.minecraft.client.entity.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
abstract class ParticleManagerMixin {
  @Inject(method = "add", at = @At("HEAD"), cancellable = true)
  private void barrier$skipVanillaMarker(Particle particle, CallbackInfo ci) {
    BarrierClient client = BarrierClient.get();
    if (particle instanceof BarrierParticle && client != null && client.replacesVanillaBarriers())
      ci.cancel();
  }
}
