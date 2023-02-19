package com.carlos.widgets;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

public class MarqueeTextView extends AppCompatTextView {

    private boolean isStop = false;

    public MarqueeTextView(Context context) {
        super(context);
    }

    public MarqueeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MarqueeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public boolean isFocused() {
        if (this.isStop) {
            return super.isFocused();
        }
        return true;
    }

    public void stopScroll() {
        this.isStop = true;
    }

    public void start() {
        this.isStop = false;
    }

    protected void onDetachedFromWindow() {
        stopScroll();
        super.onDetachedFromWindow();
    }
}