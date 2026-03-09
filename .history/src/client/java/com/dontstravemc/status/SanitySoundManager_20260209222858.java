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
    
    private static boolean isSessionActive = false;
    private static int sessionTicks = 0;
    private static int nextSessionDelay = 0;
    
    private static SoundInstance currentLoop = null;
    private static SoundInstance currentWhisper = null;
    private static SoundInstance currentExtra = null;
    
    private static int whisperTicksLeft = 0;
    private static int loopDurationTicks = 0;
    private static float currentSanityFactor = 0.0f;

    public static void tick(Player player, float sanity) {
        if (player == null) return;
        
        // Calculate scaling factor: 45% -> 0.0, 0% -> 1.0
        if (sanity < 45.0f) {
            currentSanityFactor = (45.0f - sanity) / 45.0f;
        } else {
            currentSanityFactor = 0.0f;
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
        
        boolean useLoop1 = RANDOM.nextBoolean();
        SoundEvent loopEvent = useLoop1 ? ModSounds.SANITY_LOOP1 : ModSounds.SANITY_LOOP2;
        loopDurationTicks = useLoop1 ? 23 * 20 : 10 * 20;
        
        // Loop volume lower as requested (max 0.3)
        currentLoop = SimpleSoundInstance.forUI(loopEvent, 1.0f, 0.3f * currentSanityFactor);
        client.getSoundManager().play(currentLoop);
    }

    private static void updateSession() {
        Minecraft client = Minecraft.getInstance();
        sessionTicks++;
        
        // Loop Fading and Scaling
        if (currentLoop != null) {
            // Factor in sanity and a 2s fade-in
            float baseVol = 0.4f * currentSanityFactor;
            float fadeFactor = Math.min(1.0f, sessionTicks / 40.0f);
            // Note: SimpleSoundInstance volume isn't dynamically updatable usually, 
            // but we can at least set it better at start. For dynamic scaling mid-loop, 
            // we'd need a custom TickableSoundInstance. 
            // For now, we'll stick to scaling the trigger volume of randoms/extras.
        }
        
        // 1s later: start random whispers
        if (sessionTicks == 20) {
            startNextWhisper();
        }
        
        // 3s later: start crazy extra
        if (sessionTicks == 60) {
            float extraVol = 0.45f * currentSanityFactor;
            currentExtra = SimpleSoundInstance.forUI(ModSounds.CRAZY_EXTRA, 1.0f, extraVol);
            client.getSoundManager().play(currentExtra);
        }
        
        // Sequential random whispers with overlap
        if (sessionTicks > 20 && currentWhisper != null) {
            if (whisperTicksLeft > 0) {
                whisperTicksLeft--;
                // Start next whisper 10 ticks (0.5s) early to overlap
                if (whisperTicksLeft == 10) {
                    startNextWhisper();
                }
            }
        }
        
        // End session
        if (sessionTicks >= loopDurationTicks) {
            stopAll();
            isSessionActive = false;
            nextSessionDelay = 100 + RANDOM.nextInt(200);
        }
    }

    private static void startNextWhisper() {
        Minecraft client = Minecraft.getInstance();
        float whisperVol = 0.65f * currentSanityFactor;
        
        currentWhisper = SimpleSoundInstance.forUI(ModSounds.SANITY_RANDOM, 1.0f, whisperVol);
        client.getSoundManager().play(currentWhisper);
        
        // Duration check: between 2 and 5 seconds
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
