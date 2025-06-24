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

    //Variables de estado
    private boolean isPowerOn = false;
    private Program currentProgram = Program.NONE;
    private NoiseType currentNoiseType = NoiseType.SURF;

    // Variables para la generacion de audio
    private volatile boolean isAudioPlaying = false; // Control general del hilo de audio
    private volatile boolean generateNoise = false; // Controla si se genera el ruido
    private AudioTrack audioTrack;
    private Thread audioThread;
    private final int SAMPLE_RATE = 44100; // Calidad de CD

    // Interfaz
    private Button btnPwr, btnPgmSel, btnPseRun, btnWnTone, btnChangeNoise;
    private ImageView ledEarsR, ledEyesL, ledB, ledA;
    private List<ImageView> programLeds;
    private TextView programDisplayText; // Muestra el programa actual
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

        //Tenemos programa A, B, AB, A0, A1, .... , B3, B4, B5, ....
        // Con nuestra secuencia B3, B4 Y B5 se deberia de ver prendido el led B y el led 3, asi con todos
        findViewById(R.id.btn_vol_up).setOnClickListener(v -> showToast("Volumen +"));
        findViewById(R.id.btn_vol_down).setOnClickListener(v -> showToast("Volumen -"));
        findViewById(R.id.btn_pitch_up).setOnClickListener(v -> showToast("Pitch +"));
        findViewById(R.id.btn_pitch_down).setOnClickListener(v -> showToast("Pitch -"));
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
        btnWnTone.setOnClickListener(v -> toggleNoiseGeneration());
        btnChangeNoise.setOnClickListener(v -> changeNoiseType());
    }

    private void togglePower() {
        isPowerOn = !isPowerOn;
        if (isPowerOn) {
            startAudioThread();
            showToast("Dispositivo Encendido");
        } else {
            stopAudioThread();
            currentProgram = Program.NONE;
            showToast("Dispositivo Apagado");
            ledEarsR.setImageResource(ledOff);
            ledB.setImageResource(ledOff);
            for (ImageView led : programLeds) {
                led.setImageResource(ledOff);
            }
        }

    }

    private void selectNextProgram() {
        if (!isPowerOn) return;
        switch (currentProgram) {
            case NONE:
                currentProgram = Program.B3;
                break;
            case B3:
                currentProgram = Program.B4;
                startAudioThread();
                break;
            case B4:
                currentProgram = Program.B5;
                startAudioThread();
                break;
            case B5:
                currentProgram = Program.NONE;
                startAudioThread();
                break;
        }
        showToast("Programa Seleccionado: " + currentProgram.name());
        updateAllLeds(currentProgram);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    private void toggleNoiseGeneration() {
        if (!isPowerOn) return;
        generateNoise = !generateNoise;
        showToast("Ruido " + (generateNoise ? "Activado" : "Desactivado"));

    }
    private void changeNoiseType() {
        if (!isPowerOn || !generateNoise) return;
        if (currentNoiseType == NoiseType.SOFT) {
            currentNoiseType = NoiseType.SURF;
            showToast("Tipo de Ruido: Surf");
        } else {
            currentNoiseType = NoiseType.SOFT;
            showToast("Tipo de Ruido: Suave");
        }
    }
    private void updateAllLeds(Program currentProgram) {
        if (!isPowerOn) {
            ledEarsR.setImageResource(ledOff);
            ledB.setImageResource(ledOff);
            for (ImageView led : programLeds) {
                led.setImageResource(ledOff);
            }
            return;
        }

        for (ImageView led : programLeds) {
            led.setImageResource(ledOff);
        }
        switch (currentProgram) {
            case NONE:
                ledEarsR.setImageResource(ledOff);
                ledB.setImageResource(ledOff);
                break;
            case B3:
                ledEarsR.setImageResource(ledOff);
                ledB.setImageResource(ledGreenOn);
                programLeds.get(2).setImageResource(ledOrangeOn);
                break;
            case B4:
                ledEarsR.setImageResource(ledOff);
                ledB.setImageResource(ledGreenOn);
                programLeds.get(3).setImageResource(ledOrangeOn);
                break;
            case B5:
                ledEarsR.setImageResource(ledOff);
                ledB.setImageResource(ledGreenOn);
                programLeds.get(4).setImageResource(ledOrangeOn);
                break;
            default:
                ledEarsR.setImageResource(ledOff);
                ledB.setImageResource(ledOff);
                break;
        }
    }
    private void startAudioThread() {
        if (isAudioPlaying) return;
        isAudioPlaying = true;

        int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
                .setBufferSizeInBytes(bufferSize)
                .build();

        audioThread = new Thread(() -> {
            // Variables para la generación de audio
            short[] buffer = new short[bufferSize];
            Random random = new Random();
            double phaseLeft = 0, phaseRight = 0, panPhase = 0;
            float lastNoiseSample = 0; // Para el ruido tipo "surf"

            audioTrack.play();

            while (isAudioPlaying) {
                // 1. OBTENER PARÁMETROS DEL PROGRAMA ACTUAL
                double baseFrequency, panSpeed;
                switch (currentProgram) {
                    case B3:
                        baseFrequency = 10.0; // Alfa
                        panSpeed = 0.5; // Lento
                        break;
                    case B4:
                        baseFrequency = 6.0; // Theta
                        panSpeed = 0.2; // Muy lento
                        break;
                    case B5:
                        baseFrequency = 15.0; // Beta
                        panSpeed = 1.5; // Rápido
                        break;
                    default: // NONE
                        baseFrequency = 0;
                        panSpeed = 0;
                        break;
                }

                double binauralBeat = 7.0; // Diferencia constante
                double freqLeft = baseFrequency;
                double freqRight = baseFrequency + binauralBeat;
                double phaseIncrementLeft = 2 * Math.PI * freqLeft / SAMPLE_RATE;
                double phaseIncrementRight = 2 * Math.PI * freqRight / SAMPLE_RATE;
                double panIncrement = 2 * Math.PI * panSpeed / SAMPLE_RATE;

                // 2. GENERAR EL BUFFER DE AUDIO
                for (int i = 0; i < buffer.length; i += 2) {
                    // --- Generación de Tonos Binaurales ---
                    short toneSampleLeft = (short) (Math.sin(phaseLeft) * Short.MAX_VALUE * 0.4);
                    short toneSampleRight = (short) (Math.sin(phaseRight) * Short.MAX_VALUE * 0.4);
                    phaseLeft += phaseIncrementLeft;
                    phaseRight += phaseIncrementRight;

                    // --- Generación de Ruido (si está activado) ---
                    short noiseSample = 0;
                    if (generateNoise) {
                        switch (currentNoiseType) {
                            case SOFT: // Ruido Blanco: Totalmente aleatorio
                                noiseSample = (short) ((random.nextDouble() * 2 - 1) * Short.MAX_VALUE * 0.3);
                                break;
                            case SURF: // Ruido Marrón/Browniano: Suena como oleaje o trueno
                                // Cada muestra es la anterior más un pequeño cambio aleatorio.
                                // Esto crea un "paseo aleatorio" que tiene más energía en bajas frecuencias.
                                float randomStep = (float) ((random.nextDouble() * 2 - 1) * 0.1f);
                                lastNoiseSample += randomStep;
                                // Limitar para que no se salga de rango (clipping)
                                if (lastNoiseSample > 1.0f) lastNoiseSample = 1.0f;
                                if (lastNoiseSample < -1.0f) lastNoiseSample = -1.0f;
                                noiseSample = (short) (lastNoiseSample * Short.MAX_VALUE * 0.5); // Más volumen que el suave
                                break;
                        }
                    }

                    // --- Cálculo del Paneo (Izquierda/Derecha) ---
                    // Usamos un oscilador de baja frecuencia (LFO) senoidal para un paneo suave.
                    double panValue = Math.sin(panPhase); // Va de -1 (izq) a 1 (der)
                    double volumeLeft = 0.5 * (1 - panValue); // Si pan es -1, vol es 1. Si pan es 1, vol es 0.
                    double volumeRight = 0.5 * (1 + panValue); // Lo opuesto.
                    panPhase += panIncrement;

                    // --- Mezcla y Aplicación del Paneo ---
                    // Sumamos el tono y el ruido para cada canal
                    int mixedLeft = toneSampleLeft + noiseSample;
                    int mixedRight = toneSampleRight + noiseSample;
                    // Aplicamos el volumen del paneo a la mezcla
                    short finalSampleLeft = (short) (mixedLeft * volumeLeft);
                    short finalSampleRight = (short) (mixedRight * volumeRight);

                    // Aseguramos que no haya clipping (saturación)
                    finalSampleLeft = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, finalSampleLeft));
                    finalSampleRight = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, finalSampleRight));

                    buffer[i] = finalSampleLeft;
                    buffer[i + 1] = finalSampleRight;
                }

                // 3. ESCRIBIR EL BUFFER AL DISPOSITIVO DE AUDIO
                if (audioTrack != null) {
                    audioTrack.write(buffer, 0, buffer.length);
                }
                final double lastPanValue = Math.sin(panPhase);
                runOnUiThread(() -> {
                    // Solo actualiza los LEDs si el programa no es NONE
                    if (currentProgram != Program.NONE) {
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
                        // Asegurarse de que estén apagados si no hay programa
                        ledEyesL.setImageResource(ledOff);
                        ledA.setImageResource(ledOff);
                    }
                });
            }

            // 4. LIMPIEZA
            if (audioTrack != null) {
                audioTrack.stop();
                audioTrack.release();
            }
        });
        audioThread.start();
    }

    private void stopAudioThread() {
        isAudioPlaying = false;
        if (audioThread != null) {
            try {
                audioThread.join(); // Esperar a que el hilo termine de forma segura
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        audioThread = null;
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Es CRUCIAL detener el audio si la app se va a segundo plano
        if (isAudioPlaying) {
            stopAudioThread();
        }
    }
}