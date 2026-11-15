package dev.barrier.mesh;

import java.util.List;

public record BarrierMesh(List<Quad> faces, List<Line> outlines) {
  public static final BarrierMesh EMPTY = new BarrierMesh(List.of(), List.of());

  public record Quad(int face, int plane, int u, int v, int width, int height) {
    public boolean facesCamera(double x, double y, double z) {
      return switch (face) {
        case 0 -> y <= plane;
        case 1 -> y >= plane;
        case 2 -> z <= plane;
        case 3 -> z >= plane;
        case 4 -> x <= plane;
        case 5 -> x >= plane;
        default -> false;
      };
    }

    public int area() {
      return width * height;
    }
  }

  public record Line(int axis, int start, int a, int b, int length, int faces) {
    public boolean facesCamera(double x, double y, double z) {
      double px = axis == 0 ? start : a;
      double py = axis == 1 ? start : axis == 0 ? a : b;
      double pz = axis == 2 ? start : b;
      return ((faces & 1) != 0 && y <= py)
          || ((faces & 2) != 0 && y >= py)
          || ((faces & 4) != 0 && z <= pz)
          || ((faces & 8) != 0 && z >= pz)
          || ((faces & 16) != 0 && x <= px)
          || ((faces & 32) != 0 && x >= px);
    }
  }
}
