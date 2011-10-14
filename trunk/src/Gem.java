package com.cody.android.gemsrising;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.util.Random;


public class Gem implements Comparable<Gem> {
	public final int GEM_TYPE;
	public static Bitmap[] GEM_ARRAY;
	public static Bitmap[] BORDER_ARRAY;
	public static Bitmap[] DEAD_ARRAY;
	public boolean falling;
	public boolean selected; //whether the gem is matched or not
	public boolean disappeared; //whether the gem has disappeared after matching, but isn't removed yet.
								//Makes the gems above matches fall all at once.
	public int row;
	public int column;
	public int oldRow;
	public int currY; //Used for falling
	
	public Gem(int type, Context context) {
		Resources res = context.getResources();
		if(GEM_ARRAY == null) {
			GEM_ARRAY = new Bitmap[] {BitmapFactory.decodeResource(res, R.drawable.red),
				         BitmapFactory.decodeResource(res, R.drawable.blue),
						 BitmapFactory.decodeResource(res, R.drawable.green),
						 BitmapFactory.decodeResource(res, R.drawable.yellow),
						 BitmapFactory.decodeResource(res, R.drawable.purple),
						 BitmapFactory.decodeResource(res, R.drawable.orange)
						};
			
			BORDER_ARRAY = new Bitmap[] {BitmapFactory.decodeResource(res, R.drawable.red_border),
			         BitmapFactory.decodeResource(res, R.drawable.blue_border),
					 BitmapFactory.decodeResource(res, R.drawable.green_border),
					 BitmapFactory.decodeResource(res, R.drawable.yellow_border),
					 BitmapFactory.decodeResource(res, R.drawable.purple_border), 
					 BitmapFactory.decodeResource(res, R.drawable.orange_border)
					};
			
			DEAD_ARRAY = new Bitmap[] {BitmapFactory.decodeResource(res, R.drawable.dead_red),
			         BitmapFactory.decodeResource(res, R.drawable.dead_blue),
					 BitmapFactory.decodeResource(res, R.drawable.dead_green),
					 BitmapFactory.decodeResource(res, R.drawable.dead_yellow),
					 BitmapFactory.decodeResource(res, R.drawable.dead_purple),
					 BitmapFactory.decodeResource(res, R.drawable.dead_orange)
					};
			
		}
		GEM_TYPE = type;
		selected = false;
	}
	
	public boolean equalsType(Gem other) {
		return other != null && this.GEM_TYPE == other.GEM_TYPE;
	}
	
	public boolean equalsType(int num) {
		return this.GEM_TYPE == num;
	}
	
	private static int placementHelper(int first, int second, int types) {
		Random r = new Random();
		int num = r.nextInt(types);
		while(num == first || num == second)
			num = r.nextInt(types);
		return num;
	}
	
	//Guarantees the placement of new gems is not illegal (no 3 in a row)
	public static int checkPlacement(int curRow, int curCol, Gem[][] grid, int types) {
		int first = -1;
		int second = -1;
		if(curCol >= 2 && grid[curRow][curCol - 1].equalsType(grid[curRow][curCol - 2])) {
			first = grid[curRow][curCol - 1].GEM_TYPE;
		}
		if(curRow >=2 && grid[curRow - 1][curCol].equalsType(grid[curRow - 2][curCol])) {
			second = grid[curRow - 1][curCol].GEM_TYPE;
		}
		return placementHelper(first, second, types);
	}
	
	public static int checkDeadPlacement(int curCol, Gem[] grid, int types) {
		int first = -1;
		if(curCol >= 2) {
			if(grid[curCol - 2].equalsType(grid[curCol - 2])) {
				first = grid[curCol - 1].GEM_TYPE;
			}
		}
		return placementHelper(first, -1, types);
	}
	
	public Bitmap getGem() {
		return GEM_ARRAY[GEM_TYPE];
	}
	
	public Bitmap getBorder() {
		return BORDER_ARRAY[GEM_TYPE];
	}
	
	public Bitmap getDead() {
		return DEAD_ARRAY[GEM_TYPE];
	}

	//used primarily for falling gems
	@Override
	public int compareTo(Gem other) {
		if(this.column != other.column) {
			return this.column - other.column;
		} else if(this.falling || other.falling) {
			return other.currY - this.currY;
		}
		return other.row - this.row;
	}
}
