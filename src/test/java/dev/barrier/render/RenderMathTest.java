package dev.barrier.render;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RenderMathTest {
  @Test
  void fadesAtBothHorizontalAndVerticalLimits() {
    assertEquals(1, RenderMath.fade(0, 0, 24, 16, true, 65));
    assertEquals(1, RenderMath.fade(12, 8, 24, 16, true, 65));
    assertEquals(0, RenderMath.fade(24, 0, 24, 16, true, 65));
    assertEquals(0, RenderMath.fade(0, 16, 24, 16, true, 65));
    assertEquals(0, RenderMath.fade(25, 0, 24, 16, false, 65));
    assertEquals(1, RenderMath.fade(23, 0, 24, 16, false, 65));
  }

  @Test
  void fadeIsMonotonicAndFiniteWithInvalidSettings() {
    float previous = 1;
    for (double d = 0; d <= 24; d += 0.1) {
      float current = RenderMath.fade(d, 0, 24, 16, true, 1000);
      assertTrue(current >= 0 && current <= previous);
      previous = current;
    }
    assertTrue(Float.isFinite(RenderMath.fade(0, 0, 0, 0, true, -100)));
  }
}
