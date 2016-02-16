import javafx.scene.Group;
import javafx.scene.paint.Color;

public class TetrisGrid extends Group {
	private TetrisGridCell[][] cells;
	private int width;
	private int height;
	private Location shapeLoc;		
	private TetrisBlockShape currShape;
	
	private final class Location implements Cloneable{
		public int x;
		public int y;
		public Location(int x, int y){
			this.x = x;
			this.y = y;
		}
		public boolean isValid(){
			return (x>=0&&x<height&&y>=0&&y<width);
		}
		public void move(int x, int y)
		{
			this.x += x;
			this.y += y;
		}
		@Override
		public Location clone(){
			Location newLocation = null;
			try {
				newLocation = (Location)super.clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
			return newLocation;
		}
	}
	
	public TetrisGrid(int height, int width) {
		resize(width*20, height*20);
		this.width = width;
		this.height = height;
		cells = new TetrisGridCell[height][width];
		for(int row=0; row<height; row++)
			for(int col=0; col<width; col++){
				cells[row][col] = new TetrisGridCell(col*20, row*20);
				getChildren().add(cells[row][col]);
			}
	}
	
	public void clear(){
		for(int row=0; row<height; row++)
			for(int col=0; col<width; col++){
				cells[row][col].setColor(Color.DARKGRAY);
			}
	}
	
	private void drawBlock(){
		byte[][] blockShape = currShape.getCurrentBlock();
		Location curr = new Location(0,0);
		for(int r=0; r<4; r++){
			curr.x = r+shapeLoc.x;
			for(int c=0; c<4; c++){
				curr.y = c+shapeLoc.y;
				if(blockShape[r][c]==1&&curr.isValid()){
					cells[curr.x][curr.y].setColor(currShape.getColor());
				}
			}
		}
	}
	
	private void eraseBlock(){
		byte[][] blockShape = currShape.getCurrentBlock();
		Location curr = new Location(0,0);
		for(int r=0; r<4; r++){
			curr.x = r+shapeLoc.x;
			for(int c=0; c<4; c++){
				curr.y = c+shapeLoc.y;
				if(blockShape[r][c]==1&&curr.isValid())
					cells[curr.x][curr.y].setColor(Color.DARKGRAY);
			}
		}
	}
	
	public boolean insertShape(TetrisBlockShape currShape){
		this.currShape = currShape;
		int startY = (width-4)/2;
		shapeLoc = new Location(0,startY);
		Location newLoc = new Location(shapeLoc.x, shapeLoc.y);
		if(canMove(newLoc, currShape.getCurrentBlock()) == true) {
			drawBlock();
			return true;
		}
		else
			return false;
	}
	
	// nextGrid의 블록이 중앙에 표시되지 않아 따로 만들어 줬다.
	public boolean insertNextShape(TetrisBlockShape currShape){
		this.currShape = currShape;
		int startY = (width-4)/2;
		shapeLoc = new Location(2,startY);
		drawBlock();
		return true;
	}
	
	
	private boolean canMove(Location newLoc, byte[][] blockShape){
		for(int r=0; r<4; r++){
			int x = r+newLoc.x;
			for(int c=0; c<4; c++){
				int y = c+newLoc.y;
				if( (blockShape[r][c]==1) && !(x>=0&&x<height&&y>=0&&y<width)) // (1)
					return false;
				if(blockShape[r][c]==1 && !cells[x][y].isEmpty()) // (2)
					return false;
			}
		}
		return true;
	}
	// reFactoring moveShape
	public boolean moveShape(int x, int y) {
		boolean canMove = false;
		eraseBlock();
		Location newLoc = new Location(shapeLoc.x+x, shapeLoc.y+y);
		if(canMove(newLoc, currShape.getCurrentBlock()) == true) {
			shapeLoc.move(x, y);
			canMove = true;
		}
		drawBlock();
		return canMove;
	}
	public boolean moveShapeDown() {
		return moveShape(1,0);
	}

	public void moveShapeLeft() {
		if(moveShape(0,-1)) Sound.play("Move");
	}
	
	public void moveShapeRight(){
		if(moveShape(0,1)) Sound.play("Move");
	}
	
	public void rotateShape(){
		eraseBlock();
		Location newLoc = new Location(shapeLoc.x, shapeLoc.y);
		currShape.rotate();
		if(canMove(newLoc, currShape.getCurrentBlock()) == true)
			Sound.play("Rotate");
		else 
			currShape.rotateBack();
		drawBlock();
	}

	public void moveShapeToBottom(){
		Sound.play("HardDrop");
		while(true) {
			if(moveShapeDown()==false)
				break;
		}
	}
	
	public Color getColor(int x, int y) {
		return cells[x][y].getColor();
	}
	
	private boolean isEmptyRow(int row){
		int sum = 0;
		for(int i=0; i<width; i++)
			if(!cells[row][i].isEmpty())
				sum++;
		if(sum == 10) return true;
		else return false;
		
	}
	public void pullDownLine(int row) {
		for(int i=row; i>2; i--) {
			for(int j=0; j<width; j++) {
				cells[i][j].setColor(cells[i-1][j].getColor());
			}
		}
		
	}
	public int removeFullRow(){
		int combo = 0;
		for(int i=height-1; i>2; i--) {
			if(isEmptyRow(i)) {
				pullDownLine(i);
				combo++;
				i++;
			}	
		}
		return combo;
	}
	public void repaint(){	
		for(int row=0; row<height; row++)
			for(int col=0; col<width; col++){
				cells[row][col].draw();
			}
	}
}
