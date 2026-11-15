package dev.barrier.mesh;

import dev.barrier.mesh.BarrierMesh.Line;
import dev.barrier.mesh.BarrierMesh.Quad;
import java.util.*;
import java.util.concurrent.CancellationException;

public final class MeshBuilder {
  private static final int[][] DIRECTIONS = {
    {0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}
  };

  private MeshBuilder() {}

  public static BarrierMesh build(Set<Voxel> blocks, boolean connected) {
    if (blocks.isEmpty()) return BarrierMesh.EMPTY;
    List<Quad> faces = new ArrayList<>();
    List<Line> lines = new ArrayList<>();
    Map<Plane, Set<Long>> planes =
        new TreeMap<>(Comparator.comparingInt(Plane::face).thenComparingInt(Plane::coordinate));
    Set<UnitEdge> candidates = new HashSet<>();
    for (Voxel p : blocks) {
      if (Thread.currentThread().isInterrupted()) throw new CancellationException();
      for (int face = 0; face < 6; face++) {
        int[] d = DIRECTIONS[face];
        if (blocks.contains(p.offset(d[0], d[1], d[2]))) continue;
        Quad q = face(p, face);
        if (!connected) faces.add(q);
        planes
            .computeIfAbsent(new Plane(face, q.plane()), key -> new HashSet<>())
            .add(cell(q.u(), q.v()));
      }
      Collections.addAll(candidates, edges(p));
    }
    if (connected)
      for (Map.Entry<Plane, Set<Long>> entry : planes.entrySet())
        mergeFaces(entry.getKey(), entry.getValue(), faces);

    Map<LineKey, SortedSet<Integer>> segments = new HashMap<>();
    for (UnitEdge e : candidates) {
      int mask = edgeFaces(e, blocks, connected);
      if (mask != 0 && !connected) lines.add(new Line(e.axis, e.start, e.a, e.b, 1, mask));
      else if (mask != 0)
        segments
            .computeIfAbsent(new LineKey(e.axis, e.a, e.b, mask), key -> new TreeSet<>())
            .add(e.start);
    }
    for (Map.Entry<LineKey, SortedSet<Integer>> entry : segments.entrySet()) {
      int start = 0, end = 0;
      boolean first = true;
      LineKey key = entry.getKey();
      for (int point : entry.getValue()) {
        if (first) {
          start = point;
          end = point + 1;
          first = false;
        } else if (point == end) end++;
        else {
          lines.add(new Line(key.axis, start, key.a, key.b, end - start, key.faces));
          start = point;
          end = point + 1;
        }
      }
      if (!first) lines.add(new Line(key.axis, start, key.a, key.b, end - start, key.faces));
    }
    lines.sort(
        Comparator.comparingInt(Line::axis)
            .thenComparingInt(Line::a)
            .thenComparingInt(Line::b)
            .thenComparingInt(Line::start));
    return new BarrierMesh(List.copyOf(faces), List.copyOf(lines));
  }

  private static Quad face(Voxel p, int f) {
    return switch (f) {
      case 0 -> new Quad(f, p.y(), p.x(), p.z(), 1, 1);
      case 1 -> new Quad(f, p.y() + 1, p.x(), p.z(), 1, 1);
      case 2 -> new Quad(f, p.z(), p.x(), p.y(), 1, 1);
      case 3 -> new Quad(f, p.z() + 1, p.x(), p.y(), 1, 1);
      case 4 -> new Quad(f, p.x(), p.z(), p.y(), 1, 1);
      case 5 -> new Quad(f, p.x() + 1, p.z(), p.y(), 1, 1);
      default -> throw new IllegalArgumentException("Unknown face " + f);
    };
  }

  private static void mergeFaces(Plane plane, Set<Long> cells, List<Quad> output) {
    List<Long> ordered = new ArrayList<>(cells);
    ordered.sort(Comparator.comparingInt((Long c) -> v(c)).thenComparingInt(c -> u(c)));
    for (long origin : ordered) {
      if (!cells.contains(origin)) continue;
      int x = u(origin), y = v(origin), width = 1, height = 1;
      while (cells.contains(cell(x + width, y))) width++;
      boolean complete = true;
      while (complete) {
        for (int i = 0; i < width; i++) {
          if (!cells.contains(cell(x + i, y + height))) {
            complete = false;
            break;
          }
        }
        if (complete) height++;
      }
      for (int dy = 0; dy < height; dy++)
        for (int dx = 0; dx < width; dx++) cells.remove(cell(x + dx, y + dy));
      output.add(new Quad(plane.face, plane.coordinate, x, y, width, height));
    }
  }

  private static UnitEdge[] edges(Voxel p) {
    UnitEdge[] edges = new UnitEdge[12];
    int i = 0;
    for (int a = 0; a <= 1; a++)
      for (int b = 0; b <= 1; b++) {
        edges[i++] = new UnitEdge(0, p.x(), p.y() + a, p.z() + b);
        edges[i++] = new UnitEdge(1, p.y(), p.x() + a, p.z() + b);
        edges[i++] = new UnitEdge(2, p.z(), p.x() + a, p.y() + b);
      }
    return edges;
  }

  private static int edgeFaces(UnitEdge e, Set<Voxel> blocks, boolean creasesOnly) {
    int mask = 0;
    for (int a = 0; a <= 1; a++)
      for (int b = 0; b <= 1; b++) {
        Voxel p =
            switch (e.axis) {
              case 0 -> new Voxel(e.start, e.a - 1 + a, e.b - 1 + b);
              case 1 -> new Voxel(e.a - 1 + a, e.start, e.b - 1 + b);
              case 2 -> new Voxel(e.a - 1 + a, e.b - 1 + b, e.start);
              default -> throw new IllegalArgumentException();
            };
        if (blocks.contains(p)) mask |= 1 << (a * 2 + b);
      }
    int count = Integer.bitCount(mask);

    if (mask == 0 || mask == 15) return 0;
    if (creasesOnly && !(count == 1 || count == 3 || mask == 0b1001 || mask == 0b0110)) return 0;
    int faces = 0;
    for (int i = 0; i < 4; i++) {
      if ((mask & (1 << i)) == 0) continue;
      if ((mask & (1 << (i ^ 2))) == 0) {
        int negative = e.axis == 0 ? 0 : 4;
        faces |= 1 << (negative + ((i & 2) == 0 ? 1 : 0));
      }
      if ((mask & (1 << (i ^ 1))) == 0) {
        int negative = e.axis == 2 ? 0 : 2;
        faces |= 1 << (negative + ((i & 1) == 0 ? 1 : 0));
      }
    }
    return faces;
  }

  private static long cell(int u, int v) {
    return (long) v << 32 | (u & 0xFFFFFFFFL);
  }

  private static int u(long cell) {
    return (int) cell;
  }

  private static int v(long cell) {
    return (int) (cell >> 32);
  }

  private record Plane(int face, int coordinate) {}

  private record LineKey(int axis, int a, int b, int faces) {}

  private record UnitEdge(int axis, int start, int a, int b) {}
}
