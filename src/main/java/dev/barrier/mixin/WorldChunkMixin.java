package dev.barrier.mixin;

import dev.barrier.BarrierClient;
import net.minecraft.block.Blocks;
import net.minecraft.block.state.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldChunk.class)
abstract class WorldChunkMixin {
  @Inject(method = "setBlockState", at = @At("RETURN"))
  private void barrier$changed(
      BlockPos pos, BlockState state, CallbackInfoReturnable<BlockState> ci) {
    WorldChunk chunk = (WorldChunk) (Object) this;
    BlockState previous = ci.getReturnValue();
    if (!chunk.getWorld().isClient || previous == null) return;
    boolean wasBarrier = previous.getBlock() == Blocks.BARRIER;
    boolean isBarrier = state.getBlock() == Blocks.BARRIER;
    if (wasBarrier == isBarrier) return;
    BarrierClient client = BarrierClient.get();
    if (client != null) client.blockChanged(chunk.getWorld(), pos, isBarrier);
  }
}
