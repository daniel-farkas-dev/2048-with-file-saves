package com.tpcstld.twozerogame;

import static androidx.core.content.ContextCompat.getSystemService;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.view.KeyEvent;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.tpcstld.twozerogame.snapshot.SnapshotData;
import com.tpcstld.twozerogame.snapshot.SnapshotManager;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static final String WIDTH = "width";
    public static final String HEIGHT = "height";
    public static final String SCORE = "score";
    public static final String HIGH_SCORE = "high score temp";
    public static final String UNDO_SCORE = "undo score";
    public static final String CAN_UNDO = "can undo";
    public static final String UNDO_GRID = "undo";
    public static final String GAME_STATE = "game state";
    public static final String UNDO_GAME_STATE = "undo game state";

    private static final String NO_LOGIN_PROMPT = "no_login_prompt";

    public static final String MOVE_COUNT = "number of moves";

    public static final String GAME_ID = "unique game identifier";

    private static final int RC_SIGN_IN = 9001;

    private boolean firstLoginAttempt = false;

    private MainView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Settings s = new Settings();
        s.load(this);
        view = new MainView(this, s);
        setContentView(view);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            //Do nothing
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            view.game.move(2);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            view.game.move(0);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            view.game.move(3);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            view.game.move(1);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("hasState", true);
        save();
    }

    private void save() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        Tile[][] field = view.game.grid.field;
        Tile[][] undoField = view.game.grid.undoField;
        editor.putInt(WIDTH, field.length);
        editor.putInt(HEIGHT, field[0].length);
        for (int xx = 0; xx < field.length; xx++) {
            for (int yy = 0; yy < field[0].length; yy++) {
                if (field[xx][yy] != null) {
                    editor.putInt(xx + " " + yy, field[xx][yy].getValue());
                } else {
                    editor.putInt(xx + " " + yy, 0);
                }

                if (undoField[xx][yy] != null) {
                    editor.putInt(UNDO_GRID + xx + " " + yy, undoField[xx][yy].getValue());
                } else {
                    editor.putInt(UNDO_GRID + xx + " " + yy, 0);
                }
            }
        }
        editor.putLong(SCORE, view.game.score);
        editor.putLong(HIGH_SCORE, view.game.highScore);
        editor.putLong(UNDO_SCORE, view.game.lastScore);
        editor.putBoolean(CAN_UNDO, view.game.canUndo);
        editor.putInt(GAME_STATE, view.game.gameState);
        editor.putInt(UNDO_GAME_STATE, view.game.lastGameState);
        editor.putInt(MOVE_COUNT, view.game.moveNumber);
        editor.putString(GAME_ID, view.game.gameId.toString());
        editor.apply();
    }

    protected void onResume() {
        super.onResume();
        signInToGoogle();
    }

    /**
     * Signs into Google. Used for cloud saves.
     */
    private void signInToGoogle() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean noLoginPrompt = settings.getBoolean(NO_LOGIN_PROMPT, false);
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                        .requestScopes(Drive.SCOPE_APPFOLDER)
                        .build();
        final GoogleSignInClient signInClient = GoogleSignIn.getClient(this, signInOptions);
        signInClient.silentSignIn().addOnCompleteListener(this, new OnCompleteListener<GoogleSignInAccount>() {
            @Override
            public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                if (!task.isSuccessful()) {
                    if (!firstLoginAttempt && !noLoginPrompt) {
                        firstLoginAttempt = true;
                        startActivityForResult(signInClient.getSignInIntent(), RC_SIGN_IN);
                    }
                } else {
                    System.out.println("Successfully logged into Google.");

                    if (task.getResult() != null) {
                        GamesClient client = Games.getGamesClient(MainActivity.this, task.getResult());
                        client.setViewForPopups(view);
                    }

                    SnapshotManager.loadSnapshot(MainActivity.this, new SnapshotManager.Callback() {
                        @Override
                        public void run(@NonNull SnapshotData data) {
                            view.game.handleSnapshot(data);
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != RC_SIGN_IN) {
            return;
        }

        GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
        if (result == null) {
            return;
        }

        if (!result.isSuccess()) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(NO_LOGIN_PROMPT, true);
            editor.apply();
            System.out.println(result.getStatus());
        } else {
            if (result.getSignInAccount() != null) {
                GamesClient client = Games.getGamesClient(MainActivity.this, result.getSignInAccount());
                client.setViewForPopups(view);
            }
            SnapshotManager.loadSnapshot(MainActivity.this, new SnapshotManager.Callback() {
                @Override
                public void run(@NonNull SnapshotData data) {
                    view.game.handleSnapshot(data);
                }
            });
        }
    }

    public static void copyText(String text, Context c) {
        ClipboardManager clipboard = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("label", text);
        if (clipboard == null || clip == null) return;
        clipboard.setPrimaryClip(clip);
    }
}
