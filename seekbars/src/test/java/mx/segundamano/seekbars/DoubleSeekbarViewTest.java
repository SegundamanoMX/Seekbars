package mx.segundamano.seekbars;

import android.os.Build;

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

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = Build.VERSION_CODES.LOLLIPOP)
public class DoubleSeekbarViewTest {

    private DoubleSeekbarView doubleSeekbarView;

    @Before
    public void setUp() {
        doubleSeekbarView = new DoubleSeekbarView(RuntimeEnvironment.application);

        doubleSeekbarView = new DoubleSeekbarView(RuntimeEnvironment.application,
                Robolectric.buildAttributeSet()
                        .addAttribute(android.R.attr.layout_width, "500dp")
                        .addAttribute(android.R.attr.layout_height, "wrap_content")
                        .addAttribute(R.attr.guideColor, "#FFF")
                        .addAttribute(R.attr.pointerColor, "#CCC")
                        .build());
    }

    @Test
    public void testSetOnInsertSeekBarListener() throws Exception {
        assertNull(doubleSeekbarView.getListener());

        DoubleSeekbarView.OnValuesChangeListener mockListener = mock(DoubleSeekbarView.OnValuesChangeListener.class);
        doubleSeekbarView.setOnValuesChangeListener(mockListener);

        assertEquals(mockListener, doubleSeekbarView.getListener());
    }

    @Test
    public void testSetDataMinLessThanMax() {
        int expectedValue = 20;
        doubleSeekbarView.setMaxValue(30);
        doubleSeekbarView.setMinValue(expectedValue);

        assertEquals(expectedValue, doubleSeekbarView.getMinValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetDataMinGreaterThanMax() {
        doubleSeekbarView.setMaxValue(10);
        doubleSeekbarView.setMinValue(20);
    }

    @Test
    public void testSetDataMaxGreaterThanMin() {
        int expectedValue = 20;
        doubleSeekbarView.setMinValue(5);
        doubleSeekbarView.setMaxValue(expectedValue);

        assertEquals(expectedValue, doubleSeekbarView.getMaxValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetDataMaxLessThanMin() {
        doubleSeekbarView.setMinValue(8);
        doubleSeekbarView.setMaxValue(4);
    }

}