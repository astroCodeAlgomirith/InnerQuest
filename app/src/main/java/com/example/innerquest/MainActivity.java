package com.example.innerquest;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ImageView ledDelta, ledTheta, ledAlpha, ledBeta;
    private Button btnDelta, btnTheta, btnAlpha, btnBeta;
    private SeekBar seekVolume, seekFrequency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar vistas
        ledDelta = findViewById(R.id.ledDelta);
        ledTheta = findViewById(R.id.ledTheta);
       ledAlpha = findViewById(R.id.ledAlpha);
        ledBeta = findViewById(R.id.ledBeta);

        btnDelta = findViewById(R.id.btnDelta);
        btnTheta = findViewById(R.id.btnTheta);
        btnAlpha = findViewById(R.id.btnAlpha);
        btnBeta = findViewById(R.id.btnBeta);

        seekVolume = findViewById(R.id.seekVolume);
        seekFrequency = findViewById(R.id.seekFrequency);

        // Resetear LEDs (todos apagados al inicio)
        resetLEDs();

        // Listeners para botones de programas
        btnDelta.setOnClickListener(v -> selectProgram("delta"));
        btnTheta.setOnClickListener(v -> selectProgram("theta"));
        btnAlpha.setOnClickListener(v -> selectProgram("alpha"));
        btnBeta.setOnClickListener(v -> selectProgram("beta"));

        // Listeners para SeekBars
        seekVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Aquí ajustarías el volumen del ruido blanco
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekFrequency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Aquí ajustarías la frecuencia (pitch) en Hz
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void resetLEDs() {
        ledDelta.setImageResource(R.drawable.led_off);
        ledTheta.setImageResource(R.drawable.led_off);
        ledAlpha.setImageResource(R.drawable.led_off);
        ledBeta.setImageResource(R.drawable.led_off);
    }

    private void selectProgram(String program) {
        resetLEDs();
        switch (program) {
            case "delta":
                ledDelta.setImageResource(R.drawable.led_on);
                break;
            case "theta":
                ledTheta.setImageResource(R.drawable.led_on);
                break;
            case "alpha":
                ledAlpha.setImageResource(R.drawable.led_on);
                break;
            case "beta":
                ledBeta.setImageResource(R.drawable.led_on);
                break;
        }
    }
}