package com.fsck.splitview;

import com.fsck.splitview.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.Log;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;

public class SplitView extends LinearLayout implements OnTouchListener {

    private int mHandleId;
    private View mHandle;

    private int mPrimaryContentId;
    private View mPrimaryContent;

    private int mSecondaryContentId;
    private View mSecondaryContent;

    private int mLastPrimaryContentSize;

    private boolean mDragging;
    private float mDragStartX;
    private float mDragStartY;

    private float mPointerOffset;

    final static private int MAXIMIZED_VIEW_TOLERANCE_DIP = 30;

    public SplitView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray viewAttrs = context.obtainStyledAttributes(attrs, R.styleable.SplitView);

        RuntimeException e = null;
        mHandleId = viewAttrs.getResourceId(R.styleable.SplitView_handle, 0);
        if (mHandleId == 0) {
            e = new IllegalArgumentException(viewAttrs.getPositionDescription() +
                                             ": The required attribute handle must refer to a valid child view.");
        }

        mPrimaryContentId = viewAttrs.getResourceId(R.styleable.SplitView_primaryContent, 0);
        if (mPrimaryContentId == 0) {
            e = new IllegalArgumentException(viewAttrs.getPositionDescription() +
                                             ": The required attribute primaryContent must refer to a valid child view.");
        }


        mSecondaryContentId = viewAttrs.getResourceId(R.styleable.SplitView_secondaryContent, 0);
        if (mSecondaryContentId == 0) {
            e = new IllegalArgumentException(viewAttrs.getPositionDescription() +
                                             ": The required attribute secondaryContent must refer to a valid child view.");
        }

        viewAttrs.recycle();

        if (e != null) {
            throw e;
        }
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mHandle = findViewById(mHandleId);
        if (mHandle == null) {
            String name = getResources().getResourceEntryName(mHandleId);
            throw new RuntimeException("Your Panel must have a child View whose id attribute is 'R.id." + name + "'");

        }
        mPrimaryContent = findViewById(mPrimaryContentId);
        if (mPrimaryContent == null) {
            String name = getResources().getResourceEntryName(mPrimaryContentId);
            throw new RuntimeException("Your Panel must have a child View whose id attribute is 'R.id." + name + "'");

        }

        mLastPrimaryContentSize = getPrimaryContentSize();

        mSecondaryContent = findViewById(mSecondaryContentId);
        if (mSecondaryContent == null) {
            String name = getResources().getResourceEntryName(mSecondaryContentId);
            throw new RuntimeException("Your Panel must have a child View whose id attribute is 'R.id." + name + "'");

        }

        mHandle.setOnTouchListener(this);

    }
    @Override
    public boolean onTouch(View view, MotionEvent me) {
        ViewGroup.LayoutParams thisParams = getLayoutParams();
        // Only capture drag events if we start
        if (view != mHandle) {
            return false;
        }

        if (me.getAction() == MotionEvent.ACTION_DOWN) {
            mDragging = true;
            mDragStartX = me.getX();
            mDragStartY = me.getY();
            if (getOrientation() == VERTICAL) {
                mPointerOffset = me.getRawY() - mPrimaryContent.getMeasuredHeight();
            } else {
                mPointerOffset = me.getRawX() - mPrimaryContent.getMeasuredWidth();
            }
            return true;
        }
        else if (me.getAction() == MotionEvent.ACTION_UP) {
            mDragging = false;
            if (mDragStartX == me.getX() && mDragStartY == me.getY()) {
                if (isPrimaryContentMaximized() || isSecondaryContentMaximized()) {
                    setPrimaryContentSize(mLastPrimaryContentSize);
                } else {
                    maximizeSecondaryContent();
                }
            }
            return true;
        } else if (me.getAction() == MotionEvent.ACTION_MOVE) {
            if (getOrientation() == VERTICAL) {
                setPrimaryContentHeight( (int)(me.getRawY() - mPointerOffset));
            } else {
                setPrimaryContentWidth( (int)(me.getRawX() - mPointerOffset));
            }
        }
            return true;
    }


    public int getPrimaryContentSize() {
            if (getOrientation() == VERTICAL) {
                return mPrimaryContent.getMeasuredHeight();
            } else {
             return mPrimaryContent.getMeasuredWidth();
            }

    }

    public boolean setPrimaryContentSize(int newSize) {
        if (getOrientation() == VERTICAL) {
            return setPrimaryContentHeight(newSize);
        } else {
            return setPrimaryContentWidth(newSize);
        }
    }


    private boolean setPrimaryContentHeight(int newHeight) {
        ViewGroup.LayoutParams params = mPrimaryContent.getLayoutParams();
        if (mSecondaryContent.getMeasuredHeight() < 1 && newHeight > params.height) {
            return false;
        }
        if (newHeight >= 0) {
            params.height = newHeight;
        }
        unMinimizeSecondaryContent();
        mPrimaryContent.setLayoutParams(params);
        return true;

    }

    private boolean setPrimaryContentWidth(int newWidth) {
        ViewGroup.LayoutParams params = mPrimaryContent.getLayoutParams();


        if (mSecondaryContent.getMeasuredWidth() < 1 && newWidth > params.width) {
            return false;
        }
        if (newWidth >= 0) {
            params.width = newWidth;
        }
        unMinimizeSecondaryContent();
        mPrimaryContent.setLayoutParams(params);
        return true;
    }
    public boolean isPrimaryContentMaximized() {
        if ( (getOrientation() == VERTICAL && (mSecondaryContent.getMeasuredHeight() < MAXIMIZED_VIEW_TOLERANCE_DIP) ) ||
                (getOrientation() == HORIZONTAL && (mSecondaryContent.getMeasuredWidth() < MAXIMIZED_VIEW_TOLERANCE_DIP) )) {
            return true;
        } else {
            return false;
        }

    }


    public boolean isSecondaryContentMaximized() {
        if ( (getOrientation() == VERTICAL && (mPrimaryContent.getMeasuredHeight() < MAXIMIZED_VIEW_TOLERANCE_DIP) ) ||
                (getOrientation() == HORIZONTAL && (mPrimaryContent.getMeasuredWidth() < MAXIMIZED_VIEW_TOLERANCE_DIP) )) {
            return true;
        } else {
            return false;
        }
    }

    public void maximizePrimaryContent() {

        mLastPrimaryContentSize = getPrimaryContentSize();

        ViewGroup.LayoutParams params = mPrimaryContent.getLayoutParams();
        ViewGroup.LayoutParams secondaryParams = mSecondaryContent.getLayoutParams();
        if (getOrientation() == VERTICAL) {
            params.height = LayoutParams.FILL_PARENT;// getLayoutParams().height - mHandle.getLayoutParams().height;
            secondaryParams.height = 1;
        } else {
            params.width = LayoutParams.FILL_PARENT; //getLayoutParams().width - mHandle.getLayoutParams().width;
            secondaryParams.width = 1;
        }
        mPrimaryContent.setLayoutParams(params);
        mSecondaryContent.setLayoutParams(secondaryParams);
    }

    public void maximizeSecondaryContent() {
        mLastPrimaryContentSize = getPrimaryContentSize();
        ViewGroup.LayoutParams params = mPrimaryContent.getLayoutParams();
        ViewGroup.LayoutParams secondaryParams = mSecondaryContent.getLayoutParams();
        if (getOrientation() == VERTICAL) {
            params.height = 1;
           secondaryParams.height = LayoutParams.FILL_PARENT; //getLayoutParams().height - mHandle.getLayoutParams().height;
        } else {
            params.width = 1;
            secondaryParams.width = LayoutParams.FILL_PARENT; //getLayoutParams().width - mHandle.getLayoutParams().width;
        }
        mPrimaryContent.setLayoutParams(params);
        mSecondaryContent.setLayoutParams(secondaryParams);

    }

    private void unMinimizeSecondaryContent() {
        ViewGroup.LayoutParams secondaryParams = mSecondaryContent.getLayoutParams();
        if (getOrientation() == VERTICAL) {
            secondaryParams.height = LayoutParams.FILL_PARENT;
        } else {
            secondaryParams.width = LayoutParams.FILL_PARENT;

        }
        mSecondaryContent.setLayoutParams(secondaryParams);

    }

};
