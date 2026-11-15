package dev.barrier.scan;

import dev.barrier.mesh.Voxel;
import java.util.*;

public final class BarrierIndex {
  private final int limit;
  private final Map<Long, List<Voxel>> sections = new HashMap<>();
  private int count;
  private long revision, edits;

  public BarrierIndex(int limit) {
    this.limit = limit;
  }

  public long revision() {
    return revision;
  }

  public long edits() {
    return edits;
  }

  public int count() {
    return count;
  }

  public Collection<List<Voxel>> sections() {
    return sections.values();
  }

  public int available(long key) {
    return limit - count + sections.getOrDefault(key, List.of()).size();
  }

  public boolean replace(long key, List<Voxel> blocks) {
    List<Voxel> old = sections.getOrDefault(key, List.of());
    List<Voxel> next = List.copyOf(blocks.subList(0, Math.min(blocks.size(), available(key))));
    if (old.equals(next)) return false;
    count += next.size() - old.size();
    if (next.isEmpty()) sections.remove(key);
    else sections.put(key, next);
    revision++;
    return true;
  }

  public boolean edit(Voxel pos, boolean barrier) {
    if (pos.y() < 0 || pos.y() > 255) return false;
    long key = new SectionPos(pos.x() >> 4, pos.y() >> 4, pos.z() >> 4).key();
    List<Voxel> next = new ArrayList<>(sections.getOrDefault(key, List.of()));
    if (barrier) {
      if (count >= limit || next.contains(pos)) return false;
      next.add(pos);
    } else if (!next.remove(pos)) return false;
    next.sort(
        Comparator.comparingInt(Voxel::y).thenComparingInt(Voxel::z).thenComparingInt(Voxel::x));
    replace(key, next);
    edits++;
    return true;
  }

  public void retain(Set<Long> keys) {
    Iterator<Map.Entry<Long, List<Voxel>>> iterator = sections.entrySet().iterator();
    boolean changed = false;
    while (iterator.hasNext()) {
      Map.Entry<Long, List<Voxel>> entry = iterator.next();
      if (!keys.contains(entry.getKey())) {
        count -= entry.getValue().size();
        iterator.remove();
        changed = true;
      }
    }
    if (changed) revision++;
  }

  public void clear() {
    if (!sections.isEmpty()) revision++;
    sections.clear();
    count = 0;
  }
}
