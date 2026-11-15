package dev.barrier.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class KeybindMigrationTest {
  @Test
  void repairsTheOldSdlF9DefaultOnce() {
    assertTrue(KeybindMigration.needsRepair(3, new int[] {66}, null, (byte) 0));
    assertFalse(KeybindMigration.needsRepair(4, new int[] {66}, null, (byte) 0));
  }

  @Test
  void keepsCustomKeysAndCombinations() {
    assertFalse(KeybindMigration.needsRepair(3, new int[] {5}, null, (byte) 0));
    assertFalse(KeybindMigration.needsRepair(3, new int[] {7}, null, (byte) 0));
    assertFalse(KeybindMigration.needsRepair(3, new int[] {66, 4}, null, (byte) 0));
    assertFalse(KeybindMigration.needsRepair(3, new int[] {66}, null, (byte) 1));
  }

  @Test
  void keepsMouseBindsAndUnboundKeys() {
    assertFalse(KeybindMigration.needsRepair(3, null, new int[] {0}, (byte) 0));
    assertFalse(KeybindMigration.needsRepair(3, new int[] {66}, new int[] {1}, (byte) 0));
    assertFalse(KeybindMigration.needsRepair(3, new int[] {}, null, (byte) 0));
  }
}
