package com.example.david.androidbasicsinventoryapp;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.david.androidbasicsinventoryapp.data.ProductContract;
import com.example.david.androidbasicsinventoryapp.data.ProductContract.ProductEntry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

public class EditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private Uri mCurrentProductUri;
    private EditText mNameEditText;
    private ImageView mImageView;
    private EditText mPriceEditText;
    private EditText mQuantityEditText;
    private boolean mProductHasChanged = false;
    private Uri mCurrentPhotoPath;
    private String imageUri;

    private static final String LOG_TAG = EditorActivity.class.getSimpleName();
    private static final int EXISTING_PRODUCT_LOADER = 0;
    public static final int PICK_IMAGE = 1;


    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP)
                view.performClick();
            mProductHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        if (mCurrentProductUri == null) {
            setTitle(getString(R.string.editor_activity_title_new_product));
            hideDeleteButton();
            hideOrderButton();
        } else {
            setTitle(getString(R.string.editor_activity_title_edit_product));
            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
            wireUpOrderButton();
            wireUpDeleteButton();
        }

        wireUpAddImageButton();
        wireUpIncreaseButton();
        wireUpDecreaseButton();

        mNameEditText = findViewById(R.id.edit_product_name);
        mPriceEditText = findViewById(R.id.edit_product_price);
        mPriceEditText.addTextChangedListener(new MoneyTextWatcher(mPriceEditText));
        mQuantityEditText = findViewById(R.id.edit_quantity);
        mImageView = findViewById(R.id.imageview_add_product);

        mNameEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mImageView.setOnTouchListener(mTouchListener);
    }


    // intent code sourced from https://stackoverflow.com/questions/5309190/android-pick-images-from-gallery
    // answer https://stackoverflow.com/a/5309217/59996
    // updated with some code from https://github.com/crlsndrsjmnz/MyShareImageExample with was based on code at https://developer.android.com/training/camera/index.html
    private void wireUpAddImageButton() {
        Button addImageButton = findViewById(R.id.add_product_image_button);
        addImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickIntent.setType("image/* video/*");
                startActivityForResult(pickIntent, PICK_IMAGE);
            }
        });
    }

    private void wireUpDeleteButton() {
        Button deleteButton = findViewById(R.id.delete_button);
        deleteButton.setVisibility(View.VISIBLE);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteConfirmationDialog();
            }
        });
    }

    private void hideDeleteButton() {
        Button deleteButton = findViewById(R.id.delete_button);
        deleteButton.setVisibility(View.GONE);
    }

    private void wireUpOrderButton() {
        Button orderButton = findViewById(R.id.order_button);
        orderButton.setVisibility(View.VISIBLE);
        orderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                composeEmail(getString(R.string.order_subject), String.format(getString(R.string.order_body), mNameEditText.getText().toString()));
            }
        });
    }


    private void hideOrderButton() {
        Button deleteButton = findViewById(R.id.order_button);
        deleteButton.setVisibility(View.GONE);
    }

    private void wireUpIncreaseButton() {
        Button increaseButton = findViewById(R.id.increase_button);
        increaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Integer quantity;

                try {
                    String quantityString = mQuantityEditText.getText().toString().trim();
                    quantity = Integer.parseInt(quantityString);
                    if (quantity + 1 > Integer.MAX_VALUE) {
                        return;
                    }
                } catch (NumberFormatException e) {
                    return;
                }

                quantity++;
                mQuantityEditText.setText(quantity.toString());
            }
        });
    }

    private void wireUpDecreaseButton() {
        Button decreaseButton = findViewById(R.id.decrease_button);
        decreaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Integer quantity;

                try {
                    String quantityString = mQuantityEditText.getText().toString().trim();
                    quantity = Integer.parseInt(quantityString);
                    if (quantity <= 0) {
                        return;
                    }
                } catch (NumberFormatException e) {
                    return;
                }

                quantity--;
                mQuantityEditText.setText(quantity.toString());
            }
        });
    }


    // intent code sourced from https://stackoverflow.com/questions/5309190/android-pick-images-from-gallery
    // answer https://stackoverflow.com/a/5309217/59996
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                mCurrentPhotoPath = uri;
                imageUri = uri.toString();
                setPic();
            }
        }
    }

    // Sourced from https://developer.android.com/training/camera/photobasics.html#TaskScalePhoto
    // modified using the example at https://github.com/crlsndrsjmnz/MyShareImageExample as a reference
    private void setPic() {
        // Get the dimensions of the View
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

        if (targetH == 0 || targetW == 0) {
            Log.d(LOG_TAG, "Invalid view dimensions");
            return;
        }

        Log.d(LOG_TAG, "setPic uri " + mCurrentPhotoPath.toString());

        InputStream input = null;
        try {
            input = this.getContentResolver().openInputStream(mCurrentPhotoPath);

            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();

            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // Determine how much to scale down the image
            int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            input = this.getContentResolver().openInputStream(mCurrentPhotoPath);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();
            mImageView.setImageBitmap(bitmap);

        } catch (FileNotFoundException fne) {
            Log.e(LOG_TAG, "Failed to load image.", fne);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to load image.", e);
        } finally {
            try {
                input.close();
            } catch (IOException ioe) {
                Log.e(LOG_TAG, "unhandled IOException", ioe);
            }
        }
    }


    private boolean saveProduct() {

        if (imageUri == null || imageUri.length() == 0) {
            Toast.makeText(this, R.string.invalid_please_select_image, Toast.LENGTH_SHORT).show();
            return false;
        }

        String nameString = mNameEditText.getText().toString().trim();

        if (nameString.length() == 0) {
            Toast.makeText(this, R.string.invalid_please_choose_name, Toast.LENGTH_SHORT).show();
            return false;
        }

        Integer quantity;

        try {
            String quantityString = mQuantityEditText.getText().toString().trim();
            quantity = Integer.parseInt(quantityString);
            if (quantity < 0) {
                Toast.makeText(this, R.string.invalid_please_select_quantity, Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.invalid_please_select_quantity, Toast.LENGTH_SHORT).show();
            return false;
        }

        Integer price;

        try {
            String priceString = mPriceEditText.getText().toString().trim();
            BigDecimal priceDecimal = MoneyTextWatcher.getDecimal(priceString);
            price = priceDecimal.multiply(new BigDecimal(100)).intValue();

            if (price < 0) {
                Toast.makeText(this, R.string.invalid_please_select_price, Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.invalid_please_select_price, Toast.LENGTH_SHORT).show();
            return false;
        }


        ContentValues values = new ContentValues();
        values.put(ProductContract.ProductEntry.COLUMN_PRODUCT_NAME, nameString);
        Log.d(LOG_TAG, "image url is " + imageUri);
        values.put(ProductContract.ProductEntry.COLUMN_PRODUCT_IMAGE, imageUri);
        values.put(ProductContract.ProductEntry.COLUMN_PRODUCT_QUANTITY, quantity);
        values.put(ProductContract.ProductEntry.COLUMN_PRODUCT_PRICE, price);

        if (mCurrentProductUri == null) {
            Uri newUri = getContentResolver().insert(ProductContract.ProductEntry.CONTENT_URI, values);
            if (newUri == null) {
                Toast.makeText(this, getString(R.string.editor_insert_product_failed), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.editor_insert_product_successful), Toast.LENGTH_SHORT).show();
            }
        } else {
            int rowsAffected = getContentResolver().update(mCurrentProductUri, values, null, null);
            if (rowsAffected == 0) {
                Toast.makeText(this, getString(R.string.editor_update_product_failed), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.editor_update_product_successful), Toast.LENGTH_SHORT).show();
            }
        }

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                if (saveProduct()) {
                    finish();
                    return true;
                }
                return false;
            case android.R.id.home:
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }

        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection = {
                ProductEntry._ID,
                ProductContract.ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_IMAGE,
                ProductContract.ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductContract.ProductEntry.COLUMN_PRODUCT_PRICE};

        return new CursorLoader(this,
                mCurrentProductUri,
                projection,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        if (cursor.moveToFirst()) {
            int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
            int imageColumnIndex = cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_PRODUCT_IMAGE);
            int quantityColumnIndex = cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_PRODUCT_QUANTITY);
            int weightColumnIndex = cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_PRODUCT_PRICE);

            String name = cursor.getString(nameColumnIndex);
            imageUri = cursor.getString(imageColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            int weight = cursor.getInt(weightColumnIndex);

            mNameEditText.setText(name);
            mPriceEditText.setText(Integer.toString(weight));
            mQuantityEditText.setText(Integer.toString(quantity));
            mCurrentPhotoPath = Uri.parse(imageUri);
            setPic();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mNameEditText.setText("");
        mPriceEditText.setText("");
        mQuantityEditText.setText("");
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void deleteProduct() {
        if (mCurrentProductUri != null) {
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);
            if (rowsDeleted == 0) {
                Toast.makeText(this, getString(R.string.editor_delete_product_failed), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.editor_delete_product_successful), Toast.LENGTH_SHORT).show();
            }
        }

        finish();
    }


    // code adapted from Udacity's JustJava example
    public void composeEmail(String subject, String text) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }
}