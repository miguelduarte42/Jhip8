/**
 * 
 * Saves the current pressed keys. The keys range from 0 through 9 and A through F.
 * 
 * @author miguelduarte
 */
public class Input {
	
	int[] keys = new int[0xF+1];
	public boolean keyPressed = false;
	
	public void keyPressed(int key) {
		keys[key] = 1;
		keyPressed = true;
	}
	
	public void keyReleased(int key) {
		keys[key] = 0;
	}

	public boolean isPressed(int key) {
		return keys[key] == 1;
	}

}
