package mx.segundamano.seekbars;

import android.os.Build;
import android.view.MotionEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


/**
 * Created by diego on 07/07/16.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = Build.VERSION_CODES.LOLLIPOP)
public class SeekBarViewTest {

    private SeekBarView seekBarView;

    @Before
    public void setUp() {
        seekBarView = new SeekBarView(RuntimeEnvironment.application,
                Robolectric.buildAttributeSet()
                        .addAttribute(android.R.attr.layout_width, "500dp")
                        .addAttribute(android.R.attr.layout_height, "wrap_content")
                        .addAttribute(R.attr.guideColor, "#FFF")
                        .addAttribute(R.attr.pointerColor, "#CCC")
                        .build());
    }

    @Test
    public void testSetOnInsertSeekBarListener() throws Exception {
        assertNull(seekBarView.getListener());

        SeekBarView.OnInsertSeekBarListener mockListener = mock(SeekBarView.OnInsertSeekBarListener.class);
        seekBarView.setOnInsertSeekBarListener(mockListener);

        assertEquals(mockListener, seekBarView.getListener());
    }

    @Test
    public void testSetDataMinLessThanMax() {
        int expectedValue = 20;
        seekBarView.setDataMax(30);
        seekBarView.setDataMin(expectedValue);

        assertEquals(expectedValue, seekBarView.getDataMin());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetDataMinGreaterThanMax() {
        seekBarView.setDataMax(10);
        seekBarView.setDataMin(20);
    }

    @Test
    public void testSetDataMaxGreaterThanMin() {
        int expectedValue = 20;
        seekBarView.setDataMin(5);
        seekBarView.setDataMax(expectedValue);

        assertEquals(expectedValue, seekBarView.getDataMax());
    }

    @Test
    public void testSetDataMaxLessThanActualValue() {
        int expectedValue = 8;
        seekBarView.setActualValue(9);
        seekBarView.setDataMax(expectedValue);

        assertEquals(expectedValue, seekBarView.getDataMax());
        assertEquals(expectedValue, seekBarView.getActDataValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetDataMaxLessThanMin() {
        seekBarView.setDataMin(8);
        seekBarView.setDataMax(4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetActualValueLessThanMin() {
        seekBarView.setDataMin(5);
        seekBarView.setActualValue(3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetActualValueGreaterThanMax() {
        seekBarView.setDataMax(10);
        seekBarView.setActualValue(12);
    }

    @Test
    public void testSetActualValueBetweenMinAndMax() {
        int expextedValue = 20;
        seekBarView.setDataMin(5);
        seekBarView.setDataMax(20);
        seekBarView.setActualValue(expextedValue);
        assertEquals(expextedValue, seekBarView.getActDataValue());
    }

    @Test
    public void testSetActualValueWithListener() {
        int expectedValue = 10;
        seekBarView.setSteps(10);
        assertNull(seekBarView.getListener());

        SeekBarView.OnInsertSeekBarListener mockListener = mock(SeekBarView.OnInsertSeekBarListener.class);
        seekBarView.setOnInsertSeekBarListener(mockListener);
        assertEquals(mockListener, seekBarView.getListener());

        seekBarView.setActualValue(expectedValue);
        verify(mockListener).onValueChanged(expectedValue);
    }

    @Test
    public void testSetSteps() {
        int expectedValue = 20;
        seekBarView.setSteps(expectedValue);

        assertEquals(expectedValue, seekBarView.getSteps());
    }

    @Test
    public void testOnTouch() {
        MotionEvent motionEvent = MotionEvent.obtain(100, 100, MotionEvent.ACTION_DOWN, 30, 0, 0);
        seekBarView.onTouchEvent(motionEvent);
        motionEvent = MotionEvent.obtain(100, 100, MotionEvent.ACTION_MOVE, 50, 0, 0);
        seekBarView.onTouchEvent(motionEvent);
        motionEvent = MotionEvent.obtain(100, 100, MotionEvent.ACTION_UP, 50, 0, 0);
        seekBarView.onTouchEvent(motionEvent);

        assertEquals(0, seekBarView.getActDataValue());
    }
}