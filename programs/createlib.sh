#!/bin/sh

archive=libprocessinator.a

if [ -f $archive ]; then
  rm $archive 
fi

for asm in libprocessinator/*.s
do
  obj="${asm%.s}.o"
  riscv64-linux-gnu-gcc -march=rv32izfinx -mabi=ilp32 -c $asm -o $obj
  riscv64-linux-gnu-ar rcs $archive  $obj
done
