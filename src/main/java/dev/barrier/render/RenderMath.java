package dev.barrier.render;

public final class RenderMath {
  private RenderMath() {}

  public static float fade(
      double horizontal,
      double vertical,
      int range,
      int height,
      boolean enabled,
      int startPercent) {
    double fraction = Math.max(horizontal / Math.max(1, range), vertical / Math.max(1, height));
    if (fraction > 1) return 0;
    if (!enabled) return 1;
    double start = Math.max(0.1, Math.min(0.95, startPercent / 100.0));
    double t = Math.max(0, Math.min(1, (fraction - start) / (1 - start)));
    return (float) (1 - t * t * (3 - 2 * t));
  }
}
