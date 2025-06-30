package me.Herzchen.RandomLootChest;

import java.util.function.Function;
import org.bukkit.Location;
import org.bukkit.Sound;

public class SoundWrapper {
   private Sound sound;

   private SoundWrapper(Sound sound) {
      this.sound = sound;
   }

   void play(Location location) {
      if (this.sound != null) {
         location.getWorld().playSound(location, this.sound, 1.0F, 1.0F);
      }

   }

   public static SoundWrapper create(String soundName, SoundWrapper defaultValue, Function<String, SoundWrapper> notFound) {
      if (soundName != null && !soundName.trim().isEmpty()) {
         String[] var3 = soundName.split("[\\s|;,]+");
         int var4 = var3.length;
         int var5 = 0;

         while(var5 < var4) {
            String s = var3[var5];
            if (s.equalsIgnoreCase("NONE")) {
               return null;
            }

            try {
               Sound sound = Sound.valueOf(s.toUpperCase());
               return new SoundWrapper(sound);
            } catch (Exception var8) {
               ++var5;
            }
         }

         return notFound != null ? (SoundWrapper)notFound.apply(soundName) : null;
      } else {
         return defaultValue;
      }
   }

   public static SoundWrapper create(String soundName, Function<String, SoundWrapper> notFound) {
      return create(soundName, notFound);
   }

   public static SoundWrapper createNotNull(String soundName, SoundWrapper defaultValue, Function<String, SoundWrapper> notFound) {
      SoundWrapper effect = create(soundName, defaultValue, notFound);
      return effect != null ? effect : new SoundWrapper((Sound)null);
   }

   public static SoundWrapper createNotNull(String soundName, Function<String, SoundWrapper> notFound) {
      return createNotNull(soundName, notFound);
   }
}
