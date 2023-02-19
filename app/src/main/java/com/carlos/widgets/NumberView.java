package com.carlos.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.carlos.R;


public class NumberView extends LinearLayout {
    private ImageView mIvAdd;
    private ImageView mIvMinus;
    private EditText mEditValue;
    private int mCurrentValue;//当前的数值
    private OnValueChangeListener mOnValueChangeListener;//值发生变化时的回调
    private int mMax;//最大值
    private int mMin;//最小值
    private int mStep;//步长
    private int mDefaultValue;//默认值

    public int getMax() {
        return mMax;
    }

    public void setMax(int max) {
        mMax = max;
    }

    public int getMin() {
        return mMin;
    }

    public void setMin(int min) {
        mMin = min;
    }

    public int getStep() {
        return mStep;
    }

    public void setStep(int step) {
        mStep = step;
    }

    public int getDefaultValue() {
        return mDefaultValue;
    }

    public void setDefaultValue(int defaultValue) {
        mCurrentValue = mDefaultValue = defaultValue;
        updateText();
    }

    public int getCurrentValue() {
        return mCurrentValue;
    }

    //手动设置值需要更新UI
    public void setCurrentValue(int currentValue) {
        mCurrentValue = currentValue;
        updateText();
    }

    public NumberView(Context context) {
        this(context, null, 0);
    }

    public NumberView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NumberView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
        getAttrs(context, attrs);
    }

    //获取自定义属性
    private void getAttrs(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.NumberView);
        mMax = typedArray.getInt(R.styleable.NumberView_max, 9);
        mMin = typedArray.getInt(R.styleable.NumberView_min, 0);
        mStep = typedArray.getInt(R.styleable.NumberView_step, 1);
        mDefaultValue = typedArray.getInt(R.styleable.NumberView_defaultValue, 1);
        mCurrentValue = mDefaultValue;//当前值等于默认值
        if (mCurrentValue == mMin) {//当前值为最小值时减号不能点击
            mIvMinus.setEnabled(false);
        } else {
            mIvMinus.setEnabled(true);
        }
    }

    //加载布局，定义控件以及设置监听
    private void initView(Context context) {
        View inflate = LayoutInflater.from(context).inflate(R.layout.view_number_wigets, this, false);
        this.addView(inflate);
        mIvAdd = inflate.findViewById(R.id.iv_add);
        mIvMinus = inflate.findViewById(R.id.iv_minus);
        mEditValue = inflate.findViewById(R.id.edit_value);
        mIvAdd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //先加完再比较，只要一点加号，减号就可以点击了
                mCurrentValue += mStep;
                mIvMinus.setEnabled(true);
                //为了防止超过最大值，最后一步将最大值设置成当前值
                if (mCurrentValue >= mMax) {
                    mCurrentValue = mMax;
                    mIvAdd.setEnabled(false);
                }
                //更新UI
                updateText();
                //回调当前值
                if (mOnValueChangeListener != null) {
                    mOnValueChangeListener.onValueChange(mCurrentValue);
                }
            }
        });
        mIvMinus.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //减号与加号同理
                mCurrentValue -= mStep;
                mIvAdd.setEnabled(true);
                if (mCurrentValue <= mMin) {
                    mCurrentValue = mMin;
                    mIvMinus.setEnabled(false);
                }
                updateText();
                if (mOnValueChangeListener != null) {
                    mOnValueChangeListener.onValueChange(mCurrentValue);
                }
            }
        });
    }

    private void updateText() {
        mEditValue.setText(mCurrentValue + "");
    }

    public interface OnValueChangeListener {
        void onValueChange(int value);
    }

    public void setOnValueChangeListener(OnValueChangeListener onValueChangeListener) {
        mOnValueChangeListener = onValueChangeListener;
    }
}