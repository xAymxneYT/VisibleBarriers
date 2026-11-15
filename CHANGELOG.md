# Changelog

## 0.4.0

- Renamed to Visible Barriers by xAymxneYT and updated the description.
- Corrected the SDL key code for B and migrated the previous F9 default.
- Added direct block-change updates instead of waiting for scans on edits.
- Combined edits into a next-frame rebuild and discarded stale background results.
- Moved barrier rendering before entities to keep players and armour clear in F5.

## 0.3.0

- Added the supplied icon to OneConfig and the mod metadata.
- Updated default outline to E5560000, fill to 66FF0000 and width to 4.
- Enabled distance fade by default.
- Moved outline width and marker size into Appearance beside their related controls.
- Extended outline width to 10 pixels using screen-space strokes.
- Removed rear outline edges that crossed the front texture.
- Standardized Java formatting and removed source comments.

Existing custom colours and widths are retained. Values matching the previous
appearance defaults are upgraded. Distance fade is enabled once on upgrade;
it can then be changed normally.

## 0.2.0

- Simplified settings into General and Advanced.
- Added connected surfaces, merged outlines and flat block textures.
- Moved chroma into native colour pickers.
- Removed custom highlighting and holding mode.
