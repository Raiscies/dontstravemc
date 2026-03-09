package com.dontstravemc.status;

import com.dontstravemc.event.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

import java.util.Random;

public class SanitySoundManager {
    private static final Random RANDOM = new Random();
    
    // Sesion states
    private static boolean isSessionActive = false;
    private static int sessionTicks = 0;
    private static int nextSessionDelay = 0;
    
    private static SoundInstance currentLoop = null;
    private static SoundInstance currentWhisper = null;
    private static SoundInstance nextWhisper = null;
    private static SoundInstance currentExtra = null;
    
    private static int whisperTicksLeft = 0;
    private static int loopDurationTicks = 0;

    public static void tick(Player player, float sanity) {
        if (player == null || !player.level().isClientSide) return;
        
        if (sanity >= 45.0f) {
            stopAll();
            return;
        }

        if (!isSessionActive) {
            if (nextSessionDelay > 0) {
                nextSessionDelay--;
            } else {
                startSession();
            }
        } else {
            updateSession();
        }
    }

    private static void startSession() {
        Minecraft client = Minecraft.getInstance();
        isSessionActive = true;
        sessionTicks = 0;
        
        // Pick a loop
        boolean useLoop1 = RANDOM.nextBoolean();
        SoundEvent loopEvent = useLoop1 ? ModSounds.SANITY_LOOP1 : ModSounds.SANITY_LOOP2;
        loopDurationTicks = useLoop1 ? 23 * 20 : 10 * 20;
        
        // Loop volume lower as requested
        currentLoop = SimpleSoundInstance.forUI(loopEvent, 1.0f, 0.4f);
        client.getSoundManager().play(currentLoop);
    }

    private static void updateSession() {
        Minecraft client = Minecraft.getInstance();
        sessionTicks++;
        
        // 1s (20 ticks) later: start random whispers
        if (sessionTicks == 20) {
            startNextWhisper();
        }
        
        // 3s (60 ticks) later: start crazy extra
        if (sessionTicks == 60) {
            currentExtra = SimpleSoundInstance.forUI(ModSounds.CRAZY_EXTRA, 1.0f, 0.6f);
            client.getSoundManager().play(currentExtra);
        }
        
        // Handle sequential random whispers
        if (sessionTicks > 20 && currentWhisper != null) {
            if (whisperTicksLeft > 0) {
                whisperTicksLeft--;
            } else {
                startNextWhisper();
            }
        }
        
        // End session
        if (sessionTicks >= loopDurationTicks) {
            stopAll();
            isSessionActive = false;
            // Set random delay before next session (e.g., 5-15 seconds)
            nextSessionDelay = 100 + RANDOM.nextInt(200);
        }
    }

    private static void startNextWhisper() {
        Minecraft client = Minecraft.getInstance();
        // Play random whisper
        currentWhisper = SimpleSoundInstance.forUI(ModSounds.SANITY_RANDOM, 1.0f, 0.8f);
        client.getSoundManager().play(currentWhisper);
        // We don't know the exact length of each random ogg easily from here, 
        // so we'll assume a reasonable average or a fixed minimum (e.g. 2-4 seconds)
        // Alternatively, we could just trigger another after a fixed time.
        // Let's use a random duration between 2 and 5 seconds for variety if we can't get ogg lengths.
        whisperTicksLeft = 40 + RANDOM.nextInt(60); 
    }

    private static void stopAll() {
        Minecraft client = Minecraft.getInstance();
        if (currentLoop != null) client.getSoundManager().stop(currentLoop);
        if (currentWhisper != null) client.getSoundManager().stop(currentWhisper);
        if (currentExtra != null) client.getSoundManager().stop(currentExtra);
        currentLoop = null;
        currentWhisper = null;
        currentExtra = null;
        isSessionActive = false;
    }
}
