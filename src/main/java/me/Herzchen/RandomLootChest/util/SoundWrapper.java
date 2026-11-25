package me.Herzchen.RandomLootChest.util;

import java.util.function.Function;
import org.bukkit.Location;
import org.bukkit.Sound;

public class SoundWrapper {
   private Sound sound;
   private SoundWrapper(Sound sound) { this.sound = sound; }

   public void play(Location location) {
      if (this.sound != null) location.getWorld().playSound(location, this.sound, 1.0F, 1.0F);
   }

   public static SoundWrapper create(String soundName, SoundWrapper defaultValue, Function<String, SoundWrapper> notFound) {
      if (soundName != null && !soundName.trim().isEmpty()) {
         for (String s : soundName.split("[\\s|;,]+")) {
            if (s.equalsIgnoreCase("NONE")) return null;
            try { return new SoundWrapper(Sound.valueOf(s.toUpperCase())); }
            catch (Exception ignored) {}
         }
         return notFound != null ? notFound.apply(soundName) : null;
      }
      return defaultValue;
   }

   public static SoundWrapper createNotNull(String soundName, SoundWrapper defaultValue, Function<String, SoundWrapper> notFound) {
      SoundWrapper effect = create(soundName, defaultValue, notFound);
      return effect != null ? effect : new SoundWrapper(null);
   }
}