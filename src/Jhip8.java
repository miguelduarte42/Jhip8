/**
 * 
 * Jhip8 is a simple interpreter for the Chip-8 language.
 * It implements every opcode and handles graphic and sound features.
 * It is bundled with the Space Invaders game, but it should be able
 * to run any Chip-8 game. Since this was just an experimental project, 
 * I didn't implement fancy features. If you want to try a different game,
 * just put it on the same folder as the project and change the ROM_FILE variable.
 * 
 * More info on Chip-8 can be found at: http://en.wikipedia.org/wiki/CHIP-8
 * 
 * @author miguelduarte (http://miguelduarte.pt)
 */
public class Jhip8 {
	
	private Window window;
	private Display display;
	private Processor processor;
	private Input input = new Input();
	private static String ROM_FILE = "INVADERS";
	
	public Jhip8() {
		this.display = new Display(input);
		this.window = new Window(display);
		this.processor = new Processor(this);
		processor.loadFile(ROM_FILE);
		processor.start();
	}
	
	public static void main(String[] args) {
		new Jhip8();
	}
	
	public Processor getProcessor() {
		return processor;
	}
	
	public Display getDisplay() {
		return display;
	}
	
	public Input getInput() {
		return input;
	}
	
	public boolean draw(short x, short y, short[] sprite) {
		return display.draw(x, y, sprite);
	}
}
