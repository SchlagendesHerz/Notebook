package ua.com.supersonic.android.notebook.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MotionEventCompat;
import androidx.viewpager.widget.ViewPager;

import ua.com.supersonic.android.notebook.MainActivity;

public class NonSwipeableViewPager extends ViewPager {

    private final int touchSlop;
    private boolean isEnabled = true;
    private boolean isSwiping;
    private float prevX;


    public NonSwipeableViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        ViewConfiguration vc = ViewConfiguration.get(context);
        touchSlop = vc.getScaledTouchSlop();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

/*        final int action = MotionEventCompat.getActionMasked(ev);

        // Always handle the case of the touch gesture being complete.
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            // Release the scroll.
            isSwiping = false;
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                prevX = ev.getX();
                break;
            case MotionEvent.ACTION_MOVE: {
                if (isSwiping) {
                    return true;
                }

                // If the user has dragged their finger horizontally more than
                // the touch slop, start the scroll

                // left as an exercise for the reader
                final float xDiff = calculateDistanceX(ev);

                // Touch slop should be calculated using ViewConfiguration
                // constants.
                if (xDiff > touchSlop) {
                    // Start scrolling!
                    isSwiping = true;
                    return true;
                }
                break;
            }
        }*/

        return this.isEnabled && super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return this.isEnabled && super.onTouchEvent(ev);
    }

    public void setSwipeEnabled(boolean input) {
        isEnabled = input;
    }

    private float calculateDistanceX(MotionEvent ev) {
        return Math.abs(prevX - ev.getX());
    }
}