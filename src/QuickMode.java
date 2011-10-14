package com.cody.android.gemsrising;

import java.util.Iterator;
import java.util.Set;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class QuickMode extends GemView {
	public QuickMode(Context c, AttributeSet attrs) {
		super(c, attrs);
		moving_speed = STARTING_SPEED;
		this.all_time_high = scores.getInt("Quickscore1", 0);
		speed = 1;
		gemTypes = 6;
		musicPlay = c.getSharedPreferences("settings", 0).getBoolean("music", true);
		if(musicPlay) {
			music = MediaPlayer.create(c, R.raw.quick_song);
			music.setLooping(true);
			music.start();
		}
		loadGrid(gemTypes);
		
		matchCleanup = new Runnable() {
			public void run() {
				Set<Gem> list = toRemove.remove(0);
				Iterator<Gem> i = list.iterator();
				while(i.hasNext()) {
					Gem g = i.next();
					int row = g.row;
					int column = g.column;
					gemGrid[row][column] = null;
				}
				current_high += list.size() * 5 * speed;
				int target =  ((speed) * (speed + 1) / 2) * 125;
				if(current_high > target) {
					moving_speed = STARTING_SPEED - (25 * speed);
					speed++;
				}
				if(current_high > all_time_high) {
					all_time_high = current_high;
				}
				fromMatches = true;
				resetFirst = true;
				setFallings();
				if(matchedGems.isEmpty() && toRemove.isEmpty()) {
					if(!paused) {
						mHandler.removeCallbacks(updateOffset);
						mHandler.postDelayed(updateOffset, 500);
					}
					if(!running && topRowIsEmpty()) {
						running = true;
					}
				}
				invalidate();
			}
		};
	}
	
	//Checks high scores
	@Override
	public void checkScores() {
		mHandler.removeCallbacks(updateOffset);
		if(current_high != 0) {
			int num = 11;
			int oldScore;
			do {
				num--;
				oldScore = scores.getInt("Quickscore" + num, 0);
			} while(num >= 1 && current_high > oldScore);
			if(num != 10) {
				target = num + 1;
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				View layout = LayoutInflater.from(context).inflate(R.layout.entername, null);
				builder.setView(layout);
				builder.setCancelable(false);
				builder.setPositiveButton("Continue",new DialogInterface.OnClickListener() {				
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Dialog curD = (Dialog) dialog;
						EditText e = (EditText) curD.findViewById(R.id.name);
						saveHighScores(e.getText().toString());
						dialog.dismiss();
					}
				});
				builder.show();
			}
		}
	}
	
	//Saves high scores
	private void saveHighScores(String username) {
		SharedPreferences.Editor editor = scores.edit();
		for(int i = 9; i >= target; i--) {
			int score = scores.getInt("Quickscore" + i, 0);
			if(score != 0) {
				editor.putInt("Quickscore" + (i + 1), score);
				String name = scores.getString("Quickname" + i, "");
				editor.putString("Quickname" + (i + 1), name);
			}
		}
		editor.putString("Quickname" + target, username);
		editor.putInt("Quickscore" + target, current_high);
		editor.commit();
	}
	
	//OnDraw handler
	@Override
	public void onDraw(Canvas c) {
		super.onDraw(c);
		if(START_Y == 700) {
			c.drawText("SPEED: " + speed + "x", 275, 60, p);
			c.drawText("SCORE: " + current_high, 10, 60, p);
		}
		c.rotate(90);
	    c.drawText("HIGH SCORE: " + all_time_high, 100, -400, p);
	}
	
}
