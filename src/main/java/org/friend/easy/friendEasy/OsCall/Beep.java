package org.friend.easy.friendEasy.OsCall;
import com.sun.jna.Library;
import com.sun.jna.Native;
import org.bukkit.plugin.Plugin;
import javax.sound.sampled.*;


public class Beep {
    public static void Beep(Plugin plugin) {
        int duration = 1000; // 毫秒
        int sampleRate = 44100; // 采样率
        double frequency = 440.0; // 频率（Hz）

        try {
            byte[] buffer = new byte[1 * sampleRate * 16 / 8];
            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            line.open(format);
            line.start();

            for (int i = 0; i < buffer.length; i++) {
                double angle = 2.0 * Math.PI * frequency * i / sampleRate;
                buffer[i] = (byte) (Math.sin(angle) * 127.0);
            }

            line.write(buffer, 0, buffer.length);
            line.drain();
            line.close();
        } catch (LineUnavailableException e) {

        }finally {
            plugin.getLogger().info("Beep!");
        }

    }
}