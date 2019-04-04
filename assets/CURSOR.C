#include <conio.h>
#include <string.h>

void cursor_enable(int start, int end) {
    outportb(0x3d4, 0x0a);
    outportb(0x3d5, (inportb(0x3d5) & 0xc0) | start);

    outportb(0x3d4, 0x0b);
    outportb(0x3d5, (inportb(0x3d5) & 0xe0) | end);
}

void cursor_on() {
    cursor_enable(13, 14);
}

void cursor_off() {
    outportb(0x3d4, 0x0a);
    outportb(0x3d5, 0x20);
}

int main(int argc, char *argv[]) {
    int on = argc >= 2 && !stricmp(argv[1], "on");

    if (on) {
        cursor_on();
    } else {
        cursor_off();
    }
    return 0;
}