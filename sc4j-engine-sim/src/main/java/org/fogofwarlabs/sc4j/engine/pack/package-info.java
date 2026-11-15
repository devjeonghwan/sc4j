/**
 * Reads the engine standard data pack, format {@code 1}: a folder, a zip archive or class loader resources (a mod's own jar) of
 * JSON definitions, PNG pictures and WAV sounds that a mod without its own data format ships
 * ({@link org.fogofwarlabs.sc4j.engine.pack.StandardPack}).
 * <p>
 * The pack root holds {@code pack.json}:
 * {@snippet lang = "json":
 * {
 *   "format": 1,
 *   "animationKinds": ["idle", "walk", "attack", "death"],
 *   "spriteSheets": {"footman": "units/footman.json", "fx_smoke": "effects/fx_smoke.json"},
 *   "spriteSheetDetails": {"footman-2x": "units/footman-2x.json"},
 *   "sounds": {"sword_hit": "sound/sword_hit.wav"},
 *   "palettes": {"terrain": "palettes/terrain.json"},
 *   "stringTables": {"en": "text/en.json"},
 *   "tilesets": {"meadow": "tiles/meadow.json"},
 *   "fonts": {"body": "fonts/body.json"},
 *   "dialogs": {"main-menu": "dialogs/main-menu.json"},
 *   "pictures": {"screens/menu": "screens/menu.png", "hud/panel": "hud/panel.json"},
 *   "content": "content.json"
 * }
 *}
 * Only {@code format} is required. Each map gives the id of a resource (its {@link org.fogofwarlabs.sc4j.engine.resource.ResourceKey})
 * and its pack path; the order of the maps is the index order: sprite sheet {@code n} has animation set {@code n} and
 * sound {@code n} is sound index {@code n} of the animations. A palette file is a JSON array of {@code "#RRGGBB"} or
 * {@code "#AARRGGBB"} strings, a string table file a JSON array of strings, a sound an uncompressed PCM WAV file.
 * <p>
 * A sprite sheet definition gives its PNG pictures relative to its own folder, the frame grid, the anchor and its
 * animations; other fields are ignored:
 * {@snippet lang = "json":
 * {
 *   "image": "footman.png", "teamMask": "footman_team.png", "shadow": "footman_shadow.png",
 *   "frameWidth": 72, "frameHeight": 72, "columns": 14, "rows": 8,
 *   "anchor": {"x": 32, "y": 52},
 *   "animations": {
 *     "idle": [{"frame": 0}, {"waitRandom": [30, 70]}, {"frame": 1}, {"wait": 10}, {"jump": "idle"}],
 *     "death": [{"frame": 10}, {"sound": "sword_hit"}, {"wait": 4}, {"end": true}]
 *   }
 * }
 *}
 * Frame {@code row * columns + column} is the grid cell at that row and column. A {@code frame} number is a column
 * ({@code 0} up to {@code columns - 1}); which row is shown, for example the facing, is up to the content. One
 * instruction is one object with one keyword: {@code frame}, {@code wait}, {@code waitRandom} {@code [a, b]},
 * {@code jump} label, {@code randomJump} label with {@code chance} (in {@code 256}), {@code call} label,
 * {@code return true}, {@code end true}, {@code sound} id, {@code spawnOverlay} sprite sheet id with optional
 * {@code x} and {@code y}, {@code attack} slot number or {@code "primary"} or {@code "secondary"}, and {@code move}
 * distance. A label is the name of an animation of the same sheet or an inline {@code {"label": name}}. A mod adds
 * keywords with {@link org.fogofwarlabs.sc4j.engine.pack.PackInstructionType}.
 * <p>
 * A tileset definition ({@link org.fogofwarlabs.sc4j.engine.pack.StandardPack#loadTileGraphics},
 * {@link org.fogofwarlabs.sc4j.engine.pack.StandardPack#loadTilesetLegend}) gives its PNG picture of tiles in a grid, tile
 * {@code row * columns + column}, the tile size, the tile footprint ({@code "square"} or {@code "diamond"},
 * {@link org.fogofwarlabs.sc4j.engine.resource.graphics.TileShape}) and per one-character tile code of the engine map format the tile
 * indices of its variants:
 * {@snippet lang = "json":
 * {"image": "meadow.png", "tileWidth": 32, "tileHeight": 32, "shape": "square", "tiles": {".": [0, 1, 2], "#": [3]}}
 *}
 * A font definition ({@link org.fogofwarlabs.sc4j.engine.pack.StandardPack#loadFont}) gives its PNG glyph sheet of equal cells and the
 * characters of the cells in row-major order; the alpha channel is the coverage. Each glyph is the box of the cell's pixels that are not
 * fully transparent; the pen advances by its width plus {@code spacing} (default {@code 1}), an empty cell by {@code spaceAdvance}
 * (default half the cell width); {@code lineHeight} defaults to the cell height:
 * {@snippet lang = "json":
 * {"image": "body.png", "cellWidth": 8, "cellHeight": 12, "characters": " !0123456789ABC", "lineHeight": 13, "spaceAdvance": 4}
 *}
 * A dialog definition ({@link org.fogofwarlabs.sc4j.engine.pack.StandardPack#loadDialog}) gives the dialog's bounds and its controls; a
 * control has an {@code id} unique in the dialog, a {@code kind} ({@code panel}, {@code label}, {@code button}, {@code option-button},
 * {@code check-box}, {@code list-box}, {@code combo-box}, {@code text-input}, {@code scroll-bar}, {@code animation} or {@code custom}), its
 * bounds relative to its parent, and optionally a {@code text}, an {@code align}ment ({@code left}, {@code center}, {@code right}), an
 * {@code image} id, the mod's {@code attributes} and its {@code children}:
 * {@snippet lang = "json":
 * {"x": 0, "y": 0, "width": 320, "height": 200, "controls": [
 *   {"id": 1, "kind": "button", "x": 20, "y": 20, "width": 280, "height": 32, "text": "Start", "align": "center",
 *    "attributes": {"hotkey": "s", "default": "true"}}
 * ]}
 *}
 * A picture ({@link org.fogofwarlabs.sc4j.engine.pack.StandardPack#loadPicture}) is a PNG file, a plain picture, or a JSON picture
 * definition that gives its PNG pictures relative to its own folder, an optional team colour mask (the first channel of its picture, as in a
 * sprite sheet), an optional anchor (for a cursor its hot spot) and optional nine-slice borders, each border {@code 0} by default:
 * {@snippet lang = "json":
 * {"image": "panel.png", "teamMask": "panel_team.png", "anchor": {"x": 2, "y": 2},
 *  "borders": {"left": 12, "top": 12, "right": 12, "bottom": 12}}
 *}
 * A font, picture, sprite sheet or tileset may have detail versions for displays with more than one pixel per unit: the same resource drawn
 * at a whole multiple {@code n} of its size ({@code 2} up to {@code 4}), under the id {@code <id>-<n>x}. A detail font, picture or tileset is
 * listed next to its resource, for example {@code "body-2x": "fonts/body-2x.json"} or {@code "hud/panel-2x": "hud/panel-2x.json"}; a detail
 * sprite sheet is listed under {@code spriteSheetDetails}, so that it has no animation set or index of its own. A detail font's line height is
 * {@code n} times the font's; a detail picture is {@code n} times the picture's width and height, with its own team colour mask, and its
 * anchor and borders are {@code n} times the picture's; a detail sprite sheet has the sheet's grid and layers with a frame size and anchor
 * {@code n} times the sheet's (its animations are the sheet's); a detail tileset has the tileset's tiles and shape at {@code n} times the tile
 * size (its tile codes are the tileset's). The client lays a resource out at its own size and draws it from the detail that matches the
 * display's pixels per unit; a resource without details is scaled.
 * <p>
 * The content definitions file ({@link org.fogofwarlabs.sc4j.engine.pack.StandardPack#readContent}, format {@code 1}) defines the
 * content types; each section maps the id of a type, also its debug name, to its definition, and the file order is the registration
 * order:
 * {@snippet lang = "json":
 * {
 *   "format": 1,
 *   "resourceKinds": ["gold"],
 *   "motionProfiles": {"walker": {"topSpeed": 320, "acceleration": 64, "stoppingDistance": 1024, "turnRate": 40}},
 *   "weapons": {"spear": {"damage": 7, "cooldown": 22, "maximumRange": 20, "targets": ["ground"]}},
 *   "factions": {"valley": {"worker": "farmer", "townHall": "hall", "supplyProvider": "barn", "resourceKinds": ["gold"],
 *                           "startUnits": ["farmer", "farmer"]}},
 *   "units": {"farmer": {"faction": "valley", "motionProfile": "walker", "hitPoints": 40, "sightRange": 7, "cost": {"gold": 50},
 *                        "buildTime": 60, "supplyRequired": 1, "collision": 6, "traits": ["worker"],
 *                        "idleOrders": {"human": "idle", "computer": "idle"}}},
 *   "requirements": {"farmer": [{"actor": "hall"}], "barracks": [{"actor": "farmer"}, {"completed": "hall"}]}
 * }
 *}
 * A motion profile has {@code topSpeed}, {@code acceleration}, {@code stoppingDistance}, {@code turnRate} and {@code acceleratingTurn}. A weapon has
 * {@code damage}, {@code damageBonus}, {@code damageFactor} (default {@code 1}), {@code cooldown}, {@code minimumRange},
 * {@code maximumRange}, {@code targets} ({@code ground}, {@code air}, {@code building}, {@code own-unit}), {@code projectile} (a
 * motion profile id), {@code homing} (default {@code true}) and {@code splashInnerRadius}, {@code splashMiddleRadius},
 * {@code splashOuterRadius}. A faction has {@code worker}, {@code townHall}, {@code supplyProvider}, {@code resourceKinds} and
 * {@code startUnits}. A unit has {@code faction}, {@code motionProfile}, {@code hitPoints}, {@code armor}, {@code sightRange},
 * {@code targetAcquisitionRange}, {@code groundWeapon}, {@code airWeapon}, {@code cost} (resource kind to amount), {@code buildTime},
 * {@code supplyProvided}, {@code supplyRequired}, {@code spaceRequired}, {@code spaceProvided}, {@code placementWidth},
 * {@code placementHeight}, {@code collision} (one extent or {@code left}, {@code up}, {@code right}, {@code down}), {@code traits}
 * (unit trait names in lower case with hyphens) and {@code idleOrders} ({@code human}, {@code computer}, {@code neutral} to an order
 * id of the mod's order types). Absent values are {@code 0}, {@code false}, none or no cost; every other field name is an error. A
 * requirement lists the conditions that must all hold to build or train a unit type, each one object: {@code actor} (a unit type id
 * or an array of them: the producing or building unit), {@code completed} (a unit type id, with an optional {@code count}, default
 * {@code 1}) or {@code anyOf} (an array of conditions).
 * <p>
 * Every failure is an {@link java.io.IOException} that names the file and the field; a resource id the pack does not
 * list is a {@link org.fogofwarlabs.sc4j.engine.resource.ResourceNotFoundException}.
 */
package org.fogofwarlabs.sc4j.engine.pack;
