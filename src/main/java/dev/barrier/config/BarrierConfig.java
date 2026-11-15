package dev.barrier.config;

import dev.barrier.BarrierClient;
import org.polyfrost.compose.render.PolyColor;
import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.config.v1.Tree;
import org.polyfrost.oneconfig.api.config.v1.annotations.*;
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindHelper;
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager;
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind;

@SuppressWarnings("unused")
public final class BarrierConfig extends Config {
  @Switch(
      title = "Enable barriers",
      description = "Show or hide all barrier visuals.",
      category = "General",
      subcategory = "General")
  public boolean enabled = true;

  @Info(
      title = "Check the server rules",
      description =
          "Some servers do not allow visible barriers. Check their rules before enabling this mod.",
      category = "General",
      subcategory = "General")
  public String serverRules = "";

  @Switch(
      title = "Connected textures",
      description =
          "Join touching barrier surfaces and outlines. Removes internal faces and grid lines.",
      category = "General",
      subcategory = "Appearance")
  public boolean connectedTextures = true;

  @Switch(title = "Outline", category = "General", subcategory = "Appearance")
  public boolean showOutline = true;

  @Color(title = "Outline colour", category = "General", subcategory = "Appearance")
  public PolyColor outlineTint = new PolyColor(0xE5560000);

  @Slider(
      title = "Outline width",
      min = 1,
      max = 10,
      step = 0.25f,
      category = "General",
      subcategory = "Appearance")
  public float outlineWidth = 4f;

  @Switch(title = "Fill", category = "General", subcategory = "Appearance")
  public boolean showFill = true;

  @Color(title = "Fill colour", category = "General", subcategory = "Appearance")
  public PolyColor fillTint = new PolyColor(0x66FF0000);

  @Switch(title = "Barrier texture", category = "General", subcategory = "Appearance")
  public boolean showTexture = true;

  @Dropdown(
      title = "Marker type",
      options = {"2D block texture", "Floating marker"},
      category = "General",
      subcategory = "Appearance")
  public int textureStyle = 0;

  @Slider(
      title = "Marker size",
      description = "Size of floating markers.",
      min = 0.2f,
      max = 1.2f,
      step = 0.05f,
      category = "General",
      subcategory = "Appearance")
  public float markerSize = 0.65f;

  @Color(
      title = "Texture colour",
      description = "White keeps the original texture colours.",
      category = "General",
      subcategory = "Appearance")
  public PolyColor textureTint = new PolyColor(0xE6FFFFFF);

  @Keybind(
      title = "Toggle barriers",
      description = "Enable or disable all barrier visuals. Default: B.",
      category = "General",
      subcategory = "Control")
  public final OneConfigKeybind toggleKey;

  @Slider(
      title = "Horizontal range",
      description = "How far away barriers are shown, in blocks.",
      min = 4,
      max = 64,
      category = "Advanced",
      subcategory = "Range and fade")
  public int range = 24;

  @Slider(
      title = "Vertical range",
      description = "How far above and below you barriers are shown.",
      min = 4,
      max = 64,
      category = "Advanced",
      subcategory = "Range and fade")
  public int verticalRange = 16;

  @Switch(title = "Distance fade", category = "Advanced", subcategory = "Range and fade")
  public boolean fadeEnabled = true;

  @Slider(
      title = "Fade starts at",
      description = "Percentage of the range before fading starts.",
      min = 10,
      max = 95,
      category = "Advanced",
      subcategory = "Range and fade")
  public int fadeStart = 75;

  @Switch(
      title = "Custom texture",
      description = "Use barrier:textures/marker.png from your resource pack.",
      category = "Advanced",
      subcategory = "Rendering")
  public boolean customMarker = false;

  @Switch(title = "Show through blocks", category = "Advanced", subcategory = "Rendering")
  public boolean throughBlocks = false;

  @Switch(title = "Hide in menus", category = "Advanced", subcategory = "Rendering")
  public boolean hideInMenus = true;

  @Switch(title = "Hide with F1", category = "Advanced", subcategory = "Rendering")
  public boolean hideWithGui = true;

  @Switch(title = "Singleplayer only", category = "Advanced", subcategory = "Rendering")
  public boolean singleplayerOnly = false;

  @Slider(
      title = "Block limit",
      description = "Maximum visible barrier blocks. Nearest blocks are preferred.",
      min = 64,
      max = 4096,
      step = 64,
      category = "Advanced",
      subcategory = "Performance limits")
  public int blockLimit = 2048;

  @Slider(
      title = "Sections scanned per tick",
      description = "Higher values find barriers faster but do more work.",
      min = 1,
      max = 32,
      category = "Advanced",
      subcategory = "Performance limits")
  public int sectionsPerTick = 8;

  @Slider(
      title = "Scan time budget",
      description = "Milliseconds per tick; checked between sections.",
      min = 0.25f,
      max = 4,
      step = 0.25f,
      category = "Advanced",
      subcategory = "Performance limits")
  public float scanBudgetMs = 1.5f;

  @Include public int settingsVersion = 0;

  public BarrierConfig(BarrierClient client) {
    super("barrier.json", "assets/barrier/icon.png", "Visible Barriers", Category.VISUALS);
    addAliases(new String[] {"barriers", "visible barriers"});
    toggleKey =
        KeybindHelper.builder()
            .key(KeybindMigration.KEY_B)
            .action((Runnable) client::toggle)
            .register();
    hideIf("outlineTint", "showOutline");
    hideIf("outlineWidth", "showOutline");
    hideIf("fillTint", "showFill");
    hideIf("textureStyle", "showTexture");
    hideIf("markerSize", "showTexture");
    hideIf("textureTint", "showTexture");
    addDependency("fadeStart", "fadeEnabled");
  }

  @Override
  protected void onInitialized(Tree tree) {
    getProperty("serverRules").addMetadata("type", "warning");
    if (settingsVersion < 3) {
      if (outlineTint.getRawArgb() == 0xD9D93232 && !outlineTint.getChroma())
        outlineTint = new PolyColor(0xE5560000);
      if (fillTint.getRawArgb() == 0x18CC3030 && !fillTint.getChroma())
        fillTint = new PolyColor(0x66FF0000);
      if (outlineWidth == 1.25f) outlineWidth = 4f;
      fadeEnabled = true;
    }
    if (KeybindMigration.needsRepair(
        settingsVersion, toggleKey.getKeyCodes(), toggleKey.getMouseBtns(), toggleKey.getMods())) {
      KeybindManager.rebindFromMinecraft(toggleKey, new int[] {KeybindMigration.KEY_B});
    }
    if (settingsVersion < 4) {
      settingsVersion = 4;
      save();
    }
  }
}
