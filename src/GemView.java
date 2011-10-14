package com.cody.android.gemsrising;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;




public class GemView extends View implements OnGestureListener {

	protected Gem[][] gemGrid; //Holds the Gems
	protected Gem[][] nextGems; //Holds the next two lines of gems
	protected boolean[][] fallingGrid;
	protected boolean toResetFallings; //Whether the gems must be reset for their falling targets
	protected boolean resetFirst; //Reset fallings at the very start of a gem being switched
	protected SelectedGem sGem; //Information about the selected Gem
	protected GestureDetector gDetector; //Detects gestures
	protected Set<Gem> fallingGems; //Holds the gems that are falling
	protected List<Set<Gem>> pausedGems; //Holds the gems that are paused and going to be added to the fallings
	protected List<Set<Gem>> matchedGems; //LinkedList to hold the ArrayLists of matched Gems
	//Would be easy to implement scoring, just see the size of the ArrayList you use. Simple.
	protected Handler mHandler; //Handler for timing of gems and such.
	protected boolean gemsFalling; //whether gems are falling
	protected Map<Long, Gem> currentMatches; //The ordered gems (in timed order)
	protected List<Set<Gem>> toRemove; //Gems to remove next from the grid. 
	protected int offset; //Difference between normal row position and current position
	protected boolean running; //whether the game is scrolling upwards
	protected int moving_speed; //how fast the game is moving upwards
	public boolean gameOver;
	protected boolean fromMatches; //tells the fallings whether they are from a match or not
	protected boolean fastRow; //Longpress, means the row is increasing quickly
	protected boolean musicPlay;
	protected int gemTypes;
	protected List<Gem> firstList;
	
	protected long gameOverTime;
	protected int resumeTime;
	protected int target;
	protected boolean paused;
	
	protected MediaPlayer gameOverSound;
	protected boolean playSounds;
	public MediaPlayer music;
	protected SoundPool soundPool;
	protected Map<Integer, Integer> soundPoolMap;
	
	public final Bitmap TOTAL_BORDER = 
			BitmapFactory.decodeResource(this.getResources(), R.drawable.section_border);
	public final Bitmap SELECTED_BORDER = 
			BitmapFactory.decodeResource(this.getResources(), R.drawable.border);
	public final Bitmap BACKGROUND = 
			BitmapFactory.decodeResource(this.getResources(), R.drawable.background);
	public final Bitmap CLEAR_LINE = 
			BitmapFactory.decodeResource(this.getResources(), R.drawable.clear_line);

	protected Paint p;
	protected Typeface t;
	protected Context context;
		
	protected int speed;
	protected int all_time_high;
	protected int current_high;
	protected SharedPreferences scores; 
	
	public final int START_ROWS = 6; //Number of rows in beginning
	public final int START_COLUMNS = 6; //Number of columns in beginning
	public final int TOTAL_ROWS = 10; //Total possible rows
	public final int TOTAL_COLUMNS = 6; //Total possible columns
	public final int GEM_SIZE; // = 60; //Pixel size of gems
	public final int START_X = 20; //Starting X location of 0,0
	public final int START_Y; // = 700; //Starting X location of 0,0
	public final int GEM_TOTAL; // = 60; //GEM_SIZE + SPACE
	public final int BORDER_WIDTH; // = 10; //Width of "selected" border
	public final int FALLING_SPEED = 1;
	public final int FALLING_DISTANCE = 15;
	public final int DISAPPEAR_SPEED = 175; //175
	public final int MATCHED_PAUSE = 200;
	public final int FALLING_PAUSE = 200;
	public final int OFFSET_CHANGE; //= 4;
	public final int STARTING_SPEED = 400;
	
	
	public GemView(Context c, AttributeSet attrs) {
		super(c, attrs);
		SharedPreferences settings = c.getSharedPreferences("settings", 0);
		playSounds = settings.getBoolean("sound", true);
		if(playSounds) {
			gameOverSound = MediaPlayer.create(c, R.raw.gameover);
			soundPool = new SoundPool(40, AudioManager.STREAM_MUSIC, 0);
			soundPoolMap = new HashMap<Integer, Integer>();
		    soundPoolMap.put(1, soundPool.load(c, R.raw.fall, 1));
		    soundPoolMap.put(2, soundPool.load(c, R.raw.disapp, 1));
		}
		START_Y = settings.getInt("Starty", 700);
		GEM_SIZE = settings.getInt("gemsize", 60);
		BORDER_WIDTH = settings.getInt("bordersize", 10);
		GEM_TOTAL = GEM_SIZE + BORDER_WIDTH;
		OFFSET_CHANGE = settings.getInt("offset", 4);
		this.context = c;
		current_high = 0;
	    scores = context.getSharedPreferences("scores", 0);
	    p = new Paint();
		t = Typeface.createFromAsset(context.getAssets(), "pricedown.ttf");
		p.setTypeface(t);
		p.setAntiAlias(true);
		if(settings.getString("dpi", "high").equals("high")) {
			p.setTextSize(35);
		} else {
			p.setTextSize(0);
		}
		p.setColor(Color.WHITE);
		p.setTextAlign(Paint.Align.LEFT);
		setup();
	}
	
	//Early setup
	protected void setup() {
		mHandler = new Handler();
		firstList = new LinkedList<Gem>();
		gameOverTime = -1;
		resumeTime = 0;
		paused = false;
		fastRow = false;
		running = true;
		fromMatches = false;
		gameOver = false;
		pausedGems = new ArrayList<Set<Gem>>();
		toRemove = new ArrayList<Set<Gem>>();
		currentMatches = new TreeMap<Long, Gem>();
		fallingGems = new TreeSet<Gem>();
		matchedGems = new LinkedList<Set<Gem>>();
		gDetector = new GestureDetector(this);
		fallingGrid = new boolean[TOTAL_ROWS][TOTAL_COLUMNS];
		toResetFallings = false;
		resetFirst = false;
		gemGrid = new Gem[TOTAL_ROWS][TOTAL_COLUMNS];
		nextGems = new Gem[3][TOTAL_COLUMNS];
		sGem = new SelectedGem();
		gemsFalling = false;
		offset = 0;
	}
	
	//Guaranteed method. Will be implemented by each class
	public void checkScores() {
	}
	
	//Returns paused state
	public boolean getPaused() {
		return paused;
	}
	
	//Continues the game
	public void resume() {
		paused = false;
		if(!fallingGems.isEmpty()) {
			mHandler.removeCallbacks(adjustFalling);
			mHandler.post(adjustFalling);
		}
		if(running && toRemove.isEmpty() && matchedGems.isEmpty()) {
			mHandler.post(updateOffset);
		}
		if(gameOverTime != -1) {
			mHandler.postDelayed(isGameOver,  2000 - resumeTime);
			gameOverTime = SystemClock.uptimeMillis();
		}
	}
	
	//Pauses the game
	public void pause() {
		paused = true;
		mHandler.removeCallbacks(updateOffset);
		if(gameOverTime != -1) {
			mHandler.removeCallbacks(isGameOver);
			resumeTime = (int) (SystemClock.uptimeMillis() - gameOverTime) + resumeTime;
		}
	}
	
	//Constantly runs and updates the stack of gems upwards
	protected Runnable updateOffset = new Runnable() {
		public void run() {
			//System.out.println("updateOffset");
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
	};
	
	//Transfers rows
	protected void transferRows() {
		boolean falls = false;
		//Safeguard
		if (!fallingGems.isEmpty() || !pausedGems.isEmpty())
			falls = true;
		for(int i = TOTAL_ROWS - 1; i > 0; i--) {
			gemGrid[i] = gemGrid[i-1];
			if(falls) {
				//Safeguard
				fallingGrid[i] = fallingGrid[i-1];
			}
		}
		if(sGem.row < TOTAL_ROWS - 1) {
			sGem.row++;
		}
		gemGrid[0] = nextGems[2];
		nextGems[2] = nextGems[1];
		nextGems[1] = nextGems[0];
		nextGems[0] = getNextRow(gemTypes);
		offset = 0;
		//Safeguard
		if(!currentMatches.isEmpty()) {
			Set<Long> keys = currentMatches.keySet();
			for(long l: keys) {
				currentMatches.get(l).row++;
			}
		}
		//Safeguard
		if(!toRemove.isEmpty()) {
			for(int i = 0; i < toRemove.size(); i++) {
				Iterator<Gem> iterator = toRemove.get(i).iterator();
				while(iterator.hasNext()) {
					iterator.next().row++;
				}
			}
		}
		checkMatches();
	}
	
	//Tests and reacts if the game is over
	protected Runnable isGameOver = new Runnable() {
		public void run() {
			gameOverTime = -1;
			resumeTime = 0;
			if(!running) {
				if(!matchedGems.isEmpty() || !toRemove.isEmpty()) {
					mHandler.removeCallbacks(isGameOver);
					mHandler.postDelayed(isGameOver, 50);
				} else {
					if(!topRowIsEmpty()) {
						gameOver = true;
						if(playSounds)
							gameOverSound.start();
						createDialog();
						checkScores();
					} else {
						running = true;
						mHandler.removeCallbacks(updateOffset);
						mHandler.postDelayed(updateOffset, moving_speed);
					}
				}
			}
		}
	};
	
	//Creates the game over dialog
	public void createDialog() {
		View layout = LayoutInflater.from(context).inflate(R.layout.gameover, null);
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setView(layout);
		TextView curr = (TextView) layout.findViewById(R.id.score);
		TextView high = (TextView) layout.findViewById(R.id.highscore);
		curr.setText("Score: " + current_high);
		curr.setTypeface(t);
		all_time_high = Math.max(all_time_high, current_high);
		high.setText("High Score: " + all_time_high);
		high.setTypeface(t);	
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
	
	//Removal of gems is as follows: A.) Place set of gems in a group. Then gems are added to the "currentGems"
	//Based on time of removal. Then, gems are removed from the grid and fallings are reset.
	protected Runnable matchCleanup = new Runnable() {
		public void run() {
			Set<Gem> list = toRemove.remove(0);
			Iterator<Gem> i = list.iterator();
			while(i.hasNext()) {
				Gem g = i.next();
				int row = g.row;
				int column = g.column;
				gemGrid[row][column] = null;
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
	
	//Removes visible matches
	protected Runnable removeMatchesHandler = new Runnable() {
		public void run() {
			Set<Gem> matches =  ((LinkedList<Set<Gem>>) matchedGems).removeFirst();
			toRemove.add(matches);
			mHandler.postDelayed(matchCleanup, DISAPPEAR_SPEED * (matches.size() - 1) + 5);
			int i = 0;
			for(Gem g: matches) {
				long t = SystemClock.uptimeMillis() + DISAPPEAR_SPEED * i;
				currentMatches.put(t, g);
				mHandler.postDelayed(removeGem, DISAPPEAR_SPEED *i);
				i++;
			}
		}
	};
	
	//Removes the next gem
	protected Runnable removeGem = new Runnable() {
		public void run() {
			if(!currentMatches.isEmpty()) {
				Gem g = currentMatches.remove(((TreeMap<Long, Gem>) currentMatches).firstKey());
				g.disappeared = true;
				if(playSounds)
					 soundPool.play(soundPoolMap.get(2), 1, 1, 1, 0, 2);
				invalidate();
			}
		}
	};
	
	//Moves paused gems into falling gems
	protected Runnable adjustPause = new Runnable() {
		public void run() {
			Set<Gem> s = pausedGems.remove(0);
			Iterator<Gem> i = s.iterator();
			while(i.hasNext()) {
				Gem g = i.next();
				fallingGems.add(g);
				gemGrid[g.oldRow][g.column] = null;
			}
			mHandler.removeCallbacks(adjustFalling);
			resetFirst = true;
			mHandler.post(adjustFalling);
		}
	};
	
	//Makes gems fall at falling_speed
	protected Runnable adjustFalling = new Runnable() {
		public void run() {
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
				int row1 = (int) ((START_Y - offset + GEM_TOTAL - 1 - g.currY) / GEM_TOTAL);
				int row2 = (int) ((START_Y - offset - g.currY) / GEM_TOTAL);
				int column = g.column;
				if(row1 >= 0 && row1 < TOTAL_ROWS)
					fallingHelps[row1][column] = true;
				if(row2 >= 0 && row2 < TOTAL_ROWS) {
					fallingHelps[row2][column] = true;
				}
				g.currY = g.currY + FALLING_DISTANCE;
				if(g.currY >= getRowCoords(g.row)) {
					gemGrid[g.row][g.column] = g;
					g.falling = false;
					iterator.remove();
					gemFinished = true;
					if(toResetFallings) {
						setTargetsHelper();
					}
				}
			}
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
			invalidate();
			if(!fallingGems.isEmpty()) {
				fallingGrid = fallingHelps;
				mHandler.removeCallbacks(adjustFalling);
				mHandler.postDelayed(adjustFalling, FALLING_SPEED);
			} else if(pausedGems.isEmpty()) {
				gemsFalling = false;
				toResetFallings = false;
				fallingGrid = new boolean[TOTAL_ROWS][TOTAL_COLUMNS];
			} else {
				fallingGrid = fallingHelps;
			}
		}
	};
	
	//Sets the target for falling gems
	protected void setFallTargets(int column, int countSpaces, int row, Gem current, Iterator<Gem> list) {
		if(current.column == column && current.oldRow >= row) {
			if(current.oldRow == row) {
				current.row = current.oldRow - countSpaces;
				if(list.hasNext()) {
					current = list.next();
					setFallTargets(column, countSpaces, row + 1, current, list);
				}
			} else {
				while(row < current.oldRow) {
					if(gemGrid[row][column] == null) {
						countSpaces++;
					}
					row++;
				}
				setFallTargets(column, countSpaces, row, current, list);
			}
		} else {
			setFallTargets(current.column, 0, 0, current, list);
		}
	}
	
	//Returns whether the top row is empty or not
	protected boolean topRowIsEmpty() {
		for(int i = 0; i < TOTAL_COLUMNS; i++) {
			if(gemGrid[TOTAL_ROWS - 1][i] != null && !gemGrid[TOTAL_ROWS - 1][i].falling) {
				return false;
			}
		}
		return true;
	}
	
	//Calls the appropriate touch event
	@Override
    public boolean onTouchEvent(MotionEvent me) {
		if(fastRow && me.getAction() == MotionEvent.ACTION_UP) {
			fastRow = false;
			return true;
		}
        return gDetector.onTouchEvent(me);
    }
   
	//Determines whether selection is legal 
   protected boolean isLegalSelection(int row, int column) {
	   if (column >= TOTAL_COLUMNS || row >= TOTAL_ROWS || row == -1 || column == -1 
			   		|| gemGrid[row][column] == null
			   		|| fallingGrid[row][column]
					|| gemGrid[row][column].selected
					|| gemGrid[row][column].falling) {
		   sGem.selected = false;
		   return false;
	   } else {
		   sGem.row = row;
		   sGem.column = column;
		   return true;
	   }
   }
   
   //Helper to set the targets of falling gems
   protected void setTargetsHelper() {
	   Iterator<Gem> iteratorOne = ((TreeSet<Gem>) fallingGems).iterator();
	   if(iteratorOne.hasNext()) {
		   Gem g = iteratorOne.next();
		   setFallTargets(g.column, 0, 0, g, iteratorOne);
	   }
   }

   //Checks for matches in the grid
   protected void checkMatches() {
	   List<Gem> matchesList = new ArrayList<Gem>();
	   //Check each row for matches
	   for(int i = 0; i < TOTAL_ROWS; i++) {
		   int count = 1;
		   int col = 0;
		   while(col < TOTAL_COLUMNS && (gemGrid[i][col] == null 
				   || gemGrid[i][col].falling || gemGrid[i][col].selected)) {
					   
			   //If a gem isn't in position to be matched, don't check it
			   col++;
		   }
		   if(col < TOTAL_COLUMNS) {
			   int type = gemGrid[i][col].GEM_TYPE;
			   for(int j = col + 1; j < TOTAL_COLUMNS; j++) {
				   if(gemGrid[i][j] == null || gemGrid[i][j].selected || gemGrid[i][j].falling) {
					   checkRowHelper(i, j, count, matchesList);
					   count = 0;
				   } else if(count == 0) {
					   type = gemGrid[i][j].GEM_TYPE;
					   count = 1;
				   } else {
					   if(type == gemGrid[i][j].GEM_TYPE) {
						   count++;
					   } else {
						   checkRowHelper(i, j, count, matchesList);
						   count = 1;
						   type = gemGrid[i][j].GEM_TYPE;
					   }
				   }
			   }
			   checkRowHelper(i, TOTAL_COLUMNS, count, matchesList);
		   }
	   }
	   for(int i = 0; i < TOTAL_COLUMNS; i++) {
		   int count = 1;
		   int type = -1;
		   if(gemGrid[0][i] != null && !gemGrid[0][i].falling) {
			   if(!gemGrid[0][i].selected) {
				   type = gemGrid[0][i].GEM_TYPE;
			   }
			   for(int j = 1; j < TOTAL_ROWS; j++) {
				   //If the gem is null, move to the next column (as falling/nothing is above)
				   if(gemGrid[j][i] == null || gemGrid[j][i].falling) {
					   checkColumnHelper(i, j, count, matchesList);
					   count = 0;
					   break;
				   //If the gem is selected, set the count to 0, check below
				   } else if(gemGrid[j][i].selected) {
					   checkColumnHelper(i, j, count, matchesList);
					   count = 0;
				   } else if(count == 0) {
					   type = gemGrid[j][i].GEM_TYPE;
					   count = 1;
				   } else {
					   if(type == gemGrid[j][i].GEM_TYPE) {
						   count++;
					   } else {
						   checkColumnHelper(i, j, count, matchesList);
						   count = 1;
						   type = gemGrid[j][i].GEM_TYPE;
					   }
				   }
			   }
			   checkColumnHelper(i, TOTAL_ROWS, count, matchesList);
		   }
	   }
	   Set<Gem> mList = new TreeSet<Gem>(matchesList);
	   Iterator<Gem> i = mList.iterator();
	   while(i.hasNext())
		   i.next().selected = true;
	   invalidate();
	   if(!matchesList.isEmpty()) {
		   matchedGems.add(mList);
		   mHandler.postDelayed(removeMatchesHandler, MATCHED_PAUSE);
        }
   }
   
   //Determines the gems that are falling
   protected void setFallings() {
		boolean isFalling = false;
		if(!fallingGems.isEmpty()) {
			isFalling = true;
		}
		Set<Gem> l = new TreeSet<Gem>();
	    boolean newGems = false;
	    for(int i = 0; i < TOTAL_COLUMNS; i++) {
		   boolean falling = false;
		   for(int j = 0; j < TOTAL_ROWS; j++) {
			   if(gemGrid[j][i] == null || gemGrid[j][i].falling) {
				   falling = true;
			   } else if(gemGrid[j][i].selected) {
				   falling = false;
			   } else if(falling) {
				   newGems = true;
				   Gem g = gemGrid[j][i];
				   g.falling = true;
				   g.column = i;
				   g.oldRow = j;
				   fallingGrid[j][i] = true;
				   g.currY = getRowCoords(j); 
				   if(!fromMatches && canPause(g.oldRow, g.column)) {
					   l.add(g);
				   } else {
					   fallingGems.add(g);
					   gemGrid[g.oldRow][g.column] = null;
				   }
			   }
		   }
	    }
	    if(newGems) {
	    	if(!isFalling) {
	    		gemsFalling = true;
		    	mHandler.removeCallbacks(adjustFalling);
		    	mHandler.post(adjustFalling);
	    	}
	    	resetFirst = true;
	    }
	    if(!l.isEmpty()) {
	    	gemsFalling = true;
	    	pausedGems.add(l);
	    	mHandler.postDelayed(adjustPause, FALLING_PAUSE);
	    }
	   fromMatches = false;
   }
   
   //Returns whether a gem can pause
   protected boolean canPause(int row, int column) {
	   for(int i = row + 1; i < TOTAL_ROWS; i++) {
		   if(fallingGrid[i][column] 
				   || (gemGrid[i][column] != null && gemGrid[i][column].falling)) {
			   return false;
		   }
	   }
	   return true;
   }
   
   //Adds matching gems to List
   protected void checkRowHelper(int i, int j, int count, List<Gem> matches) {
	   if(count > 2) {
		   while(count > 0) {
			   Gem gem = gemGrid[i][j - count];
			   gem.row = i;
			   gem.column = j - count;
			   matches.add(gem);
			   count--;
			   mHandler.removeCallbacks(updateOffset);
		   }
	   }
   }
   
   //Adds matching gems to List
   protected void checkColumnHelper(int i, int j, int count, List<Gem> matches) {
	   if(count > 2) {
		   while(count > 0) {
			   if(!matches.contains(gemGrid[j - count][i])) {
				   Gem gem = gemGrid[j - count][i];
				   gem.row = j - count;
				   gem.column = i;
				   matches.add(gem);
				   mHandler.removeCallbacks(updateOffset);
			   }
			   count--;
		   }
	   }
   }
   
   //Returns coordinates of a given row
   protected int getRowCoords(int row) {
	   return START_Y - offset - row * GEM_TOTAL;
   }
   
   //Determines column of the given location (in motionevent), returns -1 for illegal selection
   protected int getColumn(MotionEvent e) {
	   float xpos = e.getX();
	   int column = (int) ((xpos - START_X) / GEM_TOTAL);
	   if(column < 0) {
		   return -1;
	   } else if(column >= TOTAL_COLUMNS) {
		   return TOTAL_COLUMNS;
	   } else {
		   return column;
	   }
   }
   
   
   //Because the Y-axis begins at 0 and works upwards rather than down,and java rounds ints down,
   //659 (Start_y + gemTotal - 1) must be used instead of 600 to accurately calculate row
   protected int getRow(MotionEvent e) {
	   float ypos = e.getY();
	   int row = (int) ((START_Y - offset + GEM_TOTAL - 1 - ypos) / GEM_TOTAL);
	   if(row < 0 || row >= TOTAL_ROWS) {
		   return -1;
	   } else {
		   return row;
	   }
   }
   
   @Override
   public void onShowPress(MotionEvent e) {
   }    

 //On a downclick, determines whether to select, switch, or deselect gems
   @Override
   public boolean onDown(MotionEvent e) {
	   if(!paused && !gameOver && !fastRow) {
		   int column = getColumn(e);
		   int row = getRow(e);
			   //If the click is in an invalid area, deselect
		   if (column >= TOTAL_COLUMNS || row >= TOTAL_ROWS || row == -1 || column == -1 
				    || fallingGrid[row][column]
			   		|| (gemGrid[row][column] != null
					&& gemGrid[row][column].selected)) {
			   if(sGem.selected) {
				   	sGem.selected = false;
				   	invalidate();
			   }
			//If a gem is selected, see if a switch is possible, and switch. Then check matches. 
		   } else if(sGem.selected) {
			   Gem first = gemGrid[row][column];
			   Gem second = gemGrid[sGem.row][sGem.column];
			   //Must be next to each other, and not the same type in order to switch
			   if(Math.abs(column - sGem.column) == 1 && sGem.row == row
					   								  && !second.equalsType(first)) {
				  moveGems(row, column, first, second);
			   }
		       sGem.selected = false;
		       invalidate();
		   }
		   //None are selected, and it is a legal selection.
		   else if(gemGrid[row][column] != null && !gemGrid[row][column].falling) {
			   sGem.row = row;
		   	   sGem.column = column;
		   	   sGem.selected = true;
		   	   invalidate();
		   } else {
			   sGem.selected = false;
			   invalidate();
		   }
	   }
       return true;
   }
   
   
   //Switches gems
   protected void moveGems(int row, int column, Gem first, Gem second) {
	   if(!fallingGrid[sGem.row][sGem.column] && !fallingGrid[row][column] &&
			   !(row + 1 < TOTAL_ROWS &&
			   fallingGrid[sGem.row + 1][sGem.column] && !fallingGrid[row + 1][column]) &&
			   ((first == null || !first.falling) && second == null || !second.falling)) {
		   resetFirst = true;
		   switchGemHelper(row, column, first, second);
		   if(gemsFalling || !fallingGems.isEmpty()) {
			   if((first != null && first.falling) || (second != null && second.falling)) {
				   toResetFallings = true;
			   }
		   }
		   setTargetsHelper();
	   }
	   sGem.selected = false;
   }
   
   //Switches Gems
   protected void switchGemHelper(int row, int column, Gem first, Gem second) {
	   gemGrid[row][column] = second;
	   gemGrid[sGem.row][sGem.column] = first;
	   setFallings();
	   checkMatches();
	   sGem.selected = false;
   }
   
   //On fling of the screen, react accordingly
   @Override
   public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
	   if(!paused && !gameOver && !fastRow) {
		   int columnA = getColumn(e1);
		   int rowA = getRow(e1);
		   if(isLegalSelection(rowA, columnA)) {
			   int columnB = getColumn(e2);
			   int rowB = getRow(e2);
			   if(rowA == rowB) {
				   Gem second = gemGrid[rowA][columnA];
				   if(columnB < columnA && columnA > 0) {
					   Gem first = gemGrid[rowA][columnA - 1];
					   if(first == null || !first.selected) {
						   moveGems(rowB, columnA - 1, first, second);
					   }
				   } else if (columnB > columnA && columnA + 1 < TOTAL_COLUMNS) {
					   Gem first = gemGrid[rowA][columnA + 1];
					   if(first == null || !first.selected) {
						   moveGems(rowB, columnA + 1, first, second);
					   }
				   }
			   }
			   invalidate();
			   return true;
		   }
	   }
	   invalidate();
       return false;
   }
   
   //Longpress, make gems move quicker
   @Override
   public void onLongPress(MotionEvent e) {
	   if(topRowIsEmpty()) 
		   fastRow = true;
   }
   
   //Guarantees distinguished moves
   @Override
   public boolean onSingleTapUp(MotionEvent e) {
       return true;
   }
   
   @Override
   public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
       return true;
   }
   
   
   //Draws the Screen again
   @Override
   protected void onDraw(Canvas c) {
	   super.onDraw(c);
	   c.drawBitmap(BACKGROUND, 0, 0, null);
	   if(gemGrid[sGem.row][sGem.column] == null) {
		   sGem.selected = false;
	   }
	   c.drawBitmap(TOTAL_BORDER, START_X - 3, START_Y - 3 - GEM_TOTAL * TOTAL_ROWS + (OFFSET_CHANGE - 2), null);
	   if(gemGrid != null) {
		   //Draws each gem
		   for(int i = 0; i < TOTAL_ROWS; i++) {
			   int ypos = getRowCoords(i);
			   for(int j = 0; j < TOTAL_COLUMNS; j++) {
				   if(gemGrid[i][j] != null && !gemGrid[i][j].falling && !gemGrid[i][j].disappeared) {
					   int xpos = j * GEM_TOTAL + START_X;
					   c.drawBitmap(gemGrid[i][j].getGem(), xpos, ypos , null);
					   Log.w("" + gemGrid[i][j].selected, "");
					   if(gemGrid[i][j].selected) {
						   c.drawBitmap(gemGrid[i][j].getBorder(), 
								   xpos - BORDER_WIDTH/2, ypos - BORDER_WIDTH/2 , null);
					   }
				   }
			   }
			}
		}
	    for(Gem g: fallingGems) {
	    	int xpos = g.column * GEM_TOTAL + START_X;
	    	int ypos = g.currY;
	    	c.drawBitmap(g.getGem(), xpos, ypos, null);
	    }
	    for(int i = 0; i < pausedGems.size(); i++) {
	    	for(Gem g: pausedGems.get(i)) {
	    		int xpos = g.column * GEM_TOTAL + START_X;
		    	int ypos = g.currY;
		    	c.drawBitmap(g.getGem(), xpos, ypos, null);
	    	}
	    }
	    //Draws the border for the "selected" gem
		if(sGem.selected) {
			int xpos = sGem.column * GEM_TOTAL + START_X - BORDER_WIDTH/2;
			int ypos = START_Y - 5 - sGem.row * GEM_TOTAL - offset;
			c.drawBitmap(SELECTED_BORDER, xpos, ypos, null);
		}
		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < TOTAL_COLUMNS; j++) {
				Gem g = nextGems[i][j];
				if(g != null) {
					int xpos = j * GEM_TOTAL + START_X;
					int ypos = START_Y + 3 * GEM_TOTAL - (GEM_TOTAL * i) - offset;
					c.drawBitmap(g.getDead(), xpos, ypos, null);
				}
			}
		}
	}
	
    //
	protected Gem[] getNextRow(int types) {
		Gem[] result = new Gem[TOTAL_COLUMNS];
		for(int i = 0; i < TOTAL_COLUMNS; i++) {
			int next = Gem.checkDeadPlacement(i, result, types);
			result[i] = new Gem(next, context);
		}
		return result;
	}
	
	private Runnable loadGridHelper = new Runnable() {
		public void run() {
			Gem g = ((LinkedList<Gem>) firstList).removeFirst();
			fallingGems.add(g);
			System.out.println(fallingGems.size() + "," + firstList.size());
		}
	};
	
	protected void loadGrid(int types) {
		nextGems[0] = getNextRow(types);
		nextGems[1] = getNextRow(types);
		nextGems[2] = getNextRow(types);
		for(int i = 0; i < START_ROWS; i++) {
			for(int j = 0; j < START_COLUMNS; j++) {
				int next = Gem.checkPlacement(i, j, gemGrid, types);
				Gem g = new Gem(next, context);
				gemGrid[i][j] = g;
				g.oldRow = i + 5;
				g.row = i;
				g.falling = true;
				g.column = j;
				g.currY = getRowCoords(g.oldRow);
				firstList.add(g);
				int time = i * 300 + j * 200;
				if(!(i == 0 && j == 0))
					mHandler.postDelayed(loadGridHelper, time);
			}
		}
		gemGrid = new Gem[TOTAL_ROWS][TOTAL_COLUMNS];
		mHandler.postDelayed(updateOffset, START_ROWS * 300 + START_COLUMNS * 30);
		Gem g = ((LinkedList<Gem>) firstList).removeFirst();
		fallingGems.add(g);
		gemsFalling = true;
		mHandler.post(adjustFalling);
	}
	
	//Class to maintain information about the selected Gem
	public class SelectedGem {
		public boolean selected;
		public int row;
		public int column;
		
		public SelectedGem() {
			selected = false;
			this.row = 0;
			this.column = 0;
		}
	}
	
}