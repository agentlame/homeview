package com.monsterbutt.homeview.ui.playback;

import android.os.Bundle;
import android.view.View;

import com.monsterbutt.homeview.R;

public class ErrorFragment extends android.support.v17.leanback.app.ErrorFragment {

  public static final String MESSAGE = "message";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setTitle(getResources().getString(R.string.app_name));

    setImageDrawable(getActivity().getDrawable(R.drawable.lb_ic_sad_cloud));

    String error = getString(R.string.error);
    Bundle args = savedInstanceState != null ? savedInstanceState : getArguments();
    if (args != null)
      error = args.getString(MESSAGE);
    setMessage(error);
    setDefaultBackground(true);

    setButtonText(getResources().getString(R.string.dismiss_error));
    setButtonClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View arg0) {
        getFragmentManager().beginTransaction().remove(ErrorFragment.this).commit();
        //getActivity().finish();
      }
    });
  }
}
