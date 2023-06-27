package com.tpcstld.twozerogame;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.UUID;

public class Settings {
    public long score;
    public long highScore;
    public long lastScore;
    public boolean canUndo;
    public int gameState;
    public int lastGameState;
    public int moveNumber;
    public UUID gameId;
    public Tile[][] field;
    public Tile[][] undoField;
    public boolean empty = false;

    public Settings(long score, long highScore, long lastScore, boolean canUndo, int gameState, int lastGameState, int moveNumber, UUID gameId, Tile[][] field, Tile[][] undoField) {
        this.score = score;
        this.highScore = highScore;
        this.lastScore = lastScore;
        this.canUndo = canUndo;
        this.gameState = gameState;
        this.lastGameState = lastGameState;
        this.moveNumber = moveNumber;
        this.gameId = gameId;
        this.field = field;
        this.undoField = undoField;
    }
    public Settings() {
        this.empty = true;
    }

    public void load(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        int width = settings.getInt(MainActivity.WIDTH, -1);
        int height = settings.getInt(MainActivity.HEIGHT, -1);
        this.field = new Tile[width][height];
        this.undoField = new Tile[width][height];
        for (int xx = 0; xx < width; xx++) {
            for (int yy = 0; yy < height; yy++) {
                int value = settings.getInt(xx + " " + yy, -1);
                if (value > 0) {
                    this.field[xx][yy] = new Tile(xx, yy, value);
                } else if (value == 0) {
                    this.field[xx][yy] = null;
                }

                int undoValue = settings.getInt(MainActivity.UNDO_GRID + xx + " " + yy, -1);
                if (undoValue > 0) {
                    this.undoField[xx][yy] = new Tile(xx, yy, undoValue);
                } else if (value == 0) {
                    this.undoField[xx][yy] = null;
                }
            }
        }

        this.score = settings.getLong(MainActivity.SCORE, 0);
        this.highScore = settings.getLong(MainActivity.HIGH_SCORE, 0);
        this.lastScore = settings.getLong(MainActivity.UNDO_SCORE, 0);
        this.canUndo = settings.getBoolean(MainActivity.CAN_UNDO, false);
        this.gameState = settings.getInt(MainActivity.GAME_STATE, 0);
        this.lastGameState = settings.getInt(MainActivity.UNDO_GAME_STATE, 0);
        this.moveNumber = settings.getInt(MainActivity.MOVE_COUNT, 1);
        this.gameId = UUID.fromString(settings.getString(MainActivity.GAME_ID, UUID.randomUUID().toString()));
        this.empty = false;
    }
}
