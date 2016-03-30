package com.monsterbutt.homeview.ui.android;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.BaseCardView;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.monsterbutt.homeview.R;

public class CodecCardView extends BaseCardView {

    private ImageView mImageViewA;
    private ImageView mImageViewB;
    private TextView mTitleView;
    private boolean mAttachedToWindow;

    /**
     * Create an ImageCardView using a given theme for customization.
     *
     * @param context    The Context the view is running in, through which it can
     *                   access the current theme, resources, etc.
     * @param themeResId The resourceId of the theme you want to apply to the ImageCardView. The theme
     *                   includes attributes "imageCardViewStyle", "imageCardViewTitleStyle",
     *                   "imageCardViewContentStyle" etc. to customize individual part of ImageCardView.
     * @deprecated Calling this constructor inefficiently creates one ContextThemeWrapper per card,
     * you should share it in card Presenter: wrapper = new ContextThemeWrapper(context, themResId);
     * return new ImageCardView(wrapper);
     */
    @Deprecated
    public CodecCardView(Context context, int themeResId) {
        this(new ContextThemeWrapper(context, themeResId));
    }

    public CodecCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        buildImageCardView(attrs, defStyleAttr, android.support.v17.leanback.R.style.Widget_Leanback_ImageCardView);
    }

    private void buildImageCardView(AttributeSet attrs, int defStyleAttr, int defStyle) {
        // Make sure the ImagePlusCardView is focusable.
        setFocusable(true);
        setFocusableInTouchMode(true);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.lb_codec_card_view, this);

        mImageViewA = (ImageView) findViewById(R.id.imageA);
        mImageViewB = (ImageView) findViewById(R.id.imageB);
        mTitleView = (TextView) findViewById(R.id.main_text);
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
     * Enables or disables adjustment of view bounds on the main image.
     */
    public void setMainImageAdjustViewBounds(boolean adjustViewBounds) {
        if (mImageViewA != null) {
            mImageViewA.setAdjustViewBounds(adjustViewBounds);
        }
    }
    public void setSecondaryImageAdjustViewBounds(boolean adjustViewBounds) {
        if (mImageViewB != null) {
            mImageViewB.setAdjustViewBounds(adjustViewBounds);
        }
    }

    /**
     * Sets the ScaleType of the main image.
     */
    public void setMainImageScaleType(ImageView.ScaleType scaleType) {
        if (mImageViewA != null) {
            mImageViewA.setScaleType(scaleType);
        }
    }
    public void setSecondaryImageScaleType(ImageView.ScaleType scaleType) {
        if (mImageViewB != null) {
            mImageViewB.setScaleType(scaleType);
        }
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

    /**
     * Sets the layout dimensions of the ImageView.
     */
    public void setMainImageDimensions(int width, int height) {
        setImageDimensions(mImageViewA, width, height);
    }
    public void setSecondaryImageDimensions(int width, int height) {
        setImageDimensions(mImageViewB, width, height);
    }
    private  void setImageDimensions(ImageView image, int width, int height) {
        ViewGroup.LayoutParams lp = image.getLayoutParams();
        lp.width = width;
        lp.height = height;
        image.setLayoutParams(lp);
    }

    /**
     * Returns the ImageView drawable.
     */
    public Drawable getMainImage() {
        if (mImageViewA == null) {
            return null;
        }

        return mImageViewA.getDrawable();
    }

    public Drawable getSecondaryImage() {
        if (mImageViewB == null) {
            return null;
        }

        return mImageViewB.getDrawable();
    }

    /**
     * Sets the title text.
     */
    public void setTitleText(CharSequence text) {
        if (mTitleView == null) {
            return;
        }
        mTitleView.setText(text);
    }

    /**
     * Returns the title text.
     */
    public CharSequence getTitleText() {
        if (mTitleView == null) {
            return null;
        }

        return mTitleView.getText();
    }


    private void fadeIn(ImageView image) {
        image.setAlpha(0f);
        if (mAttachedToWindow) {
            image.animate().alpha(1f).setDuration(
                    image.getResources().getInteger(android.R.integer.config_shortAnimTime));
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

