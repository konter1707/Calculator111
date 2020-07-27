package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.autofill.FieldClassification;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Locale;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

@SuppressLint("ApplySharedPref")
public class MainActivity extends AppCompatActivity {

    public static final String SPEAKING_ENABLED = "speaking_enabled";
    private SharedPreferences prefs;
    private TextView answer, question;

    private Handler applicationHandler;
    private TextToSpeech tts;
    private ImageView ttsSwitch;

    private boolean ignoreOperator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content);
        findAndSetter();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        updateTtsIcon();
        tts = new TextToSpeech(this,new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int resultTts = tts.setLanguage(Locale.getDefault());
                    if (resultTts == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(MainActivity.this, "Language is not supported", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Language is not supported", Toast.LENGTH_LONG).show();
                }
            }
        });
        applicationHandler = new Handler(MainActivity.this.getMainLooper());
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    private void speakOut(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    private void findAndSetter() {
        //toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ttsSwitch = findViewById(R.id.val);
        ttsSwitch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean speakingEnabled = !prefs.getBoolean(SPEAKING_ENABLED, false);
                prefs.edit().putBoolean(SPEAKING_ENABLED, speakingEnabled).commit();
                updateTtsIcon();
                if (speakingEnabled) speakOut("Аудио ответы включены");
            }
        });

        //textView
        answer = findViewById(R.id.example);
        question = findViewById(R.id.otvet);

        //enter
        attachListenerToViews(new CalculatorButtonsClickListener(), R.id.solve,
                R.id.zero, R.id.one, R.id.two, R.id.three, R.id.four, R.id.five, R.id.six, R.id.seven, R.id.eight, R.id.nine,
                R.id.div, R.id.multiple, R.id.plus, R.id.minus,
                R.id.clear, R.id.backspace);
    }

    private void attachListenerToViews(OnClickListener listener, int... ids) {
        for (int id : ids) {
            findViewById(id).setOnClickListener(listener);
        }
    }

    private void updateTtsIcon() {
        boolean speakingEnabled = prefs.getBoolean(SPEAKING_ENABLED, false);
        ttsSwitch.setImageResource(speakingEnabled ? R.drawable.twotone_volume_up_black_18dp : R.drawable.twotone_volume_off_black_18dp);
    }

    private class CalculatorButtonsClickListener implements OnClickListener {
        @Override
        public void onClick(View view) {
            String btnText = ((Button) view).getText().toString();
            switch (view.getId()) {
                case R.id.solve:
                    new SolveThread().start();
                    break;
                case R.id.clear:
                    answer.setText("");
                    question.setText("");
                    break;
                case R.id.backspace:
                    answer.setText(answer.getText().toString().replaceFirst(".$", ""));
                    question.setText("");
                    break;
                case R.id.zero:
                case R.id.one:
                case R.id.two:
                case R.id.three:
                case R.id.four:
                case R.id.five:
                case R.id.six:
                case R.id.seven:
                case R.id.eight:
                case R.id.nine: {
                    answer.append(btnText);
                    ignoreOperator = false;
                    break;
                }

                case R.id.div:
                case R.id.multiple:
                case R.id.minus:
                case R.id.plus:
                case R.id.dot: {
                    if (ignoreOperator || answer.getText().toString().isEmpty() && !btnText.equals("-")) {
                        return;
                    }
                    answer.append(btnText);
                    ignoreOperator = true;
                    break;
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private class SolveThread extends Thread {

        private String displayedResult;
        @Override
        public void run() {
            try {
                ScriptEngineManager manager = new ScriptEngineManager();
                ScriptEngine engine = manager.getEngineByName("rhino");
                String evalResult = engine.eval(answer.getText().toString()).toString();
                float floatResult = Float.parseFloat(evalResult);
                int intResult = (int) floatResult;
                displayedResult= floatResult - intResult == 0f ? "" + intResult : "" + floatResult;
                if (prefs.getBoolean(SPEAKING_ENABLED, false)) {
                    if (Math.random() <= 0.1)
                        speakOut("Заебал ты уже, думай своей головой! Ха-ха-ха");
                    else
                        speakOut(displayedResult);
                }
                applicationHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        question.setText(displayedResult);
                    }
                });
            } catch (ScriptException e) {
                if (e.getMessage() != null) {
                    Log.d("Calculator", e.getMessage());
                }
            } catch (NumberFormatException e) {
                if (e.getMessage() != null) {
                    question.setText("Error");
                    Log.d("Calculator", e.getMessage());
                }
            }
        }
    }
}
