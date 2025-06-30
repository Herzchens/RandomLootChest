package me.Herzchen.RandomLootChest;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Effect.Type;

public class EffectWrapper {
   private Consumer<Location> play = null;
   private static Map<String, String> maptoParticle = new HashMap();

   private EffectWrapper(Object effect) {
      if (effect != null) {
         if (effect instanceof Effect) {
            Effect bukkitEffect = (Effect)effect;
            if (!bukkitEffect.getType().name().equals("PARTICLE")) {
               this.play = (location) -> {
                  location.getWorld().playEffect(location, bukkitEffect, 1);
               };
            } else {
               this.play = (location) -> {
                  location.getWorld().spigot().playEffect(location.clone().add(0.5D, 0.5D, 0.5D), bukkitEffect, 0, 0, 0.1F, 0.1F, 0.1F, 0.05F, 50, 30);
               };
            }
         } else {
            Particle particle = (Particle)effect;
            this.play = (location) -> {
               location.getWorld().spawnParticle(particle, location.clone().add(0.5D, 0.5D, 0.5D), 50, 0.1D, 0.1D, 0.1D, 0.05D);
            };
         }
      }

   }

   void play(Location location) {
      if (this.play != null) {
         this.play.accept(location);
      }

   }

   public static EffectWrapper create(String effectName, EffectWrapper defaultValue, Function<String, EffectWrapper> notFound) {
      if (effectName != null && !effectName.trim().isEmpty()) {
         String[] var3 = effectName.split("[\\s|;,]+");
         int var4 = var3.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            String s = var3[var5];
            String name = s.toUpperCase();
            if (name.equals("NONE")) {
               return null;
            }

            try {
               Effect effect = Effect.valueOf(name);
               if (effect.getType() != Type.SOUND) {
                  return new EffectWrapper(effect);
               }
            } catch (Exception var11) {
               try {
                  return new EffectWrapper(Particle.valueOf(name));
               } catch (Exception var10) {
                  try {
                     if (maptoParticle.containsKey(name)) {
                        return new EffectWrapper(Particle.valueOf((String)maptoParticle.get(name)));
                     }
                  } catch (Exception var9) {
                  }
               }
            }
         }

         return notFound != null ? (EffectWrapper)notFound.apply(effectName) : null;
      } else {
         return defaultValue;
      }
   }

   public static EffectWrapper create(String effectName, Function<String, EffectWrapper> notFound) {
      return create(effectName, notFound);
   }

   public static EffectWrapper createNotNull(String effectName, EffectWrapper defaultValue, Function<String, EffectWrapper> notFound) {
      EffectWrapper effect = create(effectName, defaultValue, notFound);
      return effect != null ? effect : new EffectWrapper((Object)null);
   }

   public static EffectWrapper createNotNull(String effectName, Function<String, EffectWrapper> notFound) {
      return createNotNull(effectName, notFound);
   }

   static {
      maptoParticle.put("COLOURED_DUST", "REDSTONE");
      maptoParticle.put("EXPLOSION", "EXPLOSION_NORMAL");
      maptoParticle.put("FLYING_GLYPH", "ENCHANTMENT_TABLE");
      maptoParticle.put("HAPPY_VILLAGER", "VILLAGER_HAPPY");
      maptoParticle.put("INSTANT_SPELL", "SPELL_INSTANT");
      maptoParticle.put("ITEM_BREAK", "ITEM_CRACK");
      maptoParticle.put("LARGE_SMOKE", "SMOKE_LARGE");
      maptoParticle.put("LAVA_POP", "LAVA");
      maptoParticle.put("LAVADRIP", "DRIP_LAVA");
      maptoParticle.put("MAGIC_CRIT", "CRIT_MAGIC");
      maptoParticle.put("PARTICLE_SMOKE", "SMOKE_NORMAL");
      maptoParticle.put("POTION_SWIRL", "SPELL_MOB");
      maptoParticle.put("POTION_SWIRL_TRANSPARENT", "SPELL_MOB_AMBIENT");
      maptoParticle.put("SMALL_SMOKE", "TOWN_AURA");
      maptoParticle.put("SNOWBALL_BREAK", "SNOWBALL");
      maptoParticle.put("SPLASH", "WATER_SPLASH");
      maptoParticle.put("TILE_BREAK", "BLOCK_CRACK");
      maptoParticle.put("TILE_DUST", "BLOCK_DUST");
      maptoParticle.put("VILLAGER_THUNDERCLOUD", "VILLAGER_ANGRY");
      maptoParticle.put("VOID_FOG", "SUSPENDED_DEPTH");
      maptoParticle.put("WATERDRIP", "DRIP_WATER");
      maptoParticle.put("WITCH_MAGIC", "SPELL_WITCH");
   }
}
