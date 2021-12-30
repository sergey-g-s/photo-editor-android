package com.btwb.photoeditorsdk;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

class MultiTouchListener implements OnTouchListener {

    private static final int INVALID_POINTER_ID = -1;
    private static float renderedWidth;
    private static float renderedHeight;
    private static float renderedX;
    private static float renderedY;
    private boolean isRotateEnabled = true;
    private boolean isTranslateEnabled = true;
    private boolean isScaleEnabled = true;
    private float maximumScale = 10.0f;
    private int mActivePointerId = INVALID_POINTER_ID;
    private float mPrevX, mPrevY, mPrevRawX, mPrevRawY, mLeftX, mRightX, mBottomX, mTopX, mVertical, mHorizontal;
    private ScaleGestureDetector mScaleGestureDetector;

    private int[] location = new int[2];
    private int width;
    private Rect outRect;
    private boolean editable;
    private boolean moveX = true;
    private boolean moveY = true;
    private View deleteView, leftLineView, rightLineView, bottomHorizontalLine, topHorizontalLine, verticalLine, horizontalLine;
    private String type;
    private ImageView photoEditImageView;
    private RelativeLayout parentView;
    private LinearLayout  activeView;

    private OnMultiTouchListener onMultiTouchListener;
    private OnPhotoEditorSDKListener onPhotoEditorSDKListener;

    MultiTouchListener(View deleteView,
                       RelativeLayout parentView,
                       LinearLayout activeView,
                       ImageView photoEditImageView,
                       OnPhotoEditorSDKListener onPhotoEditorSDKListener,
                       int width,
                       boolean editable,
                       String type,
                       View leftLineView,
                       View rightLineView,
                       View bottomHorizontalLine,
                       View topHorizontalLine,
                       View verticalLine,
                       View horizontalLine
    ) {
        mScaleGestureDetector = new ScaleGestureDetector(new ScaleGestureListener());
        this.deleteView = deleteView;
        this.leftLineView = leftLineView;
        this.rightLineView = rightLineView;
        this.bottomHorizontalLine = bottomHorizontalLine;
        this.topHorizontalLine = topHorizontalLine;
        this.verticalLine = verticalLine;
        this.horizontalLine = horizontalLine;
        this.parentView = parentView;
        this.activeView = activeView;
        this.editable = editable;
        this.type = type;
        this.width = width;
        this.photoEditImageView = photoEditImageView;
        this.onPhotoEditorSDKListener = onPhotoEditorSDKListener;
        outRect = new Rect(deleteView.getLeft(), deleteView.getTop(),
                deleteView.getRight(), deleteView.getBottom());
    }

    private static float adjustAngle(float degrees) {
        if (degrees > 180.0f) {
            degrees -= 360.0f;
        } else if (degrees < -180.0f) {
            degrees += 360.0f;
        }

        return degrees;
    }

    private static void move(View view, TransformInfo info) {
        computeRenderOffset(view, 0, 0);
        adjustTranslation(view, info.deltaX, info.deltaY, true, true);

        float scale = view.getScaleX() * info.deltaScale;
        scale = Math.max(info.minimumScale, Math.min(info.maximumScale, scale));
        renderedWidth = view.getWidth() * scale;
        renderedHeight = view.getHeight() * scale;
        view.setScaleX(scale);
        view.setScaleY(scale);



        float rotation = adjustAngle(view.getRotation() + info.deltaAngle);
        view.setRotation(rotation);
    }

    private static void adjustTranslation(View view, float deltaX, float deltaY, boolean moveX, boolean moveY) {
        float[] deltaVector = {deltaX, deltaY};
        view.getMatrix().mapVectors(deltaVector);
        if(moveX){
            view.setTranslationX(view.getTranslationX() + deltaVector[0]);
        }else {
            view.setTranslationX(view.getTranslationX());
        }

        if(moveY){
            view.setTranslationY(view.getTranslationY() + deltaVector[1]);
        }else {
            view.setTranslationY(view.getTranslationY());
        }
    }

    private static void computeRenderOffset(View view, float pivotX, float pivotY) {
        if (view.getPivotX() == pivotX && view.getPivotY() == pivotY) {
            return;
        }

        float[] prevPoint = {0.0f, 0.0f};
        view.getMatrix().mapPoints(prevPoint);

        view.setPivotX(pivotX);
        view.setPivotY(pivotY);

        float[] currPoint = {0.0f, 0.0f};
        view.getMatrix().mapPoints(currPoint);

        float offsetX = currPoint[0] - prevPoint[0];
        float offsetY = currPoint[1] - prevPoint[1];

        view.setTranslationX(view.getTranslationX() - offsetX);
        view.setTranslationY(view.getTranslationY() - offsetY);
    }

    private void LineAnimation(View view, float start, float end){
        AlphaAnimation animation = new AlphaAnimation(start, end);
        animation.setDuration(500);
        view.setAlpha(end);
        view.startAnimation(animation);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        mScaleGestureDetector.onTouchEvent(view, event);

        if (!isTranslateEnabled) {
            return true;
        }

        int action = event.getAction();

        int x = (int) event.getRawX();
        int y = (int) event.getRawY();

        switch (action & event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mLeftX = this.leftLineView.getX();
                mRightX = this.rightLineView.getX();
                mBottomX = this.bottomHorizontalLine.getY();
                mTopX = this.topHorizontalLine.getY();
                mVertical = this.verticalLine.getX();
                mHorizontal = this.horizontalLine.getY();
                mPrevX = event.getX();
                mPrevY = event.getY();
                mPrevRawX = event.getRawX();
                mPrevRawY = event.getRawY();
                mActivePointerId = event.getPointerId(0);
                deleteView.setVisibility(View.VISIBLE);
                activeView.setVisibility(View.GONE);
                view.bringToFront();
                firePhotoEditorSDKListener(view, true);
                break;
            case MotionEvent.ACTION_MOVE:
                int pointerIndexMove = event.findPointerIndex(mActivePointerId);
                if (pointerIndexMove != -1) {
                    float currX = event.getX(pointerIndexMove);
                    float currY = event.getY(pointerIndexMove);
                    float scaleWidth = renderedWidth == 0.0 ? view.getWidth() : renderedWidth;
                    float scaledHeight = renderedHeight == 0.0 ? view.getHeight() : renderedHeight;
                    float scaledX = view.getX();
                    float scaledY = view.getY();


                    if((int) scaledX == (int) (mLeftX + 4) && moveX){
                        this.LineAnimation(this.leftLineView, 0f, 100f);
                        moveX = false;
                    }else if (this.leftLineView.getAlpha() == 100.0 && moveX){
                        this.LineAnimation(this.leftLineView, 100f, 0f);
                    }

                    if((int) (scaledX + scaleWidth) == (int) (mRightX - 2) && moveX){
                        this.LineAnimation(this.rightLineView, 0f, 100f);
                        moveX = false;
                    }else if (this.rightLineView.getAlpha() == 100.0 && moveX){
                        this.LineAnimation(this.rightLineView, 100f, 0f);
                    }

                    if((int) (scaledY + scaledHeight) == (int) (mBottomX - 2) && moveY){
                        this.LineAnimation(this.bottomHorizontalLine, 0f, 100f);
                        moveY = false;
                    }else if (this.bottomHorizontalLine.getAlpha() == 100.0 && moveY){
                        this.LineAnimation(this.bottomHorizontalLine, 100f, 0f);
                    }

                    if((int) scaledY == (int) (mTopX + 4) && moveY){
                        this.LineAnimation(this.topHorizontalLine, 0f, 100f);
                        moveY = false;
                    }else if (this.topHorizontalLine.getAlpha() == 100.0 && moveY){
                        this.LineAnimation(this.topHorizontalLine, 100f, 0f);
                    }

                    if((int) (scaledX + scaleWidth / 2) == (int) mVertical && moveX){
                        this.LineAnimation(this.verticalLine, 0f, 100f);
                        moveX = false;
                    }else if (this.verticalLine.getAlpha() == 100.0 && moveX){
                        this.LineAnimation(this.verticalLine, 100f, 0f);
                    }

                    if((int) (scaledY + scaledHeight / 2) == (int) mHorizontal && moveY){
                        this.LineAnimation(this.horizontalLine, 0f, 100f);
                        moveY = false;
                    }else if (this.horizontalLine.getAlpha() == 100.0 && moveY){
                        this.LineAnimation(this.horizontalLine, 100f, 0f);
                    }

                    if(mPrevX - currX > 30 || mPrevX - currX < -30){
                        moveX = true;
                    }

                    if(mPrevY - currY > 30 || mPrevY - currY < -30){
                        moveY = true;
                    }

                    if (!mScaleGestureDetector.isInProgress()) {
                        adjustTranslation(view, currX - mPrevX, currY - mPrevY, moveX, moveY);
                    }



                }
                break;
            case MotionEvent.ACTION_CANCEL:
                mActivePointerId = INVALID_POINTER_ID;
                break;
            case MotionEvent.ACTION_UP:
                mActivePointerId = INVALID_POINTER_ID;
                if (isViewInBounds(deleteView, x, y)) {
                    if (onMultiTouchListener != null)
                        onMultiTouchListener.onRemoveViewListener(view);
                } else if (!isViewInBounds(photoEditImageView, x, y)) {
                view.animate().translationY(0).translationY(0);
                }
                deleteView.setVisibility(View.GONE);
                activeView.setVisibility(View.VISIBLE);
                moveY = true;
                moveX = true;
                this.LineAnimation(this.horizontalLine, 100f, 0f);
                this.LineAnimation(this.verticalLine, 100f, 0f);
                this.LineAnimation(this.topHorizontalLine, 100f, 0f);
                this.LineAnimation(this.bottomHorizontalLine, 100f, 0f);
                this.LineAnimation(this.rightLineView, 100f, 0f);
                this.LineAnimation(this.leftLineView, 100f, 0f);
                firePhotoEditorSDKListener(view, false);
                float mCurrentCancelX = event.getRawX();
                float mCurrentCancelY = event.getRawY();
                if (mCurrentCancelX == mPrevRawX || mCurrentCancelY == mPrevRawY) {
                    if(this.editable){
                        if (view instanceof TextView) {
                            if (onMultiTouchListener != null) {
                                onMultiTouchListener.onEditTextClickListener(
                                        ((TextView) view));
                            }
                            if (onPhotoEditorSDKListener != null) {
                                onPhotoEditorSDKListener.onEditTextChangeListener(
                                        (TextView) view, width, type);
                            }
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                int pointerIndexPointerUp = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                int pointerId = event.getPointerId(pointerIndexPointerUp);
                if (pointerId == mActivePointerId) {
                    int newPointerIndex = pointerIndexPointerUp == 0 ? 1 : 0;
                    mPrevX = event.getX(newPointerIndex);
                    mPrevY = event.getY(newPointerIndex);
                    mActivePointerId = event.getPointerId(newPointerIndex);
                }
                break;
        }

        return true;
    }

    private void firePhotoEditorSDKListener(View view, boolean isStart) {
        if (view instanceof TextView) {
            if (onMultiTouchListener != null) {
                if (onPhotoEditorSDKListener != null) {
                    if (isStart)
                        onPhotoEditorSDKListener.onStartViewChangeListener(ViewType.TEXT);
                    else
                        onPhotoEditorSDKListener.onStopViewChangeListener(ViewType.TEXT);
                }
            } else {
                if (onPhotoEditorSDKListener != null) {
                    if (isStart)
                        onPhotoEditorSDKListener.onStartViewChangeListener(ViewType.EMOJI);
                    else
                        onPhotoEditorSDKListener.onStopViewChangeListener(ViewType.EMOJI);
                }
            }
        } else {
            if (onPhotoEditorSDKListener != null) {
                if (isStart)
                    onPhotoEditorSDKListener.onStartViewChangeListener(ViewType.IMAGE);
                else
                    onPhotoEditorSDKListener.onStopViewChangeListener(ViewType.IMAGE);
            }
        }
    }

    private boolean isViewInBounds(View view, int x, int y) {
        view.getDrawingRect(outRect);
        view.getLocationOnScreen(location);
        outRect.offset(location[0], location[1]);
        return outRect.contains(x, y);
    }

    public void setOnMultiTouchListener(OnMultiTouchListener onMultiTouchListener) {
        this.onMultiTouchListener = onMultiTouchListener;
    }

    private class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        private float mPivotX;
        private float mPivotY;
        private Vector2D mPrevSpanVector = new Vector2D();

        @Override
        public boolean onScaleBegin(View view, ScaleGestureDetector detector) {
            mPivotX = detector.getFocusX();
            mPivotY = detector.getFocusY();
            mPrevSpanVector.set(detector.getCurrentSpanVector());
            return true;
        }

        @Override
        public boolean onScale(View view, ScaleGestureDetector detector) {
            TransformInfo info = new TransformInfo();
            info.deltaScale = isScaleEnabled ? detector.getScaleFactor() : 1.0f;
            info.deltaAngle = isRotateEnabled ? Vector2D.getAngle(mPrevSpanVector, detector.getCurrentSpanVector()) : 0.0f;
            info.deltaX = isTranslateEnabled ? detector.getFocusX() - mPivotX : 0.0f;
            info.deltaY = isTranslateEnabled ? detector.getFocusY() - mPivotY : 0.0f;
            info.pivotX = mPivotX;
            info.pivotY = mPivotY;
            info.maximumScale = maximumScale;
            move(view, info);
            return false;
        }
    }

    private class TransformInfo {
        float deltaX;
        float deltaY;
        float deltaScale;
        float deltaAngle;
        float pivotX;
        float pivotY;
        float minimumScale;
        float maximumScale;
    }

    interface OnMultiTouchListener {
        void onEditTextClickListener(TextView view);
        void onRemoveViewListener(View removedView);
    }
}