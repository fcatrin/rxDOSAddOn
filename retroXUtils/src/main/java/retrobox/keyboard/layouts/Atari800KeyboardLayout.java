package retrobox.keyboard.layouts;

import retrobox.keyboard.KeyboardLayout;
import retrobox.keyboard.KeyboardView;

public class Atari800KeyboardLayout {

	String labels_a1[] = {"Esc", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "<", ">", "Back	", "Brk"};
	
	String labels_a2[] = {"Tab", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "-", "=", "Return" };
	String labels_a3[] = {"Ctrl", "A", "S", "D", "F", "G", "H", "J", "K", "L", ";", "+", "*"};
	String labels_a4[] = {"Shift", "Z", "X", "C", "V", "B", "N", "M", ",", ".", "/", "Shift"};
	String labels_a5[] = {"Move", "[Fn]", "Space", "Inv"};
	
	String labels_b1[] = {"Reset", "Option", "Select", "Start", "Help"};
	String labels_b2[] = {"Move", "J1 Btn", "J1 Up", "J1 Dn", "J1 Lt", "J1 Rt"};
	String labels_b3[] = {"[abc]", "J2 Btn", "J2 Up", "J2 Dn", "J2 Lt", "J2 Rt"};
	
				
	String codes_a1[] = {"ATR_ESCAPE", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "ATR_LESS_THAN", "ATR_MORE_THAN", "ATR_BACKSPACE", "ATR_BREAK"};
	String codes_a2[] = {"ATR_TAB", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "ATR_MINUS", "ATR_EQUALS", "ATR_RETURN"};
	String codes_a3[] = {"ATR_CTRL", "A", "S", "D", "F", "G", "H", "J", "K", "L", "ATR_SEMICOLON", "ATR_PLUS", "ATR_STAR"};
	String codes_a4[] = {"ATR_LEFTSHIFT", "Z", "X", "C", "V", "B", "N", "M", "ATR_COMMA", "ATR_DOT", "ATR_SLASH", "ATR_RIGHTSHIFT"};
	String codes_a5[] = {"", "", "ATR_SPACE","ATR_INVERSE"};
	
	String codes_b1[] = {"ATR_RESET", "ATR_OPTION", "ATR_SELECT", "ATR_START", "ATR_HELP"};
	String codes_b2[] = {"","ATR_TRIGGER", "ATR_UP", "ATR_DOWN", "ATR_LEFT", "ATR_RIGHT"};
	String codes_b3[] = {"","ATR_TRIGGER2", "ATR_UP2", "ATR_DOWN2", "ATR_LEFT2", "ATR_RIGHT2"};
			

	public KeyboardLayout[] getKeyboardLayout() {
		
		String all_codes[][] = {
				codes_a1, codes_a2, codes_a3, codes_a4, codes_a5,
				codes_b1, codes_b2, codes_b3
		};
		
		for(String codes[] : all_codes) {
			for(int i=0; i<codes.length; i++) {
				if (codes[i].length()!=0 && !codes[i].startsWith("ATR_")) {
					codes[i] = "ATR_" + codes[i];
				}
			}
		}
		
		KeyboardLayout layout[] = new KeyboardLayout[2];
		
		KeyboardLayout kl = new KeyboardLayout();
		kl.addRow(labels_a1, codes_a1);
		kl.addRow(labels_a2, codes_a2);
		kl.addRow(labels_a3, codes_a3);
		kl.addRow(labels_a4, codes_a4);
		kl.addRow(labels_a5, codes_a5);

		kl.setKeySize("Space", 8);
		kl.setKeyCode("[Fn]", KeyboardView.SWITCH_LAYOUT + 1);
		kl.setKeyCode("Move", KeyboardView.TOGGLE_POSITION);
		kl.setKeySize("Tab", 2);
		kl.setKeySize("Ctrl", 2);
		kl.setKeySize("Return", 2);
		kl.setKeySize("Back", 2);
		kl.setKeySize("Shift", 2);

		layout[0] = kl;
		kl = new KeyboardLayout();
		kl.addRow(labels_b1, codes_b1);
		kl.addRow(labels_b2, codes_b2);
		kl.addRow(labels_b3, codes_b3);
		kl.setKeyCode("Move", KeyboardView.TOGGLE_POSITION);
		kl.setKeyCode("[abc]", KeyboardView.SWITCH_LAYOUT + 0);
		
		layout[1] = kl;
		return layout;
	}

}
