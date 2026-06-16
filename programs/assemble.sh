#!/bin/sh

asm="$1"
obj="${1%.s}.o"
bin="${1%.s}.bin"
riscv64-linux-gnu-gcc -march=rv32izfinx -mabi=ilp32 -c $asm -o $obj
riscv64-linux-gnu-objcopy -O binary $obj $bin
rm $obj
