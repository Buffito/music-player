package com.unipi.p15173.musicplayer;

import android.Manifest;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements MediaController.MediaPlayerControl {

    static boolean granted = false;
    int PERMISSION_ALL = 1;
    boolean serviceBound = false;
    ArrayList<Audio> audioList;
    ImageView collapsingImageView;
    int imageIndex = 0;
    RecyclerView recyclerView;
    RecyclerView_Adapter adapter;
    SharedPreferences preferences;
    private MusicService player;
    private MediaController controller;
    private SearchView searchView;
    //Binding this Client to the AudioPlayer Service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    //checks whether the necessary permissions have been granted
    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                    granted = true;

                    return true;
                }
            }
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //initializing variables
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        while (!granted) {
            askForPermissions();
        }

        Init();
    }

    //initializing variables & listeners
    void Init() {
        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        collapsingImageView = findViewById(R.id.collapsingImageView);
        recyclerView = findViewById(R.id.recyclerview);
        audioList = new ArrayList<>();
        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        preferences = this.getSharedPreferences("STORAGE", Context.MODE_PRIVATE);

        loadCollapsingImage(imageIndex);
        loadAudio();

    }

    //initializing the recycler view
    private void initRecyclerView() {
        if (audioList.size() > 0) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new RecyclerView_Adapter(audioList, getApplication());
            adapter.notifyDataSetChanged();
            recyclerView.setAdapter(adapter);
            recyclerView.addOnItemTouchListener(new CustomTouchListener(this, new onItemClickListener() {
                @Override
                public void onClick(View view, int index) {
                    playAudio(index);
                    setController();
                    controller.show();
                }
            }));


        }
    }

    //load image (top banner image)
    private void loadCollapsingImage(int i) {
        TypedArray array = getResources().obtainTypedArray(R.array.images);
        collapsingImageView.setImageDrawable(array.getDrawable(i));
    }

    //saving orientation changes
    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putBoolean("serviceStatus", serviceBound);
    }

    //restoring orientation changes
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("serviceStatus");
    }

    //reload spinner on resume
    @Override
    protected void onResume() {
        super.onResume();
    }

    //play the selected audio
    public void playAudio(int audioIndex) {
        //Check is service is active
        if (!serviceBound) {
            //Store Serializable audioList to SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudio(audioList);
            storage.storeAudioIndex(audioIndex);

            Intent playerIntent = new Intent(this, MusicService.class);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            //Store the new audioIndex to SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudioIndex(audioIndex);

            //Service is active
            //Send a broadcast to the service -> PLAY_NEW_AUDIO
            Intent broadcastIntent = new Intent("Broadcast_PLAY_NEW_AUDIO");
            sendBroadcast(broadcastIntent);
            controller.show(0);

        }
    }

    //load all the audio tracks and apply necessary filters (Playlists)
    public void loadAudio() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            ContentResolver contentResolver = getContentResolver();

            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
            String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
            Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);

            if (cursor != null && cursor.getCount() > 0) {

                while (cursor.moveToNext()) {
                    String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                    String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                    String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                }
            }
            if (cursor != null) {
                cursor.close();
            }

            initRecyclerView();
        } else
            askForPermissions();
    }

    //stops the music on destroy (unbound service)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            //service is active
            player.stopSelf();
        }
    }

    //creating search menu item
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                adapter.getFilter().filter(query);
                return false;
            }
        });
        return true;
    }

    //call on item selected
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_search) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // close search view on back button pressed
        if (!searchView.isIconified()) {
            searchView.setIconified(true);
            return;
        }
        super.onBackPressed();
    }

    //asks for permissions
    public void askForPermissions() {
        String[] PERMISSIONS = {
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.MEDIA_CONTENT_CONTROL,
                android.Manifest.permission.READ_PHONE_STATE
        };

        if (hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }


        if (granted) {
            this.recreate();
        }
    }

    //setting media player controller
    private void setController() {
        controller = new MediaController(MainActivity.this, false);
        //set previous and next button listeners
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.skipToNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.skipToPrevious();
            }
        });
        //set and show
        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.recyclerview));
        controller.setEnabled(true);
    }

    @Override
    public void start() {
        player.playMedia();
        controller.show();
    }

    @Override
    public void pause() {
        player.pauseMedia();
    }

    @Override
    public int getDuration() {
        if (player != null && serviceBound && player.isPng())
            return player.getDur();
        else
            return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (player != null && serviceBound && player.isPng())
            return player.getPosn();
        else
            return 0;
    }

    @Override
    public void seekTo(int pos) {

    }

    @Override
    public boolean isPlaying() {
        if (player != null && serviceBound)
            return player.isPng();
        else
            return false;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return false;
    }

    @Override
    public boolean canSeekForward() {
        return false;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }


}
