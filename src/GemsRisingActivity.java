package com.cody.android.gemsrising;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

/*
 This class manages the menu system, settings, and modes.
 */

public class GemsRisingActivity extends Activity {
	
	private GemView pView; //Maintains the game information
	private boolean playing; //Whether the game has started
	private SharedPreferences settings; //Storage for settings
	private SharedPreferences scores; //Storage for scores
	private boolean toResume; //Whether the game can be "resumed"
	private Dialog currentD; //The current dialog available
	private boolean onModes; //On modes screen or high scores screen
	private boolean inCasualScore; //Whether the casual high scores are displayed
	private MediaPlayer menuMusic; //The menu music
	private MediaPlayer startGame;
	
    /* Creates the activity, if first time running gauges dpi and saves settings*/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); 
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        settings = this.getSharedPreferences("settings", 0);
        if(settings.getBoolean("music", true)) {
        	createMusic();
        }
        startGame = MediaPlayer.create(this, R.raw.startgame);
	    scores = this.getSharedPreferences("scores", 0);
	    SharedPreferences.Editor editor = settings.edit();
	    if(!settings.getBoolean("set", false)) {
        	DisplayMetrics metrics = new DisplayMetrics();
        	getWindowManager().getDefaultDisplay().getMetrics(metrics);
        	int dpi = metrics.densityDpi;
        	if(dpi == DisplayMetrics.DENSITY_LOW) {
        		editor.putString("dpi", "low");
        		editor.putInt("Starty", 300);
        		editor.putInt("gemsize", 25);
        		editor.putInt("bordersize", 5);
        		editor.putInt("offset", 3);
        	} else if(dpi == DisplayMetrics.DENSITY_MEDIUM){
        		editor.putString("dpi", "medium");
        		editor.putInt("Starty", 400);
        		editor.putInt("gemsize", 35);
        		editor.putInt("bordersize", 5);
        		editor.putInt("offset", 4);
        	} else {
        		editor.putString("dpi", "high");
        		editor.putInt("Starty", 700);
        		editor.putInt("gemsize", 50);
        		editor.putInt("bordersize", 10);
        		editor.putInt("offset", 4);
        	}
        	editor.putBoolean("set", true);
        	editor.commit();
        }
	    playing = false;
        pView = null;
        toResume = false;
        setContentView(R.layout.main);
        setClicks();
    }
    
    //Starts menu music
    private void createMusic() {
    	menuMusic = MediaPlayer.create(this, R.raw.menu_song);
    	menuMusic.setLooping(true);
    	menuMusic.start();
    }
    
    //Sets the listeners for the Menu
	private void setClicks() {
		Typeface font = Typeface.createFromAsset(getAssets(), "pricedown.ttf");
		Button b = (Button) findViewById(R.id.start);
		b.setTypeface(font);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onModes = true;
				setContentView(R.layout.modes);
				setModeClicks();
			}
		});
		b = (Button) findViewById(R.id.hscores);
		b.setTypeface(font);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onModes = true;
				displayScores("Normal");
			}
		});
		b = (Button) findViewById(R.id.how);
		b.setTypeface(font);
		b.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Dialog dialog = new Dialog(GemsRisingActivity.this);
				dialog.setContentView(R.layout.howtoplay);
				dialog.setTitle("How To Play");
				dialog.setCancelable(true);
				dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				    @Override
				    public void onCancel(DialogInterface dialog)
				    {
				    	currentD = null;
				    	dialog.dismiss();
				    }
				});
				currentD = dialog;
				dialog.setOwnerActivity(GemsRisingActivity.this);
				dialog.show();
			}
		});
		b = (Button) findViewById(R.id.options);
		b.setTypeface(font);
		b.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setOptionMenu();
			}
		});
	}
	
	//Sets the listeners for the mode page
	private void setModeClicks() {
		Typeface font = Typeface.createFromAsset(getAssets(), "pricedown.ttf");
		Button b = (Button) findViewById(R.id.normalmode);
		b.setTypeface(font);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(settings.getBoolean("music", true))
					menuMusic.pause();
				if(settings.getBoolean("sound", true)) {
					startGame.start();
				}
				setContentView(R.layout.normallayout);
				pView = (NormalMode) findViewById(R.id.normalview);
				if(getSharedPreferences("normal", 0).getBoolean("Continue", false)) {
					AlertDialog.Builder builder = new AlertDialog.Builder(GemsRisingActivity.this);
					builder.setMessage("Continue previous normal game? \n(If you start a new game, progress will be lost)")
							.setCancelable(false)
					       .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
					           public void onClick(DialogInterface dialog, int id) {
					        	  ((NormalMode) pView).resumeGame();
					           }
					       })
					       .setNegativeButton("New Game", new DialogInterface.OnClickListener() {
					           public void onClick(DialogInterface dialog, int id) {
					                dialog.cancel();
					                ((NormalMode) pView).newGame();
					           }
					       });
					AlertDialog alert = builder.show();
				} else {
					((NormalMode) pView).newGame();
				}
				playing = true;
				onModes = false;
			}
		});
		b = (Button) findViewById(R.id.casualmode);
		b.setTypeface(font);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(settings.getBoolean("music", true))
					menuMusic.pause();
				if(settings.getBoolean("sound", true)) {
					startGame.start();
				}
				setContentView(R.layout.casuallayout);
				pView = (CasualMode) findViewById(R.id.casualview);
				int speed = settings.getInt("speed", 0);
				((CasualMode) pView).setSpeed(speed);
				playing = true;
				onModes = false;
			}
		});
		b = (Button) findViewById(R.id.quickmode);
		b.setTypeface(font);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(settings.getBoolean("music", true))
					menuMusic.pause();
				if(settings.getBoolean("sound", true)) {
					startGame.start();
				}
				setContentView(R.layout.quicklayout);
				pView = (QuickMode) findViewById(R.id.quickview);
				playing = true;
				onModes = false;
			}
		});
	}
	
	//Sets the option menu
	private void setOptionMenu() {
		View layout = getLayoutInflater().inflate(R.layout.options, null);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(layout);
		Spinner spinner = (Spinner) layout.findViewById(R.id.speed);
	    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
	            this, R.array.numbers, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinner.setAdapter(adapter);
	    spinner.setSelection(settings.getInt("speed", 1) - 1);
		((CheckBox) layout.findViewById(R.id.music)).setChecked(settings.getBoolean("music", true));
		((CheckBox) layout.findViewById(R.id.sound)).setChecked(settings.getBoolean("sound", true));
		builder.setCancelable(true);
		builder.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
			   @Override
	           public void onClick(DialogInterface dialog, int id) {
	        	   Dialog curDialog = (Dialog) dialog;
        		   SharedPreferences.Editor editor = settings.edit();
	        	   Spinner s = (Spinner) curDialog.findViewById(R.id.speed);
	        	   int speed = Integer.parseInt(s.getSelectedItem().toString());
	        	   editor.putInt("speed", speed);
	        	   boolean oldMusicOn = settings.getBoolean("music", true);
	        	   boolean newMusic = ((CheckBox) curDialog.findViewById(R.id.music)).isChecked();
	        	   if(oldMusicOn && !newMusic) {
	        		   menuMusic.stop();
	        	   } else if(!oldMusicOn && newMusic) {
	        		   createMusic();
	        	   }
	        	   editor.putBoolean("music", newMusic);
	        	   editor.putBoolean("sound", ((CheckBox) curDialog.findViewById(R.id.sound)).isChecked());
	        	   editor.commit();
	           }
	       })
	       .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	    	   @Override
	           public void onClick(DialogInterface dialog, int id) {
	        	    currentD = null;
	                dialog.dismiss();
	           }
	       });
		Button resetScores = (Button) layout.findViewById(R.id.reset);
		resetScores.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(GemsRisingActivity.this);
				builder.setMessage("Clear All Scores Permanently?")
				       .setCancelable(true)
				       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
			        		   SharedPreferences.Editor editor = scores.edit();
			        		   editor.clear();
			        		   editor.commit();
			        		   dialog.cancel();
				           }
				       })
				       .setNegativeButton("No", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				                dialog.cancel();
				           }
				       });
				AlertDialog alert = builder.show();
			}
		});
		currentD = builder.show();
	}
	
	//Displays default scores
	private void displayScores(String mode) {		
		setContentView(R.layout.casualhigh);
		//When the spinner is created and adapter applied, the change of spinner listener
		//Will see the listener has changed, and call the appropriate value.
		Spinner spinner = (Spinner) findViewById(R.id.spinner2);
	    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
	            this, R.array.modes_array, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinner.setAdapter(adapter);
	    Spinner speedSpinner = (Spinner) findViewById(R.id.speedSpin);
	    adapter = ArrayAdapter.createFromResource(
	            this, R.array.numbers, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    speedSpinner.setAdapter(adapter);
	    /*
	    if(!mode.equals("Casual")) {
	    	getHighScoreText(mode);
	    } else {
	    	setCasualScoreText(settings.getInt("speed", 0));
	    	inCasualScore = true;
	    }
	    */
	    spinner.setOnItemSelectedListener(new MyOnItemSelectedListener());
	    speedSpinner.setOnItemSelectedListener(new AltMyOnItemSelectedListener());
	}
	
	//Returns casual scores in a string
	private String setCasualScoreText(int speed) {
		((Spinner) findViewById(R.id.speedSpin)).setVisibility(View.VISIBLE);
    	TextView casHighScore = (TextView) findViewById(R.id.casHighScore);
    	casHighScore.setVisibility(View.VISIBLE);
		String result = "\n";
		String nameResult = "\n";
		int[] score = new int[10];
		String[] names = new String[10];
		for(int i = 0; i < 10; i++) {
			score[i] = scores.getInt("" + speed + "Casualscore" + (i + 1), 0);
			if(score[i] == 0) {
				break;
			}
			names[i] = scores.getString("" + speed + "Casualname" + (i + 1), "");
		}
		for(int i = 0; i < 10; i++) {
			if(names[i] != null) {
				nameResult += (i + 1) + ": " + names[i] + "\n";
				result += CasualMode.getTime(score[i]) + "\n";
			}
		}
		((TextView) findViewById(R.id.casualscores)).setText(result);
		((TextView) findViewById(R.id.casualnames)).setText(nameResult);
		return result;
	}
	
	//Returns quick/normal scores in a string
	private String getHighScoreText(String mode) {
	    ((Spinner) findViewById(R.id.speedSpin)).setVisibility(View.INVISIBLE);
    	TextView casHighScore = (TextView) findViewById(R.id.casHighScore);
    	casHighScore.setVisibility(View.INVISIBLE);
		String result = "\n";
		String nameResult = "\n";
		int[] score = new int[10];
		String[] names = new String[10];
		for(int i = 0; i < 10; i++) {
			score[i] = scores.getInt(mode + "score" + (i + 1), 0);
			if(score[i] == 0) {
				break;
			}
			names[i] = scores.getString(mode + "name" + (i + 1), "");
		}
		for(int i = 0; i < 10; i++) {
			if(names[i] != null) {
				result += score[i] + "\n";
				nameResult += (i + 1) + ": " + names[i] + "\n";
			}
		}
		((TextView) findViewById(R.id.casualscores)).setText(result);
		((TextView) findViewById(R.id.casualnames)).setText(nameResult);
		return result;
	}
	
	//On pause, either pause the music, pause the game, both, or cancel the dialog up
	@Override
	public void onPause() {
		super.onPause();
		if(pView != null) {
			if(settings.getBoolean("music", true))
				pView.music.pause();
			if(!pView.getPaused()) {
				pView.pause();
			}
		} else {
			if(currentD != null) {
				currentD.cancel();
				currentD = null;
			}
			if(settings.getBoolean("music", true))
				menuMusic.pause();
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if(pView != null && settings.getBoolean("music", true) && !pView.music.isPlaying()) {
			pView.music.start();
		} else if(pView == null) {
			if(settings.getBoolean("music", true) && !menuMusic.isPlaying()) {
				menuMusic.start();
			}
		}
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if(hasFocus) {
			if(pView != null) {
				pView.resume();
				if(settings.getBoolean("music", true) && !pView.music.isPlaying())
					pView.music.start();
			} else {
				if(settings.getBoolean("music", true) && !menuMusic.isPlaying())
					menuMusic.start();
			}
		}
	}
	
	//When playing, this dialog will appear when back is pressed
	private void setDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Return to the Main Menu? \n\n(Your score will be saved, but your game will not)")
		       .setOnCancelListener(new DialogInterface.OnCancelListener() {
				    @Override
				    public void onCancel(DialogInterface dialog)
				    {
				    	pView.resume();
				    	dialog.dismiss();
				    }
				})
		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   pView.checkScores();
		        	   if(settings.getBoolean("music", true))
		        		   pView.music.stop();
		        	   pView = null;
		        	   playing = false;
		        	   setContentView(R.layout.main);
		        	   if(settings.getBoolean("music", true))
		        		   menuMusic.start();
		        	   setClicks();
		           }
		       })
		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		           }
		       });
		AlertDialog alert = builder.show();
		alert.setOwnerActivity(GemsRisingActivity.this);
	}
	
	//Listeners for pressing back
	@Override
	public void onBackPressed() {
		if(playing) {
			pView.pause();
			setDialog();
		} else if(onModes){ 
			onModes = false;
			setContentView(R.layout.main);
			setClicks();
		} else { //On main menu
			moveTaskToBack(true);
		}
	}
	
	//Spinner manager class
	public class MyOnItemSelectedListener implements OnItemSelectedListener {
		
	    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
	    	String mode = parent.getItemAtPosition(pos).toString();
	    	if(mode.equals("Casual")) {
	    		if(!inCasualScore) {
	    			inCasualScore = true;
	    			GemsRisingActivity.this.setCasualScoreText(1);
	    		}
	    	} else {
	    		if(inCasualScore) {
	    			inCasualScore = false;
	    		}
	    		getHighScoreText(mode);
	    	}
	    }

	    public void onNothingSelected(AdapterView parent) {
	      // Do nothing.
	    }
	}
	
	//Speed spinner manager
	public class AltMyOnItemSelectedListener implements OnItemSelectedListener {
			
		    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
		    	if(inCasualScore) {
		    		String mode = parent.getItemAtPosition(pos).toString();
		    		int speed = Integer.parseInt(mode);
		    		setCasualScoreText(speed);
		    	}
		    }
	
		    public void onNothingSelected(AdapterView parent) {
		      // Do nothing.
		    }
	}
}