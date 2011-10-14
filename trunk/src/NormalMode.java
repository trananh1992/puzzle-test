package com.cody.android.gemsrising;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.media.MediaPlayer;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class NormalMode extends GemView {
	private String[] completeText = {
			"Warm up complete. Ready to play?",
			"Round One complete. Speed up",
			"Round Two complete. Speed up",
			"Round Three complete. Speed up",
			"Round Four complete, BONUS ROUND! (30 seconds or death)",
			"Round Five complete, Who ordered the Gems?",
			"Round Six complete, Speed up",
			"Round Seven complete, Speed up",
			"Round Eight complete. Speed up",
			"Round Nine complete, BONUS ROUND TWO!",
			"Round Ten complete, MAXIMUM GEM POWER",
			"Round Eleven complete, Speed up",
			"Round Twelve complete, Speed up",
			"Round Thirteen complete, two more to go! (Speed up)",
			"Round Fourteen complete, Final Round! Good luck!",
			"Congrats! You've completed the game! Try and beat your score!"
	};
	
	private int[] speedSet = {1, 2, 5, 8, 11, 23, 4, 7, 12, 15, 23, 5, 9, 12, 16, 20};
	
	private int round;
	private int roundScore;
	private int lineLocation;
	private SharedPreferences normal; 
	private boolean roundOver;
	private int bonusTime;
	private int bonusPause;

	public NormalMode(Context c, AttributeSet attrs) {
		super(c, attrs);
		all_time_high = scores.getInt("Normalscore1", 0);
		musicPlay = c.getSharedPreferences("settings", 0).getBoolean("music", true);
		if(musicPlay) {
			music = MediaPlayer.create(c, R.raw.quick_song);
			music.setLooping(true);
			music.start();
		}
		normal = context.getSharedPreferences("normal", 0);
		roundScore = 0;
		
		//Moves gems upwards
		updateOffset = new Runnable() {
			public void run() {
				if(!roundOver) {
					if(running && !gemsFalling && toRemove.isEmpty() && matchedGems.isEmpty()) {
						offset += OFFSET_CHANGE;
						if(offset == GEM_TOTAL - OFFSET_CHANGE) {
							if(!topRowIsEmpty()) {
								running = false;
								mHandler.removeCallbacks(updateOffset);
								gameOverTime = SystemClock.uptimeMillis();
								mHandler.postDelayed(isGameOver, 2000);
							}
						} else if(offset >= GEM_TOTAL) {
							transferRows();
						}
					}
					if(running) {
						int speed;
						if(fastRow) {
							speed = 10;
						} else {
							speed = moving_speed;
						}
						mHandler.removeCallbacks(updateOffset);
						mHandler.postDelayed(updateOffset, speed);
					}
					invalidate();
				}
			}
		};
		
		//Removes gems
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
				roundScore += list.size() * 2 * speed * ((round / 2) + 1);
				current_high += list.size() * 2 * speed * ((round / 2) + 1);
				fromMatches = true;
				resetFirst = true;
				setFallings();
				if(checkFinished() && round != 5 && round != 11) {
					roundComplete();
				}
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
		
		//Checks if the game is over
		isGameOver = new Runnable() {
			public void run() {
				gameOverTime = -1;
				resumeTime = 0;
				if(!running) {
					if(!matchedGems.isEmpty() || !toRemove.isEmpty()) {
						mHandler.removeCallbacks(isGameOver);
						mHandler.postDelayed(isGameOver, 50);
					} else {
						if(!topRowIsEmpty()) {
							if(round == 5 || round == 10) {
								roundComplete();
							} else {
								gameOver = true;
								if(playSounds)
									gameOverSound.start();
								createDialog();
								checkScores();
							}
						} else {
							running = true;
							mHandler.removeCallbacks(updateOffset);
							mHandler.postDelayed(updateOffset, moving_speed);
						}
					}
				}
			}
		};
		
		//Moves falling gems
		adjustFalling = new Runnable() {
			public void run() {
				if(!roundOver) {
					boolean[][] fallingHelps = new boolean[TOTAL_ROWS][TOTAL_COLUMNS];
					for(Set<Gem> s : pausedGems) {
						for(Gem g: s) {
							fallingGrid[g.oldRow][g.column] = true;
						}
					}
					boolean gemFinished = false;
					if(!gemsFalling || resetFirst) {
						gemsFalling = true;
						resetFirst = false;
						setTargetsHelper();
					}
					Iterator<Gem> iterator = ((TreeSet<Gem>) fallingGems).iterator();
					while(iterator.hasNext()) {
						Gem g = iterator.next();
						g.currY = g.currY + FALLING_DISTANCE;
						if(g.currY >= getRowCoords(g.row)) {
							gemGrid[g.row][g.column] = g;
							g.falling = false;
							iterator.remove();
							gemFinished = true;
							if(toResetFallings) {
								setTargetsHelper();
							}
						} else {
							int row1 = (int) ((START_Y - offset + GEM_TOTAL - 1 - g.currY) / GEM_TOTAL);
							int row2 = (int) ((START_Y - offset - g.currY) / GEM_TOTAL);
							int column = g.column;
							if(row1 >= 0 && row1 < TOTAL_ROWS)
								fallingHelps[row1][column] = true;
							if(row2 >= 0 && row2 < TOTAL_ROWS) {
								fallingHelps[row2][column] = true;
							}
						}
					}
					invalidate();
					if(fallingGems.isEmpty() && pausedGems.isEmpty()) {
						fallingGrid = new boolean[TOTAL_ROWS][TOTAL_COLUMNS];
					} else {
						fallingGrid = fallingHelps;
					}
					if(gemFinished && checkFinished()) {
						if(playSounds)
							 soundPool.play(soundPoolMap.get(1), 1, 1, 1, 0, 1);
						roundComplete();
					} else {
						if(gemFinished) {
							 if(playSounds)
								 soundPool.play(soundPoolMap.get(1), 1, 1, 1, 0, 1);
							 checkMatches();
							 if(!running && topRowIsEmpty() && toRemove.isEmpty() && matchedGems.isEmpty()) {
									running = true;
									if(!paused) {
										mHandler.removeCallbacks(updateOffset);
										mHandler.postDelayed(updateOffset, moving_speed);
									}
							   }
						}
						if(!fallingGems.isEmpty()) {
							mHandler.removeCallbacks(adjustFalling);
							mHandler.postDelayed(adjustFalling, FALLING_SPEED);
						} else if(pausedGems.isEmpty()) {
							gemsFalling = false;
							toResetFallings = false;
						}
					}
				}
			}
		};
	}
	
	//Timer for bonus rounds
	private Runnable stillPlaying = new Runnable() {
		public void run() {
			if(round == 5 || round == 10) {
				roundComplete();
			}
		}
	};
	
	//Onpause handler
	@Override
	public void pause() {
		paused = true;
		mHandler.removeCallbacks(updateOffset);
		if(gameOverTime != -1) {
			mHandler.removeCallbacks(isGameOver);
			resumeTime = (int) (SystemClock.uptimeMillis() - gameOverTime + resumeTime);
		}
		if(round == 5 || round == 10) {
			bonusPause = (int) (SystemClock.uptimeMillis() - bonusTime + bonusPause);
			mHandler.removeCallbacks(stillPlaying);
		}
	}
	
	//on resume handler
	@Override
	public void resume() {
		paused = false;
		if(!roundOver) {
			if(running && toRemove.isEmpty() && matchedGems.isEmpty()) {
				mHandler.post(updateOffset);
			}
			if(gameOverTime != -1) {
				mHandler.postDelayed(isGameOver, 2000 - resumeTime);
				gameOverTime = SystemClock.uptimeMillis();
			}
		}
		if(round == 5 || round == 10) {
			mHandler.postDelayed(stillPlaying, 30000 - bonusPause);
		}
	}
	
	//Checks if the clear line has been cleared
	public boolean checkFinished() {
		if(lineLocation >= -1 && round != 5 && round != 10) {
			for(int i = lineLocation + 1; i < TOTAL_ROWS; i++) {
				for(int j = 0; j < TOTAL_COLUMNS; j++) {
					if(gemGrid[i][j] != null || fallingGrid[i][j]) {
						return false;
					}
				}
			}
			return true;
		}
		return false;
	}
	
	//Switches rows
	@Override
	public void transferRows() {
		super.transferRows();
		lineLocation++;
	}
	
	//Begins the new game
	public void newGame() {
		gemTypes = 3;
		round = 0;
		SharedPreferences.Editor editor = normal.edit();
		editor.putBoolean("Continue", false);
		editor.commit();
		speed = 1;
		moving_speed = 500;
		lineLocation = -3;
		roundOver = false;
		loadGrid(gemTypes);
	}
	
	//Saves the game between rounds
	private void saveGame() {
		SharedPreferences.Editor editor = normal.edit();
		editor.putBoolean("Continue", true);
		editor.putInt("Round", round);
		editor.putInt("Score", current_high);
		editor.commit();
		Intent startOver = new Intent(context, GemsRisingActivity.class);
        context.startActivity(startOver);
	}
	
	//Resumes from the previous game
	public void resumeGame() {
		if(round == 5 || round == 10) {
			bonusTime = (int) SystemClock.uptimeMillis();
			mHandler.postDelayed(stillPlaying, 30000);
  	    }
		round = normal.getInt("Round", 0);
		current_high = normal.getInt("Score", 0);
		SharedPreferences.Editor editor = normal.edit();
		editor.putBoolean("Continue", false);
		editor.commit();
		roundOver = false;
		gemTypes = getGemTypes();
		speed = speedSet[round];
		moving_speed = 500 - (speed) * 20;
		lineLocation = -(round * 2 + 3);
		loadGrid(gemTypes);
	}
	
	//Returns corresponding gem type amount
	private int getGemTypes() {
		if(round == 5) {
			return 3;
		} else if(round > 0 && round < 5 || round == 10) {
			return 4;
		} else if(round > 5 && round < 10) {
			return 5;
		} else {
			return 6;
		}
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
				oldScore = scores.getInt("Normalscore" + num, 0);
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
			int score = scores.getInt("Normalscore" + i, 0);
			if(score != 0) {
				editor.putInt("Normalscore" + (i + 1), score);
				String name = scores.getString("Normalname" + i, "");
				editor.putString("Normalname" + (i + 1), name);
			}
		}
		editor.putString("Normalname" + target, username);
		editor.putInt("Normalscore" + target, current_high);
		editor.commit();
	}
	
	//Post round dialog
	private void createRoundDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		View layout = LayoutInflater.from(context).inflate(R.layout.roundcomplete, null);
		builder.setView(layout);
		builder.setCancelable(false);
		TextView curr = (TextView) layout.findViewById(R.id.round);
		TextView high = (TextView) layout.findViewById(R.id.rcurrent);
		curr.setTypeface(t);
		curr.setText(completeText[round - 1] + "\n\n You may exit now and save your progress, or continue. ");
		high.setTypeface(t);
		high.setText("Round Score: " + roundScore + "\nCurrent Score: " + current_high);
		builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
           public void onClick(DialogInterface dialog, int id) {
        	   dialog.cancel();
        	   roundOver = false;
        	   fastRow = false;
        	   roundScore = 0;
        	   if(round == 5 || round == 10) {
        		   mHandler.postDelayed(stillPlaying, 30000);
        	   }
        	   loadGrid(gemTypes);
           }
        });
        builder.setNegativeButton("Return To Menu", new DialogInterface.OnClickListener() {
           public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                music.stop();
                saveGame();
           }
        });
        builder.show();
	}
	
	//Dialog for the game being over
	private void gameOverDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		View layout = LayoutInflater.from(context).inflate(R.layout.success, null);
		builder.setView(layout);
		TextView curr = (TextView) layout.findViewById(R.id.sscore);
		TextView high = (TextView) layout.findViewById(R.id.shigh);
		curr.setTypeface(t);
		curr.setText("Score: " + current_high);
		high.setTypeface(t);
		high.setText("High Score: " + Math.max(scores.getInt("Normalscore1", current_high), current_high));
		builder.setCancelable(true);
		builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
		    @Override
		    public void onCancel(DialogInterface dialog)
		    {
		         Intent startOver = new Intent(context, GemsRisingActivity.class);
		         context.startActivity(startOver);
		         music.stop();
		    }
		});
		builder.setPositiveButton("Menu", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	        	    dialog.cancel();
	           }
	       });
		builder.show();
	}
	
	//Round complete maintenance
	private void roundComplete() {
		roundOver = true;
		bonusPause = 0;
		bonusTime = 0;
		fastRow = false;
		mHandler.removeCallbacks(updateOffset);
		mHandler.removeCallbacks(adjustFalling);
		mHandler.removeCallbacks(stillPlaying);
		mHandler.removeCallbacks(isGameOver);
		mHandler.removeCallbacks(removeGem);
		mHandler.removeCallbacks(adjustPause);
		mHandler.removeCallbacks(removeMatchesHandler);
		mHandler.removeCallbacks(matchCleanup);
		if(round < 15) {
			setup();
			round++;
			gemTypes = getGemTypes();
			createRoundDialog();
			speed = speedSet[round];
			if(round == 5 || round == 10) {
				moving_speed = 500 - (16 * 20);
			}
			moving_speed = 500 - (speed - 1) * 20;
			lineLocation = -(round * 2 + 8) - 3;
		} else {
			gameOver = true;
			gameOverDialog();
			checkScores();
		}
	}
	
	//On down handler
	@Override
	public boolean onDown(MotionEvent e) {
		if(!roundOver) {
			return super.onDown(e);
		}
		return true;
	}
	
	//OnFling handler
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		if(!roundOver) {
			return super.onFling(e1, e2, velocityX, velocityY);
		}
		return true;
	}
	
	//OnDraw handler
	@Override
	public void onDraw(Canvas c) {
		super.onDraw(c);
		if(START_Y == 700) {
			c.drawText("SPEED: " + speed + "x", 275, 60, p);
			c.drawText("SCORE: " + current_high, 10, 60, p);
		}
		if(round != 5 && round != 10)
			c.drawBitmap(CLEAR_LINE, 10, getRowCoords(lineLocation) - 7, null);
		
	}
}
