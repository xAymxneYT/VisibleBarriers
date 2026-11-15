package dev.barrier.config;

public final class KeybindMigration {
  public static final int KEY_B = 5;

  private KeybindMigration() {}

  public static boolean needsRepair(int version, int[] keys, int[] mouse, byte modifiers) {
    return version < 4
        && keys != null
        && keys.length == 1
        && keys[0] == 66
        && (mouse == null || mouse.length == 0)
        && modifiers == 0;
  }
}
