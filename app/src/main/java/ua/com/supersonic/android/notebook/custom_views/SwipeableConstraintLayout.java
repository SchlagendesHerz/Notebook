package ua.com.supersonic.android.notebook.custom_views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.MotionEventCompat;

public class SwipeableConstraintLayout extends ConstraintLayout {

    private boolean isInterceptOn;
    private float mDownPointX;
    private float mDownPointY;
    private boolean mIsScrolling;
    private int mTouchSlop;

    public SwipeableConstraintLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        ViewConfiguration vc = ViewConfiguration.get(context);
        mTouchSlop = vc.getScaledTouchSlop();
    }

    public float getDownPointX() {
        return mDownPointX;
    }

    public float getDownPointY() {
        return mDownPointY;
    }

    public boolean isInterceptOn() {
        return isInterceptOn;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);
//        Log.d("RECORD", "-------------------");
//        Log.d("RECORD", "ACTION = " + MotionEvent.actionToString(action));
//        Log.d("RECORD", "isInterceptOn = " + Boolean.toString(isInterceptOn).toUpperCase());

        if (action == MotionEvent.ACTION_DOWN) {
//            Log.d("RECORD", "ACTION = DOWN");

            mDownPointX = ev.getX();
            mDownPointY = ev.getY();
        }

        if (isInterceptOn) {

//            if (MainActivity.recordsFragment.isFindBtPressed()) {

            switch (action) {
                case MotionEvent.ACTION_CANCEL:
//                    Log.d("RECORD", "ACTION = CANCEL");
                case MotionEvent.ACTION_UP:
//                    Log.d("RECORD", "ACTION = UP");
                    mIsScrolling = false;
                    return false; // Don't intercept touch event. Let the child handle it.
                case MotionEvent.ACTION_MOVE:
//                    Log.d("RECORD", "ACTION = MOVE");

//                  If the user drags their finger more than the
//                  touch slop, start the scroll.
                    final int dist = (int) calculateDist(ev);

//                    Log.d("RECORD", "DIST = " + dist);
//                    Log.d("RECORD", "THRESHOLD = " + mTouchSlop);

//                  Touch slop is calculated using ViewConfiguration constants.
                    if (dist > mTouchSlop) {
//                      Start scrolling.
                        mIsScrolling = true;
                        return true;
                    }

                    break;
            }
        }
//        }
        return super.onInterceptTouchEvent(ev);
    }

    public void setInterceptOff() {
        isInterceptOn = false;
    }

    public void setInterceptOn() {
        isInterceptOn = true;
    }

    private double calculateDist(MotionEvent me) {
        return Math.sqrt(Math.pow((me.getX() - mDownPointX), 2) + Math.pow((me.getY() - mDownPointY), 2));
    }
}
