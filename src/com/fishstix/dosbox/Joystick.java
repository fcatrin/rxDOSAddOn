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
	int keyCodeLeft;
	int keyCodeRight;
	int keyCodeUp;
	int keyCodeDown;
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
