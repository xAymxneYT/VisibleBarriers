package dev.barrier.mesh;

public record Voxel(int x, int y, int z) {
  public Voxel offset(int dx, int dy, int dz) {
    return new Voxel(x + dx, y + dy, z + dz);
  }
}
