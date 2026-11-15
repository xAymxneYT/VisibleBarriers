package dev.barrier;

import dev.barrier.config.BarrierConfig;
import dev.barrier.render.BarrierRenderer;
import dev.barrier.scan.BarrierCache;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class BarrierClient implements ClientModInitializer {
  private static BarrierClient instance;
  private final BarrierCache cache = new BarrierCache();
  private final BarrierRenderer renderer = new BarrierRenderer();
  private BarrierConfig config;
  private Thread clientThread;

  @Override
  public void onInitializeClient() {
    instance = this;
    clientThread = Thread.currentThread();
    config = new BarrierConfig(this);
  }

  public static BarrierClient get() {
    return instance;
  }

  public BarrierConfig config() {
    return config;
  }

  public boolean replacesVanillaBarriers() {
    Minecraft mc = Minecraft.getInstance();
    return config != null
        && config.enabled
        && mc.world != null
        && (!config.singleplayerOnly || mc.isSingleplayer());
  }

  public void toggle() {
    Minecraft mc = Minecraft.getInstance();
    if (mc.world == null || mc.screen != null || !mc.focused) return;
    config.enabled = !config.enabled;
    config.save();
  }

  public void tick() {
    Minecraft mc = Minecraft.getInstance();
    if (mc.world == null || mc.player == null || !config.enabled) {
      cache.clear();
      renderer.clear();
      return;
    }
    cache.tick(mc.world, mc.player.x, mc.player.y, mc.player.z, config);
  }

  public void blockChanged(World world, BlockPos pos, boolean isBarrier) {
    Minecraft mc = Minecraft.getInstance();
    if (world != mc.world || config == null || !config.enabled || mc.player == null) return;
    BlockPos immutable = new BlockPos(pos.getX(), pos.getY(), pos.getZ());
    if (Thread.currentThread() != clientThread) {
      mc.executeTask(
          () -> {
            blockChanged(world, immutable, isBarrier);
            return null;
          });
      return;
    }
    cache.blockChanged(mc.world, immutable, isBarrier);
  }

  public void render(float tickDelta) {
    Minecraft mc = Minecraft.getInstance();
    if (config == null || !config.enabled || mc.world == null || mc.player == null) return;
    if (config.singleplayerOnly && !mc.isSingleplayer()) return;
    if (config.hideInMenus && mc.screen != null) return;
    if (config.hideWithGui && !mc.isDisplayGui()) return;
    if (!cache.isWorld(mc.world)) return;
    renderer.update(mc, cache, config);
    renderer.render(mc, config, tickDelta);
  }
}
