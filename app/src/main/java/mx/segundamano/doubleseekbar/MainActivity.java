package mx.segundamano.doubleseekbar;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import mx.segundamano.seekbars.DoubleSeekbarView;

public class MainActivity extends AppCompatActivity {

    private EditText minval;
    private EditText maxVal;
    private View.OnFocusChangeListener onfocuschange;
    private DoubleSeekbarView doubleSeekbarView;
    private EditText minAct;
    private EditText maxAct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TextView seekbarValues = (TextView) findViewById(R.id.double_seekbar_values);
        doubleSeekbarView = (DoubleSeekbarView) findViewById(R.id.double_seekbar);
        doubleSeekbarView.setEnabled(false);

        assert doubleSeekbarView != null;
        doubleSeekbarView.setOnValuesChangeListener(new DoubleSeekbarView.OnValuesChangeListener() {
            @Override
            public void onValuesChange(int minValue, int maxValue) {
                assert seekbarValues != null;
                seekbarValues.setText("Min: " + minValue + " - Max: " + maxValue);

                minAct.setText(String.valueOf(minValue));
                maxAct.setText(String.valueOf(maxValue));
            }
        });

        onfocuschange = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus)
                    switch (v.getId()) {
                        case R.id.minval:
                            setMinValue();
                            break;
                        case R.id.maxval:
                            setMaxValue();
                            break;
                        case R.id.minselval:
                            setMinActValue();
                            break;
                        case R.id.maxvalsel:
                            setMaxActValue();
                            break;
                 }
            }
        };

        minval = (EditText) findViewById(R.id.minval);
        assert minval != null;
        minval.setText(String.valueOf(doubleSeekbarView.getMinValue()));
        minval.setOnFocusChangeListener(onfocuschange);

        maxVal = (EditText) findViewById(R.id.maxval);
        assert maxVal != null;
        maxVal.setText(String.valueOf(doubleSeekbarView.getMaxValue()));
        maxVal.setOnFocusChangeListener(onfocuschange);

        minAct = (EditText) findViewById(R.id.minselval);
        assert minAct != null;
        minAct.setOnFocusChangeListener(onfocuschange);

        maxAct = (EditText) findViewById(R.id.maxvalsel);
        assert maxAct != null;
        maxAct.setOnFocusChangeListener(onfocuschange);
    }

    private void setMaxActValue() {
        if(maxAct.getText().toString().equals(""))
            return;

        int value = Integer.parseInt(maxAct.getText().toString());
        doubleSeekbarView.setActMaxValue(value);
    }

    private void setMinActValue() {
        if(minAct.getText().toString().equals(""))
            return;

        int value = Integer.parseInt(minAct.getText().toString());
        doubleSeekbarView.setActMinValue(value);
    }

    private void setMaxValue() {
        if(maxVal.getText().toString().equals(""))
            return;

        int val = Integer.parseInt(maxVal.getText().toString());
        doubleSeekbarView.setMaxValue(val);

        maxAct.setText(String.valueOf(doubleSeekbarView.getMaxDataValue()));
    }

    private void setMinValue() {
        if(minval.getText().toString().equals(""))
            return;

        int val = Integer.parseInt(minval.getText().toString());
        doubleSeekbarView.setMinValue(val);

        minAct.setText(String.valueOf(doubleSeekbarView.getMinDataValue()));
    }
}
