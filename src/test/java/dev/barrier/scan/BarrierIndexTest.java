package dev.barrier.scan;

import static org.junit.jupiter.api.Assertions.*;

import dev.barrier.mesh.Voxel;
import java.util.*;
import org.junit.jupiter.api.Test;

class BarrierIndexTest {
  @Test
  void editsAreVisibleWithoutWaitingForAScan() {
    BarrierIndex index = new BarrierIndex(100);
    Voxel block = new Voxel(-1, 64, 2);
    assertTrue(index.edit(block, true));
    assertTrue(index.sections().stream().anyMatch(s -> s.contains(block)));
    assertEquals(1, index.count());
    assertEquals(1, index.edits());
    assertTrue(index.edit(block, false));
    assertTrue(index.sections().isEmpty());
    assertEquals(0, index.count());
    assertEquals(2, index.edits());
  }

  @Test
  void scansDoNotTriggerTheInteractiveRebuildPath() {
    BarrierIndex index = new BarrierIndex(100);
    Voxel block = new Voxel(0, 64, 0);
    long key = new SectionPos(0, 4, 0).key();
    assertTrue(index.replace(key, List.of(block)));
    assertEquals(1, index.revision());
    assertEquals(0, index.edits());
    assertFalse(index.replace(key, List.of(block)));
    assertEquals(1, index.revision());
    assertTrue(index.edit(new Voxel(1, 64, 0), true));
    assertEquals(1, index.edits());
  }

  @Test
  void unchangedEditsDoNotInvalidateGeometry() {
    BarrierIndex index = new BarrierIndex(100);
    Voxel p = new Voxel(0, 64, 0);
    assertFalse(index.edit(p, false));
    assertTrue(index.edit(p, true));
    assertFalse(index.edit(p, true));
    assertEquals(1, index.edits());
    assertEquals(1, index.revision());
  }

  @Test
  void replacingASectionAtTheLimitReusesItsSpace() {
    BarrierIndex index = new BarrierIndex(2);
    long key = new SectionPos(0, 4, 0).key();
    index.edit(new Voxel(0, 64, 0), true);
    index.edit(new Voxel(1, 64, 0), true);
    assertFalse(index.edit(new Voxel(2, 64, 0), true));
    assertTrue(index.replace(key, List.of(new Voxel(3, 64, 0), new Voxel(4, 64, 0))));
    assertEquals(2, index.count());
    assertTrue(index.edit(new Voxel(3, 64, 0), false));
    assertTrue(index.edit(new Voxel(5, 64, 0), true));
    assertEquals(2, index.count());
  }

  @Test
  void editsAndScanOrderAgree() {
    BarrierIndex index = new BarrierIndex(100);
    long key = new SectionPos(0, 4, 0).key();
    Voxel a = new Voxel(1, 64, 0), b = new Voxel(0, 64, 1);
    index.edit(b, true);
    index.edit(a, true);
    long revision = index.revision();
    assertFalse(index.replace(key, List.of(a, b)));
    assertEquals(revision, index.revision());
  }

  @Test
  void pruningAndClearingPreserveAccurateCounts() {
    BarrierIndex index = new BarrierIndex(100);
    index.edit(new Voxel(0, 64, 0), true);
    index.edit(new Voxel(16, 64, 0), true);
    index.retain(Set.of(new SectionPos(0, 4, 0).key()));
    assertEquals(1, index.count());
    index.clear();
    assertEquals(0, index.count());
    assertTrue(index.sections().isEmpty());
  }
}
