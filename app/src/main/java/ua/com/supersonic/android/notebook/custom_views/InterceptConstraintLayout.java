package ua.com.supersonic.android.notebook.custom_views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.MotionEventCompat;

import java.util.ArrayList;
import java.util.List;

public class InterceptConstraintLayout extends ConstraintLayout {

    public interface DirectMotionListener {
        void receiveEvent(MotionEvent event);
    }

    private boolean isInterceptOn;
    private float mDownPointX;
    private float mDownPointY;
    private List<DirectMotionListener> listeners;

    public InterceptConstraintLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void addDirectMotionListener(DirectMotionListener newListener) {
        if (newListener != null) {
            if (listeners == null) listeners = new ArrayList<>();
            listeners.add(newListener);
        }
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
        broadcastMotionEvent(ev);
        int action = MotionEventCompat.getActionMasked(ev);

        if (action == MotionEvent.ACTION_DOWN) {
            mDownPointX = ev.getX();
            mDownPointY = ev.getY();
        }

        if (isInterceptOn) {

            switch (action) {
                case MotionEvent.ACTION_CANCEL:
//                    Log.d("RECORD", "ACTION = CANCEL");
                case MotionEvent.ACTION_UP:
//                    Log.d("RECORD", "ACTION = UP");
                    return false; // Don't intercept touch event. Let the child handle it.
                case MotionEvent.ACTION_MOVE:
//                    Log.d("RECORD", "ACTION = MOVE");

//                  If the user drags their finger more than the
//                  touch slop, start the scroll.
                    final int dist = (int) calculateDist(ev);

//                    Log.d("RECORD", "DIST = " + dist);
//                    Log.d("RECORD", "THRESHOLD = " + mTouchSlop);

//                  Touch slop is calculated using ViewConfiguration constants.
                    if (dist > getTouchSlop()) {
//                      Start scrolling.
                        return true;
                    }

                    break;
            }
        }
//        }
        return super.onInterceptTouchEvent(ev);
    }

    private void broadcastMotionEvent(MotionEvent event) {
        if (listeners != null) {
            for (DirectMotionListener curListener : listeners) {
                curListener.receiveEvent(event);
            }
        }
    }

    private double calculateDist(MotionEvent me) {
        return Math.sqrt(Math.pow((me.getX() - mDownPointX), 2) + Math.pow((me.getY() - mDownPointY), 2));
    }

    private int getTouchSlop() {
        return ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    public void removeDirectMotionListener(DirectMotionListener remListener) {
        if (remListener != null && listeners != null) {
            listeners.remove(remListener);
        }
    }

    public void setInterceptOff() {
        isInterceptOn = false;
    }

    public void setInterceptOn() {
        isInterceptOn = true;
    }
}
