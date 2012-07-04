import javax.swing.JFrame;

public class Window extends JFrame {
	
	private static int PADDING = 22;
	
	public Window(Display display) {
		super("Jhip8");
		add(display);
		setSize(Display.WIDTH*Display.SCALE,Display.HEIGHT*Display.SCALE+PADDING);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setVisible(true);
	}
}
