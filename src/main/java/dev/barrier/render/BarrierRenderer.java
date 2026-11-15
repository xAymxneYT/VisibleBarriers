package dev.barrier.render;

import dev.barrier.config.BarrierConfig;
import dev.barrier.mesh.*;
import dev.barrier.mesh.BarrierMesh.Line;
import dev.barrier.mesh.BarrierMesh.Quad;
import dev.barrier.scan.BarrierCache;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.texture.TextureAtlas;
import net.minecraft.client.render.vertex.BufferBuilder;
import net.minecraft.client.render.vertex.DefaultVertexFormat;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.resource.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.opengl.GL11;

public final class BarrierRenderer {
  private static final Identifier VANILLA_TEXTURE =
      new Identifier("minecraft", "textures/items/barrier.png");
  private static final Identifier CUSTOM_TEXTURE = new Identifier("barrier", "textures/marker.png");
  private final ExecutorService mesher =
      Executors.newSingleThreadExecutor(
          r -> {
            Thread t = new Thread(r, "Barrier mesh");
            t.setDaemon(true);
            return t;
          });
  private Snapshot snapshot = Snapshot.EMPTY;
  private Future<Snapshot> pending;
  private Request requested;
  private ClientWorld world;
  private long lastEdit;

  public void clear() {
    if (pending != null) pending.cancel(true);
    pending = null;
    requested = null;
    world = null;
    snapshot = Snapshot.EMPTY;
  }

  public void update(Minecraft mc, BarrierCache cache, BarrierConfig c) {
    if (world != mc.world) {
      clear();
      world = mc.world;
      lastEdit = cache.editRevision();
    }
    boolean immediate = cache.editRevision() != lastEdit;
    if (immediate && pending != null) {
      pending.cancel(true);
      pending = null;
      requested = null;
    }
    if (pending != null && pending.isDone()) {
      try {
        snapshot = pending.get();
      } catch (CancellationException ignored) {
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
        LogManager.getLogger("Barrier").error("Could not build barrier geometry", e.getCause());
      }
      pending = null;
    }
    Request next =
        new Request(
            cache.revision(),
            (int) Math.floor(mc.player.x),
            (int) Math.floor(mc.player.y),
            (int) Math.floor(mc.player.z),
            Math.max(4, Math.min(64, c.range)),
            Math.max(4, Math.min(64, c.verticalRange)),
            Math.max(64, Math.min(4096, c.blockLimit)),
            c.connectedTextures);
    if (!immediate && (next.equals(requested) || pending != null)) return;
    PriorityQueue<Candidate> nearest =
        new PriorityQueue<>(Comparator.comparingDouble(Candidate::distance).reversed());
    for (List<Voxel> section : cache.sections())
      for (Voxel p : section) {
        double dx = p.x() + 0.5 - mc.player.x,
            dy = p.y() + 0.5 - mc.player.y,
            dz = p.z() + 0.5 - mc.player.z;
        double horizontal = dx * dx + dz * dz, distance = horizontal + dy * dy;
        if (horizontal > next.range * (double) next.range || Math.abs(dy) > next.height) continue;
        if (nearest.size() == next.limit && distance >= nearest.peek().distance) continue;
        if (nearest.size() == next.limit) nearest.poll();
        nearest.offer(new Candidate(p, distance));
      }
    Set<Voxel> blocks = new HashSet<>();
    for (Candidate candidate : nearest) {
      Voxel p = candidate.pos;
      BlockPos pos = new BlockPos(p.x(), p.y(), p.z());
      if (mc.world.isChunkLoaded(pos) && mc.world.getBlockState(pos).getBlock() == Blocks.BARRIER)
        blocks.add(p);
    }
    Set<Voxel> immutable = Set.copyOf(blocks);
    requested = next;
    lastEdit = cache.editRevision();
    if (immutable.equals(snapshot.members) && c.connectedTextures == snapshot.connected) return;
    if (immediate) snapshot = build(immutable, next);
    else pending = mesher.submit(() -> build(immutable, next));
  }

  private static Snapshot build(Set<Voxel> blocks, Request request) {
    BarrierMesh mesh = MeshBuilder.build(blocks, request.connected);
    List<Quad> faces = new ArrayList<>(mesh.faces());
    faces.sort(Comparator.comparingDouble((Quad q) -> faceDistance(q, request)).reversed());
    return new Snapshot(
        new BarrierMesh(List.copyOf(faces), mesh.outlines()),
        List.copyOf(blocks),
        blocks,
        request.connected);
  }

  public void render(Minecraft mc, BarrierConfig c, float delta) {
    Snapshot draw = snapshot;
    if (draw.blocks.isEmpty() || !(c.showOutline || c.showFill || c.showTexture)) return;
    Entity camera = mc.getCamera();
    if (camera == null) return;
    double cx = camera.lastX + (camera.x - camera.lastX) * delta;
    double cy = camera.lastY + (camera.y - camera.lastY) * delta;
    double cz = camera.lastZ + (camera.z - camera.lastZ) * delta;
    Vec3d view = Camera.getPos(camera, delta);

    GL11.glPushAttrib(
        GL11.GL_ENABLE_BIT
            | GL11.GL_COLOR_BUFFER_BIT
            | GL11.GL_DEPTH_BUFFER_BIT
            | GL11.GL_POLYGON_BIT
            | GL11.GL_LINE_BIT
            | GL11.GL_CURRENT_BIT
            | GL11.GL_TEXTURE_BIT
            | GL11.GL_LIGHTING_BIT);
    GL11.glMatrixMode(GL11.GL_MODELVIEW);
    GL11.glPushMatrix();
    try {
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glDisable(GL11.GL_TEXTURE_2D);
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_FOG);
      GL11.glDisable(GL11.GL_ALPHA_TEST);
      GL11.glEnable(GL11.GL_CULL_FACE);
      GL11.glDepthMask(false);
      if (c.throughBlocks) GL11.glDisable(GL11.GL_DEPTH_TEST);
      else GL11.glEnable(GL11.GL_DEPTH_TEST);
      GL11.glCullFace(GL11.GL_BACK);
      GL11.glFrontFace(GL11.GL_CCW);
      GL11.glTranslated(-cx, -cy, -cz);
      Tesselator tess = Tesselator.getInstance();
      BufferBuilder b = tess.getBuffer();
      if (c.showFill) {
        b.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
        faces(b, draw.mesh.faces(), mc, c, c.fillTint.getArgb(), false, 0.001, view);
        tess.end();
      }
      if (c.showTexture) textures(mc, c, draw, b, tess, view);
      if (c.showOutline) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glLineWidth(Math.max(1, Math.min(10, c.outlineWidth)));
        b.begin(GL11.GL_LINES, DefaultVertexFormat.POSITION_COLOR);
        int color = c.outlineTint.getArgb();
        for (Line line : draw.mesh.outlines()) {
          if (!line.facesCamera(view.x, view.y, view.z)) continue;
          int step = c.fadeEnabled ? 4 : line.length();
          for (int i = 0; i < line.length(); i += step) {
            lineVertex(b, line, i, color, mc, c);
            lineVertex(b, line, Math.min(line.length(), i + step), color, mc, c);
          }
        }
        tess.end();
      }
    } finally {
      GL11.glPopMatrix();
      GL11.glPopAttrib();
    }
  }

  private static void faces(
      BufferBuilder b,
      List<Quad> faces,
      Minecraft mc,
      BarrierConfig c,
      int color,
      boolean textured,
      double expand,
      Vec3d view) {
    for (Quad q : faces) {
      if (!q.facesCamera(view.x, view.y, view.z)) continue;
      int step = c.fadeEnabled ? 4 : Math.max(q.width(), q.height());
      for (int v = 0; v < q.height(); v += step)
        for (int u = 0; u < q.width(); u += step) {
          int right = Math.min(q.width(), u + step), top = Math.min(q.height(), v + step);
          faceVertex(b, q, u, v, color, textured, expand, mc, c);
          boolean reverse = q.face() == 1 || q.face() == 2 || q.face() == 5;
          faceVertex(b, q, reverse ? u : right, reverse ? top : v, color, textured, expand, mc, c);
          faceVertex(b, q, right, top, color, textured, expand, mc, c);
          faceVertex(b, q, reverse ? right : u, reverse ? v : top, color, textured, expand, mc, c);
        }
    }
  }

  private static void faceVertex(
      BufferBuilder b,
      Quad q,
      int u,
      int v,
      int color,
      boolean textured,
      double expand,
      Minecraft mc,
      BarrierConfig c) {
    double plane = q.plane() + ((q.face() & 1) == 0 ? -expand : expand);
    double a = q.u() + u, d = q.v() + v;
    double x = q.face() < 4 ? a : plane;
    double y = q.face() < 2 ? plane : d;
    double z = q.face() < 2 ? d : q.face() < 4 ? plane : a;
    b.vertex(x, y, z);
    if (textured) b.texture(u, q.face() < 2 ? v : -v);
    tint(b, color, opacity(x, y, z, mc, c));
    b.nextVertex();
  }

  private static void lineVertex(
      BufferBuilder b, Line line, int offset, int color, Minecraft mc, BarrierConfig c) {
    double s = line.start() + offset;
    double x = line.axis() == 0 ? s : line.a();
    double y = line.axis() == 1 ? s : line.axis() == 0 ? line.a() : line.b();
    double z = line.axis() == 2 ? s : line.b();
    b.vertex(x, y, z);
    tint(b, color, opacity(x, y, z, mc, c));
    b.nextVertex();
  }

  private static void textures(
      Minecraft mc, BarrierConfig c, Snapshot draw, BufferBuilder b, Tesselator tess, Vec3d view) {
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    mc.getTextureManager().bind(c.customMarker ? CUSTOM_TEXTURE : VANILLA_TEXTURE);
    try {
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
      b.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      if (c.textureStyle == 0)
        faces(b, draw.mesh.faces(), mc, c, c.textureTint.getArgb(), true, 0.002, view);
      else {
        GL11.glDisable(GL11.GL_CULL_FACE);
        billboards(mc, c, draw.blocks, b);
      }
      tess.end();
    } finally {
      mc.getTextureManager().bind(TextureAtlas.BLOCKS_LOCATION);
    }
  }

  private static void billboards(
      Minecraft mc, BarrierConfig c, List<Voxel> blocks, BufferBuilder b) {
    EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
    double yaw = Math.toRadians(dispatcher.cameraYaw),
        pitch = Math.toRadians(dispatcher.cameraPitch);
    double rx = Math.cos(yaw), rz = Math.sin(yaw), ux = -Math.sin(yaw) * Math.sin(pitch);
    double uy = Math.cos(pitch), uz = Math.cos(yaw) * Math.sin(pitch);
    double half = Math.max(0.2, Math.min(1.2, c.markerSize)) * 0.5;
    int color = c.textureTint.getArgb();
    for (Voxel p : blocks) {
      double x = p.x() + 0.5, y = p.y() + 0.5, z = p.z() + 0.5;
      float fade = opacity(x, y, z, mc, c);
      marker(
          b,
          x - rx * half - ux * half,
          y - uy * half,
          z - rz * half - uz * half,
          0,
          1,
          color,
          fade);
      marker(
          b,
          x + rx * half - ux * half,
          y - uy * half,
          z + rz * half - uz * half,
          1,
          1,
          color,
          fade);
      marker(
          b,
          x + rx * half + ux * half,
          y + uy * half,
          z + rz * half + uz * half,
          1,
          0,
          color,
          fade);
      marker(
          b,
          x - rx * half + ux * half,
          y + uy * half,
          z - rz * half + uz * half,
          0,
          0,
          color,
          fade);
    }
  }

  private static void marker(
      BufferBuilder b, double x, double y, double z, double u, double v, int color, float fade) {
    b.vertex(x, y, z).texture(u, v);
    tint(b, color, fade);
    b.nextVertex();
  }

  private static void tint(BufferBuilder b, int color, float opacity) {
    b.color(
        (color >> 16) & 255, (color >> 8) & 255, color & 255, Math.round((color >>> 24) * opacity));
  }

  private static float opacity(double x, double y, double z, Minecraft mc, BarrierConfig c) {
    if (!c.fadeEnabled) return 1;
    double dx = x - mc.player.x, dz = z - mc.player.z;
    double fadeStart = Math.max(.1, Math.min(.95, c.fadeStart / 100.0));
    double horizontalStart = c.range * fadeStart;
    double vertical = Math.abs(y - mc.player.y);
    if (dx * dx + dz * dz <= horizontalStart * horizontalStart
        && vertical <= c.verticalRange * fadeStart) return 1;
    return RenderMath.fade(
        Math.sqrt(dx * dx + dz * dz),
        Math.abs(y - mc.player.y),
        c.range,
        c.verticalRange,
        c.fadeEnabled,
        c.fadeStart);
  }

  private static double faceDistance(Quad q, Request r) {
    double u = q.u() + q.width() * 0.5, v = q.v() + q.height() * 0.5;
    double x = q.face() < 4 ? u : q.plane(), y = q.face() < 2 ? q.plane() : v;
    double z = q.face() < 2 ? v : q.face() < 4 ? q.plane() : u;
    return (x - r.x) * (x - r.x) + (y - r.y) * (y - r.y) + (z - r.z) * (z - r.z);
  }

  private record Candidate(Voxel pos, double distance) {}

  private record Request(
      long revision, int x, int y, int z, int range, int height, int limit, boolean connected) {}

  private record Snapshot(
      BarrierMesh mesh, List<Voxel> blocks, Set<Voxel> members, boolean connected) {
    static final Snapshot EMPTY = new Snapshot(BarrierMesh.EMPTY, List.of(), Set.of(), true);
  }
}
