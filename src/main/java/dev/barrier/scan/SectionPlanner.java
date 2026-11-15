package dev.barrier.scan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SectionPlanner {
  private SectionPlanner() {}

  public static List<SectionPos> around(double x, double y, double z, int radius, int height) {
    int minX = (int) Math.floor(x - radius) >> 4, maxX = (int) Math.floor(x + radius) >> 4;
    int minZ = (int) Math.floor(z - radius) >> 4, maxZ = (int) Math.floor(z + radius) >> 4;
    int minY = Math.max(0, Math.min(15, (int) Math.floor(y - height) >> 4));
    int maxY = Math.max(0, Math.min(15, (int) Math.floor(y + height) >> 4));
    List<SectionPos> result = new ArrayList<>();
    for (int sx = minX; sx <= maxX; sx++)
      for (int sz = minZ; sz <= maxZ; sz++)
        for (int sy = minY; sy <= maxY; sy++) result.add(new SectionPos(sx, sy, sz));
    result.sort(Comparator.comparingDouble(p -> p.distanceSquared(x, y, z)));
    return result;
  }
}
