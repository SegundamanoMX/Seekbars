package mx.segundamano.seekbars;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RemoteViews.RemoteView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by diego on 17/05/16.
 */
@RemoteView
public class SeekBarView extends View {
    private static final String TAG = SeekBarView.class.getSimpleName();

    // Drawing values
    private float drawActVal;
    private float drawMin;
    private float drawMax;
    private float guideTop;
    private float guideBottom;
    private float drawMiddleHeight;
    private float circleRadius;

    //Drawing shapes
    private RectF guide = new RectF();
    private RectF progress = new RectF();

    //Drawing paints
    private Paint paintGuide = new Paint();
    private Paint paintProgress = new Paint();
    private Paint paintPointer = new Paint();

    //Data values
    private int dataMin = 0;
    private int dataMax = 10;
    private int actDataValue;
    private int steps = 1;
    private List<Step> stepList;

    //Listener for callback value changes
    public interface OnInsertSeekBarListener {
        void onValueChanged(int value);
    }

    private OnInsertSeekBarListener listener;

    public SeekBarView(Context context) {
        this(context, null);
    }

    public SeekBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSaveEnabled(true);

        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.seekbar, 0, 0);
        try{
            paintPointer.setColor(ta.getColor(R.styleable.seekbar_pointerColor, ContextCompat.getColor(context, R.color.defaultPointerColor)));
            paintProgress.setColor(ta.getColor(R.styleable.seekbar_guideColor, ContextCompat.getColor(context, R.color.defaultGuideColor)));
        } finally {
            ta.recycle();
        }

        paintGuide.setAntiAlias(true);
        paintGuide.setColor(ContextCompat.getColor(context, R.color.defaultBaseColor));

        paintProgress.setAntiAlias(true);

        paintPointer.setStyle(Paint.Style.FILL);
        paintPointer.setAntiAlias(true);

        stepList = new ArrayList<>();

        normalPointer();
    }

    /**
     * Set OnInsertSeekBarListener for receive value changes
     * @param l
     */
    public void setOnInsertSeekBarListener(OnInsertSeekBarListener l) {
        listener = l;
    }

    public OnInsertSeekBarListener getListener() {
        return listener;
    }

    /**
     * Set actual value in seek bar
     * @param actDataValue Value to be set between min value and max value, take in count the steps!
     */
    public void setActualValue(int actDataValue) {
        if(actDataValue < dataMin) {
             throw new IllegalArgumentException("Value is minor than mininum data set");
        } else if (actDataValue > dataMax) {
            throw new IllegalArgumentException("Value is major than maximum data set");
        }

        this.actDataValue = actDataValue;
        updatePositionByStep(this.actDataValue);
    }

    /**
     * Get the actual value in seek bar
     * @return Actual value selected
     */
    public int getActDataValue() {
        return actDataValue;
    }

    /**
     * Set min value can be selected. By default this value is 0
     * @param dataMin Min value it can be selected
     */
    public void setDataMin(int dataMin) {
        if(dataMin >= dataMax) {
            throw new IllegalArgumentException("Min value should be less than max value");
        }

        this.dataMin = dataMin;
        if(actDataValue < dataMin) {
            actDataValue = dataMin;
        }
        setScale();
    }

    /**
     * Obtain the min value set in the seek bar
     * @return Min value set
     */
    public int getDataMin() {
        return dataMin;
    }

    /**
     * Set max value can be selected. By default this value is 10
     * @param dataMax Max value it can be selected
     */
    public void setDataMax(int dataMax) {
        if(dataMax <= dataMin) {
            throw new IllegalArgumentException("Max value should be greater than min value");
        }

        this.dataMax = dataMax;
        if(actDataValue > dataMax) {
            actDataValue = dataMax;
        }
        setScale();
    }

    /**
     * Obtain the max value set in the seek bar
     * @return Max value set
     */
    public int getDataMax() {
        return dataMax;
    }

    /**
     * Set the value of unit that each step will have between dataMin value and dataMax value.
     * The pointer will be set in the nearest step. By default this value is 1
     * @param steps Value of unit between each step
     */
    public void setSteps(int steps) {
        this.steps = steps;
        setScale();
    }

    public int getSteps() {
        return steps;
    }

    private void updatePositionByStep(int actDataValue) {
        if(stepList == null || stepList.isEmpty()) {
            this.actDataValue = actDataValue;
            return;
        }

        for(Step step : stepList) {
            if(step.value == actDataValue) {
                updateStepPosition(step.drawValue);
                break;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                pressedPointer();
                updateStepPosition(event.getX());
                break;
            case MotionEvent.ACTION_UP:
                normalPointer();
            case MotionEvent.ACTION_MOVE:
                updateStepPosition(event.getX());
                break;
        }
        return true;
    }

    private void normalPointer() {
        circleRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 9, getResources().getDisplayMetrics());
    }

    private void pressedPointer() {
        circleRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
    }

    private void updateStepPosition(float val) {
        if(val < drawMin) {
            drawActVal = drawMin + 1;
        } else if (val > drawMax) {
            drawActVal = drawMax;
        } else {
            drawActVal = val;
        }

        for(Step step : stepList) {
            if(step.contains(drawActVal)) {
                actDataValue = step.value;
                drawActVal = step.drawValue;
                break;
            }
        }

        updateProgress();
        invalidate();
    }

    private void updateProgress() {
        progress.set(drawMin, guideTop, drawActVal, guideBottom);
        if(listener != null) {
            listener.onValueChanged(actDataValue);
        } else {
            Log.d(TAG, "Val: " + drawActVal + " - " + actDataValue);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(guide, paintGuide);
        canvas.drawRect(progress, paintProgress);
        canvas.drawCircle(drawActVal, drawMiddleHeight, circleRadius, paintPointer);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int paddings = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, getResources().getDisplayMetrics());

        drawMin = getPaddingLeft() + paddings;
        drawMax = w - getPaddingRight() - paddings;
        drawActVal = drawMin;

        drawMiddleHeight = h/2;
        guideTop = (int) (drawMiddleHeight - TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics()));
        guideBottom = (int) (drawMiddleHeight + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics()));

        guide.set(drawMin, guideTop, drawMax, guideBottom);
        setScale();
    }

    private void setScale() {
        stepList.clear();

        int range = (dataMax - dataMin) / steps;
        float drawRange = drawMax - drawMin;
        float longStep = drawRange / range;
        float actDrawStep = drawMin;
        for(int actStep = dataMin; actStep <= dataMax; actStep+=steps, actDrawStep += longStep) {
            stepList.add(new Step(actStep, actDrawStep, longStep));
        }

        updatePositionByStep(actDataValue);
    }

    private class Step {
        int value;
        float drawValue;
        float minRangeValue;
        float maxRangeValue;

        public Step(int value, float drawValue, float longStep) {
            this.value = value;
            this.drawValue = drawValue;
            this.minRangeValue = drawValue - (longStep/2);
            this.maxRangeValue = drawValue + (longStep/2);
        }

        public boolean contains(float drawActVal) {
            return minRangeValue <= drawActVal && drawActVal < maxRangeValue;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, getResources().getDisplayMetrics());
        setMeasuredDimension(widthMeasureSpec, height);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.minValue = getDataMin();
        ss.maxValue = getDataMax();
        ss.actValue = getActDataValue();
        ss.steps = steps;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setDataMin(ss.minValue);
        setDataMax(ss.maxValue);
        setActualValue(ss.actValue);
        setSteps(ss.steps);
    }

    static class SavedState extends BaseSavedState {
        int minValue;
        int maxValue;
        int actValue;
        int steps;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            minValue = in.readInt();
            maxValue = in.readInt();
            actValue = in.readInt();
            steps = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(minValue);
            out.writeInt(maxValue);
            out.writeInt(actValue);
            out.writeInt(steps);
            super.writeToParcel(out, flags);
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
