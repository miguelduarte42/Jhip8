import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Random;

/**
 * The Processor class is responsible for decoding the opcodes and executing the instructions.
 * For a full list of the opcodes and what they mean, see the following URL:
 * 
 * http://devernay.free.fr/hacks/chip8/C8TECH10.HTM
 * 
 * @author miguelduarte
 */
public class Processor extends Thread{
	
	/*
	 * Since there are no unsigned variable types in Java, we need to use the next biggest value type.
	 * If you need a byte (8 bits), use a short (16 bits). If you need 16 bits, use an int (32 bits)
	 */
	public short[] memory;
	public int[] stack = new int[0xF+1];
	public short[] V = new short[0xF+1];//Registers
	public int I;//Auxiliary pointer to memory
	public int PC = 0x200;//Program counter
	public short SP;//Stack pointer
	public int DT;//Delay timer
	public int ST;//Sound timer
	private Jhip8 emulator;
	
	private static int MEMORY_SIZE = 0xFFF;
	private static final int REFRESH_RATE = 1000/60; //60Hz
	
	public Processor(Jhip8 emulator) {
		this.emulator = emulator;
		initMemory();
	}
	
	@Override
	public void run() {
		
		try{
			new Timers().start();
		
			while(PC < 0xFFF) {
				int opcode = memory[PC++] << 8 | memory[PC++];
				decode(opcode);					
				
				Thread.sleep(1);
			}
		}catch(InterruptedException e) {
			//let the processor die
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void loadFile(String file) {
		try{
			File f = new File(file);
			
			int addr = PC;
			int b = -1;
			
			InputStream in = new FileInputStream(f);
			
			do{
				b = in.read();
				if (b != -1) {
					memory[addr++] = (short)(b & 0xFF);
				}
			} while (b != -1);
		}catch(Exception e) {
			System.out.println("Problem reading file "+file);
		}
	}
	
	/**
	 * Receives an opcode and executes the corresponding instruction.
	 */
	public void decode(int i) {
		
		if(i == 0x00E0) { // CLS (0x00E0)
			/*
			 * Clear the display.
			 */
			emulator.getDisplay().clear();
			
		} else if(i == 0x00EE) { // RET (0x00EE)
			/*
			 * The interpreter sets the program counter to the address at the top of the stack,then
			 * subtracts 1from the stack pointer.
			 */
			PC = stack[SP--];
			
		}else if ((i & 0xF000) == 0x1000) { // JMP (0x1nnn)
			/*
			 * The interpreter sets the program counter to nnn.
			 */
			short nnn = (short)(i & 0xFFF);
			PC = nnn;
			
		} else if ((i & 0xF000) == 0x2000) { // CALL (0x2nnn)
			/*
			 * The interpreter increments the stack pointer, then puts the current PC
			 * on the top of the stack. The PC is then set to nnn.
			 */
			short nnn = (short)(i & 0xFFF);
			stack[++SP] = PC;
			PC = nnn;
		
		} else if ((i & 0xF000) == 0x3000) { //SE Vx, kk (0x3xkk)
			/*
			 * The interpreter compares register Vx to kk, and if they are equal, increments the program counter by 2.
			 */
			short x = (short)((i & 0xF00) >>> 4*2);
			short kk = (short)(i & 0xFF);
			
			if(V[x] == kk)
				PC+=2;
			
		} else if ((i & 0xF000) == 0x4000) { //SNE Vx, kk (0x4xkk)
			/*
			 * The interpreter compares register Vx to kk, and if they are not equal, increments the program counter by 2.
			 */
			short x = (short)((i & 0xF00) >>> 4*2);
			short kk = (short)(i & 0xFF);
			
			if(V[x] != kk)
				PC+=2;
			
		} else if ((i & 0xF00F) == 0x5000) { //SNE Vx, Vy (0x5xy0)
			/*
			 * The interpreter compares register Vx to register Vy, and if they are equal, increments the program counter by 2.
			 */
			short x = (short)((i & 0xF00) >>> 4*2);
			short y = (short)((i & 0xF0) >>> 4);
				
			if(V[x] == V[y])
				PC+=2;
			
		} else if ((i & 0xF000) == 0x6000) { //LD Vx, kk (0x6xkk)
			/*
			 * The interpreter puts the value kk into register Vx
			 */
			short x = (short)((i & 0xF00) >>> 4*2);
			short kk = (short)(i & 0xFF);
			V[x] = kk;
			
		} else if ((i & 0xF000) == 0x7000) { //ADD Vx, kk (0x7xkk)
			/*
			 * Adds the value kk to the value of register Vx, then stores the result in Vx.
			 */
			short x = (short)((i & 0xF00) >>> 4*2);
			short kk = (short)(i & 0x0FF);
			
			V[x]= (short)((V[x] + kk) & 0xFF);

		} else if ((i & 0xF00F) == 0x8000) { //LD Vx, Vy (0x8xy0)
			/*
			 * Stores the value of register Vy in register Vx.
			 */
			short x = (short)((i & 0xF00) >>> 4*2);
			short y = (short)((i & 0xF0) >>> 4);
			
			V[x] = V[y];
		} else if ((i & 0xF00F) == 0x8001) { //OR Vx, Vy (0x8xy1)
			/*
			 * Performs a bitwise OR on the values of Vx and Vy, then stores the result in Vx.
			 * A bitwise OR compares the corrseponding bits from two values, and if either bit is 1,
			 * then the same bit in the result is also 1. Otherwise, it is 0.
			 */
			short x = (short)((i & 0xF00) >>> 4*2);
			short y = (short)((i & 0xF0) >>> 4);
			V[x] = (short)(V[x] | V[y]);
			
		} else if ((i & 0xF00F) == 0x8002) { //AND Vx, Vy (0x8xy2)
			/*
			 * Performs a bitwise AND on the values of Vx and Vy, then stores the result in Vx.
			 * A bitwise AND compares the corresponding bits from two values, and if both bits are 1,
			 * then the same bit in the result is also 1. Otherwise, it is 0
			 */	
			short x = (short)((i & 0xF00) >>> 4*2);
			short y = (short)((i & 0x0F0) >>> 4);
			V[x] = (short)(V[x] & V[y]);
				
		} else if ((i & 0xF00F) == 0x8003) { //XOR Vx, Vy (0x8xy3)
			/*
			 * Performs a bitwise exclusive OR on the values of Vx and Vy, then stores the result in Vx.
			 * An exclusive OR compares the corresponding bits from two values, and if the bits are not both the same,
			 * then the corresponding bit in the result is set to 1. Otherwise, it is 0.
			 */
			short x = (short)((i & 0xF00) >>> 4*2);
			short y = (short)((i & 0xF0) >>> 4);
			V[x] = (short)(V[x] ^ V[y]);

		} else if ((i & 0xF00F) == 0x8004) { //ADD Vx, Vy Carry (0x8xy4)
			/*
			 * The values of Vx and Vy are added together.
			 * If the result is greater than 8 bits (i.e., > 255,) VF is set to 1, otherwise 0.
			 * Only the lowest 8 bits of the result are kept, and stored in Vx. 
			 */
			short x = (short)((i & 0xF00) >>> 4*2);
			short y = (short)((i & 0xF0) >>> 4);
			
			int sum = V[x] + V[y];
			
			if(sum > 0xFF)
				V[0xF] = 1;
			else
				V[0xF] = 0;
			
			V[x] = (short)(sum & 0xFF);
				
		} else if ((i & 0xF00F) == 0x8005) { //SUB Vx, Vy (0x8xy5)
			/*
			 * If Vx > Vy, then VF is set to 1, otherwise 0. Then Vy is subtracted from Vx, and the results stored in Vx.
			 */
			short x = (short)((i & 0xF00) >>> 4*2);
			short y = (short)((i & 0xF0) >>> 4);
			
			if(V[x] > V[y])
				V[0xF] = 1;
			else
				V[0xF] = 0;
			
			short sub = (short)((V[x] - V[y]) & 0xFF);
			
			V[x] = sub;

		} else if ((i & 0xF00F) == 0x8006) { //SHR Vx {, Vy} (0x8xy6)
			/*
			 * If the least-significant bit of Vx is 1, then VF is set to 1, otherwise 0. Then Vx is divided by 2.	
			 */
			short x = (short)((i & 0xF00) >>> 4*2);
				
			byte lsb = (byte)(V[x] & 0x1);
			
			if(lsb == 1)
				V[0xF] = 1;
			else
				V[0xF] = 0;
			
			V[x]= (short)(V[x] >>> 1);

		} else if ((i & 0xF00F) == 0x8007) { //SUBN Vx, Vy (0x8xy7)
			/*
			 * If Vy > Vx, then VF is set to 1, otherwise 0. Then Vx is subtracted from Vy, and the results stored in Vx.
			 */
			short x = (short)((i & 0xF00) >>> 4*2);
			short y = (short)((i & 0xF0) >>> 4);
			
			if(V[y] > V[x])
				V[0xF] = 1;
			else
				V[0xF] = 0;
			
			short sub = (short)(V[y] - V[x]);
			
			V[x] = (short)(sub & 0xFF);
			
		} else if ((i & 0xF00F) == 0x800E) { //SHL Vx {, Vy}
			/*
			 * If the most-significant bit of Vx is 1, then VF is set to 1, otherwise to 0. Then Vx is multiplied by 2.
			 */
			short x = (short)((i & 0xF00) >>> (4*2));
			
			byte lsb = (byte)(V[x] >>> 7);
			
			if(lsb == 0x1)
				V[0xF] = 1;
			else
				V[0xF] = 0;
			
			V[x] = (short)((V[x] << 1) & 0xFF);
			
		} else if ((i & 0xF00F) == 0x9000) { //SNE Vx, Vy (0x9xy0)
			/*
			 * The values of Vx and Vy are compared, and if they are not equal, the program counter is increased by 2.
			 */	
			short x = (short)((i & 0xF00) >>> (4*2));
			short y = (short)((i & 0xF0) >>> 4);
			
			if(V[x] != V[y])
				PC+=2;

		} else if ((i & 0xF000) == 0xA000) { //LD I, addr (0xAnnn)
			/*
			 * The value of register I is set to nn
			 */
			I = (short)(i & 0xFFF);

		} else if ((i & 0xF000) == 0xB000) { //JP V0, addr (0xBnnn)
			/*
			 * The program counter is set to nnn plus the value of V0.	
			 */
			PC = (short)(i & 0xFFF)+V[0];
				
		} else if ((i & 0xF000) == 0xC000) { //RND Vx, byte (0xCxkk)
			/*
			 * The interpreter generates a random number from 0 to 255, which is then ANDed with the value kk.
			 * The results are stored in Vx. See instruction 8xy2 for more information on AND.
			 */
			short x = (short)((i & 0xF00) >>> 4*2);
			short kk = (short)(i & 0x0FF);
			
			V[x] = (short)((new Random().nextInt(256)) & kk);
			
		} else if ((i & 0xF000) == 0xD000) { //DRW Vx, Vy, nibble (0xDxyn)
			/*
			 * The interpreter reads n bytes from memory, starting at the address stored in I. These bytes are
			 * then displayed as sprites on screen at coordinates (Vx, Vy). Sprites are XORed onto the existing screen.
			 * If this causes any pixels to be erased, VF is set to 1, otherwise it is set to 0. If the sprite is
			 * positioned so part of it is outside the coordinates of the display, it wraps around to the opposite
			 * side of the screen. See instruction 8xy3 for more information on XOR, and section 2.4, Display, for
			 * more information on the Chip-8 screen and sprites.
			 */
			short x = (short)((i & 0xF00) >>> 4*2);
			short y = (short)((i & 0xF0) >>> 4);
			short n = (short)(i & 0xF);
			
			short[] sprite = new short[n];
	        int count = 0;
	        
	        for (int j = I; j < I + n; j++)
	            sprite[count++] = memory[j];
	        
	        boolean erased = emulator.draw(V[x], V[y], sprite);
	        
	        V[0xF] = (short)(erased ? 1 : 0);
				
		} else if ((i & 0xF0FF) == 0xE09E) { //SKP Vx (0xEx9E)
			/*
			 * Checks the keyboard, and if the key corresponding to the value of Vx is currently in the down position, PC is increased by 2.
			 */
			short x = (short)((i & 0xF00) >>> (4*2));
			
			if(emulator.getInput().isPressed(V[x]))
				PC+=2;
				
		} else if ((i & 0xF0FF) == 0xE0A1) { //SKNP Vx (0xExA1)
			/*
			 * Checks the keyboard, and if the key corresponding to the value of Vx is currently in the up position, PC is increased by 2.
			 */
			short x = (short)((i & 0xF00) >>> (4*2));
			
			if(!emulator.getInput().isPressed(V[x]))
				PC+=2;
			
		} else if ((i & 0xF0FF) == 0xF007) { //LD Vx, DT (0xFx07)	
			/*
			 * The value of DT is placed into Vx.
			 */
			short x = (short)((i & 0xF00) >>> 4*2);
			
			V[x] = (short)(DT & 0xFF);

		} else if ((i & 0xF0FF) == 0xF00A) { //LD Vx, K (0xFx0A)
			/*
			 * All execution stops until a key is pressed, then the value of that key is stored in Vx.
			 */
			short x = (short)((i & 0xF00) >>> 4*2);
			
			emulator.getInput().keyPressed = false;
			while(!emulator.getInput().keyPressed);
			
			for(int j = 0 ; j <= 0xF ; j++) {
				if(emulator.getInput().keys[j] == 1) {
					V[x] = (short)j;
					break;
				}
			}
		} else if ((i & 0xF0FF) == 0xF015) { //LD DT, Vx (0xFx15)
			/*
			 * DT is set equal to the value of Vx.
			 */
			short x = (short)((i & 0xF00) >>> 4*2);
			DT = V[x];

		} else if ((i & 0xF0FF) == 0xF018) { //LD ST, Vx (0xFx18)
			/*
			 * ST is set equal to the value of Vx.
			 */
			short x = (short)((i & 0xF00) >>> 4*2);
			ST = V[x];
			
		} else if ((i & 0xF0FF) == 0xF01E) { //ADD I, Vx (0xFx1E)
			/*
			 * The values of I and Vx are added, and the results are stored in I.
			 */
			short x = (short)((i & 0xF00) >>> 4*2);
			I = (I+V[x]) & 0xFFF;

		} else if ((i & 0xF0FF) == 0xF029) { //LD F, Vx (0xFx29)
			/*
			 * The value of I is set to the location for the hexadecimal sprite corresponding to the value of Vx.
			 * See section 2.4, Display, for more information on the Chip-8 hexadecimal font.
			 */	    
			short x = (short)((i & 0xF00) >>> 4*2);
			I = V[x]*5;
				
		} else if ((i & 0xF0FF) == 0xF033) { //LD B, Vx (0xFx33)
			/*
			 * The interpreter takes the decimal value of Vx, and places the hundreds digit in memory at location in I,
			 * the tens digit at location I+1, and the ones digit at location I+2.
			 */
			short x = (short)((i & 0xF00) >>> 4*2);
	        
	        memory[I] =  (short)(V[x] / 100);
	        memory[I+1] = (short)((V[x] - memory[I]) / 10);
	        memory[I+2] = (short)(V[x] - memory[I] - memory[I+1]);
			
		} else if ((i & 0xF0FF) == 0xF055) { //LD [I], Vx (0xFx55)
			/*
			 * The interpreter copies the values of registers V0 through Vx into memory, starting at the address in I.
			 */
			short x = (short)((i & 0xF00) >>> 4*2);
			for(int j = 0 ; j <= x ; j++)
				memory[I+j] = V[j];

		} else if ((i & 0xF0FF) == 0xF065) { //LD Vx, [I] (0xFx65)
			/*
			 * The interpreter reads values from memory starting at location I into registers V0 through Vx.
			 */
			short x = (short)((i & 0xF00) >>> 4*2);
			
			for(int j = 0 ; j <= x ; j++)
				V[j] = memory[I+j];
		}
	}
	
	public void initMemory() {
    	memory = new short[MEMORY_SIZE];
    	memory[0x000] = 0xF0;
        memory[0x001] = 0x90;
        memory[0x002] = 0x90;
        memory[0x003] = 0x90;
        memory[0x004] = 0xF0;
        //1 SPRITE
        memory[0x005] = 0x20;
        memory[0x006] = 0x60;
        memory[0x007] = 0x20;
        memory[0x008] = 0x20;
        memory[0x009] = 0x70;
        //2 SPRITE
        memory[0x00A] = 0xF0;
        memory[0x00B] = 0x10;
        memory[0x00C] = 0xF0;
        memory[0x00D] = 0x80;
        memory[0x00E] = 0xF0;
        //3 SPRITE
        memory[0x00F] = 0xF0;
        memory[0x010] = 0x10;
        memory[0x011] = 0xF0;
        memory[0x012] = 0x10;
        memory[0x013] = 0xF0;
        //4 SPRITE
        memory[0x014] = 0x90;
        memory[0x015] = 0x90;
        memory[0x016] = 0xF0;
        memory[0x017] = 0x10;
        memory[0x018] = 0x10;
        //5 SPRITE
        memory[0x019] = 0xF0;
        memory[0x01A] = 0x80;
        memory[0x01B] = 0xF0;
        memory[0x01C] = 0x10;
        memory[0x01D] = 0xF0;
        //6 SPRITE
        memory[0x01E] = 0xF0;
        memory[0x01F] = 0x80;
        memory[0x020] = 0xF0;
        memory[0x021] = 0x90;
        memory[0x022] = 0xF0;
        //7 SPRITE
        memory[0x023] = 0xF0;
        memory[0x024] = 0x10;
        memory[0x025] = 0x20;
        memory[0x026] = 0x40;
        memory[0x027] = 0x40;
        //8 SPRITE
        memory[0x028] = 0xF0;
        memory[0x029] = 0x90;
        memory[0x02A] = 0xF0;
        memory[0x02B] = 0x90;
        memory[0x02C] = 0xF0;
        //9 SPRITE
        memory[0x02D] = 0xF0;
        memory[0x02E] = 0x90;
        memory[0x02F] = 0xF0;
        memory[0x030] = 0x10;
        memory[0x031] = 0xF0;
        //A SPRITE
        memory[0x032] = 0xF0;
        memory[0x033] = 0x90;
        memory[0x034] = 0xF0;
        memory[0x035] = 0x90;
        memory[0x036] = 0x90;
        //B SPRITE
        memory[0x037] = 0xE0;
        memory[0x038] = 0x90;
        memory[0x039] = 0xE0;
        memory[0x03A] = 0x90;
        memory[0x03B] = 0xE0;
        //C SPRITE
        memory[0x03C] = 0xF0;
        memory[0x03D] = 0x80;
        memory[0x03E] = 0x80;
        memory[0x03F] = 0x80;
        memory[0x040] = 0xF0;
        //D SPRITE
        memory[0x041] = 0xE0;
        memory[0x042] = 0x90;
        memory[0x043] = 0x90;
        memory[0x044] = 0x90;
        memory[0x045] = 0xE0;
        //E SPRITE
        memory[0x046] = 0xF0;
        memory[0x047] = 0x80;
        memory[0x048] = 0xF0;
        memory[0x049] = 0x80;
        memory[0x04A] = 0xF0;
        //F SPRITE
        memory[0x04B] = 0xF0;
        memory[0x04C] = 0x80;
        memory[0x04D] = 0xF0;
        memory[0x04E] = 0x80;
        memory[0x04F] = 0x80;
    }
	
	/**
	 * Decreases the Delay and Sound timers if their value is greater than 0, at a rate of 60Hz.
	 * 
	 * @author miguelduarte
	 */
	class Timers extends Thread{
		
		public void run() {
			
			while(true) {
				try {
					if(DT > 0) DT--;
					if(ST > 0) ST--;
					Thread.sleep(REFRESH_RATE);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
