package dev.barrier.scan;

public final class SectionPos {
  public final int x, y, z;

  public SectionPos(int x, int y, int z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public long key() {
    return ((long) x & 0x3FFFFFFL) << 30 | ((long) z & 0x3FFFFFFL) << 4 | y;
  }

  public double distanceSquared(double px, double py, double pz) {
    double dx = x * 16.0 + 8 - px, dy = y * 16.0 + 8 - py, dz = z * 16.0 + 8 - pz;
    return dx * dx + dy * dy + dz * dz;
  }
}
