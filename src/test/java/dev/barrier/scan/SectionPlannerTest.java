package dev.barrier.scan;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class SectionPlannerTest {
  @Test
  void includesNegativeCoordinatesAcrossOrigin() {
    List<SectionPos> plan = SectionPlanner.around(-0.1, 64, -0.1, 4, 4);
    Set<Long> keys = new HashSet<>();
    for (SectionPos p : plan) keys.add(p.key());
    assertTrue(keys.contains(new SectionPos(-1, 3, -1).key()));
    assertTrue(keys.contains(new SectionPos(0, 4, 0).key()));
    assertEquals(8, plan.size());
  }

  @Test
  void clampsToWorldHeightAndContainsNoDuplicateKeys() {
    for (int y : new int[] {-64, 0, 64, 255, 320}) {
      List<SectionPos> plan = SectionPlanner.around(-30000000, y, 30000000, 64, 64);
      Set<Long> unique = new HashSet<>();
      for (SectionPos p : plan) {
        assertTrue(p.y >= 0 && p.y < 16);
        assertTrue(unique.add(p.key()));
      }
    }
  }

  @Test
  void nearestSectionsAreScannedFirst() {
    List<SectionPos> plan = SectionPlanner.around(8, 72, 8, 32, 32);
    assertEquals(new SectionPos(0, 4, 0).key(), plan.get(0).key());
    double previous = -1;
    for (SectionPos p : plan) {
      double current = p.distanceSquared(8, 72, 8);
      assertTrue(current >= previous);
      previous = current;
    }
  }
}
