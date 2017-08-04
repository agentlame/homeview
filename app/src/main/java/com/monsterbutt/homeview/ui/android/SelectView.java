package com.monsterbutt.homeview.ui.android;

import android.app.Activity;
import android.app.Fragment;
import android.view.View;

import com.monsterbutt.homeview.R;

public abstract class SelectView {

  public interface SelectViewCaller {

    void selectionViewState(boolean isVisible);
  }

  protected final Activity activity;
  private Fragment fragment = null;
  private SelectViewCaller caller;
  private boolean isReleased = false;

  public SelectView(Activity activity) {

    this.activity = activity;
  }

  protected abstract String getTag();
  protected abstract int getHeight();

  protected void setFragment(Fragment fragment, SelectViewCaller caller) {

    this.fragment = fragment;
    this.caller = caller;
    if (fragment != null) {
      if (caller != null)
        caller.selectionViewState(true);
      View view = activity.findViewById(R.id.selection_fragment);
      view.setVisibility(View.VISIBLE);
      view.requestFocus();
      activity.getFragmentManager().beginTransaction().add(R.id.selection_fragment, fragment, getTag()).commit();
    }
    else
      release();
  }

  public void release() {

    synchronized (this) {
      if (isReleased)
        return;
      isReleased = true;
    }

    if (fragment != null && activity != null && !activity.isDestroyed() && !activity.isFinishing()) {
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          activity.getFragmentManager().beginTransaction().remove(fragment).commit();
          activity.findViewById(R.id.selection_fragment).setVisibility(View.INVISIBLE);
        }
      });
    }
    if (caller != null)
     caller.selectionViewState(false);
  }
}
