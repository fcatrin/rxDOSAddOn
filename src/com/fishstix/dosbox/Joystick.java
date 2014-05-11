package com.fishstix.dosbox;

public class Joystick {
	enum Position {MIN, CENTER, MAX};
	enum Type {DIGITAL, ANALOG}
	int x;
	int y;
	int radius;
	int radiusBall;
	int color;
	int colorBall;
	VirtualKey keyLeft;
	VirtualKey keyRight;
	VirtualKey keyUp;
	VirtualKey keyDown;
	boolean hasValidKeys;
	int maxValue;
	
	// percent required to be considered out of center
	float threshold;
	
	// analog
	int positionX;
	int positionY;
	
	// digital
	Position axisX, axisY;
	
	// default to digital
	Type type = Type.DIGITAL; 
}
