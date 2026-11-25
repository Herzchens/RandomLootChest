package me.Herzchen.RandomLootChest.util;

import me.Herzchen.RandomLootChest.Main;
import org.bukkit.Material;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

public class MaterialCondition {
    private boolean empty = true;
    private boolean negative;
    private boolean Fuel, Record, Occluding, Transparent, Block, Burnable, Edible, Flammable, Solid, Gravity;
    public HashSet<Material> Materials = new HashSet<>();

    public MaterialCondition() {}

    public MaterialCondition(String txt, boolean negative) {
        if (txt != null) {
            this.negative = negative;
            HashMap<String, Material> allMaterial = new HashMap<>(Arrays.stream(Material.values())
                    .collect(Collectors.toMap(x -> x.name().toUpperCase(), x -> x)));
            for (String word : txt.split("[\\s;,]+")) {
                boolean isWordNegative = word.startsWith("!");
                if (isWordNegative == negative) {
                    this.empty = false;
                    String upperWord = (isWordNegative ? word.substring(1) : word).toUpperCase();
                    switch (upperWord) {
                        case "_FUEL_": Fuel = true; break;
                        case "_RECORD_": Record = true; break;
                        case "_OCCLUDING_": Occluding = true; break;
                        case "_TRANSPARENT_": Transparent = true; break;
                        case "_BLOCK_": Block = true; break;
                        case "_BURNABLE_": Burnable = true; break;
                        case "_EDIBLE_": Edible = true; break;
                        case "_FLAMMABLE_": Flammable = true; break;
                        case "_SOLID_": Solid = true; break;
                        case "_GRAVITY_": Gravity = true; break;
                        default:
                            Material m = allMaterial.get(upperWord);
                            if (m != null) Materials.add(m);
                            else MessageUtil.sendConsole("<red>cảnh báo: Vật liệu không xác định '<yellow>" + upperWord + "<red>' trong điều kiện");
                    }
                }
            }
        }
    }

    public boolean isMatch(Material material) {
        return empty || negative != (Fuel && material.isFuel() || Record && material.isRecord()
                || Occluding && material.isOccluding() || Transparent && material.isTransparent()
                || Block && material.isBlock() || Burnable && material.isBurnable()
                || Edible && material.isEdible() || Flammable && material.isFlammable()
                || Solid && material.isSolid() || Gravity && material.hasGravity()
                || Materials.contains(material));
    }
}