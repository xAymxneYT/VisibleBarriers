package dev.barrier.scan;

import dev.barrier.config.BarrierConfig;
import dev.barrier.mesh.Voxel;
import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.WorldChunkSection;

public final class BarrierCache {
  private static final int MAX_CACHED_BLOCKS = 65536;
  private final BarrierIndex index = new BarrierIndex(MAX_CACHED_BLOCKS);
  private final Set<Long> scheduled = new HashSet<>();
  private List<SectionPos> plan = Collections.emptyList();
  private ClientWorld world;
  private int cursor;
  private int lastX = Integer.MIN_VALUE, lastY, lastZ, lastRange, lastHeight;

  public void clear() {
    index.clear();
    scheduled.clear();
    plan = Collections.emptyList();
    world = null;
    cursor = 0;
    lastX = Integer.MIN_VALUE;
  }

  public boolean isWorld(ClientWorld current) {
    return world == current;
  }

  public long revision() {
    return index.revision();
  }

  public long editRevision() {
    return index.edits();
  }

  public void blockChanged(ClientWorld current, BlockPos pos, boolean barrier) {
    if (world != current || pos.getY() < 0 || pos.getY() > 255) return;
    long key = new SectionPos(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4).key();
    if (!scheduled.contains(key)) return;
    index.edit(new Voxel(pos.getX(), pos.getY(), pos.getZ()), barrier);
  }

  public Collection<List<Voxel>> sections() {
    return index.sections();
  }

  public void tick(ClientWorld current, double x, double y, double z, BarrierConfig config) {
    if (world != current) {
      clear();
      world = current;
    }
    int range = clamp(config.range, 4, 64), height = clamp(config.verticalRange, 4, 64);

    int bx = (int) Math.floor(x), by = (int) Math.floor(y), bz = (int) Math.floor(z);
    if (bx != lastX || by != lastY || bz != lastZ || range != lastRange || height != lastHeight) {
      plan = SectionPlanner.around(x, y, z, range, height);
      scheduled.clear();
      for (SectionPos p : plan) scheduled.add(p.key());
      index.retain(scheduled);

      cursor = plan.isEmpty() ? 0 : cursor % plan.size();
      lastX = bx;
      lastY = by;
      lastZ = bz;
      lastRange = range;
      lastHeight = height;
    }
    if (plan.isEmpty()) return;
    int budget = clamp(config.sectionsPerTick, 1, 32);
    long deadline =
        System.nanoTime() + (long) (Math.max(0.25f, Math.min(4, config.scanBudgetMs)) * 1_000_000);
    for (int i = 0; i < budget; i++) {
      scan(plan.get(cursor));
      cursor = (cursor + 1) % plan.size();
      if (cursor == 0 || System.nanoTime() >= deadline) break;
    }
  }

  private void scan(SectionPos p) {
    if (!world.getChunkSource().hasChunk(p.x, p.z)) {
      index.replace(p.key(), List.of());
      return;
    }
    WorldChunk chunk = world.getChunkAt(p.x, p.z);
    WorldChunkSection section = chunk.getSections()[p.y];
    if (section == null || section.isEmpty()) {
      index.replace(p.key(), List.of());
      return;
    }
    char[] states = section.getBlockStates();
    int barrierId = Block.getId(Blocks.BARRIER);
    List<Voxel> found = new ArrayList<>();
    int available = index.available(p.key());

    for (int index = 0; index < states.length && found.size() < available; index++) {
      if ((states[index] >>> 4) == barrierId) {
        found.add(
            new Voxel(
                (p.x << 4) + (index & 15),
                (p.y << 4) + (index >> 8),
                (p.z << 4) + ((index >> 4) & 15)));
      }
    }
    index.replace(p.key(), found);
  }

  private static int clamp(int n, int min, int max) {
    return Math.max(min, Math.min(max, n));
  }
}
