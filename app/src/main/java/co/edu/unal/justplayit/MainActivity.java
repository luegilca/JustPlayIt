package co.edu.unal.justplayit;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

public class MainActivity extends AppCompatActivity implements
        RecognitionListener {

    /* Named searches allow to quickly reconfigure the decoder */
    private static final String KWS_SEARCH = "wakeup";
    private static final String MEDIAPLAYER_SEARCH = "player";

    private static final String PLEASE_PLAY = "please play";
    private static final String PLEASE_PAUSE = "please pause";
    private static final String PLEASE_NEXT = "please next";
    private static final String PLEASE_PREVIOUS = "please previous";
    private static final String PLEASE_STOP = "please stop";
    private static final String PLEASE_REPEAT = "please repeat";

    /* Keyword we are looking for to activate menu */
    private final String KEYPHRASE = "just wakeup";

    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    public final HashSet<String> menuOptions = new HashSet<>();
    private SpeechRecognizer recognizer;
    private HashMap<String, Integer> captions;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick( View v )
            {
                AlertDialog alertdialog = new AlertDialog.Builder(MainActivity.this).create();
                alertdialog.setTitle(getString(R.string.about_us));
                alertdialog.setMessage("Luis Ernesto Gil Castellanos\nJuan Camilo Rubio Ávila\n\n2017");

                alertdialog.show();
            }
        });

        menuOptions.add(getString(R.string.play));
        menuOptions.add(getString(R.string.pause));
        menuOptions.add(getString(R.string.next));
        menuOptions.add(getString(R.string.prev));
        menuOptions.add(getString(R.string.stop));

        // Prepare the data for UI
        captions = new HashMap<>();
        captions.put(KWS_SEARCH, R.string.kws_caption );
        captions.put(MEDIAPLAYER_SEARCH, R.string.player_caption );


        // Check if user has given permission to record audio
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }

        NestedScrollView vi = (NestedScrollView) findViewById(R.id.content_shared);
        LinearLayout li = (LinearLayout) vi.findViewById(R.id.linear_container);
        TextView tv = ((TextView) li.findViewById(R.id.textView));
        tv.setText(getString(R.string.prepare));

        runRecognizerSetup();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void runRecognizerSetup() {
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(MainActivity.this);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    NestedScrollView vi = (NestedScrollView) findViewById(R.id.content_shared);
                    LinearLayout li = (LinearLayout) vi.findViewById(R.id.linear_container);
                    TextView tv = ((TextView) li.findViewById(R.id.textView));
                    String s = getString(R.string.failed) + result;
                    tv.setText(s);
                } else {
                    switchSearch(KWS_SEARCH);
                }
            }
        }.execute();
    }

    @Override
    @NonNull
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                runRecognizerSetup();
            } else {
                finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }

    /**
     * In partial result we get quick updates about current hypothesis. In
     * keyword spotting mode we can react here, in other modes we need to wait
     * for final result in onResult.
     */
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE))
        {
            MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.served);
            mp.start();
            switchSearch(MEDIAPLAYER_SEARCH);
        }
    }

    private void playPlayMusic( )
    {
        Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY));
        sendOrderedBroadcast(i, null);

        i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY));
        sendOrderedBroadcast(i, null);
    }

    private void playStopMusic( )
    {
        Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_STOP));
        sendOrderedBroadcast(i, null);

        i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_STOP));
        sendOrderedBroadcast(i, null);
    }

    private void playNextMusic( )
    {
        Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT));
        sendOrderedBroadcast(i, null);

        i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT));
        sendOrderedBroadcast(i, null);

        playPlayMusic();
    }

    private void playPreviousMusic( )
    {
        Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS));
        sendOrderedBroadcast(i, null);

        i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS));
        sendOrderedBroadcast(i, null);

        playPlayMusic();
    }

    private void playPauseMusic( )
    {
        Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE));
        sendOrderedBroadcast(i, null);

        i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE));
        sendOrderedBroadcast(i, null);
    }

    /**
     * This callback is called when we stop the recognizer.
     */
    @Override
    public void onResult(Hypothesis hypothesis) {
        //Toast.makeText(getApplicationContext(), getString(R.string.im_listening), Toast.LENGTH_SHORT).show();
        if (hypothesis != null) {
            String text = hypothesis.getHypstr();

            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();

            switch (text){
                case PLEASE_NEXT:
                    playNextMusic();
                    break;
                case PLEASE_PAUSE:
                    playPauseMusic();
                    break;
                case PLEASE_STOP:
                    playStopMusic();
                    break;
                case PLEASE_PLAY:
                    playPlayMusic();
                    break;
                case PLEASE_PREVIOUS:
                    playPreviousMusic();
                    break;
                default:
                    MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.ohboy);
                    mp.start();
                    break;
            }
        }
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    /**
     * We stop recognizer here to get a final result
     */
    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(KWS_SEARCH))
            switchSearch(MEDIAPLAYER_SEARCH);
        else
            switchSearch(KWS_SEARCH);
    }

    private void switchSearch(String searchName) {
        recognizer.stop();

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(KWS_SEARCH))
            recognizer.startListening(searchName);
        else
            recognizer.startListening(searchName, 1500);

        String caption = getResources().getString(captions.get(searchName));

        NestedScrollView vi = (NestedScrollView) findViewById(R.id.content_shared);
        LinearLayout li = (LinearLayout) vi.findViewById(R.id.linear_container);
        TextView tv = ((TextView) li.findViewById(R.id.textView));

        if( searchName.equals( KWS_SEARCH ) )
        {
            tv.setText(caption + "\n\n" + getString(R.string.wakeup));
        }
        else if( searchName.equals( MEDIAPLAYER_SEARCH ) )
        {
            StringBuilder sb = new StringBuilder();
            sb.append(caption);
            sb.append("\n");
            for( String s : menuOptions )
            {
                sb.append("\n");
                sb.append(s);
            }
            tv.setText(sb.toString());
        }
    }

    private void setupRecognizer(File assetsDir) throws IOException
    {
        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))
                .getRecognizer();
        recognizer.addListener(this);

        /** In your |plication you might not need to add all those searches.
         * They are added here for demonstration. You can leave just one.
         */

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

        File mpGrammar = new File(assetsDir, "player.gram");
        recognizer.addGrammarSearch(MEDIAPLAYER_SEARCH, mpGrammar);
    }

    @Override
    public void onError(Exception error) {
        ((TextView) findViewById(R.id.caption_text)).setText(error.getMessage());
    }

    @Override
    public void onTimeout() {
        switchSearch(MEDIAPLAYER_SEARCH);
    }

}
