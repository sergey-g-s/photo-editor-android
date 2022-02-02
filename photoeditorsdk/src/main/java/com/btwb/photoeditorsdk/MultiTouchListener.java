package com.btwb.photoeditorsdk;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Arrays;

class MultiTouchListener implements OnTouchListener {

    private static final int INVALID_POINTER_ID = -1;
    private static float renderedWidth;
    private static float renderedHeight;
    private boolean isRotateEnabled = true;
    private boolean isTranslateEnabled = true;
    private boolean isScaleEnabled = true;
    private float maximumScale = 10.0f;
    private int mActivePointerId = INVALID_POINTER_ID;
    private int mPrevX, mPrevY, mPrevRawX, mPrevRawY, mLeftX, mRightX, mBottomX, mTopX, mVertical, mHorizontal;
    private ScaleGestureDetector mScaleGestureDetector;

    private int[] location = new int[2];
    private int width, statusBar;
    private Rect outRect;
    private boolean editable;
    private boolean moveX = true;
    private boolean moveY = true;
    private View deleteView, leftLineView, rightLineView, bottomHorizontalLine, topHorizontalLine, verticalLine, horizontalLine;
    private String type;
    private ImageView photoEditImageView, stickerHorizontalLine;
    private ConstraintLayout parentView;
    private LinearLayout  activeView;

    private OnMultiTouchListener onMultiTouchListener;
    private OnPhotoEditorSDKListener onPhotoEditorSDKListener;

    MultiTouchListener(View deleteView,
                       ConstraintLayout parentView,
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
                       View horizontalLine,
                       ImageView stickerHorizontalLine
    ) {
        mScaleGestureDetector = new ScaleGestureDetector(new ScaleGestureListener());
        this.deleteView = deleteView;
        this.leftLineView = leftLineView;
        this.rightLineView = rightLineView;
        this.bottomHorizontalLine = bottomHorizontalLine;
        this.stickerHorizontalLine = stickerHorizontalLine;
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

    private void move(View view, TransformInfo info) {
//        computeRenderOffset(view, 100, 100);
        float rotation = adjustAngle(view.getRotation() + info.deltaAngle);
        adjustTranslation(view, info.deltaX, info.deltaY, true, true);

        float scale = view.getScaleX() * info.deltaScale;
        scale = Math.max(info.minimumScale, Math.min(info.maximumScale, scale));
        renderedWidth = view.getWidth() * scale;
        renderedHeight = view.getHeight() * scale;
        view.setScaleX(scale);
        view.setScaleY(scale);

        int[] coordinate = new int[2];
        view.getLocationInWindow(coordinate);
        int scaledY = coordinate[1] - 60;

        if(rotation <= 3 && rotation >= -3){
            view.setRotation(0);
            this.LineAnimation(this.stickerHorizontalLine, 0f, 60f);
            this.stickerHorizontalLine.setY(scaledY + (renderedHeight / 2));
        } else {
            view.setRotation(rotation);
            this.LineAnimation(this.stickerHorizontalLine, 60f, 0f);
        }



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

        renderedWidth = view.getWidth() * view.getScaleX();
        renderedHeight = view.getHeight() * view.getScaleX();


        switch (action & event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mLeftX = (int) this.leftLineView.getX();
                mRightX = (int) this.rightLineView.getX();
                mBottomX = (int) this.bottomHorizontalLine.getY();
                mTopX = (int) this.topHorizontalLine.getY();
                mVertical = (int) this.verticalLine.getX();
                mHorizontal = (int) this.horizontalLine.getY();
                mPrevX = (int) event.getX();
                mPrevY = (int) event.getY();
                mPrevRawX = (int) event.getRawX();
                mPrevRawY = (int) event.getRawY();
                mActivePointerId = event.getPointerId(0);
                deleteView.setVisibility(View.VISIBLE);
                activeView.setVisibility(View.GONE);
                view.bringToFront();
                firePhotoEditorSDKListener(view, true);
                break;
            case MotionEvent.ACTION_MOVE:
                int pointerIndexMove = event.findPointerIndex(mActivePointerId);
                if (pointerIndexMove != -1) {
                    int currX = (int) event.getX(pointerIndexMove);
                    int currY = (int) event.getY(pointerIndexMove);
                    float scaleWidth = renderedWidth == 0.0 ? view.getWidth() : renderedWidth;
                    float scaledHeight = renderedHeight == 0.0 ? view.getHeight() : renderedHeight;
                    int[] coordinate = new int[2];
                    view.getLocationInWindow(coordinate);
                    if (view.getScaleY() == 1.0){
                        statusBar = (int) (coordinate[1] - view.getY());
                    }
                    int scaledX = coordinate[0];
                    int scaledY = coordinate[1] - statusBar;
                    view.setPivotX(scaleWidth / 2);
                    view.setPivotY(scaledHeight / 2);


                    float[] src = {
                            scaledX,scaledY, // left, top
                            (scaledX + scaleWidth),scaledY, // right, top
                            (scaledX + scaleWidth),(scaledY + scaledHeight), // right, bottom
                            (scaledX),(scaledY + scaledHeight) // left, bottom
                    };
                    float[] dist = new float[8];
                    Matrix matrix = new Matrix();
                    matrix.preRotate(view.getRotation(), scaledX, scaledY);
                    matrix.mapPoints(dist,src);
                    float[] horizontal = new float[] { dist[0],dist[2],dist[4],dist[6] };
                    float[] vertical = new float[] { dist[1],dist[3],dist[5],dist[7] };
                    Arrays.sort(horizontal);
                    Arrays.sort(vertical);
                    int minLeft = (int) horizontal[0];
                    int maxRight = (int) horizontal[horizontal.length - 1];
                    int minTop = (int) vertical[0];
                    int maxBottom = (int) vertical[vertical.length - 1];
                    float verticalPoint = (minLeft +  ((maxRight - minLeft) / 2));
                    float horizontalPoint = (minTop +  ((maxBottom - minTop) / 2));


                    if(minLeft == (mLeftX + 4) && moveX){
                        this.LineAnimation(this.leftLineView, 0f, 100f);
                        moveX = false;
                    }else if (this.leftLineView.getAlpha() == 100.0 && moveX){
                        this.LineAnimation(this.leftLineView, 100f, 0f);
                    }

                    if(maxRight == (mRightX)  && moveX){
                        this.LineAnimation(this.rightLineView, 0f, 100f);
                        moveX = false;
                    }else if (this.rightLineView.getAlpha() == 100.0 && moveX){
                        this.LineAnimation(this.rightLineView, 100f, 0f);
                    }

                    if(maxBottom == mBottomX && moveY){
                        this.LineAnimation(this.bottomHorizontalLine, 0f, 100f);
                        moveY = false;
                    }else if (this.bottomHorizontalLine.getAlpha() == 100.0 && moveY){
                        this.LineAnimation(this.bottomHorizontalLine, 100f, 0f);
                    }

                    if(minTop == (mTopX + 4) && moveY){
                        this.LineAnimation(this.topHorizontalLine, 0f, 100f);
                        moveY = false;
                    }else if (this.topHorizontalLine.getAlpha() == 100.0 && moveY){
                        this.LineAnimation(this.topHorizontalLine, 100f, 0f);
                    }
//
                    if((verticalPoint > (mVertical - 2) && verticalPoint < (mVertical + 2)) && moveX){
                        this.LineAnimation(this.verticalLine, 0f, 100f);
                        moveX = false;
                    }else if (this.verticalLine.getAlpha() == 100.0 && moveX){
                        this.LineAnimation(this.verticalLine, 100f, 0f);
                    }
//
                    if((horizontalPoint >  (mHorizontal - 2) && horizontalPoint <  (mHorizontal + 2)) && moveY){
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
                this.LineAnimation(this.stickerHorizontalLine, 100f, 0f);
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
                    mPrevX = (int) event.getX(newPointerIndex);
                    mPrevY = (int) event.getY(newPointerIndex);
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