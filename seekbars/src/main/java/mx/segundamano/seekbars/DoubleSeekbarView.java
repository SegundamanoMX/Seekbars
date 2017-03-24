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
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.RemoteViews.RemoteView;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;

/**
 * Created by diego on 23/05/16.
 * Custom seekbar with double pointer
 */
@RemoteView
public class DoubleSeekbarView extends View {
    private static final String TAG = DoubleSeekbarView.class.getSimpleName();

    private static final int NO_ALPHA = 0xFF;
    private static final int ALPHA = 0x99;
    private static final int TIMEOUT_SEND_ACCESSIBILITY_EVENT = 200;
    private AccessibilityEventSender mAccessibilityEventSender;

    //Drawing values
    private float drawMinActVal;
    private float drawMaxActVal;
    private float drawMin;
    private float drawMax;
    private float guideTop;
    private float guideBottom;
    private float drawMiddleHeight;
    private float minCircleRadius;
    private float maxCircleRadius;
    private float normalRadius;
    private float pressedRadius;
    private boolean left;

    //Drawing shapes
    private RectF guide = new RectF();
    private RectF range = new RectF();

    //Drawing paints
    private Paint paintGuide = new Paint();
    private Paint paintRange = new Paint();
    private Paint paintPointers = new Paint();

    //Data values
    private int dataMin;
    private int dataMax;
    private int minDataValue;
    private int maxDataValue;
    private int steps;
    private List<Step> stepList;

    //Listener for callback values changes
    public interface OnValuesChangeListener {
        void onValuesChange(int minValue, int maxValue);
    }

    private OnValuesChangeListener listener;

    public DoubleSeekbarView(Context context) {
        this(context, null);
    }

    public DoubleSeekbarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSaveEnabled(true);

        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.seekbar, 0, 0);
        try{
            setColors(context, ta);
            setValues(ta);
        } finally {
            ta.recycle();
        }

        paintPointers.setAntiAlias(true);
        paintPointers.setStyle(Paint.Style.FILL);

        stepList = new ArrayList<>();
        normalRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 9, getResources().getDisplayMetrics());
        pressedRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());

        normalPointers();
    }

    private void setColors(Context context, TypedArray ta) {
        paintPointers.setColor(ta.getColor(R.styleable.seekbar_pointerColor, ContextCompat.getColor(context, R.color.defaultPointerColor)));
        paintRange.setColor(ta.getColor(R.styleable.seekbar_guideColor, ContextCompat.getColor(context, R.color.defaultGuideColor)));
        paintGuide.setColor(ta.getColor(R.styleable.seekbar_baseColor,ContextCompat.getColor(context, R.color.defaultBaseColor)));
    }

    private void setValues(TypedArray ta) {
        minDataValue = ta.getInt(R.styleable.seekbar_minVal, 0);
        maxDataValue = ta.getInt(R.styleable.seekbar_maxVal, 10);
        steps = ta.getInt(R.styleable.seekbar_steps, 1);
        dataMin = minDataValue;
        dataMax = maxDataValue;
    }

    /**
     * Set OnValuesChangeListener for receive values changes
     * @param l
     */
    public void setOnValuesChangeListener(OnValuesChangeListener l) {
        listener = l;
    }

    public OnValuesChangeListener getListener() {
        return listener;
    }

    /**
     * Set min value can be selected. By default this value is 0
     * @param minValue Min value it can be selected
     */
    public void setMinValue(int minValue) {
        if(minValue >= dataMax) {
            throw new IllegalArgumentException();
        }

        this.dataMin = minValue;
        if(minDataValue < dataMin){
            minDataValue = dataMin;
        }
        setScale();
    }

    /**
     * Obtain the min value set in the seek bar
     * @return Min value set
     */
    public int getMinValue() {
        return dataMin;
    }

    /**
     * Set max value can be selected. By default this value is 10
     * @param maxValue Max value it can be selected
     */
    public void setMaxValue(int maxValue) {
        if(maxValue <= dataMin) {
            throw new IllegalArgumentException();
        }

        this.dataMax = maxValue;
        if(maxDataValue > dataMax) {
            maxDataValue = dataMax;
        }
        setScale();
    }

    /**
     * Obtain the max value set in the seek bar
     * @return Max value set
     */
    public int getMaxValue() {
        return dataMax;
    }

    /**
     * Set min actual value in seek bar
     * @param minValue Min value to be set between min value and max value.
     */
    public void setActMinValue(int minValue) {
        if(minValue < dataMin) {
            new DataFormatException("Value is minor than minimum data set").printStackTrace();
        } else if (minValue >= maxDataValue) {
            new IllegalArgumentException("Value is major than the actual max value").printStackTrace();
        }

        minDataValue = minValue;
        updateMinPositionByStep(minDataValue);
    }

    /**
     * Get the actual min value in seek bar
     * @return Actual min value selected
     */
    public int getMinDataValue() {
        return minDataValue;
    }

    /**
     * Set max actual value in seek bar
     * @param maxValue Max value to be set between min value and max value
     */
    public void setActMaxValue(int maxValue) {
        if(maxValue < minDataValue) {
            throw new IllegalArgumentException("Value is minor than the actual min value");
        } else if (maxValue > dataMax) {
            throw new IllegalArgumentException("Value is major than maximum data set");
        }

        maxDataValue = maxValue;
        updateMaxPositionByStep(maxDataValue);
    }

    /**
     * Get the actual max value in seek bar
     * @return Actual max value selected
     */
    public int getMaxDataValue() {
        return maxDataValue;
    }

    /**
     * Set the value of unit that each step will have between min value and max value.
     * The pointer will be set in the nearest step. By default this value is 1
     * @param steps Value of unit between each step
     */
    public void setSteps(int steps) {
        this.steps = steps;
        setScale();
    }

    private void updateMinPositionByStep(int minActValue) {
        if(stepList == null || stepList.isEmpty()) {
            this.minDataValue = minActValue;
            return;
        }

        for(Step step : stepList) {
            if(step.value == minActValue && minActValue < maxDataValue){
                isLeftPressed(step.drawValue);
                updateStepPositions(true, step.drawValue);
                break;
            }
        }
    }

    private void updateMaxPositionByStep(int maxActValue) {
        if(stepList == null || stepList.isEmpty()) {
            this.maxDataValue = maxActValue;
            return;
        }

        for(Step step : stepList) {
            if(step.value == maxActValue && maxActValue > minDataValue){
                isLeftPressed(step.drawValue);
                updateStepPositions(false, step.drawValue);
                break;
            }
        }
    }

    /**
     * Get the actual color value in the base guide bar
     * @return int color value
     */
    public int getGuideBarColor() {
        return paintGuide.getColor();
    }

    /**
     * Set guide bar color value
     * @param color Guide bar color value
     */
    public void setGuideBarColor(int color) {
        paintGuide.setColor(color);
    }

    /**
     * Get the actual color value in the range bar
     * @return int color value
     */
    public int getRangeBarColor() {
        return paintRange.getColor();
    }

    /**
     * Set range bar color value
     * @param color Range bar color value
     */
    public void setPaintRange(int color) {
        paintRange.setColor(color);
    }

    /**
     * Get the actual color value in the pointers
     * @return int color value
     */
    public int getPointersColor() {
        return paintPointers.getColor();
    }

    /**
     * Set pointers color value
     * @param color Pointers color value
     */
    public void setPointersColor(int color) {
        paintPointers.setColor(color);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        paintPointers.setAlpha(enabled ? NO_ALPHA : ALPHA);
        paintRange.setAlpha(enabled ? NO_ALPHA : ALPHA);
        paintGuide.setAlpha(enabled ? NO_ALPHA : ALPHA);

        invalidate();
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return DoubleSeekbarView.class.getName();
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setItemCount(getMinValue());
        event.setCurrentItemIndex(getMinDataValue());
        event.setItemCount(getMaxValue());
        event.setCurrentItemIndex(getMaxDataValue());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);

        if (isEnabled()) {
            info.addAction(-1000920);
        }
    }

    private void scheduleAccessibilityEventSender() {
        if (mAccessibilityEventSender == null) {
            mAccessibilityEventSender = new AccessibilityEventSender();
        } else {
            removeCallbacks(mAccessibilityEventSender);
        }
        postDelayed(mAccessibilityEventSender, TIMEOUT_SEND_ACCESSIBILITY_EVENT);
    }

    private class AccessibilityEventSender implements Runnable {
        public void run() {
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!isEnabled()) {
            return false;
        }

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                isLeftPressed(event.getX());
                pressedPointers(left);
                updateStepPositions(left, event.getX());
                break;
            case MotionEvent.ACTION_UP:
                normalPointers();
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                pressedPointers(left);
                updateStepPositions(left, event.getX());
                break;
        }
        return true;
    }

    private void isLeftPressed(float pos) {
        float middleRange = ((drawMaxActVal - drawMinActVal) / 2) + drawMinActVal;
        left = pos < middleRange;
    }

    private void normalPointers() {
        minCircleRadius = normalRadius;
        maxCircleRadius = normalRadius;
    }

    private void pressedPointers(boolean left) {
        if(left) {
            minCircleRadius = pressedRadius;
            maxCircleRadius = normalRadius;
        } else {
            maxCircleRadius = pressedRadius;
            minCircleRadius = normalRadius;
        }
    }

    private void updateStepPositions(boolean left, float posX) {
        if(posX < drawMin) {
            drawMinActVal = drawMin;
        } else if (posX > drawMax) {
            drawMaxActVal = drawMax;
        } else if (left) {
            if(posX < drawMaxActVal) {
                drawMinActVal = posX;
            } else if (posX > drawMaxActVal) {
                drawMinActVal = drawMaxActVal;
                isLeftPressed(posX);
            }
        } else {
            if(posX > drawMinActVal) {
                drawMaxActVal = posX;
            } else if (posX < drawMinActVal) {
                drawMaxActVal = drawMinActVal;
                isLeftPressed(posX);
            }
        }

        for(Step step : stepList) {
            if(left && step.contains(drawMinActVal)) {
                minDataValue = step.value;
                drawMinActVal = step.drawValue;
            } else if(!left && step.contains(drawMaxActVal)) {
                maxDataValue = step.value;
                drawMaxActVal = step.drawValue;
            }
        }

        updateRange();
        invalidate();
    }

    private void updateRange() {
        range.set(drawMinActVal, guideTop, drawMaxActVal, guideBottom);
        if(listener != null) {
            listener.onValuesChange(minDataValue, maxDataValue);
        } else {
            Log.d(TAG, "MinVal: " + minDataValue + ", MaxVal: " + maxDataValue);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(guide, paintGuide);
        canvas.drawRect(range, paintRange);
        canvas.drawCircle(drawMinActVal, drawMiddleHeight, minCircleRadius, paintPointers);
        canvas.drawCircle(drawMaxActVal, drawMiddleHeight, maxCircleRadius, paintPointers);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int paddings = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, getResources().getDisplayMetrics());

        drawMin = getPaddingLeft() + paddings;
        drawMax = w - getPaddingRight() - paddings;
        drawMinActVal = drawMin;
        drawMaxActVal = drawMax;

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
        for (int actStep = dataMin; actStep <= dataMax; actStep+=steps, actDrawStep+=longStep) {
            stepList.add(new Step(actStep, actDrawStep, longStep));
        }

        updateMinPositionByStep(minDataValue);
        updateMaxPositionByStep(maxDataValue);
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

        public boolean contains(float drawActValue) {
            return minRangeValue <= drawActValue && drawActValue < maxRangeValue;
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
        ss.minValue =  getMinValue();
        ss.maxValue = getMaxValue();
        ss.minActValue = getMinDataValue();
        ss.maxActValue = getMaxDataValue();
        ss.steps = steps;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if(!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setMinValue(ss.minValue);
        setMaxValue(ss.maxValue);
        setActMinValue(ss.minActValue);
        setActMaxValue(ss.maxActValue);
        setSteps(ss.steps);
    }

    static class SavedState extends BaseSavedState {
        Parcelable superState;
        int minValue;
        int maxValue;
        int minActValue;
        int maxActValue;
        int steps;

        SavedState(Parcelable superState) {
            super(EMPTY_STATE);
            this.superState = superState;
        }

        private SavedState(Parcel in) {
            super(in);
            superState = in.readParcelable(DoubleSeekbarView.SavedState.class.getClassLoader());
            minValue = in.readInt();
            maxValue = in.readInt();
            minActValue = in.readInt();
            maxActValue = in.readInt();
            steps = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeParcelable(superState, flags);
            out.writeInt(minValue);
            out.writeInt(maxValue);
            out.writeInt(minActValue);
            out.writeInt(maxActValue);
            out.writeInt(steps);
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
