import javafx.scene.paint.Color;

public class TetrisBlockShape {
	private Color color;
	private byte[][][] block;
	private int formIndex  = 0;
	private int prevForm;
	public TetrisBlockShape(byte[][][] block, Color color){
		this.block = block;
		this.color = color;
	}
	public void rotate(){
		prevForm = formIndex;
		formIndex = (formIndex+1) % block.length;
	}
	public void rotateBack() {
		formIndex = prevForm;
	}
	public byte[][] getCurrentBlock(){
		return block[formIndex];
	}
	public byte[][] getNextBlock(){
		int nextIndex = (formIndex+1) % block.length;
		return block[nextIndex];
	}
	public Color getColor(){
		return color;
	}
}
