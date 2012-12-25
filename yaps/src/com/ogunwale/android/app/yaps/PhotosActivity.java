package com.ogunwale.android.app.yaps;

import java.util.Locale;

import com.google.api.services.picasa.model.AlbumEntry;
import com.google.api.services.picasa.model.UserFeed;

import android.os.Bundle;
import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.view.ActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

/**
 * Activity display photos based on a selected source (Picasa or Facebook) and
 * sort type (Albums, Locations, ...)
 *
 * @author ogunwale
 *
 */
public class PhotosActivity extends Activity implements LoaderCallbacks<Cursor> {

    /**
     * Intents extras and actions consumed by this activity when it starts.
     *
     * @author ogunwale
     *
     */
    public static class Extras {
        public static final String INTENT_PREFIX = "com.ogunwale.android.apps.yaps.";

        /**
         * Activity starts with the photo source set to Picasa
         */
        public static final String ACTION_SET_PHOTO_SOURCE_PICASA = INTENT_PREFIX + "ACTION_SET_PHOTO_SOURCE_PICASA";

        /**
         * Activity starts with the photo source set to Facebook
         */
        public static final String ACTION_SET_PHOTO_SOURCE_FACEBOOK = INTENT_PREFIX + "ACTION_SET_PHOTO_SOURCE_FACEBOOK";
    }

    private static PhotosSourceEnum mSourceSelection = PhotosSourceEnum.FACEBOOK;

    private SimpleCursorAdapter mAdapter;

    private GridView mGridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        if (intent != null) {
            String action = intent.getAction();

            if (Extras.ACTION_SET_PHOTO_SOURCE_PICASA.equals(action))
                mSourceSelection = PhotosSourceEnum.PICASA;
            else if (Extras.ACTION_SET_PHOTO_SOURCE_FACEBOOK.equals(action))
                mSourceSelection = PhotosSourceEnum.FACEBOOK;
        }

        setContentView(R.layout.activity_photos);

        // Set-up cursor adapter
        String[] from = new String[] { PhotosProvider.AlbumTable.COLUMN_NAME_COVER_URL, PhotosProvider.AlbumTable.COLUMN_NAME_TITLE,
                PhotosProvider.AlbumTable.COLUMN_NAME_PHOTOS_COUNT };
        int[] to = new int[] { R.id.thumbnail_image, R.id.thumbnail_description, R.id.thumbnail_count };
        mAdapter = new SimpleCursorAdapter(this, R.layout.layout_photo_thumbnail, null, from, to, 0);

        // Set-up thumbnail grid
        mGridView = (GridView) findViewById(R.id.photo_gridview);
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                // TODO
            }
        });

        // Request/update album data from source
        updateAlbumData();

        // Prepare the database loader.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.action_bar_sort_photos_providers, menu);
        return true;
    }

    /**
     * Updates album data
     */
    private void updateAlbumData() {
        switch (mSourceSelection) {
        case FACEBOOK:
            break;
        case INVALID:
            break;
        case PICASA: {
            new PicasaDataTimerTask(this, false, new PicasaDataAlbumListener() {
                @Override
                public void RequestFailed(FailureCause cause) {
                    // TODO
                }

                @Override
                public void RequestComplete() {
                    // TODO
                }

                @Override
                public void userFeed(UserFeed feed) {
                }

                @Override
                public void albumEntry(AlbumEntry album) {
                    PhotosProviderAccess.Album.updateIfChanged(getContentResolver(), album);
                }
            });
            break;
        }
        default:
            break;

        }
    }

    /**
     * Class implements action provider used to display photo source and sort
     * selection in the action bar.
     *
     * @author ogunwale
     *
     */
    public static class PhotosActionProvider extends ActionProvider {

        private final Context mContext;

        public PhotosActionProvider(Context context) {
            super(context);
            mContext = context;
        }

        @Override
        public View onCreateActionView() {
            LayoutInflater li = LayoutInflater.from(mContext);
            View view = li.inflate(R.layout.action_bar_sort_photos_providers, null);

            // Set-up source selection spinner
            Spinner sourceSpinner = (Spinner) view.findViewById(R.id.photo_source);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(mContext, R.array.sources,
                    android.R.layout.simple_spinner_dropdown_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sourceSpinner.setAdapter(adapter);
            sourceSpinner.setSelection(mSourceSelection.getValue());
            sourceSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    // TODO Auto-generated method stub
                    if (position != mSourceSelection.getValue()) {
                        // changeSource(PhotosSourceEnum.getEnum(position));
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            // Set-up sort selection spinner
            Spinner sortSpinner = (Spinner) view.findViewById(R.id.photo_sort);
            adapter = ArrayAdapter.createFromResource(mContext, R.array.sort_photos_by, android.R.layout.simple_spinner_dropdown_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sortSpinner.setAdapter(adapter);
            sortSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            return view;
        }
    }

    /**
     * Change photo srouce
     */
    private void changeSource(PhotosSourceEnum source) {
        mSourceSelection = source;
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created. We
        // only has one Loader, so we don't care about the ID.
        String[] projection = new String[] { PhotosProvider.AlbumTable._ID, PhotosProvider.AlbumTable.COLUMN_NAME_COVER_URL,
                PhotosProvider.AlbumTable.COLUMN_NAME_TITLE, PhotosProvider.AlbumTable.COLUMN_NAME_PHOTOS_COUNT };
        String selection = String.format(Locale.getDefault(), "%s=?", PhotosProvider.AlbumTable.COLUMN_NAME_SOURCE);
        String[] selectionArgs = new String[] { String.valueOf(mSourceSelection.getValue()) };

        // Create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(this, PhotosProvider.AlbumTable.CONTENT_URI, projection, selection, selectionArgs, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);

        // The list should now be shown.
        // if (isResumed()) {
        // setListShown(true);
        // } else {
        // setListShownNoAnimation(true);
        // }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no longer
        // using it.
        mAdapter.swapCursor(null);
    }
}
