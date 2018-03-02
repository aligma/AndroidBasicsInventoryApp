package com.example.david.androidbasicsinventoryapp;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.text.NumberFormat;

// https://stackoverflow.com/questions/5107901/better-way-to-format-currency-input-edittext
// answer https://stackoverflow.com/a/24621325/59996 by ToddH
// modifications to add reusable getDecimal and to trim the string before parsing
public class MoneyTextWatcher implements TextWatcher {
    private final WeakReference<EditText> editTextWeakReference;

    public MoneyTextWatcher(EditText editText) {
        editTextWeakReference = new WeakReference<EditText>(editText);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        EditText editText = editTextWeakReference.get();
        if (editText == null) return;
        String s = editable.toString();
        // don't allow typing of currency symbols for example $,€.
        s = s.replaceAll("[^\\d.]", "");
        if (s.isEmpty()) return;
        editText.removeTextChangedListener(this);
        String formatted = NumberFormat.getCurrencyInstance().format(getDecimal(s));
        editText.setText(formatted);
        editText.setSelection(formatted.length());
        editText.addTextChangedListener(this);
    }

    // refactored from the original example on stackoverflow
    public static BigDecimal getDecimal(String s)
    {
        String cleanString = s.trim().replaceAll("[$,.]", "");
        BigDecimal parsed = new BigDecimal(cleanString).setScale(2, BigDecimal.ROUND_FLOOR).divide(new BigDecimal(100), BigDecimal.ROUND_FLOOR);
        return parsed;
    }
}