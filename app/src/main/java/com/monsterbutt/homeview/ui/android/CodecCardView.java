package com.monsterbutt.homeview.ui.android;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.BaseCardView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.monsterbutt.homeview.R;

public class CodecCardView extends BaseCardView {

    private ImageView mImageViewA;
    private ImageView mImageViewB;
    private TextView mTitleView;
    private TextView mSubtitleView;
    private TextView mDecoderView;
    private boolean mAttachedToWindow;
    private ImageView mTracksFlag;
    private TextView mTrackCount;

    public CodecCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // Make sure the ImagePlusCardView is focusable.
        setFocusable(true);
        setFocusableInTouchMode(true);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.lb_codec_card_view, this);

        mImageViewA = (ImageView) findViewById(R.id.imageA);
        mImageViewB = (ImageView) findViewById(R.id.imageB);
        mTitleView = (TextView) findViewById(R.id.main_text);
        mSubtitleView = (TextView) findViewById(R.id.sub_text);
        mDecoderView = (TextView) findViewById(R.id.decode_text);
        mTracksFlag = (ImageView) findViewById(R.id.flag_image);
        mTrackCount = (TextView) findViewById(R.id.flag_text);
    }

    public CodecCardView(Context context) {
        this(context, null);
    }

    public CodecCardView(Context context, AttributeSet attrs) {
        this(context, attrs, android.support.v17.leanback.R.attr.imageCardViewStyle);
    }

    /**
     * Returns the main image view.
     */
    public final ImageView getMainImageView() {
        return mImageViewA;
    }
    public final ImageView getSecondaryImageView() {
        return mImageViewB;
    }


    /**
     * Sets the image drawable with fade-in animation.
     */
    public void setMainImage(Drawable drawable) {
        setMainImage(drawable, true);
    }
    public void setSecondaryImage(Drawable drawable) {
        setSecondaryImage(drawable, true);
    }
    /**
     * Sets the image drawable with optional fade-in animation.
     */
    public void setMainImage(Drawable drawable, boolean fade) {
        setImage(mImageViewA, drawable, fade);
    }
    public void setSecondaryImage(Drawable drawable, boolean fade) {
        setImage(mImageViewB, drawable, fade);
    }
    private void setImage(ImageView image, Drawable drawable, boolean fade){
        if (image == null) {
            return;
        }

        image.setImageDrawable(drawable);
        if (drawable == null) {
            image.animate().cancel();
            image.setAlpha(1f);
            image.setVisibility(View.GONE);
        } else {
            image.setVisibility(View.VISIBLE);
            if (fade) {
                fadeIn(image);
            } else {
                image.animate().cancel();
                image.setAlpha(1f);
            }
        }
    }

    public void setTitleText(CharSequence text) {
        if (mTitleView == null)
            return;
        mTitleView.setText(text);
    }

    public void setSubtitleText(CharSequence text) {
        if (mSubtitleView == null)
            return;
        mSubtitleView.setText(text);
    }

    public void setDecoderText(CharSequence text) {
        if (mDecoderView == null)
            return;
        mDecoderView.setText(text);
    }

    private void fadeIn(ImageView image) {
        image.setAlpha(0f);
        if (mAttachedToWindow) {
            image.animate().alpha(1f).setDuration(
                    image.getResources().getInteger(android.R.integer.config_shortAnimTime));
        }
    }

    public void setFlag(Drawable drawable, String text) {

        if (drawable == null)
            mTracksFlag.setVisibility(View.INVISIBLE);
        else {
            mTracksFlag.setImageDrawable(drawable);
            mTracksFlag.setVisibility(View.VISIBLE);
        }
        if (TextUtils.isEmpty(text))
            mTrackCount.setVisibility(View.INVISIBLE);
        else {
            mTrackCount.setText(text);
            mTrackCount.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttachedToWindow = true;
        if (mImageViewA.getAlpha() == 0) {
            fadeIn(mImageViewA);
        }
        if (mImageViewB.getAlpha() == 0) {
            fadeIn(mImageViewB);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        mAttachedToWindow = false;
        mImageViewA.animate().cancel();
        mImageViewA.setAlpha(1f);
        mImageViewB.animate().cancel();
        mImageViewB.setAlpha(1f);
        super.onDetachedFromWindow();
    }
}

