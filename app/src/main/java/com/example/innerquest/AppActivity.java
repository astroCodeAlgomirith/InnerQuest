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
    private AudioButtonMode currentAudioButtonMode = AudioButtonMode.OFF; // Cambiado de generateNoise

    // Variables para la generacion de audio
    private volatile boolean isAudioPlaying = false; // Control general del hilo de audio
    private AudioTrack audioTrack;
    private Thread audioThread;
    private final int SAMPLE_RATE = 44100; // Calidad de CD

    // Variable para controlar el tiempo de ejecución del programa
    private long programStartTime; // Guarda el System.currentTimeMillis() cuando el programa comienza

    // Interfaz
    private Button btnPwr, btnPgmSel, btnPseRun, btnWnTone, btnChangeNoise;
    private ImageView ledEarsR, ledEyesL, ledB, ledA;
    private List<ImageView> programLeds;
    private TextView programDisplayText; // Muestra el programa actual (no usado en tu código original, se mantiene el comentario)
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

        findViewById(R.id.btn_vol_up).setOnClickListener(v -> showToast("Volumen +"));
        findViewById(R.id.btn_vol_down).setOnClickListener(v -> showToast("Volumen -"));
        findViewById(R.id.btn_pitch_up).setOnClickListener(v -> showToast("Pitch +"));
        findViewById(R.id.btn_pitch_down).setOnClickListener(v -> showToast("Pitch -"));
        findViewById(R.id.btn_pse_run).setOnClickListener(v -> showToast("PSE RUN click")); // Se mantiene el listener si existe el botón

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
        btnWnTone.setOnClickListener(v -> toggleAudioButtonMode()); // Cambiado para llamar a la nueva función
        btnChangeNoise.setOnClickListener(v -> changeNoiseType());
    }

    private void togglePower() {
        isPowerOn = !isPowerOn;
        if (isPowerOn) {
            // Al encender, siempre resetear el tiempo de inicio
            programStartTime = System.currentTimeMillis();
            startAudioThread();
            showToast("Dispositivo Encendido");
        } else {
            stopAudioThread();
            currentProgram = Program.NONE; // Restablecer programa al apagar
            currentAudioButtonMode = AudioButtonMode.OFF; // Resetear el modo del botón WnTone al apagar
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
        updateAllLeds(currentProgram); // Asegurar que los LEDs se actualizan al cambiar el estado de encendido
    }

    private void selectNextProgram() {
        if (!isPowerOn) {
            showToast("Enciende el dispositivo primero.");
            return;
        }

        Program oldProgram = currentProgram; // Guarda el programa anterior
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
                currentProgram = Program.NONE; // Vuelve al estado inicial sin programa
                break;
        }

        // Si el programa cambió o si se acaba de seleccionar B3 desde NONE
        if (currentProgram != oldProgram) {
            // Resetear el tiempo de inicio del programa solo si el programa cambia.
            // Si el hilo de audio ya está corriendo, se ajustará automáticamente a los nuevos parámetros.
            programStartTime = System.currentTimeMillis();
            showToast("Programa Seleccionado: " + currentProgram.name());
            updateAllLeds(currentProgram);

            // Asegurarse de que el hilo de audio esté corriendo si hay un programa activo,
            // o detenerlo si se vuelve a NONE.
            if (currentProgram != Program.NONE || currentAudioButtonMode != AudioButtonMode.OFF) { // Revisa si hay programa O si el botón de audio está activo
                if (!isAudioPlaying) { // Solo iniciar si no está ya corriendo
                    startAudioThread();
                }
            } else {
                stopAudioThread(); // Detener el audio si volvemos a NONE y el botón de audio está apagado
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Nueva función para alternar entre los modos del botón WnTone
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

        // Asegurarse de que el hilo de audio esté corriendo si un modo está activo
        // o detenerlo si el programa está en NONE y el botón de audio también se apagó.
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
        // Este botón ahora solo afecta el tipo de ruido cuando estamos en modo Ruido Blanco
        if (currentAudioButtonMode != AudioButtonMode.WHITE_NOISE) { // Cambiado: solo si estamos en modo ruido blanco
            showToast("Activa el Ruido Blanco primero con el botón de Tono/Ruido."); // Mensaje adaptado
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
        // Asegurarse de apagar todos los LEDs de programa y tipo B/A
        ledEarsR.setImageResource(ledOff); // LED asociado a "A" en tu XML
        ledB.setImageResource(ledOff); // LED asociado a "B" en tu XML
        for (ImageView led : programLeds) {
            led.setImageResource(ledOff);
        }
        // Los LEDs de ojo (pan) se manejan dentro del hilo de audio.
        // Pero al apagar, también deben apagarse aquí.
        if (!isPowerOn) {
            ledEyesL.setImageResource(ledOff);
            ledA.setImageResource(ledOff);
            return;
        }

        // Encender los LEDs según el programa seleccionado
        switch (currentProgram) {
            case B3:
                ledB.setImageResource(ledGreenOn); // LED 'B' encendido
                programLeds.get(2).setImageResource(ledOrangeOn); // LED '3' encendido (índice 2 para led_3)
                break;
            case B4:
                ledB.setImageResource(ledGreenOn); // LED 'B' encendido
                programLeds.get(3).setImageResource(ledOrangeOn); // LED '4' encendido (índice 3 para led_4)
                break;
            case B5:
                ledB.setImageResource(ledGreenOn); // LED 'B' encendido
                programLeds.get(4).setImageResource(ledOrangeOn); // LED '5' encendido (índice 4 para led_5)
                break;
            case NONE:
                // Todos los LEDs ya están apagados por el bloque inicial o el togglePower
                break;
            default:
                // Para cualquier otro programa futuro si se añaden
                break;
        }
    }

    private void startAudioThread() {
        // Añadida una comprobación para asegurar que el hilo solo se inicie si hay algo que reproducir
        if (currentProgram == Program.NONE && currentAudioButtonMode == AudioButtonMode.OFF) {
            if (isAudioPlaying) { // Si estaba reproduciendo algo que se detenga
                stopAudioThread();
            }
            return;
        }

        if (isAudioPlaying) return; // Si ya está reproduciendo, no hacer nada

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
            short[] buffer = new short[bufferSize / 2]; // Dividir por 2 porque son pares de samples (L/R)
            Random random = new Random();
            double phaseLeft = 0, phaseRight = 0, panPhase = 0;
            float lastNoiseSample = 0; // Para el ruido tipo "surf"

            // Parámetros para la onda sinusoidal pura (si se activa desde el botón)
            double sineWaveFrequency = 440.0; // Ejemplo: Tono de La (A4)
            double sineWavePhase = 0;
            double sineWaveIncrement = 2 * Math.PI * sineWaveFrequency / SAMPLE_RATE;


            while (isAudioPlaying) {
                // Capturar el estado actual del programa y ruido para este ciclo del bucle
                Program currentProgramInThread = currentProgram;
                NoiseType currentNoiseTypeInThread = currentNoiseType;
                AudioButtonMode currentAudioButtonModeInThread = currentAudioButtonMode; // Capturar el nuevo estado

                // Calcular el tiempo transcurrido desde que se inició/cambió el programa
                double elapsedSeconds = (System.currentTimeMillis() - programStartTime) / 1000.0; // En segundos

                double baseFrequency = 0;
                double panSpeed = 0;
                double masterVolumeMultiplier = 1.0; // Multiplicador para el volumen general (afecta tono y ruido)

                // 1. OBTENER PARÁMETROS DEL PROGRAMA ACTUAL BASADOS EN EL TIEMPO
                // Esto solo se aplica si un programa está seleccionado
                if (currentProgramInThread != Program.NONE) {
                    switch (currentProgramInThread) {
                        case B3: // 46 minutos: Relajación Profunda y Alta Creatividad
                            // Frecuencia: Ramp down de Alpha a Theta en los primeros 10 minutos (600 segundos)
                            if (elapsedSeconds < 600) { // Primeros 10 minutos
                                // Linealmente de 12 Hz (Alpha) a 5 Hz (Theta)
                                baseFrequency = 12.0 - (7.0 / 600.0) * elapsedSeconds;
                            } else { // Después de 10 minutos, mantener en Theta
                                baseFrequency = 5.0;
                            }
                            panSpeed = 0.8; // Más activo para "patrones alternantes"

                            // Amplitud: Atenúa gradualmente hasta el 60% en los primeros 5 minutos (300 segundos)
                            if (elapsedSeconds < 300) {
                                masterVolumeMultiplier = 1.0 - (0.4 / 300.0) * elapsedSeconds; // Baja de 1.0 a 0.6
                            } else {
                                masterVolumeMultiplier = 0.6; // Se mantiene en 60%
                            }
                            break;

                        case B4: // 25 minutos: Puesta a Punto General
                            baseFrequency = 10.0; // Constante en Alpha (ej. 10 Hz)
                            panSpeed = 0.6; // Moderado para "múltiples patrones"
                            masterVolumeMultiplier = 1.0; // Brillo constante
                            break;

                        case B5: // 45 minutos: Uso General
                            // Frecuencia: Ramp down de Alpha a Theta sobre toda la duración (45 min = 2700 segundos)
                            double maxDurationB5 = 45 * 60.0; // 45 minutos en segundos
                            if (elapsedSeconds < maxDurationB5) {
                                // Linealmente de 12 Hz (Alpha) a 6 Hz (Theta)
                                baseFrequency = 12.0 - (6.0 / maxDurationB5) * elapsedSeconds;
                            } else {
                                baseFrequency = 6.0; // Si el programa dura más, se mantiene en 6 Hz
                            }
                            panSpeed = 0.7; // Un poco más activo que B4
                            masterVolumeMultiplier = 1.0; // Brillo constante
                            break;
                    }
                }

                // Si no hay programa activo Y tampoco el botón de audio está activo, pausar o apagar
                if (currentProgramInThread == Program.NONE && currentAudioButtonModeInThread == AudioButtonMode.OFF) {
                    runOnUiThread(() -> {
                        ledEyesL.setImageResource(ledOff);
                        ledA.setImageResource(ledOff);
                    });
                    try {
                        Thread.sleep(100); // Pequeña pausa para no saturar la CPU
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        isAudioPlaying = false; // Set flag to false to terminate the loop cleanly
                    }
                    continue; // Saltar a la siguiente iteración del bucle while
                }


                double binauralBeat = 7.0; // Diferencia constante para el efecto binaural
                double freqLeft = baseFrequency;
                double freqRight = baseFrequency + binauralBeat;
                double phaseIncrementLeft = 2 * Math.PI * freqLeft / SAMPLE_RATE;
                double phaseIncrementRight = 2 * Math.PI * freqRight / SAMPLE_RATE;
                double panIncrement = 2 * Math.PI * panSpeed / SAMPLE_RATE;

                // 2. GENERAR EL BUFFER DE AUDIO
                for (int i = 0; i < buffer.length; i += 2) {
                    short toneSampleLeft = 0;
                    short toneSampleRight = 0;
                    short noiseSample = 0;
                    short pureSineSample = 0;

                    // Generar tono binaural SOLO si hay un programa seleccionado
                    if (currentProgramInThread != Program.NONE) {
                        toneSampleLeft = (short) (Math.sin(phaseLeft) * Short.MAX_VALUE * 0.4 * masterVolumeMultiplier);
                        toneSampleRight = (short) (Math.sin(phaseRight) * Short.MAX_VALUE * 0.4 * masterVolumeMultiplier);
                        phaseLeft += phaseIncrementLeft;
                        phaseRight += phaseIncrementRight;
                    }

                    // Generar onda sinusoidal pura O ruido blanco, según el modo del botón
                    if (currentAudioButtonModeInThread == AudioButtonMode.SINE_WAVE) {
                        pureSineSample = (short) (Math.sin(sineWavePhase) * Short.MAX_VALUE * 0.3); // Volumen un poco más bajo
                        sineWavePhase += sineWaveIncrement;
                    } else if (currentAudioButtonModeInThread == AudioButtonMode.WHITE_NOISE) {
                        switch (currentNoiseTypeInThread) {
                            case SOFT: // Ruido Blanco: Totalmente aleatorio
                                noiseSample = (short) ((random.nextDouble() * 2 - 1) * Short.MAX_VALUE * 0.3 * masterVolumeMultiplier);
                                break;
                            case SURF: // Ruido Marrón/Browniano: Suena como oleaje o trueno
                                float randomStep = (float) ((random.nextDouble() * 2 - 1) * 0.1f);
                                lastNoiseSample += randomStep;
                                if (lastNoiseSample > 1.0f) lastNoiseSample = 1.0f;
                                if (lastNoiseSample < -1.0f) lastNoiseSample = -1.0f;
                                noiseSample = (short) (lastNoiseSample * Short.MAX_VALUE * 0.5 * masterVolumeMultiplier);
                                break;
                        }
                    }

                    // --- Cálculo del Paneo (Izquierda/Derecha) ---
                    // Solo aplica paneo si hay un programa activo que lo defina
                    double panValue = 0;
                    if (currentProgramInThread != Program.NONE && panSpeed > 0) {
                        panValue = Math.sin(panPhase); // Va de -1 (izq) a 1 (der)
                        panPhase += panIncrement;
                    }
                    double volumeLeft = 0.5 * (1 - panValue); // Si pan es -1, vol es 1. Si pan es 1, vol es 0.
                    double volumeRight = 0.5 * (1 + panValue); // Lo opuesto.

                    // --- Mezcla y Aplicación del Paneo ---
                    int mixedLeft = toneSampleLeft + pureSineSample + noiseSample; // Sumar todos los componentes de audio
                    int mixedRight = toneSampleRight + pureSineSample + noiseSample;

                    short finalSampleLeft = (short) (mixedLeft * volumeLeft);
                    short finalSampleRight = (short) (mixedRight * volumeRight);

                    // Aseguramos que no haya clipping (saturación)
                    finalSampleLeft = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, finalSampleLeft));
                    finalSampleRight = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, finalSampleRight));

                    buffer[i] = finalSampleLeft;
                    buffer[i + 1] = finalSampleRight;
                }

                // 3. ESCRIBIR EL BUFFER AL DISPOSITIVO DE AUDIO
                if (audioTrack != null && audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                    audioTrack.write(buffer, 0, buffer.length);
                }

                // Actualizar LEDs de paneo en el UI thread
                final double lastPanValue = Math.sin(panPhase); // Usar el valor actual de panPhase
                runOnUiThread(() -> {
                    // Solo actualiza los LEDs si hay un programa activo y está encendido
                    if (isPowerOn && currentProgramInThread != Program.NONE) {
                        if (lastPanValue < -0.2) { // Umbral para la izquierda
                            ledEyesL.setImageResource(ledOrangeOn);
                            ledA.setImageResource(ledOff);
                        } else if (lastPanValue > 0.2) { // Umbral para la derecha
                            ledEyesL.setImageResource(ledOff);
                            ledA.setImageResource(ledOrangeOn);
                        } else { // Cerca del centro
                            ledEyesL.setImageResource(ledOff);
                            ledA.setImageResource(ledOff);
                        }
                    } else {
                        // Asegurarse de que estén apagados si no hay programa o está apagado
                        ledEyesL.setImageResource(ledOff);
                        ledA.setImageResource(ledOff);
                    }
                });
            }

            // 4. LIMPIEZA al salir del bucle
            if (audioTrack != null) {
                if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                    audioTrack.stop();
                }
                audioTrack.release();
                audioTrack = null; // Liberar referencia
            }
        });
        audioThread.start();
    }

    private void stopAudioThread() {
        isAudioPlaying = false; // Señal para que el hilo termine
        if (audioThread != null) {
            try {
                audioThread.join(500); // Esperar a que el hilo termine, con un timeout de 500ms
                if (audioThread.isAlive()) {
                    audioThread.interrupt(); // Interrumpir si no termina a tiempo
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restaurar el estado de interrupción
                e.printStackTrace();
            } finally {
                audioThread = null; // Asegurarse de que la referencia se nulifica
            }
        }
        // También apagar los LEDs de paneo al detener el hilo de audio
        runOnUiThread(() -> {
            ledEyesL.setImageResource(ledOff);
            ledA.setImageResource(ledOff);
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Es CRUCIAL detener el audio si la app se va a segundo plano
        // o si la actividad se destruye (ej. el usuario navega a la pantalla de login).
        if (isAudioPlaying) {
            stopAudioThread();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Asegurarse de que el hilo se detiene completamente si la actividad se destruye.
        if (isAudioPlaying) {
            stopAudioThread();
        }
    }
}