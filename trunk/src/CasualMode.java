package com.cody.android.gemsrising;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;


public class CasualMode extends GemView {
	
	public CasualMode(Context c, AttributeSet attrs) {
		super(c, attrs);
		gemTypes = 6;
		musicPlay = c.getSharedPreferences("settings", 0).getBoolean("music", true);
		if(musicPlay) {
			music = MediaPlayer.create(c, R.raw.casual_song);
			music.setLooping(true);
			music.start();
		}
	}
	
	//Updates the time of play
	private Runnable updateTime = new Runnable() {
		public void run() {
			current_high++;
			if(current_high >= all_time_high) {
				all_time_high = current_high;
			}
			mHandler.postDelayed(updateTime, 1000);
			invalidate();
		}
	};
	
	//Game over dialog
	@Override
	public void createDialog() {
		View layout = LayoutInflater.from(context).inflate(R.layout.gameover, null);
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setView(layout);
		TextView curr = (TextView) layout.findViewById(R.id.score);
		TextView high = (TextView) layout.findViewById(R.id.highscore);
		curr.setTypeface(t);
		curr.setText("Score: " + getTime(current_high));
		high.setTypeface(t);
		high.setText("High Score: " + getTime(all_time_high));	
		builder.setCancelable(true);
		builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
		    @Override
		    public void onCancel(DialogInterface dialog)
		    {
		    	if(musicPlay) {
		        	 music.stop();
		         }
		         Intent startOver = new Intent(context, GemsRisingActivity.class);
		         context.startActivity(startOver);
		    }
		});
		builder.setPositiveButton("Menu", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	        	    dialog.cancel();
	           }
	       });
		builder.show();
	}
	
	//Checks high scores
	@Override
	public void checkScores() {
		mHandler.removeCallbacks(updateTime);
		mHandler.removeCallbacks(updateOffset);
		if(current_high != 0) {
			int num = 11;
			int oldScore;
			do {
				num--;
				oldScore = scores.getInt("" + speed + "Casualscore" + num, 0);
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
			int score = scores.getInt("" + speed + "Casualscore" + i, 0);
			if(score != 0) {
				editor.putInt("" + speed + "Casualscore" + (i + 1), score);
				String name = scores.getString("" + speed + "Casualname" + i, "");
				editor.putString("" + speed + "Casualname" + (i + 1), name);
			}
		}
		editor.putString("" + speed + "Casualname" + target, username);
		editor.putInt("" + speed + "Casualscore" + target, current_high);
		editor.commit();

	}
	
	//Sets the speed and starts the game
	public void setSpeed(int speed) {
		this.speed = speed;
		moving_speed = 540 - (speed * 20);
		this.all_time_high = scores.getInt("" + speed + "Casualscore1", 0);
		loadGrid(gemTypes);
		mHandler.postDelayed(updateTime, 1000);
	}
	
	//Resume handler
	@Override
	public void resume() {
		super.resume();
		if(!gameOver) {
			mHandler.postDelayed(updateTime, 1000);
		}
	}
	
	//Pause Handler
	@Override
	public void pause() {
		super.pause();
		mHandler.removeCallbacks(updateTime);
	}
	
	//Draws the grid
	@Override
	public void onDraw(Canvas c) {
		super.onDraw(c);
		if(START_Y == 700) {
			c.drawText("SCORE: " + getTime(current_high), 10, 60, p);
			c.drawText("SPEED: " + speed + "x", 275, 60, p);
			c.rotate(90);
			c.drawText("HIGH SCORE: " + getTime(all_time_high), 100, -400, p);
		}
	}
	
	//Returns the formatted score
	public static String getTime(int score) {
		String result = "";
		result += "" + score/3600 + ":";
		int minutes = score / 60;
		if(minutes < 10) {
			result +=  "0" + minutes + ":";
		} else {
			result += "" + minutes + ":";
		}
		int seconds = score % 60;
		if(seconds < 10) {
			result += "0" + seconds;
		} else {
			result += "" + seconds;
		}
		return result;
	}
}
