.text
.global _start
_start:
li s0, 320 # max x-value
li s1, 240 # max y-value

li s4, -240 # initial x offset
li s5, -120 # initial y offset

li s6, 32 # move amount
li s7, 6 # zoom amount
li s8, 64 # max iterations

clear_screen:
# # clear screen
# li s3, 0 # y value
# clear_yloop:
# li s2, 0 # x value
# clear_xloop:
# mv a0, s2 # move x to x input
# mv a1, s3 # move y to y input
# li a2, 0x3f
# jal ra, display

# # end of loop
# addi s2, s2, 1
# blt s2, s0, clear_xloop
# addi s3, s3, 1
# blt s3, s1, clear_yloop

# Low resolution preview:
li s3, 0 # y value
low_yloop:
li s2, 0 # x value
low_xloop:

# loop pixels
mv a0, s2 # move x to x input
mv a1, s3 # move y to y input

add a0, a0, s4
add a1, a1, s5

sll a0, a0, s7
sll a1, a1, s7

jal ra, iterate
mv a2, a0

mv a0, s2 # move x to x input
mv a1, s3 # move y to y input
jal ra, display_16

# end of loop
addi s2, s2, 4
blt s2, s0, low_xloop
addi s3, s3, 4
blt s3, s1, low_yloop

# Start of mandelbrot generation:
li s3, 0 # y value
yloop:
li s2, 0 # x value
xloop:

# loop pixels
mv a0, s2 # move x to x input
mv a1, s3 # move y to y input

add a0, a0, s4
add a1, a1, s5

sll a0, a0, s7
sll a1, a1, s7

jal ra, iterate
mv a2, a0

mv a0, s2 # move x to x input
mv a1, s3 # move y to y input
jal ra, display

jal ra, check_for_input # restart if input is given

# end of loop
addi s2, s2, 1
blt s2, s0, xloop
addi s3, s3, 1
blt s3, s1, yloop

wait_for_input:
jal ra, check_for_input
beq x0, x0, wait_for_input

check_for_input:
li t0, 0xa000  # Btn address
lw t1, 0(t0)   # Read board buttons
srli t1, t1, 4
lw t2, 4(t0)   # Read Pmod buttons
# li t0, 0
andi t2, t2, 0xf0
# slli t2, t2, 4
add t2, t2, t1
bne x0, t2, handle_input
jalr x0, 0(ra)

handle_input:

# Check U
andi t3, t2, 1
beq x0, t3, skip_button_U
sub s5, s5, s6
skip_button_U:

# Check R
andi t3, t2, 2
beq x0, t3, skip_button_R
add s4, s4, s6
skip_button_R:

# Check D
andi t3, t2, 4
beq x0, t3, skip_button_D
add s5, s5, s6
skip_button_D:

# Check L
andi t3, t2, 8
beq x0, t3, skip_button_L
sub s4, s4, s6
skip_button_L:

# Check 0
andi t3, t2, 16
beq x0, t3, skip_button_0
slli s8, s8, 1
skip_button_0:

# Check 1
andi t3, t2, 32
beq x0, t3, skip_button_1
srli s8, s8, 1
skip_button_1:

# Check 2
andi t3, t2, 64
beq x0, t3, skip_button_2
srai s4, s4, 1
srai s5, s5, 1
addi s7, s7, 1
skip_button_2:

# Check 3
andi t3, t2, 128
beq x0, t3, skip_button_3
slli s4, s4, 1
slli s5, s5, 1
addi s7, s7, -1
skip_button_3:

# rerender image:
nop         # This nop is essential
beq x0, x0, clear_screen

display_16:
mv a4, ra
addi t2, a0, 4
addi t3, a1, 4
display_16_yloop:
display_16_xloop:
jal ra, display
addi a0, a0, 1
blt a0, t2, display_16_xloop
addi a1, a1, 1
addi a0, a0, -4
blt a1, t3, display_16_yloop
mv ra, a4
jalr x0, 0(ra)

display:
li t0, 0x100000  # start of video memory
mv t1, a1        # address = a1 << 10 + a0 + t0 
slli t1, t1, 9
add t1, t1, a0
add t1, t1, t0
sw a2, 0(t1)
jalr x0, 0(ra)

iterate:
# a0 = fixed real (input)
# a1 = fixed imag (input)
# a2 = float 2.0
# a4 = float real0
# a5 = float imag0
# a6 = ra
# a7 = temp
# t0 = float real
# t1 = float imag
# t2 = float realsq
# t3 = float imagsq
# t4 = int i
# t5 = int 50
# t6 = float 4.0

# save return address
mv a6, ra

# fixed to float and save input
mv t0, a0
mv t1, a1
call fixedToFloat
mv t0, a0
mv a0, t1
call fixedToFloat
mv t1, a0
mv a4, t0
mv a5, t1

# initialize real, imag
mv t0, a4
mv t1, a5

# initialize i, 50, 4.0, 2.0
li t4, 0        # i = 0
# li t5, 64       # t5 = 50
mv t5, s8
li t6, 0x40800000 # t6 = (float) 4.0
li a2, 0x40000000 # a2 = (float) 2.0

# for (i = 0; i < 50; i ++)
iterate_loop:

# realsq = real * real
fmul.s t2, t0, t0, rne

# imagsq = imag * imag
fmul.s t3, t1, t1, rne

## if (realsq + imagsq) > (fixed) 4 then break 
fadd.s a7, t2, t3, rne
bge a7, t6, iterate_return

# imag = real * imag * 2 + imag0
fmul.s t1, t0, t1, rne
fmul.s t1, t1, a2, rne
fadd.s t1, t1, a5, rne

# real = realsq - imagsq + real0
fsub.s t0, t2, t3, rne
fadd.s t0, t0, a4

# i ++
addi t4, t4, 1
# for i < 50
blt t4, t5, iterate_loop

iterate_return:
mv a0, t4
bne t4, t5, iterate_return2
li a0, 0

iterate_return2:
# restore return address
mv ra, a6
jalr x0, 0(ra)

# fixed to float subroutine:
fixed_to_float:
li t0, 0 # sign = 0
bgez a0, fixed_to_float_skip_negative
sub a0, x0, a0
li t0, 1 # sign = 1
# todo set sign of floating point t0
fixed_to_float_skip_negative:
li t1, 145 # exponent = 18 (+127)
mv t2, a0 # significand
bnez t2, fixed_to_float_skip_zero
li t1, 0 # exponent = 0
j fixed_to_float_exit_loop
fixed_to_float_skip_zero:
li t3, 0x80000000
fixed_to_float_loop:
and t4, t2, t3
bnez t4, fixed_to_float_exit_loop
addi t1, t1, -1
slli t2, t2, 1
j fixed_to_float_loop
fixed_to_float_exit_loop:
slli t2, t2, 1
srli t2, t2, 9
# construct fp
slli t0, t0, 31
andi t1, t1, 0xff
slli t1, t1, 23
mv a0, t0
add a0, a0, t1
add a0, a0, t2
ret

# test
fixedToFloat:
	blt	a0,zero,.L8
	mv	a5,a0
	li	a4,1
	bne	a0,zero,.L9
.L4:
	slli	a5,a5,1
	slli	a4,a4,23
	srli	a5,a5,9
	li	a3,2139095040
	and	a4,a4,a3
	add	a5,a5,a4
	slli	a0,a0,31
	add	a0,a5,a0
	ret
.L8:
	neg	a5,a0
	li	a0,1
.L3:
	li	a4,145
.L5:
	addi	a4,a4,-1
	slli	a5,a5,1
	bge	a5,zero,.L5
	j	.L4
.L9:
	li	a0,0
	j	.L3
# test end
