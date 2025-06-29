package com.example.innerquest;

import androidx.appcompat.app.AppCompatActivity;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AppActivity extends AppCompatActivity {

    private enum Program { NONE, B3, B4, B5 }
    private enum NoiseType { SOFT, SURF }

    // Nuevo enum para el modo del botón WnTone
    private enum AudioButtonMode { OFF, SINE_WAVE, WHITE_NOISE }

    //Variables de estado
    private boolean isPowerOn = false;
    private Program currentProgram = Program.NONE;
    private NoiseType currentNoiseType = NoiseType.SURF;

    // Controla el modo del botón WnTone: apagado, tono sinusoidal, o ruido blanco
    private AudioButtonMode currentAudioButtonMode = AudioButtonMode.OFF;

    // Nueva variable para controlar el volumen interno (de 0.0 a 1.0)
    private float internalVolume = 0.5f; // Inicializa a un volumen medio (50%)
    private final float VOLUME_STEP = 0.1f; // Paso para aumentar/disminuir el volumen

    // Nuevas variables para controlar la frecuencia del tono puro
    private double pureToneFrequency = 440.0; // Frecuencia inicial (ej. La 440Hz)
    private final double PITCH_STEP = 10.0; // Paso para aumentar/disminuir la frecuencia
    private final double MIN_TONE_FREQ = 50.0; // Frecuencia mínima
    private final double MAX_TONE_FREQ = 1000.0; // Frecuencia máxima

    // NUEVA: Variable para controlar el estado de pausa/reanudación
    private volatile boolean isPaused = false; // Estado de pausa

    // Variables para la generacion de audio
    private volatile boolean isAudioPlaying = false; // Control general del hilo de audio
    private AudioTrack audioTrack;
    private Thread audioThread;
    private final int SAMPLE_RATE = 44100; // Calidad de CD

    // Variable para controlar el tiempo de ejecución del programa
    private long programStartTime; // Guarda el System.currentTimeMillis() cuando el programa comienza
    private long pauseStartTime; // Guarda el tiempo cuando se pausa
    private long totalPausedTime = 0; // Acumula el tiempo total en pausa

    // Interfaz
    private Button btnPwr, btnPgmSel, btnPseRun, btnWnTone, btnChangeNoise;
    private ImageView ledEarsR, ledEyesL, ledB, ledA;
    private List<ImageView> programLeds;
    private TextView programDisplayText;
    // Recursos Drawable para los LEDs
    private int ledOff;
    private int ledRedOn;
    private int ledOrangeOn;
    private int ledGreenOn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app);
        // Inicializar los recursos drawable
        ledOff = R.drawable.led_off;
        ledOrangeOn = R.drawable.led_orange_on;
        ledGreenOn = R.drawable.led_green_on;
        setupUI();
        setupClickListeners();
    }

    private void setupUI() {
        btnPwr = findViewById(R.id.btn_pwr); //Boton encendido
        btnPgmSel = findViewById(R.id.btn_pgm_sel); // Boton Para seleccionar programa
        btnWnTone = findViewById(R.id.btn_wn_tone); // Boton para activar el ruido blanco
        btnChangeNoise = findViewById(R.id.btn_change_noise); // Boton para cambiar a ruido tipo surf
        ledEyesL = findViewById(R.id.led_eyes_l); //Led ojo Izquierdo
        ledEarsR = findViewById(R.id.led_ears_r); //Led programa A
        ledB = findViewById(R.id.led_b); // Led progama B
        ledA = findViewById(R.id.led_a); //Led ojo derecho

        findViewById(R.id.btn_vol_up).setOnClickListener(v -> increaseVolume());
        findViewById(R.id.btn_vol_down).setOnClickListener(v -> decreaseVolume());

        findViewById(R.id.btn_pitch_up).setOnClickListener(v -> increasePitch());
        findViewById(R.id.btn_pitch_down).setOnClickListener(v -> decreasePitch());

        // MODIFICADO: Asignar el listener para el botón PSE RUN
        btnPseRun = findViewById(R.id.btn_pse_run);
        btnPseRun.setOnClickListener(v -> togglePauseRun());

        programLeds = new ArrayList<>();
        programLeds.add(findViewById(R.id.led_1));
        programLeds.add(findViewById(R.id.led_2));
        programLeds.add(findViewById(R.id.led_3));
        programLeds.add(findViewById(R.id.led_4));
        programLeds.add(findViewById(R.id.led_5));
        programLeds.add(findViewById(R.id.led_6));
    }

    private void setupClickListeners() {
        btnPwr.setOnClickListener(v -> togglePower());
        btnPgmSel.setOnClickListener(v -> selectNextProgram());
        btnWnTone.setOnClickListener(v -> toggleAudioButtonMode());
        btnChangeNoise.setOnClickListener(v -> changeNoiseType());
    }

    private void togglePower() {
        isPowerOn = !isPowerOn;
        if (isPowerOn) {
            programStartTime = System.currentTimeMillis();
            totalPausedTime = 0; // Resetear tiempo de pausa al encender
            isPaused = false; // Asegurarse de que no esté en pausa
            startAudioThread();
            showToast("Dispositivo Encendido");
        } else {
            stopAudioThread();
            currentProgram = Program.NONE;
            currentAudioButtonMode = AudioButtonMode.OFF;
            isPaused = false; // Asegurarse de que no esté en pausa al apagar
            showToast("Dispositivo Apagado");
            // Apagar todos los LEDs al apagar el dispositivo
            ledEarsR.setImageResource(ledOff);
            ledB.setImageResource(ledOff);
            ledEyesL.setImageResource(ledOff);
            ledA.setImageResource(ledOff);
            for (ImageView led : programLeds) {
                led.setImageResource(ledOff);
            }
        }
        updateAllLeds(currentProgram);
    }

    private void selectNextProgram() {
        if (!isPowerOn) {
            showToast("Enciende el dispositivo primero.");
            return;
        }

        Program oldProgram = currentProgram;
        switch (currentProgram) {
            case NONE:
                currentProgram = Program.B3;
                break;
            case B3:
                currentProgram = Program.B4;
                break;
            case B4:
                currentProgram = Program.B5;
                break;
            case B5:
                currentProgram = Program.NONE;
                break;
        }

        if (currentProgram != oldProgram) {
            programStartTime = System.currentTimeMillis();
            totalPausedTime = 0; // Resetear tiempo de pausa al cambiar de programa
            isPaused = false; // Asegurarse de que no esté en pausa
            showToast("Programa Seleccionado: " + currentProgram.name());
            updateAllLeds(currentProgram);

            if (currentProgram != Program.NONE || currentAudioButtonMode != AudioButtonMode.OFF) {
                if (!isAudioPlaying) {
                    startAudioThread();
                }
            } else {
                stopAudioThread();
            }
        }
    }

    // NUEVA: Función para alternar la pausa/reanudación
    private void togglePauseRun() {
        if (!isPowerOn) {
            showToast("Enciende el dispositivo primero.");
            return;
        }
        // Solo permitir pausa si hay un programa o audio activo
        if (currentProgram == Program.NONE && currentAudioButtonMode == AudioButtonMode.OFF) {
            showToast("No hay programa o audio activo para pausar.");
            return;
        }

        isPaused = !isPaused; // Alternar el estado de pausa

        if (isPaused) {
            pauseStartTime = System.currentTimeMillis(); // Registrar el inicio de la pausa
            // Si el AudioTrack está en reproducción, pausarlo
            if (audioTrack != null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                audioTrack.pause();
                audioTrack.flush(); // Limpiar el buffer para evitar que siga sonando un poco
            }
            showToast("Reproducción en Pausa");
        } else {
            // Calcular el tiempo que estuvo en pausa y sumarlo
            totalPausedTime += (System.currentTimeMillis() - pauseStartTime);
            // Reanudar la reproducción del AudioTrack
            if (audioTrack != null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PAUSED) {
                audioTrack.play();
            }
            showToast("Reproducción Reanudada");
        }
    }

    // Funciones para controlar el volumen interno
    private void increaseVolume() {
        if (!isPowerOn) {
            showToast("Enciende el dispositivo primero.");
            return;
        }
        internalVolume = Math.min(1.0f, internalVolume + VOLUME_STEP);
        showToast(String.format("Volumen: %.0f%%", internalVolume * 100));
    }

    private void decreaseVolume() {
        if (!isPowerOn) {
            showToast("Enciende el dispositivo primero.");
            return;
        }
        internalVolume = Math.max(0.0f, internalVolume - VOLUME_STEP);
        showToast(String.format("Volumen: %.0f%%", internalVolume * 100));
    }

    // Nuevas funciones para controlar la frecuencia del tono puro
    private void increasePitch() {
        if (!isPowerOn) {
            showToast("Enciende el dispositivo primero.");
            return;
        }
        if (currentAudioButtonMode != AudioButtonMode.SINE_WAVE) {
            showToast("Activa el modo Onda Sinusoidal primero.");
            return;
        }
        pureToneFrequency = Math.min(MAX_TONE_FREQ, pureToneFrequency + PITCH_STEP);
        showToast(String.format("Frecuencia Tono Puro: %.0f Hz", pureToneFrequency));
    }

    private void decreasePitch() {
        if (!isPowerOn) {
            showToast("Enciende el dispositivo primero.");
            return;
        }
        if (currentAudioButtonMode != AudioButtonMode.SINE_WAVE) {
            showToast("Activa el modo Tono primero.");
            return;
        }
        pureToneFrequency = Math.max(MIN_TONE_FREQ, pureToneFrequency - PITCH_STEP);
        showToast(String.format("Frecuencia Tono Puro: %.0f Hz", pureToneFrequency));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void toggleAudioButtonMode() {
        if (!isPowerOn) {
            showToast("Enciende el dispositivo primero.");
            return;
        }

        switch (currentAudioButtonMode) {
            case OFF:
                currentAudioButtonMode = AudioButtonMode.SINE_WAVE;
                showToast("Modo de audio: Onda sinusoidal");
                break;
            case SINE_WAVE:
                currentAudioButtonMode = AudioButtonMode.WHITE_NOISE;
                showToast("Modo de audio: Ruido Blanco");
                break;
            case WHITE_NOISE:
                currentAudioButtonMode = AudioButtonMode.OFF;
                showToast("Modo de audio: Apagado");
                break;
        }
        // Asegurarse de que no esté en pausa si se cambia el modo de audio del botón
        isPaused = false;
        // Reiniciar el tiempo de pausa si se activa el audio del botón (aunque no el programa)
        if (currentAudioButtonMode != AudioButtonMode.OFF && currentProgram == Program.NONE) {
            programStartTime = System.currentTimeMillis();
            totalPausedTime = 0;
        }


        if (currentAudioButtonMode != AudioButtonMode.OFF || currentProgram != Program.NONE) {
            if (!isAudioPlaying) {
                startAudioThread();
            }
        } else {
            stopAudioThread();
        }
    }

    private void changeNoiseType() {
        if (!isPowerOn) {
            showToast("Enciende el dispositivo primero.");
            return;
        }
        if (currentAudioButtonMode != AudioButtonMode.WHITE_NOISE) {
            showToast("Activa el Ruido Blanco primero con el botón de Tono/Ruido.");
            return;
        }
        if (currentNoiseType == NoiseType.SOFT) {
            currentNoiseType = NoiseType.SURF;
            showToast("Tipo de Ruido: Surf");
        } else {
            currentNoiseType = NoiseType.SOFT;
            showToast("Tipo de Ruido: Suave");
        }
    }

    private void updateAllLeds(Program currentProgram) {
        ledEarsR.setImageResource(ledOff);
        ledB.setImageResource(ledOff);
        for (ImageView led : programLeds) {
            led.setImageResource(ledOff);
        }
        if (!isPowerOn) {
            ledEyesL.setImageResource(ledOff);
            ledA.setImageResource(ledOff);
            return;
        }

        switch (currentProgram) {
            case B3:
                ledB.setImageResource(ledGreenOn);
                programLeds.get(2).setImageResource(ledOrangeOn);
                break;
            case B4:
                ledB.setImageResource(ledGreenOn);
                programLeds.get(3).setImageResource(ledOrangeOn); // Corregido: programLads -> programLeds
                break;
            case B5:
                ledB.setImageResource(ledGreenOn);
                programLeds.get(4).setImageResource(ledOrangeOn);
                break;
            case NONE:
                break;
            default:
                break;
        }
    }

    private void startAudioThread() {
        if (currentProgram == Program.NONE && currentAudioButtonMode == AudioButtonMode.OFF) {
            if (isAudioPlaying) {
                stopAudioThread();
            }
            return;
        }

        if (isAudioPlaying) return;

        isAudioPlaying = true;

        int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSize == AudioTrack.ERROR_BAD_VALUE || bufferSize == AudioTrack.ERROR) {
            showToast("Error al obtener el tamaño del buffer de audio.");
            isAudioPlaying = false;
            return;
        }

        try {
            audioTrack = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
                    .setBufferSizeInBytes(bufferSize)
                    .build();

            audioTrack.play();
        } catch (UnsupportedOperationException | IllegalArgumentException e) {
            showToast("Error al inicializar AudioTrack: " + e.getMessage());
            isAudioPlaying = false;
            return;
        }

        audioThread = new Thread(() -> {
            short[] buffer = new short[bufferSize / 2];
            Random random = new Random();
            double phaseLeft = 0, phaseRight = 0, panPhase = 0;
            float lastNoiseSample = 0;

            double sineWavePhase = 0;

            while (isAudioPlaying) {
                // Si está en pausa, el hilo espera.
                if (isPaused) {
                    runOnUiThread(() -> {
                        // Apagar LEDs de paneo cuando está en pausa
                        ledEyesL.setImageResource(ledOff);
                        ledA.setImageResource(ledOff);
                    });
                    try {
                        Thread.sleep(100); // Pequeña pausa para no saturar la CPU
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        isAudioPlaying = false;
                        break; // Salir del bucle si el hilo es interrumpido
                    }
                    continue; // Saltar a la siguiente iteración del bucle while (sin generar audio)
                }

                Program currentProgramInThread = currentProgram;
                NoiseType currentNoiseTypeInThread = currentNoiseType;
                AudioButtonMode currentAudioButtonModeInThread = currentAudioButtonMode;
                float currentInternalVolume = internalVolume;
                double currentPureToneFrequency = pureToneFrequency;
                double sineWaveIncrement = 2 * Math.PI * currentPureToneFrequency / SAMPLE_RATE;

                // Calcular el tiempo transcurrido, ajustando por el tiempo total de pausa
                double elapsedSeconds = (System.currentTimeMillis() - programStartTime - totalPausedTime) / 1000.0;

                double baseFrequency = 0;
                double panSpeed = 0;
                double masterVolumeMultiplier = 1.0;

                if (currentProgramInThread != Program.NONE) {
                    switch (currentProgramInThread) {
                        case B3:
                            if (elapsedSeconds < 600) {
                                baseFrequency = 12.0 - (7.0 / 600.0) * elapsedSeconds;
                            } else {
                                baseFrequency = 5.0;
                            }
                            panSpeed = 0.8;
                            if (elapsedSeconds < 300) {
                                masterVolumeMultiplier = 1.0 - (0.4 / 300.0) * elapsedSeconds;
                            } else {
                                masterVolumeMultiplier = 0.6;
                            }
                            break;
                        case B4:
                            baseFrequency = 10.0;
                            panSpeed = 0.6;
                            masterVolumeMultiplier = 1.0;
                            break;
                        case B5:
                            double maxDurationB5 = 45 * 60.0;
                            if (elapsedSeconds < maxDurationB5) {
                                baseFrequency = 12.0 - (6.0 / maxDurationB5) * elapsedSeconds;
                            } else {
                                baseFrequency = 6.0;
                            }
                            panSpeed = 0.7;
                            masterVolumeMultiplier = 1.0;
                            break;
                    }
                }

                // Si no hay programa activo Y tampoco el botón de audio está activo, y no está en pausa activa,
                // entonces el hilo debería detenerse o esperar. Si está en pausa, ya lo manejamos arriba.
                if (currentProgramInThread == Program.NONE && currentAudioButtonModeInThread == AudioButtonMode.OFF && !isPaused) {
                    runOnUiThread(() -> {
                        ledEyesL.setImageResource(ledOff);
                        ledA.setImageResource(ledOff);
                    });
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        isAudioPlaying = false;
                        break;
                    }
                    continue;
                }

                double binauralBeat = 7.0;
                double freqLeft = baseFrequency;
                double freqRight = baseFrequency + binauralBeat;
                double phaseIncrementLeft = 2 * Math.PI * freqLeft / SAMPLE_RATE;
                double phaseIncrementRight = 2 * Math.PI * freqRight / SAMPLE_RATE;
                double panIncrement = 2 * Math.PI * panSpeed / SAMPLE_RATE;

                for (int i = 0; i < buffer.length; i += 2) {
                    short toneSampleLeft = 0;
                    short toneSampleRight = 0;
                    short noiseSample = 0;
                    short pureSineSample = 0;

                    if (currentProgramInThread != Program.NONE) {
                        toneSampleLeft = (short) (Math.sin(phaseLeft) * Short.MAX_VALUE * 0.4 * masterVolumeMultiplier);
                        toneSampleRight = (short) (Math.sin(phaseRight) * Short.MAX_VALUE * 0.4 * masterVolumeMultiplier);
                        phaseLeft += phaseIncrementLeft;
                        phaseRight += phaseIncrementRight;
                    }

                    if (currentAudioButtonModeInThread == AudioButtonMode.SINE_WAVE) {
                        pureSineSample = (short) (Math.sin(sineWavePhase) * Short.MAX_VALUE * 0.3);
                        sineWavePhase += sineWaveIncrement;
                    } else if (currentAudioButtonModeInThread == AudioButtonMode.WHITE_NOISE) {
                        switch (currentNoiseTypeInThread) {
                            case SOFT:
                                noiseSample = (short) ((random.nextDouble() * 2 - 1) * Short.MAX_VALUE * 0.3);
                                break;
                            case SURF:
                                float randomStep = (float) ((random.nextDouble() * 2 - 1) * 0.1f);
                                lastNoiseSample += randomStep;
                                if (lastNoiseSample > 1.0f) lastNoiseSample = 1.0f;
                                if (lastNoiseSample < -1.0f) lastNoiseSample = -1.0f;
                                noiseSample = (short) (lastNoiseSample * Short.MAX_VALUE * 0.5);
                                break;
                        }
                    }

                    double panValue = 0;
                    if (currentProgramInThread != Program.NONE && panSpeed > 0) {
                        panValue = Math.sin(panPhase);
                        panPhase += panIncrement;
                    }
                    double volumeLeft = 0.5 * (1 - panValue);
                    double volumeRight = 0.5 * (1 + panValue);

                    double mixedSampleLeftDouble = (double)toneSampleLeft + pureSineSample + noiseSample;
                    double mixedSampleRightDouble = (double)toneSampleRight + pureSineSample + noiseSample;

                    mixedSampleLeftDouble *= volumeLeft;
                    mixedSampleRightDouble *= volumeRight;

                    mixedSampleLeftDouble *= currentInternalVolume;
                    mixedSampleRightDouble *= currentInternalVolume;

                    short finalSampleLeft = (short) mixedSampleLeftDouble;
                    short finalSampleRight = (short) mixedSampleRightDouble;

                    finalSampleLeft = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, finalSampleLeft));
                    finalSampleRight = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, finalSampleRight));

                    buffer[i] = finalSampleLeft;
                    buffer[i + 1] = finalSampleRight;
                }

                if (audioTrack != null && audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                    audioTrack.write(buffer, 0, buffer.length);
                }

                final double lastPanValue = Math.sin(panPhase);
                runOnUiThread(() -> {
                    // Los LEDs de paneo solo se actualizan si no está en pausa Y hay un programa activo
                    if (isPowerOn && currentProgramInThread != Program.NONE && !isPaused) {
                        if (lastPanValue < -0.2) {
                            ledEyesL.setImageResource(ledOrangeOn);
                            ledA.setImageResource(ledOff);
                        } else if (lastPanValue > 0.2) {
                            ledEyesL.setImageResource(ledOff);
                            ledA.setImageResource(ledOrangeOn);
                        } else {
                            ledEyesL.setImageResource(ledOff);
                            ledA.setImageResource(ledOff);
                        }
                    } else {
                        // Asegurarse de que estén apagados si no hay programa, está apagado, o en pausa
                        ledEyesL.setImageResource(ledOff);
                        ledA.setImageResource(ledOff);
                    }
                });
            }

            if (audioTrack != null) {
                if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                    audioTrack.stop();
                }
                audioTrack.release();
                audioTrack = null;
            }
        });
        audioThread.start();
    }

    private void stopAudioThread() {
        isAudioPlaying = false;
        if (audioThread != null) {
            try {
                audioThread.join(500);
                if (audioThread.isAlive()) {
                    audioThread.interrupt();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            } finally {
                audioThread = null;
            }
        }
        runOnUiThread(() -> {
            ledEyesL.setImageResource(ledOff);
            ledA.setImageResource(ledOff);
        });
        // Asegurarse de que el AudioTrack se detenga y libere si se detiene el hilo
        if (audioTrack != null) {
            if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                audioTrack.stop();
            }
            audioTrack.release();
            audioTrack = null;
        }
        isPaused = false; // Resetear el estado de pausa al detener el audio completamente
        totalPausedTime = 0; // Resetear el tiempo de pausa acumulado
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isAudioPlaying) {
            // Cuando la app se va a segundo plano, lo mejor es pausar o detener completamente
            // Para una experiencia de usuario típica, pausar sería ideal
            if (!isPaused) { // Si no estaba ya pausado por el usuario, lo pausamos automáticamente
                togglePauseRun(); // Llama a la función para pausar
            }
            // O si prefieres detenerlo completamente al salir de la app:
            // stopAudioThread();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isAudioPlaying) {
            stopAudioThread();
        }
    }
}