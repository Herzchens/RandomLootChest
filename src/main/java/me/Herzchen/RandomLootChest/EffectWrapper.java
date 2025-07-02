package me.Herzchen.RandomLootChest;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class EffectWrapper {
   private Consumer<Location> play = null;
   private static final Map<String, String> PARTICLE_MAPPINGS = new HashMap<>();
   private static final boolean IS_NEW_API;

   static {
      String serverVersion = Bukkit.getBukkitVersion();
      String mainVersion = serverVersion.split("-")[0];
      int majorVersion = Integer.parseInt(mainVersion.split("\\.")[0]);
      int minorVersion = Integer.parseInt(mainVersion.split("\\.")[1]);
      int patchVersion = 0;
      if (mainVersion.split("\\.").length > 2) {
         patchVersion = Integer.parseInt(mainVersion.split("\\.")[2]);
      }

      IS_NEW_API = (minorVersion >= 13);
      PARTICLE_MAPPINGS.put("COLOURED_DUST", "REDSTONE");
      PARTICLE_MAPPINGS.put("EXPLOSION", IS_NEW_API ? "POOF" : "EXPLOSION_NORMAL");
      PARTICLE_MAPPINGS.put("FLYING_GLYPH", "ENCHANT");
      PARTICLE_MAPPINGS.put("HAPPY_VILLAGER", "VILLAGER_HAPPY");
      PARTICLE_MAPPINGS.put("INSTANT_SPELL", "INSTANT_EFFECT");
      PARTICLE_MAPPINGS.put("ITEM_BREAK", "ITEM_CRACK");
      PARTICLE_MAPPINGS.put("LARGE_SMOKE", "LARGE_SMOKE");
      PARTICLE_MAPPINGS.put("LAVA_POP", "LAVA");
      PARTICLE_MAPPINGS.put("LAVADRIP", "DRIPPING_LAVA");
      PARTICLE_MAPPINGS.put("MAGIC_CRIT", "ENCHANTED_HIT");
      PARTICLE_MAPPINGS.put("PARTICLE_SMOKE", "SMOKE");
      PARTICLE_MAPPINGS.put("POTION_SWIRL", "ENTITY_EFFECT");
      PARTICLE_MAPPINGS.put("POTION_SWIRL_TRANSPARENT", "AMBIENT_ENTITY_EFFECT");
      PARTICLE_MAPPINGS.put("SMALL_SMOKE", "SMALL_SMOKE");
      PARTICLE_MAPPINGS.put("SNOWBALL_BREAK", "ITEM_SNOWBALL");
      PARTICLE_MAPPINGS.put("SPLASH", "SPLASH");
      PARTICLE_MAPPINGS.put("TILE_BREAK", "BLOCK_CRACK");
      PARTICLE_MAPPINGS.put("TILE_DUST", "BLOCK_DUST");
      PARTICLE_MAPPINGS.put("VILLAGER_THUNDERCLOUD", "ANGRY_VILLAGER");
      PARTICLE_MAPPINGS.put("VOID_FOG", "UNDERWATER");
      PARTICLE_MAPPINGS.put("WATERDRIP", "DRIPPING_WATER");
      PARTICLE_MAPPINGS.put("WITCH_MAGIC", "WITCH");

      if (IS_NEW_API) {
         PARTICLE_MAPPINGS.put("EXPLOSION_NORMAL", "POOF");
         PARTICLE_MAPPINGS.put("REDSTONE", "DUST");
         PARTICLE_MAPPINGS.put("ITEM_CRACK", "ITEM");
         PARTICLE_MAPPINGS.put("BLOCK_CRACK", "BLOCK");
         PARTICLE_MAPPINGS.put("BLOCK_DUST", "BLOCK");
         PARTICLE_MAPPINGS.put("SPELL_MOB", "ENTITY_EFFECT");
         PARTICLE_MAPPINGS.put("SPELL_MOB_AMBIENT", "AMBIENT_ENTITY_EFFECT");
      }
   }

   private EffectWrapper(Object effect) {
      if (effect instanceof Particle particle) {
         this.play = location -> {
            World world = location.getWorld();
            Location center = location.clone().add(0.5, 0.5, 0.5);
            if (IS_NEW_API) {
               world.spawnParticle(particle, center, 50, 0.1, 0.1, 0.1, 0.05);
            } else {
               switch (particle.name()) {
                  case "REDSTONE":
                     break;
                  default:
                     world.spawnParticle(particle, center, 50, 0.1, 0.1, 0.1, 0.05);
               }
            }
         };
      } else if (effect instanceof Effect) {
         Effect eff = (Effect) effect;
         this.play = location -> location.getWorld().playEffect(location, eff, 1);
      }
   }

   void play(Location location) {
      if (this.play != null) {
         this.play.accept(location);
      }
   }

   public static EffectWrapper create(String effectName, EffectWrapper defaultValue, Function<String, EffectWrapper> notFound) {
      if (effectName == null || effectName.trim().isEmpty()) {
         return defaultValue;
      }

      String[] parts = effectName.split("[\\s|;,]+");
      for (String part : parts) {
         String name = part.toUpperCase().trim();
         if (name.equals("NONE")) {
            return null;
         }

         String mappedName = PARTICLE_MAPPINGS.getOrDefault(name, name);

         try {
            return new EffectWrapper(Particle.valueOf(mappedName));
         } catch (IllegalArgumentException e1) {
            if (!IS_NEW_API) {
               try {
                  Effect effect = Effect.valueOf(mappedName);
                  if (effect.getType() != Effect.Type.SOUND) {
                     return new EffectWrapper(effect);
                  }
               } catch (IllegalArgumentException e2) {
               }
            }
         }
      }

      return notFound != null ? notFound.apply(effectName) : null;
   }

   public static EffectWrapper create(String effectName, Function<String, EffectWrapper> notFound) {
      return create(effectName, null, notFound);
   }

   public static EffectWrapper createNotNull(String effectName, EffectWrapper defaultValue, Function<String, EffectWrapper> notFound) {
      EffectWrapper effect = create(effectName, defaultValue, notFound);
      return effect != null ? effect : new EffectWrapper((Object) null);
   }

   public static EffectWrapper createNotNull(String effectName, Function<String, EffectWrapper> notFound) {
      return createNotNull(effectName, null, notFound);
   }
}