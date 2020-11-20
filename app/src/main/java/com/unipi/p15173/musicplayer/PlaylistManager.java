package com.unipi.p15173.musicplayer;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;

public class PlaylistManager extends AppCompatActivity {
    Spinner spinner;
    SharedPreferences preferences;
    RecyclerView playlistRecyclerView;
    ArrayList<Audio> songList;
    RecyclerView_Adapter adapter;

    //Initializing variables
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_manager);

        spinner = findViewById(R.id.spinner4);
        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        preferences = this.getSharedPreferences("STORAGE", Context.MODE_PRIVATE);
        playlistRecyclerView = findViewById(R.id.playlistrecyclerview);

        songList = new ArrayList<>();

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        loadAudio();
    }


    //initializing recycler
    private void initRecyclerView() {
        if (!songList.isEmpty()) {
            if (songList.size() > 0) {
                playlistRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                adapter = new RecyclerView_Adapter(songList, getApplication());
                adapter.notifyDataSetChanged();
                playlistRecyclerView.setAdapter(adapter);
                playlistRecyclerView.addOnItemTouchListener(new CustomTouchListener(this, new onItemClickListener() {
                    @Override
                    public void onClick(View view, int index) {
                        //Add the clicked song to the selected (from spinner) playlist
                        SharedPreferences.Editor editor = preferences.edit();
                        String temp = preferences.getString(spinner.getSelectedItem().toString(), "");
                        if (temp.isEmpty())
                            temp = songList.get(index).getTitle();
                        else
                            temp += "," + songList.get(index).getTitle();

                        editor.putString(spinner.getSelectedItem().toString(), temp);
                        temp = "";
                        editor.apply();


                        Toast.makeText(PlaylistManager.this, "Song Added To Playlist", Toast.LENGTH_SHORT).show();
                    }
                }));


            }
        }
    }


    //load the audio tracks from the media directory
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


                    // Save to audioList
                    songList.add(new Audio(data, title, album, artist));
                }
            }
            if (cursor != null) {
                cursor.close();
            }

            initRecyclerView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
