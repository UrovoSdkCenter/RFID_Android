package com.rfid.base.custom;

import android.view.View;
import android.widget.AdapterView;

public abstract class EnhancedItemSelectedListener  implements AdapterView.OnItemSelectedListener {

    private boolean isInitialized = false;

    public abstract void onItemSelected(int position);

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (!isInitialized) {
            isInitialized = true;
            return;
        }
        onItemSelected(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }
}