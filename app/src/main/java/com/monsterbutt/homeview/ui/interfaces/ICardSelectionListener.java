package com.monsterbutt.homeview.ui.interfaces;


import android.os.Bundle;

import com.monsterbutt.homeview.ui.presenters.CardObject;

public interface ICardSelectionListener {

    void onCardSelected(CardObject card);
    Bundle getPlaySelectionBundle(boolean cardWasScene);
}
