#!/usr/bin/env python3
"""
Generates every asset and data file for Dyeable Poplar.

Run from the project root:  python3 tools/generate_resources.py
Needs Pillow (pip install pillow). Re-running overwrites the generated files.

The dyed textures are NOT shipped as images. Minecraft builds them at load time
from the vanilla textures using `paletted_permutations` atlas sources: each colour
in a vanilla texture is swapped for a dyed shade. Boats are drawn from entity
textures that are not in an atlas, so the mod recolours those itself on the client
with the same palettes. This script only writes the small palette strips, in
three styles:

  planks   full recolour, tuned from the vanilla wool colours
  bark     a partial tint that keeps the wood's natural brown showing
  pastel   a soft, light pastel tint
"""
import json
import math
import os
import re
import shutil

from PIL import Image, ImageDraw

MOD_ID = "dyeable_poplar"
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src", "main", "resources")
TEMPLATES = os.path.join(ROOT, "tools", "vanilla_templates")

# Creative-tab order used by vanilla's Colored Blocks tab.
COLORS = ["white", "light_gray", "gray", "black", "brown", "red", "orange", "yellow",
          "lime", "green", "cyan", "light_blue", "blue", "purple", "magenta", "pink"]

# Average colour of each vanilla wool texture (26.3); every style is tuned from these.
WOOL = {
    "white": "#eaeced", "light_gray": "#8e8e87", "gray": "#3f4448", "black": "#15151a",
    "brown": "#724829", "red": "#a12723", "orange": "#f17614", "yellow": "#f9c628",
    "lime": "#70b91a", "green": "#556e1c", "cyan": "#158a91", "light_blue": "#3aafd9",
    "blue": "#35399d", "purple": "#7a2aad", "magenta": "#be45b4", "pink": "#ee8dac",
}

# Texture families. Each lists the vanilla 26.3 textures it recolours and every colour
# that gets dyed, with pixel counts. Colours that are not listed stay as they are:
# chains on hanging signs, the chest on chest boats, the embers on campfire logs.
FAMILIES = {
    "poplar_planks": {
        "style": "planks", "ref": "poplar_planks",
        "blocks": ["poplar_planks"],
        "palette": {"#766665": 29, "#7e6f6c": 24, "#877974": 18, "#91857b": 47,
                    "#9c9086": 48, "#a79b8c": 52, "#aaa191": 38},
    },
    "poplar_log": {
        "style": "bark", "ref": "poplar_log",
        "blocks": ["poplar_log", "poplar_log_top"],
        "palette": {"#382c1f": 16, "#42302a": 27, "#473626": 27, "#4b3b2e": 89, "#56402a": 88,
                    "#59432b": 26, "#5d492d": 35, "#645132": 8, "#7e6f6c": 4, "#80756e": 1,
                    "#877974": 38, "#91857b": 23, "#93867c": 2, "#9c9086": 70, "#a79b8c": 52,
                    "#aaa191": 6},
    },
    "stripped_poplar_log": {
        "style": "pastel", "ref": "stripped_poplar_log",
        "blocks": ["stripped_poplar_log", "stripped_poplar_log_top"],
        "palette": {"#7e6f6c": 4, "#80756e": 1, "#877974": 47, "#91857b": 55, "#93867c": 2,
                    "#9a8d83": 10, "#9c9086": 89, "#9e9187": 28, "#a2958b": 70, "#a79b8c": 165,
                    "#aaa191": 41},
    },
    # doors, trapdoors, signs and boats: made from planks, so they get the planks colours
    "poplar_details": {
        "style": "planks", "ref": "poplar_planks",
        "blocks": ["poplar_door_top", "poplar_door_bottom", "poplar_trapdoor", "poplar_sign"],
        "items": ["poplar_door", "poplar_sign", "poplar_boat", "poplar_chest_boat"],
        "entities": ["boat/poplar", "chest_boat/poplar"],   # recoloured on the client by the mod
        "palette": {"#332b2b": 10, "#382c1f": 21, "#42302a": 11, "#473626": 14, "#473d3d": 30,
                    "#4b3b2e": 35, "#56402a": 31, "#59432b": 12, "#594d4d": 33, "#5c4f4c": 14,
                    "#5d492d": 18, "#645132": 8, "#766665": 384, "#7e6f6c": 168, "#7f706d": 336,
                    "#877974": 560, "#897b76": 84, "#91857b": 758, "#92867c": 94, "#9c9086": 385,
                    "#9e9187": 573, "#a69a8c": 119, "#a79b8c": 855, "#aaa191": 172, "#b2a998": 452,
                    "#bdb3a2": 11},
    },
    # hanging signs and shelves: made from stripped logs, so they get the pastel colours
    "stripped_poplar_details": {
        "style": "pastel", "ref": "stripped_poplar_log",
        "blocks": ["poplar_hanging_sign", "poplar_shelf"],
        "items": ["poplar_hanging_sign"],
        "palette": {"#5c4f4c": 12, "#6b5d5d": 50, "#766665": 88, "#877974": 287, "#91857b": 281,
                    "#9a8d83": 173, "#9c9086": 44, "#9e9187": 59, "#a2958b": 193, "#a79b8c": 265,
                    "#aaa191": 151, "#b2a998": 81},
    },
    # campfires: the logs get the bark tint, the fire, embers and ash stay as they are
    "campfire_log": {
        "style": "bark", "ref": "campfire_log",
        "blocks": ["campfire_log"],
        "items": ["campfire"],
        "palette": {"#362b1b": 20, "#4c3d26": 22, "#5f4a2b": 15, "#745a36": 31, "#917142": 24,
                    "#967441": 4, "#987849": 10, "#9f844d": 8, "#b8945f": 11, "#c29d62": 11},
    },
}
SPRITE_SEPARATOR = "_dyed_"   # minecraft:block/poplar_door_top -> minecraft:block/poplar_door_top_dyed_red

# Smoke tint per dye (RGB). The smoke sprites are light grey, so these multiply them.
SMOKE = {
    "white": "#f2f2f2", "light_gray": "#a9a9a2", "gray": "#5b6167", "black": "#1d1d23",
    "brown": "#8c5a35", "red": "#c4322b", "orange": "#ff8c2b", "yellow": "#ffd942",
    "lime": "#8cdd2d", "green": "#6b8c2b", "cyan": "#2ba9b1", "light_blue": "#5cc8f2",
    "blue": "#4b51c9", "purple": "#9b42d1", "magenta": "#e05cd4", "pink": "#ffa9c5",
}


class Shape:
    """One kind of dyeable poplar object, mirroring a vanilla one."""

    def __init__(self, vanilla, name, blockstates=None, item=True, loot=True, tags=None,
                 category=None, folder="building_blocks", crafted_from=None, stackable=True,
                 lang_kind="block"):
        self.vanilla = vanilla                      # vanilla id, e.g. poplar_door
        self.name = name                            # English name, e.g. Poplar Door
        self.blockstates = blockstates if blockstates is not None else [vanilla]
        self.item = item                            # has an item (wall signs don't)
        self.loot = loot
        self.tags = tags or {}                      # {"block": {id: [tags]}, "item": [tags], ...}
        self.category = category                    # recipe-book category (None = misc)
        self.folder = folder                        # recipe advancement folder
        self.crafted_from = crafted_from            # dyed shape whose recipe is reused in dyed form
        self.stackable = stackable                  # unstackable items get no 8-at-once dye recipe
        self.lang_kind = lang_kind                  # "block" or "item"


def blk(*tags):
    return list(tags)


# In creative-tab order (the order vanilla lists the poplar set, then the campfire).
SHAPES = [
    Shape("poplar_log", "Poplar Log", tags={"block": blk("logs_that_burn"), "item": blk("logs_that_burn")},
          category="building"),
    Shape("poplar_wood", "Poplar Wood", tags={"block": blk("logs_that_burn"), "item": blk("logs_that_burn")},
          category="building", crafted_from="poplar_log"),
    Shape("stripped_poplar_log", "Stripped Poplar Log",
          tags={"block": blk("logs_that_burn"), "item": blk("logs_that_burn")}, category="building"),
    Shape("stripped_poplar_wood", "Stripped Poplar Wood",
          tags={"block": blk("logs_that_burn"), "item": blk("logs_that_burn")}, category="building",
          crafted_from="stripped_poplar_log"),
    Shape("poplar_planks", "Poplar Planks", tags={"block": blk("planks"), "item": blk("planks")}, category="building"),
    Shape("poplar_stairs", "Poplar Stairs", tags={"block": blk("wooden_stairs"), "item": blk("wooden_stairs")},
          category="building", crafted_from="poplar_planks"),
    Shape("poplar_slab", "Poplar Slab", tags={"block": blk("wooden_slabs"), "item": blk("wooden_slabs")},
          category="building", crafted_from="poplar_planks"),
    Shape("poplar_fence", "Poplar Fence", tags={"block": blk("wooden_fences"), "item": blk("wooden_fences")},
          folder="decorations", crafted_from="poplar_planks"),
    Shape("poplar_fence_gate", "Poplar Fence Gate", tags={"block": blk("fence_gates"), "item": blk("fence_gates")},
          category="redstone", folder="redstone", crafted_from="poplar_planks"),
    Shape("poplar_door", "Poplar Door", tags={"block": blk("wooden_doors"), "item": blk("wooden_doors")},
          category="redstone", folder="redstone", crafted_from="poplar_planks"),
    Shape("poplar_trapdoor", "Poplar Trapdoor",
          tags={"block": blk("wooden_trapdoors"), "item": blk("wooden_trapdoors")},
          category="redstone", folder="redstone", crafted_from="poplar_planks"),
    Shape("poplar_pressure_plate", "Poplar Pressure Plate",
          tags={"block": blk("wooden_pressure_plates"), "item": blk("wooden_pressure_plates")},
          category="redstone", folder="redstone", crafted_from="poplar_planks"),
    Shape("poplar_button", "Poplar Button", tags={"block": blk("wooden_buttons"), "item": blk("wooden_buttons")},
          category="redstone", folder="redstone", crafted_from="poplar_planks"),
    Shape("poplar_sign", "Poplar Sign", blockstates=["poplar_sign", "poplar_wall_sign"],
          tags={"block": {"poplar_sign": ["standing_signs"], "poplar_wall_sign": ["wall_signs"]}, "item": blk("signs")},
          folder="decorations", crafted_from="poplar_planks"),
    Shape("poplar_hanging_sign", "Poplar Hanging Sign", blockstates=["poplar_hanging_sign", "poplar_wall_hanging_sign"],
          tags={"block": {"poplar_hanging_sign": ["ceiling_hanging_signs"],
                          "poplar_wall_hanging_sign": ["wall_hanging_signs"]}, "item": blk("hanging_signs")},
          folder="decorations", crafted_from="stripped_poplar_log"),
    Shape("poplar_shelf", "Poplar Shelf", tags={"block": blk("wooden_shelves"), "item": blk("wooden_shelves")},
          folder="decorations", crafted_from="stripped_poplar_log"),
    Shape("campfire", "Campfire",
          tags={"block": blk("campfires", "mineable/axe", "dangerous_for_teleportation", "blocks_motion_no_leaves")},
          folder="decorations"),
    Shape("poplar_boat", "Poplar Boat", blockstates=[], loot=False, tags={"item": blk("boats"), "entity_type": blk("boat")},
          folder="transportation", crafted_from="poplar_planks", stackable=False, lang_kind="item"),
    Shape("poplar_chest_boat", "Poplar Boat with Chest", blockstates=[], loot=False, tags={"item": blk("chest_boats")},
          folder="transportation", crafted_from="poplar_boat", stackable=False, lang_kind="item"),
]
SHAPE_BY_ID = {s.vanilla: s for s in SHAPES}
LOG_SHAPES = ["poplar_log", "poplar_wood", "stripped_poplar_log", "stripped_poplar_wood"]


def dyed_id(color, vanilla):
    return f"{color}_dyed_{vanilla}"


def title(color):
    return " ".join(w.capitalize() for w in color.split("_"))


def display_name(color, shape_name):
    return f"{title(color)} Dyed {shape_name}"

# ---------------------------------------------------------------- colour maths (OKLab)


def _to_lin(c):
    c /= 255.0
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def _to_srgb(c):
    c = 12.92 * c if c <= 0.0031308 else 1.055 * (c ** (1 / 2.4)) - 0.055
    return c * 255.0


def oklab(rgb):
    r, g, b = (_to_lin(x) for x in rgb)
    l = 0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b
    m = 0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b
    s = 0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b
    l, m, s = (math.copysign(abs(x) ** (1 / 3), x) for x in (l, m, s))
    return (0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
            1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
            0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s)


def from_oklab(L, a, b):
    """OKLab -> sRGB, pulling chroma in (same hue and lightness) until it fits the gamut."""
    L = max(0.03, min(0.98, L))
    C, h = math.hypot(a, b), math.atan2(b, a)
    for _ in range(80):
        a, b = C * math.cos(h), C * math.sin(h)
        l = (L + 0.3963377774 * a + 0.2158037573 * b) ** 3
        m = (L - 0.1055613458 * a - 0.0638541728 * b) ** 3
        s = (L - 0.0894841775 * a - 1.2914855480 * b) ** 3
        lin = (4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
               -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
               -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s)
        if all(-1e-6 <= x <= 1 + 1e-6 for x in lin):
            break
        C *= 0.97
    return tuple(max(0, min(255, round(_to_srgb(max(0.0, min(1.0, x)))))) for x in lin)


def parse_hex(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def family_keys(family):
    """The family's colours sorted dark -> light, plus pixel counts."""
    counts = {parse_hex(h): n for h, n in FAMILIES[family]["palette"].items()}
    return sorted(counts, key=lambda c: oklab(c)[0]), counts


def weighted_mean_l(colors, counts):
    return sum(oklab(c)[0] * counts[c] for c in colors) / sum(counts[c] for c in colors)

# ---------------------------------------------------------------- the three dye styles
# Each maps one colour; the "ref" family supplies the averages, so a colour shared by,
# say, planks and doors always becomes the same dyed colour on both.


def planks_style(color, rgb, ref):
    """Full recolour: hue and chroma from the dye, grain contrast kept from the wood."""
    ref_keys, ref_counts = family_keys(ref)
    mean_l = weighted_mean_l(ref_keys, ref_counts)
    wool_l, wa, wb = oklab(parse_hex(WOOL[color]))
    wool_c, hue = math.hypot(wa, wb), math.atan2(wb, wa)
    l_min, l_max = 0.30, 0.88
    target_l = min(l_max, max(l_min, l_min + (wool_l - 0.20) * (l_max - l_min) / (0.94 - 0.20)))
    target_c = min(wool_c * 0.80, 0.17)
    contrast = min(1.05, max(0.65, 0.70 + 0.30 * (target_l - 0.30) / (0.64 - 0.30)))
    L = target_l + (oklab(rgb)[0] - mean_l) * contrast
    return from_oklab(L, target_c * math.cos(hue), target_c * math.sin(hue))


# Brown dye sits almost on top of bark's own colour, so it gets pushed further.
BARK_TWEAKS = {"brown": {"strength": 0.65, "chroma": 1.6, "shift_l": -0.06}}


def bark_style(color, rgb, ref, strength=0.55, chroma=0.75, pull_l=0.42):
    """Partial tint: the wood's own colour still shows through under the dye."""
    tweak = BARK_TWEAKS.get(color, {})
    strength = tweak.get("strength", strength)
    chroma *= tweak.get("chroma", 1.0)
    wool_l, wa, wb = oklab(parse_hex(WOOL[color]))
    ref_keys, ref_counts = family_keys(ref)
    # dark bark and the light cut ends shift separately so each keeps its contrast
    dark = [k for k in ref_keys if oklab(k)[0] < 0.5]
    light = [k for k in ref_keys if oklab(k)[0] >= 0.5]
    L, a, b = oklab(rgb)
    group = dark if L < 0.5 else light
    mean_l = weighted_mean_l(group, ref_counts) if group else L
    L2 = L + pull_l * (wool_l - mean_l) + tweak.get("shift_l", 0.0)
    return from_oklab(L2, (1 - strength) * a + strength * chroma * wa,
                      (1 - strength) * b + strength * chroma * wb)


PASTEL_TWEAKS = {"brown": {"chroma": 1.5, "lift": -0.02}}


def pastel_style(color, rgb, ref, strength=0.80, contrast=1.15, lift=0.06):
    """Soft pastel tint: light, low-chroma, with the stripped wood's grain kept."""
    tweak = PASTEL_TWEAKS.get(color, {})
    wool_l, wa, wb = oklab(parse_hex(WOOL[color]))
    wool_c, hue = math.hypot(wa, wb), math.atan2(wb, wa)
    target_l = 0.55 + 0.37 * max(0.0, min(1.0, (wool_l - 0.20) / 0.74))
    if wool_c > 0.05:  # chromatic dyes sit a little lighter, like pastel paint
        target_l = min(0.92, target_l + tweak.get("lift", lift))
    pastel_c = min(wool_c * 0.5 * tweak.get("chroma", 1.0), 0.085)
    pa, pb = pastel_c * math.cos(hue), pastel_c * math.sin(hue)
    ref_keys, ref_counts = family_keys(ref)
    mean_l = weighted_mean_l(ref_keys, ref_counts)
    L, a, b = oklab(rgb)
    return from_oklab(target_l + (L - mean_l) * contrast,
                      (1 - strength) * a + strength * pa, (1 - strength) * b + strength * pb)


STYLES = {"planks": planks_style, "bark": bark_style, "pastel": pastel_style}


def family_palette(family, color):
    info = FAMILIES[family]
    keys, _ = family_keys(family)
    return keys, [STYLES[info["style"]](color, k, info["ref"]) for k in keys]

# ---------------------------------------------------------------- file helpers


def write_json(rel, obj):
    path = os.path.join(RES, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def template_text(kind, name):
    with open(os.path.join(TEMPLATES, kind, name + ".json"), encoding="utf-8") as f:
        return f.read()


VANILLA_ID = re.compile(r"minecraft:(block/|blocks/|item/)?((?:stripped_)?poplar_[a-z_]+|campfire(?:_off)?(?![a-z_]))")


def retarget(text, color):
    """Point a vanilla poplar/campfire blockstate, item, loot table or recipe at the dyed variant."""
    return json.loads(VANILLA_ID.sub(lambda m: f"{MOD_ID}:{m.group(1) or ''}{color}_dyed_{m.group(2)}", text))


SPRITES = {}  # vanilla sprite id -> family
for _fam, _info in FAMILIES.items():
    for _t in _info.get("blocks", []):
        SPRITES[f"minecraft:block/{_t}"] = _fam
    for _t in _info.get("items", []):
        SPRITES[f"minecraft:item/{_t}"] = _fam


def dyed_sprite(sprite, color):
    if sprite not in SPRITES:
        raise ValueError(f"no dye palette covers {sprite}")
    return sprite + SPRITE_SEPARATOR + color


def retexture_model(text, color):
    """A vanilla poplar model, re-textured with the generated dyed sprites."""
    model = json.loads(text)
    model["textures"] = {slot: dyed_sprite(s, color) for slot, s in model.get("textures", {}).items()}
    return model


def referenced_models(obj):
    """Model ids a blockstate or item definition points at."""
    found = []
    if isinstance(obj, dict):
        for k, v in obj.items():
            if k == "model" and isinstance(v, str):
                found.append(v)
            else:
                found.extend(referenced_models(v))
    elif isinstance(obj, list):
        for v in obj:
            found.extend(referenced_models(v))
    return found

# ---------------------------------------------------------------- generators


def gen_textures():
    atlases = {"blocks": [], "items": []}
    for family, info in FAMILIES.items():
        pal_dir = os.path.join(RES, "assets", MOD_ID, "textures", "palettes", family)
        os.makedirs(pal_dir, exist_ok=True)

        def strip(colors, name):
            im = Image.new("RGB", (len(colors), 1))
            im.putdata([tuple(c) for c in colors])
            im.save(os.path.join(pal_dir, name + ".png"))

        keys, _ = family_keys(family)
        strip(keys, "key")
        for color in COLORS:
            strip(family_palette(family, color)[1], color)

        for atlas, folder in (("blocks", "block"), ("items", "item")):
            textures = info.get(atlas, [])
            if textures:
                atlases[atlas].append({
                    "type": "minecraft:paletted_permutations",
                    "textures": [f"minecraft:{folder}/{t}" for t in textures],
                    "palette_key": f"{MOD_ID}:{family}/key",
                    "permutations": {c: f"{MOD_ID}:{family}/{c}" for c in COLORS},
                    "separator": SPRITE_SEPARATOR,
                })
    for atlas, sources in atlases.items():
        write_json(f"assets/minecraft/atlases/{atlas}.json", {"sources": sources})


def gen_particles():
    smoke = {"textures": [f"minecraft:big_smoke_{i}" for i in range(12)]}
    for color in COLORS:
        for kind in ("cosy", "signal"):
            write_json(f"assets/{MOD_ID}/particles/{color}_campfire_{kind}_smoke.json", smoke)


def gen_assets():
    lang = {"itemGroup.dyeable_poplar": "Dyeable Poplar"}
    for color in COLORS:
        for shape in SHAPES:
            sid = dyed_id(color, shape.vanilla)
            models = []
            for state in shape.blockstates:
                bs = retarget(template_text("blockstates", state), color)
                write_json(f"assets/{MOD_ID}/blockstates/{dyed_id(color, state)}.json", bs)
                models += referenced_models(json.loads(template_text("blockstates", state)))
                if state != shape.vanilla:  # wall variants share the standing sign's name, keep a key anyway
                    lang[f"block.{MOD_ID}.{dyed_id(color, state)}"] = display_name(color, shape.name)
            if shape.item:
                item_def = template_text("items", shape.vanilla)
                write_json(f"assets/{MOD_ID}/items/{sid}.json", retarget(item_def, color))
                models += referenced_models(json.loads(item_def))
                lang[f"{shape.lang_kind}.{MOD_ID}.{sid}"] = display_name(color, shape.name)
            if shape.vanilla in ("poplar_boat", "poplar_chest_boat"):
                lang[f"entity.{MOD_ID}.{sid}"] = display_name(color, shape.name)

            for model in sorted(set(models)):
                kind, name = model.split(":")[1].split("/")
                out = f"assets/{MOD_ID}/models/{kind}/{color}_dyed_{name}.json"
                if shape.vanilla == "campfire" and kind == "block":
                    # child models of the vanilla campfire models: dyed logs, vanilla fire and embers
                    log = dyed_sprite("minecraft:block/campfire_log", color)
                    write_json(out, {"parent": model, "textures": {"log": log, "particle": log}})
                else:
                    folder = "models" if kind == "block" else "item_models"
                    write_json(out, retexture_model(template_text(folder, name), color))
    write_json(f"assets/{MOD_ID}/lang/en_us.json", dict(sorted(lang.items())))


def unlock_advancement(recipe_id, folder, criterion, items):
    write_json(f"data/{MOD_ID}/advancement/recipes/{folder}/{recipe_id}.json", {
        "parent": "minecraft:recipes/root",
        "criteria": {
            criterion: {"conditions": {"items": [{"items": items}]}, "trigger": "minecraft:inventory_changed"},
            "has_the_recipe": {"conditions": {"recipes": f"{MOD_ID}:{recipe_id}"}, "trigger": "minecraft:recipe_unlocked"},
        },
        "requirements": [["has_the_recipe", criterion]],
        "rewards": {"recipes": [f"{MOD_ID}:{recipe_id}"]},
    })


def recipe(rtype, category, body):
    out = {"type": rtype}
    if category:
        out["category"] = category
    out.update(body)
    return out


def item_id(color, vanilla):
    return f"{MOD_ID}:{dyed_id(color, vanilla)}"


def gen_data():
    tags = {}   # (registry, tag) -> [ids]

    def tag(registry, name, value):
        tags.setdefault((registry, name), []).append(value)

    for color in COLORS:
        for shape in SHAPES:
            sid = dyed_id(color, shape.vanilla)

            # tags
            block_tags = shape.tags.get("block", [])
            if isinstance(block_tags, dict):
                for state, names in block_tags.items():
                    for name in names:
                        tag("block", name, item_id(color, state))
            else:
                for name in block_tags:
                    tag("block", name, item_id(color, shape.vanilla))
            for name in shape.tags.get("item", []):
                tag("item", name, item_id(color, shape.vanilla))
            for name in shape.tags.get("entity_type", []):
                tag("entity_type", name, item_id(color, shape.vanilla))

            # loot table: same as vanilla, pointed at the dyed block
            if shape.loot:
                write_json(f"data/{MOD_ID}/loot_table/blocks/{sid}.json",
                           retarget(template_text("loot_table", shape.vanilla), color))

            # the vanilla recipe in dyed form: wood from logs, doors from planks, ...
            if shape.crafted_from:
                write_json(f"data/{MOD_ID}/recipe/{sid}.json", retarget(template_text("recipe", shape.vanilla), color))
                source = SHAPE_BY_ID[shape.crafted_from]
                unlock_advancement(sid, shape.folder, "has_" + source.vanilla.replace("poplar_", ""),
                                   item_id(color, source.vanilla))

            # dyeing: the undyed version or any other colour can be (re)dyed
            natural = f"minecraft:{shape.vanilla}"
            sources = [natural] + [item_id(c, shape.vanilla) for c in COLORS if c != color]
            every_variant = [natural] + [item_id(c, shape.vanilla) for c in COLORS]
            group = f"dyed_{shape.vanilla}"
            dye = f"minecraft:{color}_dye"
            criterion = f"has_{shape.vanilla}"

            if shape.stackable:
                write_json(f"data/{MOD_ID}/recipe/dye_{sid}_bulk.json", recipe("minecraft:crafting_shaped", shape.category, {
                    "group": group,
                    "key": {"#": sources, "X": dye},
                    "pattern": ["###", "#X#", "###"],
                    "result": {"count": 8, "id": item_id(color, shape.vanilla)},
                }))
                unlock_advancement(f"dye_{sid}_bulk", shape.folder, criterion, every_variant)

            write_json(f"data/{MOD_ID}/recipe/dye_{sid}.json", recipe("minecraft:crafting_shapeless", shape.category, {
                "group": group,
                "ingredients": [dye, sources],
                "result": {"id": item_id(color, shape.vanilla)},
            }))
            unlock_advancement(f"dye_{sid}", shape.folder, criterion, every_variant)

        logs = [item_id(color, s) for s in LOG_SHAPES]

        # dyed logs of any kind saw into planks of the same colour
        planks = dyed_id(color, "poplar_planks")
        write_json(f"data/{MOD_ID}/recipe/{planks}_from_logs.json", recipe("minecraft:crafting_shapeless", "building", {
            "group": "planks",
            "ingredients": [logs],
            "result": {"count": 4, "id": f"{MOD_ID}:{planks}"},
        }))
        unlock_advancement(f"{planks}_from_logs", "building_blocks", "has_logs", logs)

        # a campfire built from dyed logs smokes in that colour
        campfire = dyed_id(color, "campfire")
        write_json(f"data/{MOD_ID}/recipe/{campfire}.json", recipe("minecraft:crafting_shaped", None, {
            "key": {"C": "#minecraft:coals", "L": logs, "S": "minecraft:stick"},
            "pattern": [" S ", "SCS", "LLL"],
            "result": {"id": f"{MOD_ID}:{campfire}"},
        }))
        unlock_advancement(campfire, "decorations", "has_logs", logs)

    for (registry, name), values in tags.items():
        write_json(f"data/minecraft/tags/{registry}/{name}.json", {"values": values})


def gen_icon():
    """An original 128x128 icon: a stack of boards in the 16 dye colours."""
    im = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    order = ["red", "orange", "yellow", "lime", "green", "cyan", "light_blue", "blue",
             "purple", "magenta", "pink", "white", "light_gray", "gray", "brown", "black"]
    d.rounded_rectangle([4, 4, 123, 123], radius=14, fill=(32, 30, 34, 255))
    board_h = 6
    for i, color in enumerate(order):
        pal = family_palette("poplar_planks", color)[1]
        y = 12 + i * board_h
        light, mid, dark = pal[5], pal[3], pal[0]
        d.rectangle([14, y, 113, y + board_h - 2], fill=mid)
        d.line([14, y, 113, y], fill=light)
        d.line([14, y + board_h - 2, 113, y + board_h - 2], fill=dark)
        seam = 30 + (i * 37) % 70
        d.line([seam, y, seam, y + board_h - 2], fill=dark)
    im.save(os.path.join(RES, "assets", MOD_ID, "icon.png"))


def main():
    for sub in (f"assets/{MOD_ID}", "assets/minecraft", f"data/{MOD_ID}", "data/minecraft"):
        shutil.rmtree(os.path.join(RES, sub), ignore_errors=True)
    gen_textures()
    gen_particles()
    gen_assets()
    gen_data()
    gen_icon()
    count = sum(len(files) for _, _, files in os.walk(RES))
    print(f"wrote resources ({count} files under src/main/resources)")


if __name__ == "__main__":
    main()
