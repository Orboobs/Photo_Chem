package com.example.camera;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class MainActivity2 extends AppCompatActivity {
    public Button nazad;
    String strResult = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        nazad = findViewById(R.id.button2);
        Button buton = findViewById(R.id.button3);
        TextView textView = findViewById(R.id.textView4);
        EditText etext = findViewById(R.id.editTextText);
        nazad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity2.this, MainActivity.class);
                startActivity(intent);
            }
        });

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        buton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Python py = Python.getInstance();


                PyObject module = py.getModule("helloworld");


                PyObject result = module.callAttr("helloworld", etext.getText().toString());
                String strResult = result.toString();


                textView.setText(strResult);
            }
        });


    }
}