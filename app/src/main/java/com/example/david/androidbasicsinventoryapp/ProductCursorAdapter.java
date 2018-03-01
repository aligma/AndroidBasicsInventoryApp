package com.example.david.androidbasicsinventoryapp;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.david.androidbasicsinventoryapp.data.ProductContract;
import com.example.david.androidbasicsinventoryapp.data.ProductContract.ProductEntry;

public class ProductCursorAdapter extends CursorAdapter {

    private static final String LOG_TAG = ProductCursorAdapter.class.getSimpleName();

    public ProductCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Inflate a list item view using the layout specified in list_item.xml
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    // https://www.developerfeed.com/how-to-convert-amount-in-cents-to-dollars-with-formatting/
    // didn't want to reinvent the wheel here, but did need to fix a bug in their code...
    private static String getFormattedAmount(int amount) {
        int cents = amount % 100;
        int dollars = (amount - cents) / 100;

        String camount;
        if (cents <= 9) {
            camount = "0" + cents;
        } else {
            camount = "" + cents;
        }

        return "$" + dollars + "." + camount;
    }

    @Override
    public void bindView(final View view, Context context, Cursor cursor) {

        final int id = cursor.getInt(cursor.getColumnIndex(ProductContract.ProductEntry._ID));

        TextView nameTextView = view.findViewById(R.id.name);
        TextView priceTextView = view.findViewById(R.id.price);
        TextView quantityTextView = view.findViewById(R.id.quantity);

        int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
        int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
        int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);

        final String productName = cursor.getString(nameColumnIndex);
        final int price = cursor.getInt(priceColumnIndex);
        final int quantity = cursor.getInt(quantityColumnIndex);

        nameTextView.setText(productName);
        priceTextView.setText(getFormattedAmount(price));
        quantityTextView.setText(String.format(context.getString(R.string.in_stock), Integer.toString(quantity)));

        TextView saleButton = view.findViewById(R.id.sale_button);
        saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, ProductEntry.COLUMN_PRODUCT_NAME + " " + productName);
                ContentResolver resolver = view.getContext().getContentResolver();
                ContentValues values = new ContentValues();
                Uri currentProductUri = ContentUris.withAppendedId(ProductContract.ProductEntry.CONTENT_URI, id);

                if (quantity > 0) {

                    values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantity - 1);
                    resolver.update(
                            currentProductUri,
                            values,
                            null,
                            null);

                    Toast.makeText(v.getContext(), R.string.sold_text, Toast.LENGTH_SHORT).show();

                }
                else
                {
                    Toast.makeText(v.getContext(), R.string.invalid_no_stock_available, Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
}
