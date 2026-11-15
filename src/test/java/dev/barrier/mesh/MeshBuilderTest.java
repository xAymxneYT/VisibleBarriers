package dev.barrier.mesh;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class MeshBuilderTest {
  @Test
  void singleBlockHasSixFacesAndTwelveEdges() {
    BarrierMesh mesh = MeshBuilder.build(Set.of(new Voxel(0, 0, 0)), true);
    assertEquals(6, mesh.faces().size());
    assertEquals(12, mesh.outlines().size());
  }

  @Test
  void largeSolidGroupBecomesSixFacesAndTwelveContinuousEdges() {
    Set<Voxel> blocks = cuboid(32, 8, 8, 0, 0, 0);
    BarrierMesh mesh = MeshBuilder.build(blocks, true);
    assertEquals(2048, blocks.size());
    assertEquals(6, mesh.faces().size());
    assertEquals(12, mesh.outlines().size());
    assertEquals(1152, area(mesh));
    assertTrue(mesh.faces().stream().anyMatch(q -> q.width() > 1 && q.height() > 1));
  }

  @Test
  void flatWallHasNoGridOfOutlineSegments() {
    BarrierMesh mesh = MeshBuilder.build(cuboid(16, 16, 1, 0, 0, 0), true);
    assertEquals(6, mesh.faces().size());
    assertEquals(12, mesh.outlines().size());
    assertEquals(576, area(mesh));
  }

  @Test
  void offKeepsSurfaceGridWithoutInternalOrDuplicateGeometry() {
    BarrierMesh mesh = MeshBuilder.build(cuboid(4, 4, 4, 0, 0, 0), false);
    assertEquals(96, mesh.faces().size());
    assertEquals(192, mesh.outlines().size());
    assertEquals(mesh.outlines().size(), new HashSet<>(mesh.outlines()).size());
  }

  @Test
  void concaveCornerKeepsItsCreaseWithoutFlatSeams() {
    BarrierMesh mesh =
        MeshBuilder.build(Set.of(new Voxel(0, 0, 0), new Voxel(1, 0, 0), new Voxel(0, 0, 1)), true);
    assertEquals(14, area(mesh));
    assertEquals(10, mesh.faces().size());
    assertEquals(18, mesh.outlines().size());
    assertTrue(
        mesh.outlines().stream()
            .anyMatch(
                l ->
                    l.axis() == 1
                        && l.start() == 0
                        && l.a() == 1
                        && l.b() == 1
                        && l.length() == 1));
  }

  @Test
  void diagonalTouchRetainsOneSharedCrease() {
    BarrierMesh mesh = MeshBuilder.build(Set.of(new Voxel(0, 0, 0), new Voxel(1, 0, 1)), true);
    assertEquals(12, area(mesh));
    assertEquals(
        1,
        mesh.outlines().stream()
            .filter(
                l -> l.axis() == 1 && l.start() == 0 && l.a() == 1 && l.b() == 1 && l.length() == 1)
            .count());
  }

  @Test
  void negativeCoordinatesDoNotSplitMergedSurfaces() {
    BarrierMesh mesh = MeshBuilder.build(cuboid(8, 4, 6, -4, -2, -3), true);
    assertEquals(6, mesh.faces().size());
    assertEquals(12, mesh.outlines().size());
    assertEquals(208, area(mesh));
  }

  @Test
  void enclosedCavityKeepsItsInnerWalls() {
    Set<Voxel> blocks = cuboid(3, 3, 3, 0, 0, 0);
    blocks.remove(new Voxel(1, 1, 1));
    BarrierMesh mesh = MeshBuilder.build(blocks, true);
    assertEquals(60, area(mesh));
    assertEquals(12, mesh.faces().size());
    assertEquals(24, mesh.outlines().size());
  }

  @Test
  void randomShapesCoverExactlyTheExposedUnitFacesWithoutOverlaps() {
    Random random = new Random(72814);
    int[][] d = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
    for (int trial = 0; trial < 80; trial++) {
      Set<Voxel> blocks = new HashSet<>();
      for (int x = -2; x < 3; x++)
        for (int y = 0; y < 4; y++)
          for (int z = -2; z < 3; z++) if (random.nextBoolean()) blocks.add(new Voxel(x, y, z));
      Set<String> expected = new HashSet<>();
      for (Voxel p : blocks)
        for (int f = 0; f < 6; f++)
          if (!blocks.contains(p.offset(d[f][0], d[f][1], d[f][2]))) expected.add(p + ":" + f);
      Set<String> actual = new HashSet<>();
      BarrierMesh mesh = MeshBuilder.build(blocks, true);
      for (BarrierMesh.Quad q : mesh.faces())
        for (int u = q.u(); u < q.u() + q.width(); u++)
          for (int v = q.v(); v < q.v() + q.height(); v++) {
            Voxel p =
                switch (q.face()) {
                  case 0 -> new Voxel(u, q.plane(), v);
                  case 1 -> new Voxel(u, q.plane() - 1, v);
                  case 2 -> new Voxel(u, v, q.plane());
                  case 3 -> new Voxel(u, v, q.plane() - 1);
                  case 4 -> new Voxel(q.plane(), v, u);
                  case 5 -> new Voxel(q.plane() - 1, v, u);
                  default -> throw new AssertionError();
                };
            assertTrue(actual.add(p + ":" + q.face()), "Duplicate face");
          }
      assertEquals(expected, actual);
      assertEquals(actual.size(), area(mesh));
    }
  }

  @Test
  void rearEdgesDoNotCrossTheFrontFace() {
    BarrierMesh mesh = MeshBuilder.build(Set.of(new Voxel(0, 0, 0)), true);
    assertEquals(4, mesh.outlines().stream().filter(l -> l.facesCamera(5, .5, .5)).count());
    assertEquals(4, mesh.outlines().stream().filter(l -> l.facesCamera(-5, .5, .5)).count());
    assertEquals(9, mesh.outlines().stream().filter(l -> l.facesCamera(5, 5, 5)).count());
    for (BarrierMesh.Line line : mesh.outlines()) {
      if (line.facesCamera(5, .5, .5)) assertEquals(1, line.a());
      if (line.facesCamera(-5, .5, .5)) assertEquals(0, line.a());
    }
  }

  @Test
  void surfacesUseTheActualCameraSide() {
    BarrierMesh mesh = MeshBuilder.build(Set.of(new Voxel(0, 0, 0)), true);
    assertEquals(1, mesh.faces().stream().filter(q -> q.facesCamera(5, .5, .5)).count());
    assertEquals(1, mesh.faces().stream().filter(q -> q.facesCamera(-5, .5, .5)).count());
    assertEquals(3, mesh.faces().stream().filter(q -> q.facesCamera(5, 5, 5)).count());
    assertEquals(0, mesh.faces().stream().filter(q -> q.facesCamera(.5, .5, .5)).count());
  }

  private static Set<Voxel> cuboid(int w, int h, int d, int ox, int oy, int oz) {
    Set<Voxel> blocks = new HashSet<>();
    for (int x = 0; x < w; x++)
      for (int y = 0; y < h; y++)
        for (int z = 0; z < d; z++) blocks.add(new Voxel(x + ox, y + oy, z + oz));
    return blocks;
  }

  private static int area(BarrierMesh mesh) {
    return mesh.faces().stream().mapToInt(BarrierMesh.Quad::area).sum();
  }
}
