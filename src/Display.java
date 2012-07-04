import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * This class handles the display features of Chip-8. The monochrome display
 * has a resolution of 64x32 and a refresh rate of 60Hz. I'm not particularly
 * happy with the input handling, but I don't feel like tweaking it :) 
 * 
 * @author miguelduarte
 */
public class Display extends Canvas{
	
	public static int WIDTH = 63+1;
	public static int HEIGHT = 32+1;
	public static int SCALE = 15;
	private short[][] values = new short[HEIGHT][WIDTH];
	private Input input;
	
	public Display(Input i) {
		this.input = i;
		
		setSize(WIDTH*SCALE,HEIGHT*SCALE);
		setupInput();
		requestFocus();
		new CanvasRefresher(this).start();
	}
	
	@Override
	public void paint(Graphics g) {
		for(int i = 0 ; i < values.length ; i++) {
			for(int j = 0 ; j < values[i].length ; j++) {
				if(values[i][j] > 0)
					g.setColor(Color.WHITE);
				else
					g.setColor(Color.BLACK);
				
				g.fillRect(j*SCALE, i*SCALE, SCALE, SCALE);
			}
		}
	}

	public boolean draw(short x, short y, short[] sprite) {
		
		boolean erased = false;
		
		int initY = y;
        int initX = x;

        for (int i = 0; i < sprite.length; i++) {
        	
        	String line = Integer.toBinaryString(sprite[i]);
        	
        	while(line.length() < 8)
        		line = '0'+line;
            
            for (int px = 0; px < line.length(); px++) {
                if (line.charAt(px) == '1') {
                	
                	int indexY = (initY + i) % HEIGHT;
                	int indexX = (initX + px) % WIDTH;
                    
                    values[indexY][indexX] ^= 1;
                    if(values[indexY][indexX] == 0)
                    	erased = true;
                }
            }
        }
        
        return erased;
	}
	
	public void clear() {
		values = new short[HEIGHT][WIDTH];
	}
	
	/**
	 * Listen for the keys between 0 and F (hex) and add/remove them from the Input object
	 */
	private void setupInput() {
		addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent arg0) {}
			public void keyReleased(KeyEvent arg0) {
				char c = arg0.getKeyChar();
				
				int key = 0xF+1;

				if(c >= '0' && c <= '9') {
					key = c - '0';					
				}else {
					int tempKey = c - 'a' + 10;
				
					if(tempKey > 0 && tempKey <= 0xF)
						key = tempKey;
				}
				
				if(key >= 0 && key <= 0xF)
					input.keyReleased(key);
			}
			public void keyPressed(KeyEvent arg0) {
				char c = arg0.getKeyChar();
				
				int key = 0xF+1;

				if(c >= '0' && c <= '9') {
					key = c - '0';					
				}else {
					int tempKey = c - 'a' + 10;
				
					if(tempKey > 0 && tempKey <= 0xF)
						key = tempKey;
				}
				
				if(key >= 0 && key <= 0xF)
					input.keyPressed(key);
			}
		});
	}
	
	/**
	 * 
	 * @author miguelduarte
	 *
	 */
	private class CanvasRefresher extends Thread{
		
		private Canvas canvas;
		private static final int REFRESH_RATE = 1000/60; //60Hz
		
		public CanvasRefresher(Canvas c) {
			canvas = c;
		}
		
		@Override
		public void run() {
			while(true) {
				try{
					canvas.repaint();
					Thread.sleep(REFRESH_RATE); //60Hz
				}catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
