package com.jansmoneymachine.musicplayer;

// Zusammenarbeit mit Michael Rotärmel

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static android.provider.Telephony.Mms.Part.FILENAME;


public class MainActivity extends AppCompatActivity {
    private int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 9999; // just a random ID
    private Toolbar myToolbar;
    private List<Song> songList;
    private MediaPlayer mediaPlayer;
    private TextView txt_Artist;
    private TextView txt_Title;
    private Button btn_Play;
    private int position;
    private SharedPreferences sharedPrefs;
    private SharedPreferences.Editor sharedPrefsEditor;

    // Had to initiated here, so it can be changed later and works from the beginning
    private String selection = MediaStore.Audio.Media.DATA + " like ? ";
    private String[] argumentsSelection = new String[]{"%/storage/emulated/0/%"};

    // Shuffle
    private Button btn_shuffle;
    private boolean shuffleOn = true; // Shuffle is activated from the beginning
    private Random random;

    // Loop
    private Button btn_book;
    private boolean bookOn = false;

    // Replay
    private Button btn_replay;
    private boolean replayOn = false;

    // Mute
    private Button btn_mute;
    private boolean isMuted = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPrefs = getSharedPreferences(FILENAME, 0);

        // Ask for permission to read / write in external storage
        getExternalStoragePermission();

        // Gathers the MP3´s
        getMusic();

        // Initializing toolbar
        myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        // Elements
        btn_Play = (Button) findViewById(R.id.button_play);
        btn_shuffle = (Button) findViewById(R.id.button_shuffle);
        btn_shuffle.setBackgroundResource(R.drawable.ic_shuffle_activated); // Shuffle is activated from the beginning
        btn_book = (Button) findViewById(R.id.button_book);
        btn_replay = (Button) findViewById(R.id.button_replay);
        btn_mute = (Button) findViewById(R.id.button_mute);
        txt_Title = (TextView) findViewById(R.id.txt_Title_Input);
        txt_Title.setText(sharedPrefs.getString("titleSaved","---"));
        txt_Artist = (TextView) findViewById(R.id.txt_Artist_Input);
        txt_Artist.setText(sharedPrefs.getString("artistSaved","---"));
        position = sharedPrefs.getInt("positionSaved", 0);
        // End of Elements
    }


    // Play button
    public void play(View view) {
        if (musicExists() == true) {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, Uri.parse(songList.get(position).getPath()));
                setOnCompletionListener(view);
            }
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                setInfo();
                btn_Play.setBackgroundResource(R.drawable.ic_pause);
            } else {
                mediaPlayer.pause();
                btn_Play.setBackgroundResource(R.drawable.ic_play_arrow);
            }
        }
    }


    // Next song button
    public void nextSong(View view) {
        if (musicExists() == true) {
            stopPlayer();
            if (shuffleOn == true) {
                random = new Random();
                position = random.nextInt(songList.size());
                mediaPlayer = MediaPlayer.create(this, Uri.parse(songList.get(position).getPath()));
                startPlayer(findViewById(android.R.id.content), mediaPlayer);
                return;
            } else if (position == songList.size() - 1) {
                position = 0;
            } else {
                position++;
            }
            mediaPlayer = MediaPlayer.create(this, Uri.parse(songList.get(position).getPath()));
            startPlayer(findViewById(android.R.id.content), mediaPlayer);
        }
    }

    // Previous song button
    public void previousSong(View view) {
        if (musicExists() == true) {
            stopPlayer();
            if (position == 0) {
                position = songList.size() - 1;
            } else {
                position--;
            }
            mediaPlayer = MediaPlayer.create(this, Uri.parse(songList.get(position).getPath()));
            startPlayer(findViewById(android.R.id.content), mediaPlayer);
            /*
            setOnCompletionListener(view);
            mediaPlayer.start();
            setInfo();
            */
        }
    }

    // Replay button
    private void replaySong(View view) {
        mediaPlayer.stop();
        mediaPlayer = MediaPlayer.create(this, Uri.parse(songList.get(position).getPath()));
        startPlayer(findViewById(android.R.id.content), mediaPlayer);
    }


    // Shuffle button
    public void shuffle(View view) {
        if (shuffleOn == false) {
            shuffleOn = true;
            btn_shuffle.setBackgroundResource(R.drawable.ic_shuffle_activated);
        } else {
            shuffleOn = false;
            btn_shuffle.setBackgroundResource(R.drawable.ic_shuffle);
        }
    }


    // Loop button // "Hörbuch-Funktion"
    // Does make it possible to only listen until the end of one song
    // Stops playing after the playing song is finished
    public void book(View view) {
        if (bookOn == false) {
            bookOn = true;
            btn_book.setBackgroundResource(R.drawable.ic_book_activated);
        } else {
            bookOn = false;
            btn_book.setBackgroundResource(R.drawable.ic_book);
        }
    }


    // Replay button
    public void replay(View view) {
        if (replayOn == false) {
            replayOn = true;
            btn_replay.setBackgroundResource(R.drawable.ic_replay_activated);
        } else {
            replayOn = false;
            btn_replay.setBackgroundResource(R.drawable.ic_replay);
        }
    }


    // Mute button
    public void mute(View view) {
        if (mediaPlayer.isPlaying()) {
            //
        } else if (isMuted == false) {
            isMuted = true;
            mediaPlayer.setVolume(0, 0);
            btn_mute.setBackgroundResource(R.drawable.ic_volume_mute);
        } else {
            isMuted = false;
            mediaPlayer.setVolume(1, 1);
            btn_mute.setBackgroundResource(R.drawable.ic_volume_up);
        }
    }


    // Release mediaPlayer
    public void stopPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    // Starts mediaPlayer, sets the OnCompleteListener, sets info
    public void startPlayer(View view, MediaPlayer mediaplayer) {
        mediaplayer.start();
        setOnCompletionListener(view);
        setInfo();
    }


    // Set info for title and artist
    public void setInfo() {
        if (songList.get(position).getTitle().equals(null)) {
            txt_Title.setText("unknown");
        } else {
            txt_Title.setText(songList.get(position).getTitle());
        }

        if (songList.get(position).getArtist().equals(null)) {
            txt_Artist.setText("unknown");
        } else {
            txt_Artist.setText(songList.get(position).getArtist());
        }
    }


    // OnCompletionListener
    MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            if (replayOn == true) {
                replaySong(findViewById(android.R.id.content));
            } else if (bookOn == false) {
                nextSong(findViewById(android.R.id.content));
            }
        }
    };

    private void setOnCompletionListener(View view) {
        mediaPlayer.setOnCompletionListener(onCompletionListener);
    }


    // Menu
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_load_music:
                getMusic();
                return true;

            case R.id.item_select_path:
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                i.addCategory(Intent.CATEGORY_DEFAULT);
                startActivityForResult(Intent.createChooser(i, "Choose directory"), 999);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    //Check if music exists
    private boolean musicExists() {
        if (songList.isEmpty()) {
            Toast.makeText(this, "Unfortunately, there´s no music to play.", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            return true;
        }
    }


    // Gather the MP3´s
    private void getMusic() {
        String selection = this.selection;
        String[] selectionArguments = argumentsSelection;
        /*
        For a special path
        Cursor cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songProjection, MediaStore.Audio.Media.DATA + " like ? ", new String[]{"%/storage/emulated/0/Download/Music/%"}, null, null);
         */

        ContentResolver contentResolver = getContentResolver();
        songList = new ArrayList<>();

        String[] songProjection = new String[]{
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST};

        Cursor cursor = contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                songProjection,
                selection,
                selectionArguments,
                null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String currentPath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String currentTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String currentArtist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));

                songList.add(new Song(currentTitle, currentArtist, currentPath));
            } while (cursor.moveToNext());
        }
        cursor.close();
    }


    // Gets permission to the external storage
    private void getExternalStoragePermission() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Explain to the user why we need to read the contacts
            }
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
            // app-defined int constant that should be quite unique
            return;
        }
    }


    // Solution -- MENU Symbol didn't exist
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    // Provides directory path after the user has chosen a directory
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            switch (requestCode) {
                case 999:
                    String directory = data.getData().getLastPathSegment();
                    directory = directory.substring(4);  // Gets the start of the path / cuts "raw:"
                    argumentsSelection = new String[]{"%" + directory + "/%"};
                    getMusic();
                    break;
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        sharedPrefsEditor = getSharedPreferences(FILENAME, 0).edit();
        sharedPrefsEditor.putInt("positionSaved", position);
        sharedPrefsEditor.putString("titleSaved", songList.get(position).getTitle());
        sharedPrefsEditor.putString("artistSaved", songList.get(position).getArtist());
        sharedPrefsEditor.apply();
    }
}
