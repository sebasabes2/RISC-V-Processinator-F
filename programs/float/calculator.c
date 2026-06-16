#include "../processinator.h"

int receiveHex() {
  transmit('0');
  transmit('x');
  int result = 0;
  int i = 0; 
  while (i < 8) {
    int c = receive();
    if (c >= '0' && c <= '9') {
      transmit(c);
      result = (result << 4) + (c - '0');
    } else if (c >= 'a' && c <= 'f') {
      transmit(c);
      result = (result << 4) + (c - 'a' + 10);
    } else {
      continue;
    }
    i++;
  }
  return result;
}

int receiveOperation() {
  while (1) {
    int c = receive();
    if (c == '+') {
      transmit('+');
      return 0;
    }
    if (c == '-') {
      transmit('-');
      return 1;
    }
    if (c == '*') {
      transmit('*');
      return 2;
    }
  }
}

void transmitHex(int x) {
  transmit('0');
  transmit('x');
  for (int i = 28; i >= 0; i -= 4) {
    int c = (x >> i) & 0xf;
    if (c <= 9) {
      transmit(c + '0');
    } else {
      transmit(c - 10 + 'a');
    }
  }
}

int fadd(int x, int y);
int fsub(int x, int y);
int fmul(int x, int y);

int main() {
  while (1) {
    int f1 = receiveHex();
    transmit(' ');
    int op = receiveOperation();
    transmit(' ');
    int f2 = receiveHex();
    transmit(' ');
    transmit('=');
    transmit(' ');
    int f3 = 0;
    switch (op) {
      case 0: f3 = fadd(f1, f2); break;
      case 1: f3 = fsub(f1, f2); break;
      case 2: f3 = fmul(f1, f2); break;
    }
    transmitHex(f3);
    transmit('\n');
  }
}
