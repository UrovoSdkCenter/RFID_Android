package com.rfid.base.custom;

import android.text.InputFilter;
import android.text.Spanned;

public class HexInputFilter implements InputFilter {
    private static final String HEX_CHARS = "0123456789ABCDEFabcdef";

    @Override
    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned dest, int dstart, int dend) {
        StringBuilder filtered = new StringBuilder();
        for (int i = start; i < end; i++) {
            char c = source.charAt(i);
            if (HEX_CHARS.indexOf(c) >= 0) {
                filtered.append(c);
            }
        }
        if (filtered.length() == end - start) {
            return null;
        }
        return filtered.toString();
    }
}